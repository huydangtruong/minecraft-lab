package io.github.huydangtruong.permahearts.technique;

import com.mojang.serialization.Codec;
import io.github.huydangtruong.permahearts.PermaHearts;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Aqua Marine (P-022, D64): a timed buff like Flight. While it lasts the caster
 * <ul>
 * <li>never loses air: the air supply is refilled every tick (bubbles stay full);</li>
 * <li>swims like with Dolphin's Grace: the real effect (hidden) is refreshed every few ticks, plus a transient
 *     WATER_MOVEMENT_EFFICIENCY modifier that raises the water acceleration so the bonus grows to x2 at level 20;</li>
 * <li>mines faster underwater: a transient SUBMERGED_MINING_SPEED modifier up to the level's value.</li>
 * </ul>
 * Movement is simulated on the client, so only synced things work: the effect and the attributes are synced, a
 * server-side velocity push would not be.
 */
public final class AquaMarine {
	/** Game time at which the buff ends. Saved and synced to the owner (HUD timer). */
	public static final AttachmentType<Long> END = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "aqua_end"),
		b -> b.persistent(Codec.LONG).syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));
	/** Technique level the buff was cast at (server only, saved). */
	public static final AttachmentType<Integer> LEVEL = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "aqua_level"), b -> b.persistent(Codec.INT));

	public static final Identifier MINING_ID = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "aqua_mining");
	public static final Identifier SWIM_ID = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "aqua_swim");
	/** The hidden Dolphin's Grace is refreshed to this many ticks; it runs out by itself shortly after the buff. */
	private static final int EFFECT_TICKS = 40;

	private AquaMarine() {}

	public static void init() {
		END.identifier();
		LEVEL.identifier();
	}

	/** Starts (or restarts) the buff at a technique level. */
	public static boolean start(ServerPlayer p, int level) {
		ServerLevel sl = p.level();
		p.setAttached(END, sl.getGameTime() + Math.round(TechniqueRules.aquaSeconds(level) * 20));
		p.setAttached(LEVEL, level);
		AttributeInstance old = p.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
		if (old != null) {
			old.removeModifier(MINING_ID); // a recast at another level starts from the player's own value
		}
		apply(p, level);
		sl.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1.0, p.getZ(), 24, 0.5, 0.7, 0.5, 0.02);
		sl.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1f, 1.2f);
		return true;
	}

	public static boolean active(ServerPlayer p) {
		Long end = p.getAttached(END);
		return end != null && p.level().getGameTime() < end;
	}

	/** Remaining ticks given the saved end tick (null = not active) and the game time; never negative. */
	public static long remaining(Long end, long now) {
		return end == null ? 0 : Math.max(0, end - now);
	}

	/** Ends the buff now: forgets the state and takes the modifiers and the hidden effect away. */
	public static void end(ServerPlayer p) {
		p.removeAttached(END);
		p.removeAttached(LEVEL);
		AttributeInstance mining = p.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
		if (mining != null) {
			mining.removeModifier(MINING_ID);
		}
		AttributeInstance swim = p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		if (swim != null) {
			swim.removeModifier(SWIM_ID);
		}
		MobEffectInstance dg = p.getEffect(MobEffects.DOLPHINS_GRACE);
		if (dg != null && !dg.isVisible() && dg.getDuration() <= EFFECT_TICKS) {
			p.removeEffect(MobEffects.DOLPHINS_GRACE); // ours (hidden, short); a real potion is left alone
		}
	}

	/** Refills the air, keeps the hidden Dolphin's Grace and sets both attribute modifiers for the level. Idempotent. */
	static void apply(ServerPlayer p, int level) {
		p.setAirSupply(p.getMaxAirSupply());
		MobEffectInstance dg = p.getEffect(MobEffects.DOLPHINS_GRACE);
		if (dg == null || dg.getDuration() < EFFECT_TICKS / 2) {
			p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_TICKS, 0, false, false, false));
		}
		AttributeInstance swim = p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		if (swim != null) {
			double want = TechniqueRules.aquaWaterEfficiency(level, p.isSprinting(), p.onGround());
			AttributeModifier have = swim.getModifier(SWIM_ID);
			if (want < 1e-6) {
				if (have != null) {
					swim.removeModifier(SWIM_ID);
				}
			} else if (have == null || Math.abs(have.amount() - want) > 1e-6) {
				swim.addOrUpdateTransientModifier(new AttributeModifier(SWIM_ID, want, AttributeModifier.Operation.ADD_VALUE));
			}
		}
		AttributeInstance mining = p.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
		if (mining != null && !mining.hasModifier(MINING_ID)) {
			// top up to the level's value: counts what the player already has (Aqua Affinity), never goes over it
			double add = TechniqueRules.aquaMiningSpeed(level) - mining.getValue();
			if (add > 0) {
				mining.addOrUpdateTransientModifier(new AttributeModifier(MINING_ID, add, AttributeModifier.Operation.ADD_VALUE));
			}
		}
	}

	/** Every server tick for every player. */
	static void tick(ServerPlayer p, long serverTick) {
		Long end = p.getAttached(END);
		if (end == null) {
			return;
		}
		if (p.level().getGameTime() >= end) {
			end(p);
			return;
		}
		Integer level = p.getAttached(LEVEL);
		apply(p, level == null ? 1 : level);
		if (serverTick % 5 == 0) {
			ServerLevel sl = p.level();
			sl.sendParticles(p.isUnderWater() ? ParticleTypes.BUBBLE : ParticleTypes.BUBBLE_POP,
				p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.6, 0.4, 0.01);
		}
	}
}
