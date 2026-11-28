package io.github.huydangtruong.immortalcultivation.godslayer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Lv 4: a flame cone 10 blocks long, 30 degrees wide (D287), for 2.5 s. Damage every 0.5 s and sets on fire. No block fire. */
public final class FlameBreathSkill implements GodSlayerSkill {
	private static final double RANGE = GodSlayerRules.FLAME_RANGE;
	private static final double HALF = GodSlayerRules.FLAME_HALF_ANGLE;
	private long t0 = -1;
	private boolean done = true;

	@Override
	public int unlockLevel() {
		return 4;
	}

	@Override
	public boolean canStart(GodSlayer boss, LivingEntity target) {
		return boss.distanceTo(target) <= RANGE;
	}

	@Override
	public void tick(GodSlayer boss, LivingEntity target) {
		if (!(boss.level() instanceof ServerLevel sl)) {
			done = true;
			return;
		}
		done = false;
		boss.faceTarget(target);
		if (t0 < 0) {
			t0 = sl.getGameTime();
			sl.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 0.4f);
		}
		long e = sl.getGameTime() - t0;
		Vec3 origin = new Vec3(boss.getX(), boss.getY() + boss.getBbHeight() * 0.65, boss.getZ());
		Vec3 look = Vec3.directionFromRotation(0, boss.getYRot()).normalize();
		if (e < 10) {
			sl.sendParticles(ParticleTypes.FLAME, origin.x + look.x, origin.y, origin.z + look.z, 5, 0.5, 0.4, 0.5, 0.02);
			return;
		}
		// cone particles
		for (int i = 0; i < 18; i++) {
			double len = 1 + sl.getRandom().nextDouble() * (RANGE - 1);
			double ang = Math.toRadians((sl.getRandom().nextDouble() - 0.5) * 2 * HALF);
			double c = Math.cos(ang);
			double s = Math.sin(ang);
			Vec3 d = new Vec3(look.x * c - look.z * s, (sl.getRandom().nextDouble() - 0.5) * 0.4, look.x * s + look.z * c);
			Vec3 p = origin.add(d.scale(len));
			sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
		if (e % 10 == 0) {
			float dmg = GodSlayerRules.skillDamage(GodSlayerRules.FLAME_START, unlockLevel(), boss.bossLevel());
			for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(RANGE, 3, RANGE))) {
				if (le == boss || !le.isAlive()) {
					continue;
				}
				Vec3 to = le.position().add(0, le.getBbHeight() / 2, 0).subtract(origin);
				double dist = to.length();
				if (dist > RANGE + 0.5) {
					continue;
				}
				Vec3 flat = new Vec3(to.x, 0, to.z);
				if (flat.lengthSqr() > 1e-4 && flat.normalize().dot(look) < Math.cos(Math.toRadians(HALF))) {
					continue;
				}
				le.hurtServer(sl, sl.damageSources().mobAttack(boss), dmg);
				le.igniteForSeconds(3);
			}
		}
		if (e >= 60) {
			t0 = -1;
			done = true;
		}
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
