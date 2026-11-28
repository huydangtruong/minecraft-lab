package io.github.huydangtruong.immortalcultivation.godslayer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Lv 5: in water or on land (D287). Charges 1 s, then a particle beam from the eyes for 2 s, damage every 0.5 s. */
public final class EyeBeamSkill implements GodSlayerSkill {
	private long t0 = -1;
	private boolean done = true;

	@Override
	public int unlockLevel() {
		return 5;
	}

	@Override
	public boolean canStart(GodSlayer boss, LivingEntity target) {
		return boss.distanceTo(target) <= 24 && boss.hasLineOfSight(target);
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
			sl.playSound(null, boss.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.5f, 0.6f);
		}
		long e = sl.getGameTime() - t0;
		Vec3 eye = boss.getEyePosition();
		if (e < 20) {
			sl.sendParticles(boss.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.ELECTRIC_SPARK, eye.x, eye.y, eye.z, 6, 0.4, 0.3, 0.4, 0.05);
			return;
		}
		Vec3 end = target.getEyePosition();
		Vec3 line = end.subtract(eye);
		double len = line.length();
		Vec3 step = line.normalize();
		for (double d = 0.5; d < len; d += 0.5) {
			Vec3 p = eye.add(step.scale(d));
			sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		if (e % 10 == 0) {
			target.hurtServer(sl, sl.damageSources().mobAttack(boss), GodSlayerRules.skillDamage(GodSlayerRules.EYE_START, unlockLevel(), boss.bossLevel()));
			sl.playSound(null, target.blockPosition(), SoundEvents.GUARDIAN_HURT, SoundSource.HOSTILE, 1.0f, 1.0f);
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
