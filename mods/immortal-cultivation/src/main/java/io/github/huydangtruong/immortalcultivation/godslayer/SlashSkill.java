package io.github.huydangtruong.immortalcultivation.godslayer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/** Lv 1: a katana slash at a target within reach, once per second. The damage is the boss's attack damage (30 x scale). */
public final class SlashSkill implements GodSlayerSkill {
	private boolean done = true;
	private long readyAt;

	@Override
	public int unlockLevel() {
		return 1;
	}

	@Override
	public boolean canStart(GodSlayer boss, LivingEntity target) {
		return boss.level().getGameTime() >= readyAt && boss.inReachOf(target);
	}

	@Override
	public void tick(GodSlayer boss, LivingEntity target) {
		done = false;
		if (!(boss.level() instanceof ServerLevel sl)) {
			done = true;
			return;
		}
		boss.swingForAttack(InteractionHand.MAIN_HAND);
		boss.faceTarget(target);
		boss.doHurtTarget(sl, target);
		sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
			1, 0, 0, 0, 0);
		sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.7f);
		readyAt = sl.getGameTime() + GodSlayerRules.SLASH_COOLDOWN;
		done = true;
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
