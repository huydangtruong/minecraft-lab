package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * D158, D159, D180 Life Stealer: four bats fly in around the entity under the crosshair (16 blocks), lift it 2 blocks, drain it in
 * 10 pulses over 2 s while healing the caster, then fly out and leave it bleeding and slowed. The bats are plain server Bat
 * entities (no AI, no physics, silent, invulnerable) tagged so that none survives a restart or a crash.
 */
public final class LifeStealer {
	private static final DustParticleOptions BLOOD = new DustParticleOptions(0xb01010, 1.0f);
	/** Tag of the visual bats: discarded when the server stops and when one is ever loaded from disk. */
	private static final String BAT_TAG = "permahearts_stealer_bat";

	private static final class Session {
		final ServerLevel level;
		final LivingEntity target;
		final UUID caster;
		final int stealLevel;
		final List<Bat> bats = new ArrayList<>();
		final boolean boss;
		int tick; // ticks since the cast
		int outTick = -1; // >= 0 once the bats fly out
		boolean held;
		boolean wasNoGravity;
		double holdX, holdZ, baseY, lift;

		Session(ServerLevel level, LivingEntity target, UUID caster, int stealLevel) {
			this.level = level;
			this.target = target;
			this.caster = caster;
			this.stealLevel = stealLevel;
			this.boss = Grasp.isBoss(target);
		}
	}

	private static final List<Session> SESSIONS = new ArrayList<>();

	private LifeStealer() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LifeStealer::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> clear());
		ServerEntityEvents.ENTITY_LOAD.register((e, level) -> {
			if (e instanceof Bat && e.entityTags().contains(BAT_TAG)) { // a leftover from a crash
				e.discard();
			}
		});
	}

	private static void clear() {
		for (Session s : SESSIONS) {
			release(s);
			discardBats(s);
		}
		SESSIONS.clear();
	}

	/** The living entity the crosshair points at within the range with a clear line of sight, or null. */
	public static LivingEntity aim(ServerPlayer p) {
		ServerLevel sl = p.level();
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getViewVector(1f).scale(StealerRules.RANGE));
		BlockHitResult block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, p, eye, limit, new AABB(eye, limit).inflate(1.0),
			e -> e instanceof LivingEntity le && TechniqueDamage.affectable(le, p) && !Lance.isFriend(le, p), 0.3f);
		return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
	}

	/** Casts: false (no cooldown) when nothing is aimed at (or the target is already being drained). */
	public static boolean cast(ServerPlayer p, int level) {
		LivingEntity target = aim(p);
		if (target == null) {
			return false;
		}
		return start(p, target, level);
	}

	/** Starts the bat drain on target, no aim checks. False when the target is already being drained. */
	public static boolean start(ServerPlayer p, LivingEntity target, int level) {
		if (draining(target)) {
			return false;
		}
		Session s = new Session(p.level(), target, p.getUUID(), level);
		for (int i = 0; i < 4; i++) {
			Bat bat = EntityTypes.BAT.create(s.level, EntitySpawnReason.EVENT);
			if (bat == null) {
				continue;
			}
			bat.setNoAi(true);
			bat.setNoGravity(true);
			bat.noPhysics = true;
			bat.setSilent(true);
			bat.setPersistenceRequired();
			bat.setPermanentlyInvulnerable(true);
			bat.setResting(false);
			placeBat(s, bat, i);
			s.level.addFreshEntity(bat);
			bat.addTag(BAT_TAG); // after the add: ENTITY_LOAD fires inside addFreshEntity
			s.bats.add(bat);
		}
		SESSIONS.add(s);
		return true;
	}

	/** True while target is being drained (bats flying in, lifting, draining or flying out). */
	public static boolean draining(LivingEntity target) {
		for (Session s : SESSIONS) {
			if (s.target == target) {
				return true;
			}
		}
		return false;
	}

	/** Bats around the upper body of the target on the four sides (N, E, S, W). */
	private static void placeBat(Session s, Bat bat, int i) {
		double dist = s.outTick >= 0 ? StealerRules.batDistance(s.outTick, true) : StealerRules.batDistance(s.tick, false);
		double ang = i * Math.PI / 2;
		double dx = Math.sin(ang), dz = -Math.cos(ang); // i = 0 north (-Z), 1 east, 2 south, 3 west
		double bob = Math.sin((s.tick + i * 2) * 0.9) * 0.12;
		double x = s.target.getX() + dx * dist;
		double z = s.target.getZ() + dz * dist;
		double y = s.target.getY() + s.target.getBbHeight() * 0.65 - 0.45 + bob;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)); // looks at the target
		bat.snapTo(x, y, z, yaw, 0f);
		bat.setDeltaMovement(Vec3.ZERO);
	}

	private static void discardBats(Session s) {
		for (Bat b : s.bats) {
			b.discard();
		}
		s.bats.clear();
	}

	/** How high the target can be lifted (0..LIFT_HEIGHT): stops below the first block that would touch it. */
	private static double liftRoom(Session s) {
		double room = 0;
		AABB box = s.target.getBoundingBox();
		for (double h = 0.1; h <= StealerRules.LIFT_HEIGHT + 1e-9; h += 0.1) {
			if (!s.level.noCollision(s.target, box.move(0, h, 0))) {
				break;
			}
			room = h;
		}
		return room;
	}

	private static void hold(Session s, double y) {
		LivingEntity t = s.target;
		t.setDeltaMovement(Vec3.ZERO);
		t.resetFallDistance();
		if (t instanceof ServerPlayer tp) {
			if (tp.distanceToSqr(s.holdX, y, s.holdZ) > 1e-4) { // keeps the look direction: only the position is forced
				tp.teleportTo(s.level, s.holdX, y, s.holdZ, Set.of(Relative.X_ROT, Relative.Y_ROT), 0f, 0f, false);
			}
			tp.setDeltaMovement(Vec3.ZERO);
		} else {
			t.snapTo(s.holdX, y, s.holdZ, t.getYRot(), t.getXRot());
		}
	}

	/** Gives the target its gravity back. */
	private static void release(Session s) {
		if (s.held) {
			s.held = false;
			s.target.setNoGravity(s.wasNoGravity);
			s.target.setDeltaMovement(Vec3.ZERO);
			s.target.resetFallDistance();
		}
	}

	private static void pulse(Session s, ServerPlayer caster) {
		LivingEntity t = s.target;
		float amount = (float) StealerRules.pulse(s.stealLevel, t.getMaxHealth());
		float before = t.getHealth() + t.getAbsorptionAmount();
		t.damageCooldownTime = 0; // the 10-tick damage cooldown of vanilla would block pulses 4 ticks apart
		TechniqueDamage.hurt(s.level, t, caster, null, DamageTypes.MAGIC, amount);
		float dealt = Math.max(0f, before - (t.getHealth() + t.getAbsorptionAmount()));
		if (dealt > 0) {
			caster.heal(dealt);
		}
		Vec3 body = t.position().add(0, t.getBbHeight() * 0.6, 0);
		s.level.sendParticles(BLOOD, body.x, body.y, body.z, 8, t.getBbWidth() * 0.35, t.getBbHeight() * 0.25, t.getBbWidth() * 0.35, 0.05);
	}

	private static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) {
			return;
		}
		for (Iterator<Session> it = SESSIONS.iterator(); it.hasNext();) {
			Session s = it.next();
			ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
			boolean casterGone = caster == null || !caster.isAlive() || caster.level() != s.level;
			boolean targetGone = s.target.isRemoved() || s.target.level() != s.level;
			if (casterGone || targetGone) { // stop at once: the bats vanish and the target is let go
				release(s);
				discardBats(s);
				it.remove();
				continue;
			}
			if (s.outTick < 0 && !s.target.isAlive()) { // killed mid-drain: the bats leave at once
				release(s);
				s.outTick = 0;
			}
			if (s.outTick < 0) {
				drainStep(s, caster);
			}
			if (s.outTick >= StealerRules.FLY_OUT_TICKS) {
				discardBats(s);
				it.remove();
				continue;
			}
			for (int i = 0; i < s.bats.size(); i++) {
				placeBat(s, s.bats.get(i), i);
			}
			if (s.outTick >= 0) {
				s.outTick++;
			}
			s.tick++;
		}
	}

	/** One tick of the fly-in, lift and drain phases. */
	private static void drainStep(Session s, ServerPlayer caster) {
		int arrive = StealerRules.FLY_IN_TICKS;
		if (s.tick < arrive) {
			return;
		}
		LivingEntity t = s.target;
		int since = s.tick - arrive;
		if (since == 0) { // the bats bite: the target is caught where it stands
			s.holdX = t.getX();
			s.holdZ = t.getZ();
			s.baseY = t.getY();
			s.lift = s.boss ? 0 : liftRoom(s);
			if (!s.boss) {
				s.held = true;
				s.wasNoGravity = t.isNoGravity();
				t.setNoGravity(true);
			}
			s.level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.8f);
		}
		if (since >= StealerRules.drainTicks()) { // the end: bats leave, the target falls, then D159 effects
			release(s);
			s.outTick = 0;
			if (t.isAlive()) {
				t.addEffect(new MobEffectInstance(Bleeding.EFFECT, StealerRules.BLEED_SECONDS * 20, 0, false, true, true));
				t.addEffect(new MobEffectInstance(StealerSlow.EFFECT, StealerRules.SLOW_SECONDS * 20, 0, false, true, true));
			}
			return;
		}
		if (s.held) {
			hold(s, s.baseY + s.lift * StealerRules.liftProgress(since));
		}
		if (since % StealerRules.PULSE_TICKS == 0) {
			pulse(s, caster);
		}
		Vec3 from = t.position().add(0, t.getBbHeight() * 0.6, 0); // the blood flies to the caster for the whole drain
		Vec3 to = caster.getEyePosition().subtract(0, 0.4, 0);
		double t0 = since / (double) StealerRules.drainTicks();
		double t1 = (since + 1) / (double) StealerRules.drainTicks();
		for (int i = 0; i < 5; i++) {
			Vec3 at = from.lerp(to, Mth.lerp(i / 5.0, t0, t1));
			s.level.sendParticles(BLOOD, at.x, at.y, at.z, 2, 0.08, 0.08, 0.08, 0.0);
		}
	}
}
