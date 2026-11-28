package io.github.huydangtruong.immortalcultivation.godslayer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Lv 2: stands 0.75 s, then six fireballs 0.15 s apart (D287). Damage from 15 at Lv 2, no block damage. */
public final class FireballSkill implements GodSlayerSkill {
	private long t0 = -1;
	private boolean done = true;

	@Override
	public int unlockLevel() {
		return 2;
	}

	@Override
	public boolean canStart(GodSlayer boss, LivingEntity target) {
		double d = boss.distanceTo(target);
		return d >= 6 && d <= 32 && boss.hasLineOfSight(target);
	}

	@Override
	public void tick(GodSlayer boss, LivingEntity target) {
		if (!(boss.level() instanceof ServerLevel sl)) {
			done = true;
			return;
		}
		if (t0 < 0) {
			t0 = sl.getGameTime();
			sl.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 0.6f);
		}
		done = false;
		boss.faceTarget(target);
		long e = sl.getGameTime() - t0;
		Vec3 hand = new Vec3(boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ());
		if (e < 15) {
			sl.sendParticles(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 4, 0.8, 0.5, 0.8, 0.02);
		} else if (e >= 15 && (e - 15) % 3 == 0 && (e - 15) / 3 < GodSlayerRules.FIREBALL_COUNT) {
			Vec3 dir = target.getEyePosition().subtract(hand).normalize();
			Ball b = new Ball(sl, boss, dir.scale(1.2), GodSlayerRules.skillDamage(GodSlayerRules.FIREBALL_START, unlockLevel(), boss.bossLevel()));
			Vec3 p = hand.add(dir.scale(2.0));
			b.setPos(p.x, p.y, p.z);
			sl.addFreshEntity(b);
			sl.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2f, 1.0f);
		}
		if (e >= 15 + 3 * GodSlayerRules.FIREBALL_COUNT) {
			t0 = -1;
			done = true;
		}
	}

	@Override
	public boolean isDone() {
		return done;
	}

	/** A small fireball that hurts for a fixed amount as the boss and never lights or breaks blocks. */
	public static final class Ball extends SmallFireball {
		private final float damage;
		private final GodSlayer owner;

		Ball(Level level, GodSlayer boss, Vec3 move, float damage) {
			super(level, boss, move);
			this.damage = damage;
			this.owner = boss;
		}

		@Override
		protected void onHitEntity(EntityHitResult hit) {
			if (level() instanceof ServerLevel sl && hit.getEntity() != owner) {
				hit.getEntity().hurtServer(sl, sl.damageSources().mobAttack(owner), damage);
				hit.getEntity().igniteForSeconds(3);
			}
			discard();
		}

		@Override
		protected void onHitBlock(BlockHitResult hit) {
			discard();
		}
	}
}
