package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** D158, D159 Life Stealer: drains the living entity under the crosshair (16 blocks), heals the caster, leaves it bleeding and slowed. */
public final class LifeStealer {
	private static final DustParticleOptions BLOOD = new DustParticleOptions(0xb01010, 1.0f);
	private static final int STREAM_TICKS = 10; // blood flies for 0.5 s

	private static final class Stream {
		final ServerLevel level;
		final LivingEntity target;
		final UUID caster;
		int tick;

		Stream(ServerLevel level, LivingEntity target, UUID caster) {
			this.level = level;
			this.target = target;
			this.caster = caster;
		}
	}

	private static final List<Stream> STREAMS = new ArrayList<>();

	private LifeStealer() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LifeStealer::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> STREAMS.clear());
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

	/** Casts: false (no cooldown) when nothing is aimed at. */
	public static boolean cast(ServerPlayer p, int level) {
		LivingEntity target = aim(p);
		if (target == null) {
			return false;
		}
		drain(p, target, level);
		return true;
	}

	/** The drain itself, no aim checks. Returns the health the target lost (which the caster gets back). */
	public static float drain(ServerPlayer p, LivingEntity target, int level) {
		ServerLevel sl = p.level();
		float amount = (float) StealerRules.amount(level, target.getMaxHealth());
		float before = target.getHealth() + target.getAbsorptionAmount();
		TechniqueDamage.hurt(sl, target, p, null, DamageTypes.MAGIC, amount);
		float dealt = Math.max(0f, before - (target.getHealth() + target.getAbsorptionAmount()));
		if (dealt > 0) {
			p.heal(dealt);
		}
		if (target.isAlive()) {
			target.addEffect(new MobEffectInstance(Bleeding.EFFECT, StealerRules.BLEED_SECONDS * 20, 0, false, true, true));
			target.addEffect(new MobEffectInstance(StealerSlow.EFFECT, StealerRules.SLOW_SECONDS * 20, 0, false, true, true));
		}
		// the blood splash on the target
		Vec3 body = target.position().add(0, target.getBbHeight() * 0.6, 0);
		sl.sendParticles(BLOOD, body.x, body.y, body.z, 24, target.getBbWidth() * 0.35, target.getBbHeight() * 0.25, target.getBbWidth() * 0.35, 0.05);
		sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.8f);
		STREAMS.add(new Stream(sl, target, p.getUUID()));
		return dealt;
	}

	private static void tick(MinecraftServer server) {
		if (STREAMS.isEmpty()) {
			return;
		}
		for (Iterator<Stream> it = STREAMS.iterator(); it.hasNext();) {
			Stream s = it.next();
			ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
			if (caster == null || caster.level() != s.level || s.tick >= STREAM_TICKS) {
				it.remove();
				continue;
			}
			Vec3 from = s.target.position().add(0, s.target.getBbHeight() * 0.6, 0);
			Vec3 to = caster.getEyePosition().subtract(0, 0.4, 0);
			double t0 = s.tick / (double) STREAM_TICKS;
			double t1 = (s.tick + 1) / (double) STREAM_TICKS;
			for (int i = 0; i < 5; i++) { // a short run of drops that travels from the target to the caster
				double t = Mth.lerp(i / 5.0, t0, t1);
				Vec3 at = from.lerp(to, t);
				s.level.sendParticles(BLOOD, at.x, at.y, at.z, 2, 0.08, 0.08, 0.08, 0.0);
			}
			s.tick++;
		}
	}
}
