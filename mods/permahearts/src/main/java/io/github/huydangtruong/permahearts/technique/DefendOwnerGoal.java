package io.github.huydangtruong.permahearts.technique;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Targets what hurt the owner, what the owner hit and any monster close to the owner; never players or other summons. */
final class DefendOwnerGoal<T extends Mob & OwnedSummon> extends TargetGoal {
	private final T g;
	private int wait;

	DefendOwnerGoal(T g) {
		super(g, false);
		this.g = g;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (--wait > 0) {
			return false;
		}
		wait = 10;
		Player o = OwnedSummon.ownerOf(g);
		if (o == null) {
			return false;
		}
		LivingEntity t = pick(o);
		if (t == null) {
			return false;
		}
		targetMob = t;
		return true;
	}

	private boolean valid(LivingEntity e, Player o) {
		return e != null && e.isAlive() && !(e instanceof Player) && !(e instanceof OwnedSummon) && !e.isSpectator()
			&& e.distanceToSqr(g) < 40 * 40 && e.distanceToSqr(o) < 32 * 32;
	}

	private LivingEntity pick(Player o) {
		if (o.tickCount - o.getLastHurtByMobTimestamp() < 100 && valid(o.getLastHurtByMob(), o)) {
			return o.getLastHurtByMob();
		}
		if (o.tickCount - o.getLastHurtMobTimestamp() < 100 && valid(o.getLastHurtMob(), o)) {
			return o.getLastHurtMob();
		}
		AABB box = o.getBoundingBox().inflate(12, 6, 12);
		Mob best = null;
		for (Mob m : g.level().getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && valid(m, o))) {
			if (best == null || m.distanceToSqr(g) < best.distanceToSqr(g)) {
				best = m;
			}
		}
		return best;
	}
}
