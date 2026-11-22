package io.github.huydangtruong.immortalcultivation.technique.illusion;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;

enum Mood { CHASE, BACK_OFF, RANGED }

/** Fights the target with a mood that changes every 2-5 s: chase and strafe, back off, or shoot from a distance. */
public final class FightGoal extends Goal {
	private final Illusion a;
	private Mood mood = Mood.CHASE;
	private int moodEnd;
	private int cooldown;
	private int strafeDir = 1;
	private int strafeEnd;

	public FightGoal(Illusion a) {
		this.a = a;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = a.getTarget();
		return t != null && t.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		a.setCrouching(false);
		cooldown = 10 + a.getRandom().nextInt(10);
		pickMood(a.getTarget());
	}

	@Override
	public void stop() {
		a.setSprinting(false);
		a.getNavigation().stop();
	}

	private void pickMood(LivingEntity t) {
		double hp = a.getHealth() / Math.max(1.0f, a.getMaxHealth());
		if (a.getRandom().nextDouble() < TechniqueRules.illusionBackOffChance(hp)) {
			mood = Mood.BACK_OFF;
			Vec3 away = a.position().subtract(t.position()).multiply(1, 0, 1);
			if (away.lengthSqr() < 0.01) {
				away = new Vec3(a.getRandom().nextDouble() - 0.5, 0, a.getRandom().nextDouble() - 0.5);
			}
			away = away.normalize().scale(5 + a.getRandom().nextDouble() * 3);
			Vec3 p = a.position().add(away);
			a.setSprinting(true);
			if (!a.getNavigation().moveTo(p.x, p.y, p.z, 1.0)) {
				mood = Mood.CHASE;
			}
		} else {
			mood = a.holdsRanged() ? Mood.RANGED : Mood.CHASE;
		}
		moodEnd = a.tickCount + TechniqueRules.illusionMoodTicks(a.getRandom().nextDouble());
	}

	@Override
	public void tick() {
		LivingEntity t = a.getTarget();
		if (t == null || !(a.level() instanceof ServerLevel sl)) {
			return;
		}
		cooldown--;
		a.getLookControl().setLookAt(t, 30f, 30f);
		if (a.tickCount >= moodEnd) {
			pickMood(t);
		}
		double d = Math.sqrt(a.distanceToSqr(t));
		switch (mood) {
			case CHASE -> chase(sl, t, d);
			case BACK_OFF -> {
				if (a.getNavigation().isDone()) {
					mood = Mood.CHASE;
					moodEnd = a.tickCount + TechniqueRules.illusionMoodTicks(a.getRandom().nextDouble());
				}
			}
			case RANGED -> ranged(sl, t, d);
		}
	}

	/** Sprints at the target, jumps into the hit, circles around it and hits at the owner's attack-cooldown rhythm. */
	private void chase(ServerLevel sl, LivingEntity t, double d) {
		a.setSprinting(true);
		boolean close = a.isWithinMeleeAttackRange(t);
		if (d > 3.5) {
			if (a.tickCount % 5 == 0) {
				a.getNavigation().moveTo(t, 1.0);
			}
		} else if (a.tickCount % 4 == 0) {
			if (a.tickCount >= strafeEnd) {
				strafeDir = a.getRandom().nextBoolean() ? 1 : -1;
				strafeEnd = a.tickCount + 30 + a.getRandom().nextInt(40);
			}
			Vec3 rel = a.position().subtract(t.position()).multiply(1, 0, 1);
			rel = rel.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : rel.normalize();
			double ang = strafeDir * 0.6;
			double cos = Math.cos(ang), sin = Math.sin(ang);
			Vec3 p = t.position().add(rel.x * cos - rel.z * sin, 0, rel.x * sin + rel.z * cos).add(rel.scale(1.6));
			a.getNavigation().moveTo(p.x, t.getY(), p.z, 1.0);
		}
		if (cooldown == 7 && a.onGround() && d < 3.5) {
			a.getJumpControl().jump(); // lands as a falling hit, like a crit
		}
		if (cooldown <= 0 && close && a.hasLineOfSight(t)) {
			a.doHurtTarget(sl, t);
			Player o = OwnedSummon.ownerOf(a);
			cooldown = TechniqueRules.illusionAttackTicks(o != null ? o.getAttributeValue(Attributes.ATTACK_SPEED) : 4.0);
		}
	}

	/** Keeps 6-12 blocks from the target and shoots. */
	private void ranged(ServerLevel sl, LivingEntity t, double d) {
		a.setSprinting(false);
		if (d < 6) {
			Vec3 away = a.position().subtract(t.position()).multiply(1, 0, 1).normalize().scale(4);
			if (a.tickCount % 5 == 0) {
				a.getNavigation().moveTo(a.getX() + away.x, a.getY(), a.getZ() + away.z, 1.0);
			}
		} else if (d > 12) {
			if (a.tickCount % 5 == 0) {
				a.getNavigation().moveTo(t, 1.0);
			}
		} else {
			a.getNavigation().stop();
		}
		if (cooldown <= 0 && d <= 16 && a.hasLineOfSight(t)) {
			a.shoot(sl, t);
			cooldown = 25 + a.getRandom().nextInt(10);
		}
	}
}
