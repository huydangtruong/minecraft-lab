package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.mixin.MobGoalAccess;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.WitchRules;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The AI of a grasped mob (D134, D135): its vanilla target goals are replaced by {@link TargetPick}, wandering and fleeing
 * goals are dropped and movement by mode (follow, stay, guard) is added. A mob with no attack goal of its own (villagers,
 * animals) gets {@link MeleeGoal}. {@link #apply} is idempotent: it runs on tame and again whenever the mob loads.
 */
final class GraspGoals {
	/** Class names (anywhere in the class chain) of goals that would pull the mob away from its owner. */
	private static final String[] DROP = {"Panic", "AvoidEntity", "Stroll", "RandomFly", "RandomFloat", "RandomSwimming",
		"RestrictSun", "FleeSun", "MoveThroughVillage", "MoveBackToVillage", "MoveTowardsRestriction", "WanderToPosition",
		"Flee", "Hide", "Run",
		// D191: raider goals. A patrol vindicator's HoldGroundAttack stood still staring at a far target (night: always one) and
		// blocked its melee and the follow; the patrol, raid and banner goals pull it away from the owner too.
		"HoldGround", "Patrol", "PathfindToRaid", "RaidLeaderBanner", "Celebration"};
	/** Class names of goals that already attack, so no extra melee goal is needed. */
	private static final String[] ATTACKS = {"Attack", "Shoot", "Swell", "Spit", "Throw", "Explode", "Bite", "Pounce"};

	private GraspGoals() {}

	/** Marks the goals this class added. */
	interface Ours {}

	private static boolean nameHas(Goal g, String[] words) {
		for (Class<?> c = g.getClass(); c != null && c != Object.class && c != Goal.class; c = c.getSuperclass()) {
			String n = c.getName();
			for (String w : words) {
				if (n.contains(w)) {
					return true;
				}
			}
		}
		return false;
	}

	static void apply(Mob mob) {
		var goals = mob.getGoalSelector();
		var targets = ((MobGoalAccess) mob).permahearts$targetSelector();
		goals.removeAllGoals(g -> g instanceof Ours);
		targets.removeAllGoals(g -> true);
		boolean ownAttack = false;
		for (var w : new ArrayList<>(goals.getAvailableGoals())) {
			if (nameHas(w.getGoal(), ATTACKS) && !(w.getGoal() instanceof Ours)) {
				ownAttack = true;
			}
		}
		goals.removeAllGoals(g -> nameHas(g, DROP));
		if (mob instanceof PathfinderMob pm) {
			if (!ownAttack) {
				goals.addGoal(3, new MeleeGoal(pm));
			}
			goals.addGoal(5, new FollowGoal(pm));
			goals.addGoal(5, new PostGoal(pm));
			if (pm instanceof net.minecraft.world.entity.monster.Witch w) {
				goals.addGoal(2, new WitchHealGoal(w)); // D167
			}
		}
		targets.addGoal(1, new TargetPick(mob));
		mob.setCanPickUpLoot(false);
	}

	/** Back to the vanilla goals: clears both selectors and runs the mob's own goal registration again. */
	static void restore(Mob mob) {
		mob.getGoalSelector().removeAllGoals(g -> true);
		((MobGoalAccess) mob).permahearts$targetSelector().removeAllGoals(g -> true);
		((MobGoalAccess) mob).permahearts$registerGoals();
		if (mob instanceof AbstractSkeleton s) {
			s.reassessWeaponGoal(); // skeletons add their attack goal here, not in registerGoals
		}
	}

	// ---- helpers ----

	/** Walks to a point, with the move control for mobs that fly. */
	static void goTo(Mob mob, double x, double y, double z, double speed) {
		if (!mob.getNavigation().moveTo(x, y, z, speed) && flies(mob)) {
			mob.getMoveControl().setWantedPosition(x, y, z, speed);
		}
	}

	static boolean flies(Mob mob) {
		return io.github.huydangtruong.immortalcultivation.technique.SafeTeleport.flies(mob);
	}

	/** Damage one hit of this mob does: its attribute when it has one, else 3 (villager) or 2 (D135), plus the held item. */
	static double damageOf(Mob mob) {
		if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
			return mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
		}
		double base = GraspRules.fallbackDamage(mob instanceof net.minecraft.world.entity.npc.villager.Villager);
		return base + GraspGear.weaponBonus(mob.getMainHandItem());
	}

	private static ServerPlayer ownerOf(Mob mob) {
		GraspedMob ga = mob.getAttached(GraspedMob.ATTACHMENT);
		if (ga == null || !(mob.level() instanceof ServerLevel sl)) {
			return null;
		}
		return sl.getServer().getPlayerList().getPlayer(ga.owner());
	}

	private static GraspRules.Mode modeOf(Mob mob) {
		GraspedMob ga = mob.getAttached(GraspedMob.ATTACHMENT);
		return ga == null ? GraspRules.Mode.FOLLOW : GraspRules.Mode.of(ga.mode());
	}

	private static Vec3 postOf(Mob mob) {
		GraspedMob ga = mob.getAttached(GraspedMob.ATTACHMENT);
		return ga == null ? mob.position() : new Vec3(ga.guardX() + 0.5, ga.guardY(), ga.guardZ() + 0.5);
	}

	// ---- targets ----

	/** Picks what to fight: the Death Match opponent, what hurt or was hurt by the owner, what hit us, then close hostiles. */
	private static final class TargetPick extends TargetGoal implements Ours {
		private int wait;

		TargetPick(Mob mob) {
			super(mob, false);
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (--wait > 0) {
				return false;
			}
			wait = 10;
			ServerPlayer o = ownerOf(mob);
			if (o == null || mob.getTarget() != null && mob.getTarget().isAlive()) {
				return false;
			}
			LivingEntity t = pick(o);
			if (t == null) {
				return false;
			}
			targetMob = t;
			return true;
		}

		private boolean valid(LivingEntity e, ServerPlayer o) {
			return e != null && e.isAlive() && e != o && !e.isSpectator() && e != mob
				&& (!(e instanceof Player) || Grasp.isMatchOpponent(o, e))
				&& !Grasp.isGrasped(e) && !(e instanceof io.github.huydangtruong.immortalcultivation.technique.OwnedSummon)
				&& !io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc.isWanderer(e)
				&& e.distanceToSqr(mob) < 40 * 40 && mob.canAttack(e);
		}

		private LivingEntity pick(ServerPlayer o) {
			for (Player p : mob.level().players()) {
				if (Grasp.isMatchOpponent(o, p) && p.distanceToSqr(mob) < 40 * 40 && valid(p, o)) {
					return p;
				}
			}
			if (o.tickCount - o.getLastHurtByMobTimestamp() < 100 && valid(o.getLastHurtByMob(), o)) {
				return o.getLastHurtByMob();
			}
			if (o.tickCount - o.getLastHurtMobTimestamp() < 100 && valid(o.getLastHurtMob(), o)) {
				return o.getLastHurtMob();
			}
			if (mob.tickCount - mob.getLastHurtByMobTimestamp() < 100 && valid(mob.getLastHurtByMob(), o)) {
				return mob.getLastHurtByMob();
			}
			GraspRules.Mode mode = modeOf(mob);
			double r = GraspRules.scanRadius(mode);
			Vec3 post = postOf(mob);
			AABB box = mob.getBoundingBox().inflate(r, Math.min(r, 6), r);
			Mob best = null;
			for (Mob m : mob.level().getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && valid(m, o))) {
				if (mode != GraspRules.Mode.FOLLOW && m.position().distanceToSqr(post) > (r + 2) * (r + 2)) {
					continue;
				}
				if (best == null || m.distanceToSqr(mob) < best.distanceToSqr(mob)) {
					best = m;
				}
			}
			return best;
		}
	}

	// ---- movement ----

	/** Follow mode: stay near the owner (a wolf's walk). The teleport at more than 12 blocks is done in Grasp's tick. */
	private static final class FollowGoal extends Goal implements Ours {
		private final PathfinderMob mob;

		FollowGoal(PathfinderMob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			ServerPlayer o = ownerOf(mob);
			return o != null && modeOf(mob) == GraspRules.Mode.FOLLOW && mob.getTarget() == null && o.level() == mob.level()
				&& (mob.distanceToSqr(o) > 4.5 * 4.5 || mob.distanceToSqr(o) < 2 * 2); // D165: too far, or crowding the owner
		}

		@Override
		public boolean canContinueToUse() {
			ServerPlayer o = ownerOf(mob);
			return o != null && modeOf(mob) == GraspRules.Mode.FOLLOW && mob.getTarget() == null && o.level() == mob.level()
				&& mob.position().distanceToSqr(followSpot(mob, o)) > 0.5 * 0.5;
		}

		private int repath;

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void tick() {
			// D165: a counter, not tickCount % 8: goals without requiresUpdateEveryTick tick only every other game tick,
			// so a mob with an odd tick phase never matched % 8 and stood still.
			ServerPlayer o = ownerOf(mob);
			if (o != null && --repath <= 0) {
				repath = 4;
				Vec3 at = followSpot(mob, o);
				goTo(mob, at.x, at.y, at.z, 1.2);
				if (mob.getNavigation().isDone()) { // the spot is in the next block: no path, just step there
					mob.getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.0);
				}
			}
		}

		@Override
		public void stop() {
			mob.getNavigation().stop();
		}
	}

	/** Stay and guard modes: hold the post (stay) or patrol around it (guard). Fights are left to the attack goals. */
	private static final class PostGoal extends Goal implements Ours {
		private final PathfinderMob mob;
		private int idle;

		PostGoal(PathfinderMob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		private double radius() {
			return modeOf(mob) == GraspRules.Mode.GUARD ? GraspRules.GUARD_RADIUS : GraspRules.STAY_RADIUS;
		}

		@Override
		public boolean canUse() {
			if (modeOf(mob) == GraspRules.Mode.FOLLOW || mob.getTarget() != null) {
				return false;
			}
			Vec3 post = postOf(mob);
			if (mob.position().distanceToSqr(post) > radius() * radius()) {
				return true;
			}
			return modeOf(mob) == GraspRules.Mode.GUARD && --idle <= 0;
		}

		@Override
		public boolean canContinueToUse() {
			return modeOf(mob) != GraspRules.Mode.FOLLOW && mob.getTarget() == null && !mob.getNavigation().isDone();
		}

		@Override
		public void start() {
			idle = 60 + mob.getRandom().nextInt(80);
			Vec3 post = postOf(mob);
			double x = post.x, z = post.z;
			if (modeOf(mob) == GraspRules.Mode.GUARD && mob.position().distanceToSqr(post) <= radius() * radius()) {
				double a = mob.getRandom().nextDouble() * Math.PI * 2, d = 2 + mob.getRandom().nextDouble() * (GraspRules.GUARD_RADIUS - 3);
				x += Math.cos(a) * d;
				z += Math.sin(a) * d;
			}
			goTo(mob, x, post.y, z, 1.0);
		}

		@Override
		public void stop() {
			mob.getNavigation().stop();
		}
	}

	/** Melee for mobs without an attack goal (villagers, animals); uses the attack damage attribute or the fallback of D135. */
	private static final class MeleeGoal extends Goal implements Ours {
		private final PathfinderMob mob;
		private int cooldown;

		MeleeGoal(PathfinderMob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = mob.getTarget();
			return t != null && t.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			LivingEntity t = mob.getTarget();
			if (t == null || !(mob.level() instanceof ServerLevel sl)) {
				return;
			}
			mob.getLookControl().setLookAt(t, 30f, 30f);
			if (mob.tickCount % 6 == 0) {
				goTo(mob, t.getX(), t.getY(), t.getZ(), 1.3);
			}
			if (cooldown > 0) {
				cooldown--;
			}
			double reach = mob.getBbWidth() * 2 + t.getBbWidth();
			if (cooldown == 0 && mob.distanceToSqr(t) <= reach * reach + 1) {
				cooldown = 20;
				mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
				if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
					mob.doHurtTarget(sl, t);
				} else {
					t.hurtServer(sl, sl.damageSources().mobAttack(mob), (float) damageOf(mob));
				}
			}
		}

		@Override
		public void stop() {
			mob.getNavigation().stop();
		}
	}

	/** D167: a grasped witch throws Healing or Regeneration at a hurt owner or ally within 10 blocks (its enemy potions come from the ranged attack). */
	private static final class WitchHealGoal extends Goal implements Ours {
		private final net.minecraft.world.entity.monster.Witch witch;
		private long next;
		private int wait;
		private LivingEntity ally;

		WitchHealGoal(net.minecraft.world.entity.monster.Witch witch) {
			this.witch = witch;
			setFlags(EnumSet.of(Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			GraspedMob ga = witch.getAttached(GraspedMob.ATTACHMENT);
			if (ga == null || --wait > 0 || witch.isDrinkingPotion() || witch.level().getGameTime() < next) {
				return false;
			}
			wait = 5; // a counter, not tickCount % n (D165: goals tick on odd or even game ticks)
			ally = WitchPotions.hurtAlly(witch, ga.owner());
			return ally != null;
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			next = witch.level().getGameTime() + WitchRules.ALLY_COOLDOWN_TICKS;
			witch.getLookControl().setLookAt(ally, 30f, 30f);
			WitchPotions.throwAtAlly(witch, ally);
		}
	}

	/** D165: this mob's own spot 2.5 to 3.5 blocks from its owner (stable per mob). */
	static Vec3 followSpot(Mob mob, net.minecraft.world.entity.Entity owner) {
		double[] d = GraspRules.followSpot(mob.getUUID().hashCode());
		return owner.position().add(d[0], 0, d[1]);
	}

	/** Free spot near a position for a teleport (the same position when nothing nearby is free). */
	static Vec3 freeSpotNear(ServerLevel sl, Mob mob, Vec3 want) {
		List<Vec3> tries = new ArrayList<>();
		for (int dy : new int[] {0, 1, 2, -1}) {
			tries.add(want.add(0, dy, 0));
		}
		for (Vec3 at : tries) {
			if (sl.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(at))) {
				return at;
			}
		}
		return want;
	}
}
