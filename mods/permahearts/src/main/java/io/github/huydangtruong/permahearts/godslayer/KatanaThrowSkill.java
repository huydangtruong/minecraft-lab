package io.github.huydangtruong.permahearts.godslayer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lv 3: throws a katana at the target (20 x scale damage on hit) and it flies back. The blade is drawn with
 * sweep and crit particles (no entity), so nothing can be picked up and no block is touched.
 */
public final class KatanaThrowSkill implements GodSlayerSkill {
	private static final double SPEED = 1.6;
	private long t0 = -1;
	private boolean done = true;
	private boolean returning;
	private boolean hit;
	private Vec3 pos;
	private Vec3 dir;
	private double travelled;
	private double maxTravel;

	@Override
	public int unlockLevel() {
		return 3;
	}

	@Override
	public boolean canStart(GodSlayer boss, LivingEntity target) {
		double d = boss.distanceTo(target);
		return d >= 6 && d <= 24 && boss.hasLineOfSight(target);
	}

	private Vec3 hand(GodSlayer boss) {
		return new Vec3(boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ());
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
			returning = false;
			hit = false;
			pos = null;
			sl.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2f, 0.5f);
		}
		long e = sl.getGameTime() - t0;
		Vec3 hand = hand(boss);
		if (e < 10) {
			sl.sendParticles(ParticleTypes.CRIT, hand.x, hand.y + 0.5, hand.z, 6, 0.6, 0.5, 0.6, 0.1);
			return;
		}
		if (pos == null) {
			pos = hand;
			Vec3 to = target.getEyePosition().subtract(hand);
			maxTravel = to.length() + 4;
			dir = to.normalize();
			travelled = 0;
			sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.6f);
		}
		if (!returning) {
			Vec3 next = pos.add(dir.scale(SPEED));
			travelled += SPEED;
			boolean blocked = !sl.noCollision(new AABB(next, next).inflate(0.2));
			pos = next;
			spin(sl);
			if (!hit) {
				for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(1.2))) {
					if (le != boss && le.isAlive()) {
						le.hurtServer(sl, sl.damageSources().mobAttack(boss),
							20f * (float) GodSlayerRules.scale(boss.bossLevel()));
						hit = true;
						sl.playSound(null, le.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2f, 0.8f);
						break;
					}
				}
			}
			if (blocked || hit || travelled >= maxTravel) {
				returning = true;
			}
		} else {
			Vec3 to = hand.subtract(pos);
			if (to.length() <= SPEED + 1.5) {
				t0 = -1;
				pos = null;
				done = true;
				return;
			}
			pos = pos.add(to.normalize().scale(SPEED));
			spin(sl);
		}
	}

	private void spin(ServerLevel sl) {
		sl.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 3, 0.3, 0.3, 0.3, 0.05);
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
