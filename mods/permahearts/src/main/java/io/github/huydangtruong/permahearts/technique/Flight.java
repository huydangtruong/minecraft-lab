package io.github.huydangtruong.permahearts.technique;

import com.mojang.serialization.Codec;
import java.util.Set;
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
import net.minecraft.world.entity.player.Abilities;

/**
 * Flight technique (P-014, D52): creative-like flight for a while. The end tick (game time) is saved with the player
 * and synced to the owner (the HUD timer). Creative and spectator players are never changed.
 */
public final class Flight {
	/** Game time at which flight ends. Saved; not copied on death (flight ends with the player). */
	public static final AttachmentType<Long> END = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "flight_end"),
		b -> b.persistent(Codec.LONG).syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));

	private Flight() {}

	public static void init() {
		END.identifier();
	}

	private static boolean untouchable(ServerPlayer p) {
		return p.isCreative() || p.isSpectator();
	}

	/** Starts flight at a technique level. Returns false for creative and spectator players. */
	public static boolean start(ServerPlayer p, int level) {
		if (untouchable(p)) {
			return false;
		}
		ServerLevel sl = p.level();
		p.setAttached(END, sl.getGameTime() + Math.round(TechniqueRules.flightSeconds(level) * 20));
		Abilities a = p.getAbilities();
		a.mayfly = true;
		a.flying = true;
		a.setFlyingSpeed(TechniqueRules.flyingSpeed(level));
		p.onUpdateAbilities();
		// the client drops "flying" while the player stands on the ground, so lift off at once
		if (p.onGround() && sl.noCollision(p, p.getBoundingBox().move(0, 0.6, 0))) {
			p.teleportTo(sl, p.getX(), p.getY() + 0.6, p.getZ(), Set.of(), p.getYRot(), p.getXRot(), false);
		}
		sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 20, 0.4, 0.1, 0.4, 0.03);
		sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, 1.4f);
		return true;
	}

	public static boolean active(ServerPlayer p) {
		Long end = p.getAttached(END);
		return end != null && p.level().getGameTime() < end;
	}

	/**
	 * Ends flight now: forgets the end tick, takes away mayfly / flying, restores the normal flying speed and gives
	 * 5 s of slow falling. Creative and spectator players only lose the state (their abilities are theirs).
	 */
	public static void end(ServerPlayer p) {
		if (p.getAttached(END) == null) {
			return;
		}
		p.removeAttached(END);
		if (untouchable(p)) {
			return;
		}
		Abilities a = p.getAbilities();
		a.mayfly = false;
		a.flying = false;
		a.setFlyingSpeed(TechniqueRules.CREATIVE_FLYING_SPEED);
		p.onUpdateAbilities();
		p.resetFallDistance();
		p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, TechniqueRules.FLIGHT_SLOW_FALL_TICKS, 0, false, true, true));
	}

	/** Remaining flight ticks given the saved end tick (null = no flight) and the game time; never negative. */
	public static long remaining(Long end, long now) {
		return end == null ? 0 : Math.max(0, end - now);
	}

	/** Every server tick for every player. */
	static void tick(ServerPlayer p, long serverTick) {
		Long end = p.getAttached(END);
		if (end == null) {
			return;
		}
		if (untouchable(p)) {
			p.removeAttached(END);
		} else if (p.level().getGameTime() >= end) {
			end(p);
		} else {
			if (!p.getAbilities().mayfly) { // abilities were reset by something else: keep flying until the end
				p.getAbilities().mayfly = true;
				p.onUpdateAbilities();
			}
			if (serverTick % 4 == 0 && p.getAbilities().flying) {
				p.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 2, 0.25, 0.05, 0.25, 0.01);
			}
		}
	}
}
