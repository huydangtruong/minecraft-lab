package io.github.huydangtruong.immortalcultivation.godslayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lv 3: throws three katanas in a fan at the target (D287) and they fly back. Each blade hurts the first creature it
 * meets. The blades are drawn with sweep particles and a trail of blood (no entity), so nothing can be picked up and no
 * block is touched.
 */
public final class KatanaThrowSkill implements GodSlayerSkill {
	private static final double SPEED = 1.6;
	private static final DustParticleOptions BLOOD = new DustParticleOptions(0x8A0303, 1.4f);
	private static final BlockParticleOption BLOOD_SPLASH = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());

	private long t0 = -1;
	private boolean done = true;
	private final List<Blade> blades = new ArrayList<>();

	private static final class Blade {
		Vec3 pos;
		Vec3 dir;
		double travelled;
		double maxTravel;
		boolean returning;
		boolean hit;
		boolean home;
	}

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
			blades.clear();
			sl.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2f, 0.5f);
		}
		long e = sl.getGameTime() - t0;
		Vec3 hand = hand(boss);
		if (e < 10) {
			sl.sendParticles(ParticleTypes.CRIT, hand.x, hand.y + 0.5, hand.z, 6, 0.6, 0.5, 0.6, 0.1);
			return;
		}
		if (blades.isEmpty()) {
			Vec3 to = target.getEyePosition().subtract(hand);
			Vec3 mid = to.normalize();
			int n = GodSlayerRules.KATANA_COUNT;
			for (int i = 0; i < n; i++) {
				double ang = Math.toRadians((i - (n - 1) / 2.0) * GodSlayerRules.KATANA_FAN_STEP);
				double c = Math.cos(ang);
				double s = Math.sin(ang);
				Blade b = new Blade();
				b.pos = hand;
				b.dir = new Vec3(mid.x * c - mid.z * s, mid.y, mid.x * s + mid.z * c).normalize();
				b.maxTravel = to.length() + 4;
				blades.add(b);
			}
			sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.6f);
		}
		float damage = GodSlayerRules.skillDamage(GodSlayerRules.KATANA_START, unlockLevel(), boss.bossLevel());
		boolean allHome = true;
		for (Blade b : blades) {
			if (b.home) {
				continue;
			}
			allHome = false;
			if (!b.returning) {
				Vec3 next = b.pos.add(b.dir.scale(SPEED));
				b.travelled += SPEED;
				boolean blocked = !sl.noCollision(new AABB(next, next).inflate(0.2));
				b.pos = next;
				draw(sl, b.pos);
				if (!b.hit) {
					for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos, b.pos).inflate(1.2))) {
						if (le != boss && le.isAlive()) {
							le.hurtServer(sl, sl.damageSources().mobAttack(boss), damage);
							b.hit = true;
							splash(sl, le);
							sl.playSound(null, le.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2f, 0.8f);
							break;
						}
					}
				}
				if (blocked || b.hit || b.travelled >= b.maxTravel) {
					b.returning = true;
				}
			} else {
				Vec3 to = hand.subtract(b.pos);
				if (to.length() <= SPEED + 1.5) {
					b.home = true;
					continue;
				}
				b.pos = b.pos.add(to.normalize().scale(SPEED));
				draw(sl, b.pos);
			}
		}
		if (allHome) {
			t0 = -1;
			blades.clear();
			done = true;
		}
	}

	/** A spinning blade that drips blood behind it. */
	private static void draw(ServerLevel sl, Vec3 p) {
		sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.05);
		sl.sendParticles(BLOOD, p.x, p.y, p.z, 4, 0.25, 0.25, 0.25, 0);
	}

	/** A burst of blood where a blade cuts a creature. */
	private static void splash(ServerLevel sl, LivingEntity le) {
		double y = le.getY() + le.getBbHeight() * 0.6;
		sl.sendParticles(BLOOD_SPLASH, le.getX(), y, le.getZ(), 40, 0.3, 0.4, 0.3, 0.25);
		sl.sendParticles(BLOOD, le.getX(), y, le.getZ(), 25, 0.5, 0.5, 0.5, 0);
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
