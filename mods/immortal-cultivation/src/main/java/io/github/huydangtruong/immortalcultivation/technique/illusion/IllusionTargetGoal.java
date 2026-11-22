package io.github.huydangtruong.immortalcultivation.technique.illusion;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.IllusionTargets;
import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;

/** Picks the nearest entity on the owner's list, else the nearest hostile mob near the owner (creepers included). */
public final class IllusionTargetGoal extends TargetGoal {
	private final Illusion c;
	private int wait;

	public IllusionTargetGoal(Illusion c) {
		super(c, false);
		this.c = c;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (--wait > 0) {
			return false;
		}
		wait = 6;
		Player o = OwnedSummon.ownerOf(c);
		if (o == null || !(c.level() instanceof ServerLevel sl)) {
			return false;
		}
		LivingEntity t = pick(sl, o);
		if (t == null) {
			return false;
		}
		targetMob = t;
		return true;
	}

	/** Keeps its target while valid, but switches when the list has someone the clone is not fighting yet. */
	@Override
	public boolean canContinueToUse() {
		if (!super.canContinueToUse()) {
			return false;
		}
		LivingEntity cur = c.getTarget();
		if (cur != null && !OwnedSummon.isMatchOpponent(c, cur) && c.tickCount % 10 == 0 && c.level() instanceof ServerLevel sl && c.owner() != null) {
			Set<UUID> ids = IllusionTargets.idsOf(c.owner());
			if (!ids.isEmpty() && !ids.contains(cur.getUUID()) && !IllusionTargets.of(sl, c.owner()).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private LivingEntity pick(ServerLevel sl, Player o) {
		LivingEntity opp = OwnedSummon.nearestMatchOpponent(c, 40); // P-043: the Death Match opponent
		if (opp != null && c.canAttack(opp)) {
			return opp;
		}
		LivingEntity best = null;
		for (LivingEntity e : IllusionTargets.of(sl, o.getUUID())) {
			if (e.distanceToSqr(c) < 40 * 40 && c.canAttack(e) && (best == null || e.distanceToSqr(c) < best.distanceToSqr(c))) {
				best = e;
			}
		}
		if (best != null) {
			return best;
		}
		boolean works = c.job.works(); // a working clone only fights hostile mobs that come within 6 blocks of it or the owner
		double near = TechniqueRules.ILLUSION_WORK_FIGHT_RANGE;
		AABB box = o.getBoundingBox().inflate(12, 6, 12);
		for (Mob m : sl.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && IllusionTargets.eligible(o, m) && c.canAttack(m)
			&& (!works || m.distanceTo(c) <= near || m.distanceTo(o) <= near))) {
			if (best == null || m.distanceToSqr(c) < best.distanceToSqr(c)) {
				best = m;
			}
		}
		return best;
	}
}
