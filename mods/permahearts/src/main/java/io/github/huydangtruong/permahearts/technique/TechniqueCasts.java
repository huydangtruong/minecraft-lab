package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the six techniques (P-011 part C). {@link #cast} is the single entry point: the op command
 * {@code /technique cast} and, later, the key and mouse input of part B call it. It does no cooldown or data checks.
 */
public final class TechniqueCasts {
	/** Game time (of the player's level) until which Blood Oath makes the player invulnerable. Server only, not saved. */
	public static final AttachmentType<Long> OATH_UNTIL = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "blood_oath_until"), b -> {});

	private TechniqueCasts() {}

	/** Registers the effect, attachments, events, ticking and the cast command. Called once from PermaHearts. */
	public static void init() {
		Frostbite.init();
		PhaseState.ATTACHMENT.identifier();
		OATH_UNTIL.identifier();
		ServerTickEvents.END_SERVER_TICK.register(TechniqueCasts::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> TechniqueFields.clear());
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !blocksDamage(entity, source));
		CommandRegistrationCallback.EVENT.register((d, c, s) -> TechniqueCastCommand.register(d));
	}

	/** Casts a technique for the caster at a level (1..20) with a charge (0..1, only the thrown ones use it). */
	public static boolean cast(ServerPlayer caster, Technique t, int level, float charge) {
		if (!PermaHearts.enabled(caster.level().getServer()) || !caster.isAlive()) {
			return false;
		}
		int lvl = Mth.clamp(level, 1, TechniqueRules.MAX_LEVEL);
		switch (t) {
			case FIRE, ICE -> throwOrb(caster, t, lvl, charge);
			case HEALING -> healingLight(caster, lvl);
			case PHASE -> phaseWalk(caster, lvl);
			case REPULSE -> repulse(caster, lvl);
			case SACRIFICE -> bloodOath(caster, lvl);
			case FLIGHT, BLADE -> {
				return false; // P-014: not cast this way yet
			}
		}
		return true;
	}

	// ---- Flame Burst, Frost Orb ----

	private static void throwOrb(ServerPlayer caster, Technique t, int level, float charge) {
		ServerLevel sl = caster.level();
		double fullRange = t == Technique.FIRE ? TechniqueRules.fireRange(level) : TechniqueRules.iceRange(level);
		double range = OrbBallistics.chargedRange(fullRange, charge);
		double speed = OrbBallistics.speedForRange(range, caster.getEyeHeight() - 0.1);
		TechniqueOrb orb = new TechniqueOrb(sl, caster, t, level);
		orb.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0f, (float) speed, 0f);
		sl.addFreshEntity(orb);
		sl.playSound(null, caster.blockPosition(),
			t == Technique.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1f, 1f);
	}

	// ---- Healing Light ----

	private static void healingLight(ServerPlayer caster, int level) {
		ServerLevel sl = caster.level();
		TechniqueFields.add(TechniqueFields.Kind.HEAL, sl, caster.position(), TechniqueRules.healingRadius(level),
			TechniqueRules.healingSeconds(level), level, caster.getUUID());
		sl.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.2f);
	}

	// ---- Phase Walk ----

	private static void phaseWalk(ServerPlayer caster, int level) {
		ServerLevel sl = caster.level();
		long end = sl.getGameTime() + Math.round(TechniqueRules.phaseSeconds(level) * 20);
		caster.setAttached(PhaseState.ATTACHMENT, new PhaseState(caster.blockPosition().getY(), end));
		sl.sendParticles(ParticleTypes.PORTAL, caster.getX(), caster.getY() + 1, caster.getZ(), 50, 0.4, 0.8, 0.4, 0.6);
		sl.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
	}

	/** Ends Phase Walk now: clears the state, then lifts the player to the first free space if still inside blocks. */
	public static void endPhase(ServerPlayer p) {
		p.setAttached(PhaseState.ATTACHMENT, PhaseState.NONE);
		ServerLevel sl = p.level();
		AABB box = p.getBoundingBox();
		if (sl.noCollision(p, box)) {
			return;
		}
		for (int dy = 1; dy <= sl.getMaxY() - p.getBlockY() + 1; dy++) {
			if (sl.noCollision(p, box.move(0, dy, 0))) {
				p.teleportTo(sl, p.getX(), p.getY() + dy, p.getZ(), Set.of(), p.getYRot(), p.getXRot(), false);
				p.setDeltaMovement(Vec3.ZERO);
				return;
			}
		}
	}

	// ---- Repulse ----

	private static void repulse(ServerPlayer caster, int level) {
		ServerLevel sl = caster.level();
		double radius = TechniqueRules.repulseRadius(level);
		double push = TechniqueRules.repulsePush(level);
		float damage = (float) TechniqueRules.repulseDamage(level);
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius),
			e -> TechniqueDamage.affectable(e, caster) && e.distanceTo(caster) <= radius)) {
			double dx = e.getX() - caster.getX();
			double dz = e.getZ() - caster.getZ();
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len < 1e-4) {
				double a = sl.getRandom().nextDouble() * Math.PI * 2;
				dx = Math.cos(a);
				dz = Math.sin(a);
				len = 1;
			}
			TechniqueDamage.hurt(sl, e, caster, null, DamageTypes.MAGIC, damage);
			double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
			e.setDeltaMovement(dx / len * push * resist, 0.3 * resist + 0.05, dz / len * push * resist);
			e.needsSync = true; // sends the new velocity to players
		}
		int points = 28;
		for (int ring = 1; ring <= 2; ring++) {
			double r = radius * ring / 2;
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				sl.sendParticles(ring == 2 ? ParticleTypes.POOF : new DustParticleOptions(0xffffff, 1.4f),
					caster.getX() + Math.cos(a) * r, caster.getY() + 0.3, caster.getZ() + Math.sin(a) * r,
					1, 0.05, 0.05, 0.05, 0.02);
			}
		}
		sl.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2f, 0.7f);
	}

	// ---- Blood Oath ----

	private static void bloodOath(ServerPlayer caster, int level) {
		ServerLevel sl = caster.level();
		float health = caster.getHealth();
		caster.setHealth(Math.max(1f, health - (float) (health * TechniqueRules.sacrificeCost(level))));
		long until = sl.getGameTime() + Math.round(TechniqueRules.sacrificeSeconds(level) * 20);
		caster.setAttached(OATH_UNTIL, until);
		sl.sendParticles(new DustParticleOptions(0xb01010, 1.5f), caster.getX(), caster.getY() + 1, caster.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
		sl.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 0.6f);
	}

	/** True while the entity is a player under Blood Oath. */
	public static boolean oathActive(LivingEntity e) {
		Long until = e.getAttached(OATH_UNTIL);
		return until != null && e.level().getGameTime() < until;
	}

	/** Blood Oath cancels every damage except void, /kill and the like (tag bypasses_invulnerability). */
	public static boolean blocksDamage(LivingEntity entity, DamageSource source) {
		return oathActive(entity) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	// ---- ticking ----

	private static void tick(MinecraftServer server) {
		TechniqueFields.tick(server);
		long t = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PhaseState phase = p.getAttached(PhaseState.ATTACHMENT);
			if (phase != null && phase.endTick() >= 0) {
				if (!phase.active(p.level().getGameTime())) {
					endPhase(p);
				} else if (t % 4 == 0) {
					p.level().sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 4, 0.3, 0.7, 0.3, 0.3);
				}
			}
			Long until = p.getAttached(OATH_UNTIL);
			if (until != null) {
				if (p.level().getGameTime() >= until) {
					p.removeAttached(OATH_UNTIL);
				} else if (t % 3 == 0) {
					ServerLevel sl = p.level();
					double a = sl.getRandom().nextDouble() * Math.PI * 2;
					sl.sendParticles(new DustParticleOptions(0xb01010, 1.0f), p.getX() + Math.cos(a) * 0.8, p.getY() + sl.getRandom().nextDouble() * 1.8,
						p.getZ() + Math.sin(a) * 0.8, 2, 0.1, 0.2, 0.1, 0.0);
				}
			}
		}
	}
}
