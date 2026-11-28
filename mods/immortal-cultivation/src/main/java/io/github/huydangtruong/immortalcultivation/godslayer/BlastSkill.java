package io.github.huydangtruong.immortalcultivation.godslayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lv 6 (D288): the boss stands still and glows red for 1.5 s (a red ring marks the 8 block radius), then blows up the
 * sphere around it: blocks break (like the Wither: mob griefing on, no drops, never bedrock-like blocks or chests), every creature
 * inside takes damage from 50 at Lv 6 and players inside see only red for 3 s.
 */
public final class BlastSkill implements GodSlayerSkill {
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01010, 2.0f);
	private long t0 = -1;
	private boolean done = true;

	@Override
	public int unlockLevel() {
		return GodSlayerRules.BLAST_LEVEL;
	}

	@Override
	public boolean canStart(GodSlayer boss, LivingEntity target) {
		return boss.distanceTo(target) <= GodSlayerRules.BLAST_RADIUS - 1;
	}

	@Override
	public void tick(GodSlayer boss, LivingEntity target) {
		if (!(boss.level() instanceof ServerLevel sl)) {
			done = true;
			return;
		}
		done = false;
		if (t0 < 0) {
			t0 = sl.getGameTime();
			sl.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3.0f, 0.6f);
		}
		long e = sl.getGameTime() - t0;
		boss.setDeltaMovement(Vec3.ZERO);
		Vec3 c = boss.position().add(0, boss.getBbHeight() / 2, 0);
		if (e < GodSlayerRules.BLAST_WINDUP_TICKS) {
			sl.sendParticles(RED, c.x, c.y, c.z, 12, 1.2, 2.0, 1.2, 0);
			if (e % 5 == 0) {
				double r = GodSlayerRules.BLAST_RADIUS;
				for (int i = 0; i < 48; i++) {
					double a = Math.PI * 2 * i / 48;
					sl.sendParticles(RED, boss.getX() + Math.cos(a) * r, boss.getY() + 0.3, boss.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
			}
			return;
		}
		blast(sl, boss, true);
		t0 = -1;
		done = true;
	}

	/** The blast itself. Game tests pass breakBlocks = false so the 8 block sphere does not reach a neighbouring test. */
	public static void blast(ServerLevel sl, GodSlayer boss, boolean breakBlocks) {
		Vec3 c = boss.position().add(0, boss.getBbHeight() / 2, 0);
		double r = GodSlayerRules.BLAST_RADIUS;
		sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 2.5, 1.5, 2.5, 0);
		sl.sendParticles(RED, c.x, c.y, c.z, 200, r / 2, r / 3, r / 2, 0);
		sl.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0f, 0.5f);
		float damage = GodSlayerRules.skillDamage(GodSlayerRules.BLAST_START, GodSlayerRules.BLAST_LEVEL, boss.bossLevel());
		for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r))) {
			if (le == boss || !le.isAlive() || le.position().add(0, le.getBbHeight() / 2, 0).distanceTo(c) > r) {
				continue;
			}
			le.hurtServer(sl, sl.damageSources().mobAttack(boss), damage);
			Vec3 push = le.position().subtract(boss.position());
			if (push.lengthSqr() > 1e-4) {
				le.push(push.normalize().scale(1.2).add(0, 0.4, 0));
				le.needsSync = true;
			}
			if (le instanceof ServerPlayer p) {
				ServerPlayNetworking.send(p, new GodSlayerNet.Blind(GodSlayerRules.BLIND_TICKS));
			}
		}
		if (breakBlocks) {
			breakBlocks(sl, boss);
		}
	}

	/** Clears the sphere like the Wither would: only with mob griefing, no drops, skips liquids, unbreakable blocks and blocks with contents (chests, furnaces...). */
	private static void breakBlocks(ServerLevel sl, GodSlayer boss) {
		if (!sl.getServer().getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		int r = (int) Math.ceil(GodSlayerRules.BLAST_RADIUS);
		BlockPos centre = BlockPos.containing(boss.getX(), boss.getY() + 1, boss.getZ());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					if (!GodSlayerRules.inBlast(dx, dy, dz)) {
						continue;
					}
					pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					BlockState state = sl.getBlockState(pos);
					if (state.isAir() || state.hasBlockEntity() || state.getBlock() instanceof LiquidBlock || !WitherBoss.canDestroy(state)
						|| state.getDestroySpeed(sl, pos) < 0) {
						continue;
					}
					sl.removeBlock(pos, false);
				}
			}
		}
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
