package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.GraspCalls;
import io.github.huydangtruong.immortalcultivation.technique.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.SafeTeleport;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspData;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspedMob;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import java.util.UUID;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

/** P-053: Soul Grasp on the server: the tether, the tame, the limit, the bonus, release and death. */
public class GraspGameTest {
	private static ServerPlayer grasper(GameTestHelper c, int level) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 2.5, 1, 5.5, -90f, 0f); // looking along +x
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.GRASP, level, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.GRASP));
		return p;
	}

	private static int held(GameTestHelper c, ServerPlayer p) {
		return GraspData.of(c.getLevel().getServer()).held(p.getUUID());
	}

	private static void cleanUp(GameTestHelper c, ServerPlayer p) {
		Grasp.endTether(p, false);
		for (var e : GraspData.of(c.getLevel().getServer()).get(p.getUUID()).mobs().toArray(new GraspData.Entry[0])) {
			if (c.getLevel().getEntity(e.mob()) instanceof net.minecraft.world.entity.Mob m) {
				Grasp.releaseMob(m, false);
			}
			GraspData.of(c.getLevel().getServer()).remove(p.getUUID(), e.mob());
		}
		FlightBladeGameTest.drop(c, p);
	}

	@GameTest(maxTicks = 400)
	public void tetherTamesAfterTheChargeAndStartsTheCooldown(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 5.5));
		z.setNoAi(true);
		c.assertTrue(Grasp.startTether(p, z.getId()), "the tether starts on a zombie in sight");
		int ticks = GraspRules.chargeTicks(z.getMaxHealth());
		c.runAfterDelay(ticks / 2, () -> c.assertTrue(held(c, p) == 0, "not tamed halfway"));
		c.runAfterDelay(ticks + 10, () -> {
			c.assertTrue(held(c, p) == 1, "tamed after the charge, held " + held(c, p));
			GraspedMob ga = z.getAttached(GraspedMob.ATTACHMENT);
			c.assertTrue(ga != null && ga.owner().equals(p.getUUID()) && ga.number() == 1, "attachment with owner and number 1");
			c.assertTrue(z.getAttribute(Attributes.MAX_HEALTH).getModifier(Grasp.BONUS_ID) != null, "health bonus applied");
			c.assertTrue(z.getCustomName() != null && z.getCustomName().getString().endsWith(" 1"), "named with its number");
			long now = c.getLevel().getServer().overworld().getGameTime();
			c.assertTrue(TechniqueAttachment.get(p).onCooldown(Technique.GRASP, now), "cooldown after a success");
			cleanUp(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void aBossAndAFullListAreRefused(GameTestHelper c) {
		ServerPlayer p = grasper(c, 1);
		Zombie a = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 5.5));
		a.setNoAi(true);
		Grasp.tame(p, a, 1);
		int max = GraspRules.maxMobs(1);
		for (int i = 1; i < max; i++) {
			Zombie extra = c.spawn(EntityTypes.ZOMBIE, new Vec3(7.5, 1, 3.5 + i));
			extra.setNoAi(true);
			Grasp.tame(p, extra, 1);
		}
		c.assertTrue(held(c, p) == max, "list is full, held " + held(c, p));
		Zombie one = c.spawn(EntityTypes.ZOMBIE, new Vec3(8.5, 1, 5.5));
		one.setNoAi(true);
		c.assertTrue(!Grasp.startTether(p, one.getId()), "a full list refuses another tether");
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 60)
	public void releaseRestoresTheMobAndDeathFreesTheSlot(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 5.5));
		z.setNoAi(true);
		Grasp.tame(p, z, 10);
		c.assertTrue(held(c, p) == 1, "held one");
		Grasp.releaseMob(z, true);
		GraspData.of(c.getLevel().getServer()).remove(p.getUUID(), z.getUUID());
		c.assertTrue(!z.hasAttached(GraspedMob.ATTACHMENT), "attachment gone");
		c.assertTrue(z.getAttribute(Attributes.MAX_HEALTH).getModifier(Grasp.BONUS_ID) == null, "bonus gone");
		c.assertTrue(z.getCustomName() == null, "name cleared");
		Zombie y = c.spawn(EntityTypes.ZOMBIE, new Vec3(7.5, 1, 4.5));
		y.setNoAi(true);
		Grasp.tame(p, y, 10);
		c.assertTrue(held(c, p) == 1 && y.getAttached(GraspedMob.ATTACHMENT).number() == 2, "numbers are never reused");
		y.kill(c.getLevel());
		c.runAfterDelay(5, () -> {
			c.assertTrue(held(c, p) == 0, "a dead mob frees its slot, held " + held(c, p));
			cleanUp(c, p);
			c.succeed();
		});
	}

	/** D185 (Huy's bug: a grasped Vindicator kept the raid from finishing): a grasped raider leaves its raid and never joins one. */
	@GameTest(maxTicks = 60)
	public void aGraspedRaiderLeavesItsRaid(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		net.minecraft.world.entity.monster.illager.Vindicator v = c.spawn(EntityTypes.VINDICATOR, new Vec3(6.5, 1, 5.5));
		v.setNoAi(true);
		net.minecraft.world.entity.raid.Raid raid = new net.minecraft.world.entity.raid.Raid(c.absolutePos(new net.minecraft.core.BlockPos(5, 1, 5)),
			net.minecraft.world.Difficulty.NORMAL);
		raid.joinRaid(c.getLevel(), 1, v, null, false);
		c.assertTrue(v.getCurrentRaid() == raid && raid.getTotalRaidersAlive() == 1, "the vindicator is in the raid");
		Grasp.tame(p, v, 10);
		c.assertTrue(v.getCurrentRaid() == null, "grasped: out of the raid");
		c.assertTrue(raid.getTotalRaidersAlive() == 0, "the raid no longer waits for it, alive " + raid.getTotalRaidersAlive());
		c.assertTrue(!v.canJoinRaid(), "it never joins another raid");
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aGraspedPatrolVindicatorDropsItsHoldGroundAndPatrolGoals(GameTestHelper c) { // D191
		ServerPlayer p = grasper(c, 10);
		net.minecraft.world.entity.monster.illager.Vindicator v = c.spawn(EntityTypes.VINDICATOR, new Vec3(6.5, 1, 5.5));
		v.findPatrolTarget(); // a patrol member, like the ones that spawn at night
		Grasp.tame(p, v, 10);
		var left = v.getGoalSelector().getAvailableGoals().stream().map(w -> w.getGoal().getClass().getName())
			.filter(n -> n.contains("HoldGround") || n.contains("Patrol") || n.contains("PathfindToRaid") || n.contains("Celebration")).toList();
		c.assertTrue(left.isEmpty(), "no raider goal holds it in place or pulls it away: " + left);
		c.assertTrue(v.getGoalSelector().getAvailableGoals().stream().anyMatch(w -> w.getGoal().getClass().getName().contains("MeleeAttack")),
			"it still has its melee attack");
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void theVexesOfAGraspedEvokerNeverAttackTheOwnerOtherPlayersOrVillagers(GameTestHelper c) { // D192, D194
		ServerPlayer p = grasper(c, 10);
		net.minecraft.world.entity.monster.illager.Evoker ev = c.spawn(EntityTypes.EVOKER, new Vec3(6.5, 1, 5.5));
		ev.setNoAi(true);
		Grasp.tame(p, ev, 10);
		net.minecraft.world.entity.monster.Vex mine = c.spawn(EntityTypes.VEX, new Vec3(4.5, 2, 4.5));
		mine.setOwner(ev);
		mine.setNoAi(true);
		net.minecraft.world.entity.monster.Vex wild = c.spawn(EntityTypes.VEX, new Vec3(2.5, 2, 2.5));
		wild.setNoAi(true);
		c.assertTrue(!mine.canAttack(p), "a vex of the grasped evoker never attacks its owner");
		c.assertTrue(Grasp.isGraspedBy(mine, p.getUUID()), "it counts as the owner's ally (techniques and allies skip it)");
		c.assertTrue(!ev.canAttack(mine), "the evoker's other followers leave it alone");
		c.assertTrue(wild.canAttack(p), "a wild vex still attacks the player");
		ServerPlayer other = FlightBladeGameTest.survivor(c); // D194: no Death Match here
		net.minecraft.world.entity.npc.villager.Villager villager = c.spawn(EntityTypes.VILLAGER, new Vec3(5.5, 1, 2.5));
		villager.setNoAi(true);
		c.assertTrue(!mine.canAttack(other), "a vex of the grasped evoker leaves another player alone outside a Death Match");
		c.assertTrue(!mine.canAttack(villager), "a vex of the grasped evoker never attacks a villager");
		c.assertTrue(wild.canAttack(other), "a wild vex still attacks any player");
		mine.discard();
		wild.discard();
		villager.discard();
		FlightBladeGameTest.drop(c, other);
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void graspedMobsDoNotKeepTheOwnerFromSleeping(GameTestHelper c) { // D195
		ServerPlayer p = grasper(c, 10);
		Zombie mine = c.spawn(EntityTypes.ZOMBIE, new Vec3(5.5, 1, 5.5));
		mine.setNoAi(true);
		Grasp.tame(p, mine, 10);
		net.minecraft.world.entity.monster.zombie.ZombifiedPiglin piglin = c.spawn(EntityTypes.ZOMBIFIED_PIGLIN, new Vec3(5.5, 1, 3.5));
		piglin.setNoAi(true);
		Grasp.tame(p, piglin, 10);
		Zombie wild = c.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 1, 3.5));
		wild.setNoAi(true);
		ServerPlayer other = FlightBladeGameTest.survivor(c);
		piglin.setPersistentAngerTarget(net.minecraft.world.entity.EntityReference.of(other)); // angry at another player: its own override of the check
		piglin.startPersistentAngerTimer();
		c.assertTrue(!mine.isPreventingPlayerRest(c.getLevel(), p), "a grasped zombie lets its owner sleep");
		c.assertTrue(!piglin.isPreventingPlayerRest(c.getLevel(), p), "a grasped zombified piglin lets its owner sleep");
		c.assertTrue(!mine.isPreventingPlayerRest(c.getLevel(), other), "it lets another player sleep outside a Death Match");
		c.assertTrue(!piglin.isPreventingPlayerRest(c.getLevel(), other), "a grasped zombified piglin angry at another player lets them sleep outside a Death Match");
		c.assertTrue(wild.isPreventingPlayerRest(c.getLevel(), p), "a wild zombie still keeps the player awake");
		wild.discard();
		FlightBladeGameTest.drop(c, other);
		cleanUp(c, p);
		c.succeed();
	}

	/** D200 (Huy's bug: villagers still feared grasped mobs): villagers and the wandering trader do not see a grasped mob as a threat. */
	@GameTest(maxTicks = 200)
	public void villagersDoNotFearGraspedMobs(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		net.minecraft.world.entity.npc.villager.Villager villager = c.spawn(EntityTypes.VILLAGER, new Vec3(2.5, 1, 2.5));
		net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader trader = c.spawn(EntityTypes.WANDERING_TRADER, new Vec3(2.5, 1, 6.5));
		Zombie mine = c.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, 1, 4.5));
		mine.setNoAi(true);
		Grasp.tame(p, mine, 10);
		c.runAfterDelay(60, () -> {
			c.assertTrue(villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_HOSTILE).isEmpty(),
				"a villager does not see a grasped zombie as a hostile");
			var avoid = new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(trader, Zombie.class, 8f, 0.5, 0.5);
			c.assertTrue(!avoid.canUse(), "the wandering trader does not run from a grasped zombie");
			Zombie wild = c.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, 1, 2.5));
			wild.setNoAi(true);
			c.runAfterDelay(60, () -> {
				c.assertTrue(villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_HOSTILE).isPresent(),
					"a wild zombie is still a hostile to the villager");
				wild.discard();
				cleanUp(c, p);
				c.succeed();
			});
		});
	}

	/** D165 (Huy's bug: 11 followers crowded onto him): following mobs that start on the owner step out to 2 blocks or more. */
	@GameTest(maxTicks = 140)
	public void followersKeepTwoBlocksFromTheOwner(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		FlightBladeGameTest.place(c, p, 4, 1, 4, -90f, 0f); // middle of the 8x8 test area: spots reach 3.5 out
		Zombie[] zs = new Zombie[4];
		for (int i = 0; i < zs.length; i++) {
			zs[i] = c.spawn(EntityTypes.ZOMBIE, new Vec3(4 + (i % 2 == 0 ? 0.6 : -0.6), 1, 4 + (i < 2 ? 0.6 : -0.6)));
			Grasp.tame(p, zs[i], 20);
		}
		c.runAfterDelay(120, () -> {
			for (Zombie z : zs) {
				double d = Math.sqrt(z.distanceToSqr(p.getX(), z.getY(), p.getZ()));
				c.assertTrue(d >= 1.9, "a follower stands at least 2 blocks from its owner, was " + d);
			}
			cleanUp(c, p);
			c.succeed();
		});
	}

	/** D170 (Huy's bug: followers teleported into the air and the water): a follower's teleport spot needs ground and no water. */
	@GameTest(maxTicks = 20)
	public void followTeleportOnlyLandsOnSafeGround(GameTestHelper c) {
		for (int y = 0; y <= 4; y++) {
			c.setBlock(new net.minecraft.core.BlockPos(5, y, 5), net.minecraft.world.level.block.Blocks.WATER);
		}
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(2.5, 1, 2.5));
		z.setNoAi(true);
		net.minecraft.world.entity.monster.Vex v = c.spawn(EntityTypes.VEX, new Vec3(2.5, 3, 2.5));
		v.setNoAi(true);
		Vec3 ground = SafeTeleport.spotOrNull(c.getLevel(), z, c.absoluteVec(new Vec3(2.5, 1, 2.5)));
		c.assertTrue(ground != null, "a spot just above the floor is fine");
		Vec3 low = SafeTeleport.spotOrNull(c.getLevel(), z, c.absoluteVec(new Vec3(2.5, 3, 2.5)));
		c.assertTrue(low != null && Math.abs(low.y - ground.y) < 0.3, "2 blocks higher it lands on the same floor: " + low + " vs " + ground);
		c.assertTrue(SafeTeleport.spotOrNull(c.getLevel(), z, c.absoluteVec(new Vec3(2.5, 5, 2.5))) == null, "a walker never teleports high in the air (owner flying)");
		c.assertTrue(SafeTeleport.spotOrNull(c.getLevel(), z, Vec3.atBottomCenterOf(c.absolutePos(new net.minecraft.core.BlockPos(5, 1, 5)))) == null, "a walker never teleports into water (owner swimming)");
		c.assertTrue(SafeTeleport.spotOrNull(c.getLevel(), v, c.absoluteVec(new Vec3(2.5, 5, 2.5))) != null, "a flier may teleport into the air");
		z.discard();
		v.discard();
		c.succeed();
	}

	/** Huy's bug: grasped undead burned in the sun (D134 says they never do). A plain zombie next to it must burn. */
	@GameTest(maxTicks = 260)
	public void aGraspedZombieDoesNotBurnInTheSun(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		c.getLevel().getServer().getCommands().performPrefixedCommand(c.getLevel().getServer().createCommandSourceStack(), "time set noon");
		c.getLevel().getServer().getCommands().performPrefixedCommand(c.getLevel().getServer().createCommandSourceStack(), "weather clear");
		Zombie mine = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 5.5));
		mine.setNoAi(true);
		Grasp.tame(p, mine, 10);
		Zombie wild = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 2.5));
		wild.setNoAi(true);
		// A zombie in full sun catches fire with about 4% chance per tick: 200 ticks makes a miss very unlikely.
		c.runAfterDelay(200, () -> {
			c.assertTrue(wild.isOnFire() || !wild.isAlive(), "control: a plain zombie burns at noon");
			c.assertTrue(!mine.isOnFire(), "the grasped zombie does not burn");
			cleanUp(c, p);
			c.succeed();
		});
	}

	/** Huy's bug: a working clone walked to its owner's grasped skeleton instead of farming. Allies must skip it. */
	@GameTest(maxTicks = 60)
	public void theOwnersSummonsTreatAGraspedMobAsAnAlly(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 5.5));
		z.setNoAi(true);
		Grasp.tame(p, z, 10);
		c.assertTrue(Grasp.isGraspedBy(z, p.getUUID()), "grasped by its owner");
		c.assertTrue(!Grasp.isGraspedBy(z, java.util.UUID.randomUUID()), "not grasped by someone else");
		c.assertTrue(!io.github.huydangtruong.immortalcultivation.technique.IllusionTargets.eligibleForTest(p, z), "the owner's clones do not fight it");
		cleanUp(c, p);
		c.succeed();
	}

	// ---- D167: grasped witch ----

	private static Witch witchOf(GameTestHelper c, ServerPlayer p, double x, double z) {
		Witch w = c.spawn(EntityTypes.WITCH, new Vec3(x, 1, z));
		w.setPersistenceRequired();
		Grasp.tame(p, w, 20);
		return w;
	}

	/** A hurt owner gets Healing thrown at it by its own witch. */
	@GameTest(maxTicks = 300)
	public void aGraspedWitchHealsItsHurtOwner(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		FlightBladeGameTest.place(c, p, 4, 1, 4, -90f, 0f);
		p.getFoodData().setFoodLevel(6); // below 18: no natural regeneration, only the potion can heal
		p.setHealth(4f);
		witchOf(c, p, 1.5, 4);
		c.runAfterDelay(200, () -> {
			c.assertTrue(p.getHealth() > 4.5f, "the witch healed its owner, health " + p.getHealth());
			cleanUp(c, p);
			c.succeed();
		});
	}

	/** An undead enemy gets Healing (it hurts), the owner next to it gets nothing harmful from the splash. */
	@GameTest(maxTicks = 300)
	public void aGraspedWitchHurtsAZombieAndSparesTheOwnerBesideIt(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		FlightBladeGameTest.place(c, p, 6, 1, 3.2, -90f, 0f); // beside the zombie, inside the splash
		p.getFoodData().setFoodLevel(6);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6, 1, 4.2));
		z.setNoAi(true);
		z.setPersistenceRequired();
		Witch w = witchOf(c, p, 1.5, 6.5);
		w.setTarget(z);
		float ownerHp = p.getHealth();
		c.runAfterDelay(200, () -> {
			c.assertTrue(z.getHealth() < z.getMaxHealth() || !z.isAlive(), "the zombie was hurt by a healing splash, health " + z.getHealth());
			c.assertTrue(p.getHealth() >= ownerHp, "the owner lost no health, " + p.getHealth());
			c.assertTrue(p.getActiveEffects().isEmpty(), "the owner has no potion effect, has " + p.getActiveEffects());
			cleanUp(c, p);
			c.succeed();
		});
	}

	/** A living enemy gets a harmful potion (Slowness first); the owner beside it, inside the splash, gets no effect at all. */
	@GameTest(maxTicks = 300)
	public void aGraspedWitchSplashNeverTouchesTheOwnerBesideItsEnemy(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		FlightBladeGameTest.place(c, p, 6, 1, 3.2, -90f, 0f);
		p.getFoodData().setFoodLevel(6);
		net.minecraft.world.entity.animal.pig.Pig pig = c.spawn(EntityTypes.PIG, new Vec3(6, 1, 4.2));
		pig.setNoAi(true);
		pig.setPersistenceRequired();
		Witch w = witchOf(c, p, 1.5, 6.5);
		w.setTarget(pig);
		float ownerHp = p.getHealth();
		c.runAfterDelay(200, () -> {
			c.assertTrue(pig.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) || pig.getHealth() < pig.getMaxHealth() || !pig.isAlive(),
				"the enemy got a harmful potion");
			c.assertTrue(p.getHealth() >= ownerHp, "the owner lost no health, " + p.getHealth());
			c.assertTrue(p.getActiveEffects().isEmpty(), "the owner has no potion effect, has " + p.getActiveEffects());
			cleanUp(c, p);
			c.succeed();
		});
	}

	// ---- D168: the All row ----

	private static Zombie[] three(GameTestHelper c, ServerPlayer p) {
		Zombie[] zs = new Zombie[3];
		for (int i = 0; i < zs.length; i++) {
			zs[i] = c.spawn(EntityTypes.ZOMBIE, new Vec3(0.8 + i * 0.4, 1, 0.8));
			zs[i].setNoAi(true);
			Grasp.tame(p, zs[i], 20);
		}
		return zs;
	}

	private static double flat(Zombie z, ServerPlayer p) {
		return Math.sqrt(z.distanceToSqr(p.getX(), z.getY(), p.getZ()));
	}

	@GameTest(maxTicks = 60)
	public void stayAllAndCallAllChangeEveryMob(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		FlightBladeGameTest.place(c, p, 5, 1, 5, -90f, 0f);
		Zombie[] zs = three(c, p);
		Grasp.handle(p, Grasp.ALL_MODE, "", 1);
		for (Zombie z : zs) {
			c.assertTrue(z.getAttached(GraspedMob.ATTACHMENT).mode() == 1, "every mob is on Stay");
		}
		Grasp.handle(p, Grasp.ALL_MODE, "", 2);
		for (Zombie z : zs) {
			c.assertTrue(z.getAttached(GraspedMob.ATTACHMENT).mode() == 2, "every mob is on Guard");
		}
		Grasp.handle(p, Grasp.ALL_CALL, "", 0);
		for (Zombie z : zs) {
			double d = flat(z, p);
			c.assertTrue(d >= 2.3 && d <= 3.7, "called to its own ring spot, " + d + " blocks from the owner");
		}
		// the cooldown is running: a second Call all moves nobody
		Vec3 far = c.absoluteVec(new Vec3(0.8, 1, 0.8));
		zs[0].snapTo(far.x, far.y, far.z);
		Grasp.handle(p, Grasp.ALL_CALL, "", 0);
		c.assertTrue(flat(zs[0], p) > 4, "a mob on cooldown stays where it is");
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 60)
	public void releaseAllLetsEveryMobGo(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		Zombie[] zs = three(c, p);
		c.assertTrue(held(c, p) == 3, "three held");
		Grasp.handle(p, Grasp.ALL_RELEASE, "", 0);
		c.assertTrue(held(c, p) == 0, "none held after Release all");
		for (Zombie z : zs) {
			c.assertTrue(!z.hasAttached(GraspedMob.ATTACHMENT), "attachment gone");
		}
		cleanUp(c, p);
		c.succeed();
	}

	// ---- D198: call from anywhere ----

	/** Frees whatever a D198 test left behind: the mob (wherever it is), its entry, the player. */
	private static void cleanUpAny(GameTestHelper c, ServerPlayer p) {
		for (var e : GraspData.of(c.getLevel().getServer()).get(p.getUUID()).mobs().toArray(new GraspData.Entry[0])) {
			if (c.getLevel().getServer().overworld().getEntityInAnyDimension(e.mob()) instanceof Mob m) {
				Grasp.releaseMob(m, false);
				m.discard();
			}
			GraspData.of(c.getLevel().getServer()).remove(p.getUUID(), e.mob());
		}
		FlightBladeGameTest.drop(c, p);
	}

	private static Entity anywhere(GameTestHelper c, UUID id) {
		return c.getLevel().getServer().overworld().getEntityInAnyDimension(id);
	}

	@GameTest(maxTicks = 100)
	public void aMobInTheNetherIsCalledToTheOwnerAndStaysGrasped(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		ServerLevel nether = c.getLevel().getServer().getLevel(Level.NETHER);
		nether.getChunk(0, 0);
		Zombie z = EntityTypes.ZOMBIE.create(nether, EntitySpawnReason.COMMAND);
		z.snapTo(8.5, 80, 8.5);
		z.setNoAi(true);
		nether.addFreshEntity(z);
		Grasp.tame(p, z, 20);
		UUID id = z.getUUID();
		int number = z.getAttached(GraspedMob.ATTACHMENT).number();
		c.assertTrue(z.level() == nether, "starts in the Nether");
		Grasp.handle(p, Grasp.CALL, id.toString(), 0);
		c.runAfterDelay(5, () -> {
			Entity now = anywhere(c, id);
			c.assertTrue(now instanceof Zombie, "the mob exists after the trip");
			c.assertTrue(now.level() == p.level(), "it is in the owner's dimension");
			c.assertTrue(now.distanceTo(p) < 4, "it stands next to the owner, " + now.distanceTo(p));
			GraspedMob ga = now.getAttached(GraspedMob.ATTACHMENT);
			c.assertTrue(ga != null && ga.owner().equals(p.getUUID()) && ga.number() == number, "still grasped with its number");
			c.assertTrue(((Zombie) now).getAttribute(Attributes.MAX_HEALTH).getModifier(Grasp.BONUS_ID) != null, "health bonus kept");
			c.assertTrue(((Zombie) now).getCustomName() != null && ((Zombie) now).getCustomName().getString().endsWith(" " + number), "name kept");
			c.assertTrue(held(c, p) == 1, "the slot is still taken (no death, no release), held " + held(c, p));
			c.assertTrue(GraspData.of(c.getLevel().getServer()).find(p.getUUID(), id) != null, "its entry is still there");
			c.assertTrue(Grasp.loadedOf(p.getUUID()).size() == 1 && Grasp.loadedOf(p.getUUID()).get(0) == now, "the new copy is the loaded one");
			c.assertTrue(nether.getEntity(id) == null, "the old copy is gone from the Nether");
			// the cooldown runs: moved away, a second call does nothing
			Vec3 far = c.absoluteVec(new Vec3(0.8, 1, 0.8));
			now.snapTo(far.x, far.y, far.z);
			Grasp.handle(p, Grasp.CALL, id.toString(), 0);
			c.assertTrue(now.distanceTo(p) > 4, "the Call cooldown is running");
			cleanUpAny(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void callAllBringsAMobFromTheNetherToItsRingSpot(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		FlightBladeGameTest.place(c, p, 5, 1, 5, -90f, 0f);
		ServerLevel nether = c.getLevel().getServer().getLevel(Level.NETHER);
		nether.getChunk(0, 0);
		Zombie z = EntityTypes.ZOMBIE.create(nether, EntitySpawnReason.COMMAND);
		z.snapTo(8.5, 80, 8.5);
		z.setNoAi(true);
		nether.addFreshEntity(z);
		Grasp.tame(p, z, 20);
		UUID id = z.getUUID();
		Grasp.handle(p, Grasp.ALL_CALL, "", 0);
		c.runAfterDelay(5, () -> {
			Entity now = anywhere(c, id);
			c.assertTrue(now != null && now.level() == p.level(), "the mob came into the owner's dimension");
			double d = flat((Zombie) now, p);
			c.assertTrue(d >= 2.3 && d <= 3.7, "on its ring spot, " + d + " blocks from the owner");
			c.assertTrue(now.hasAttached(GraspedMob.ATTACHMENT) && held(c, p) == 1, "still grasped, slot kept");
			cleanUpAny(c, p);
			c.succeed();
		});
	}

	/**
	 * Spawns a grasped zombie in a far chunk once that chunk is entity-ticking (a few ticks after its ticket), then gives the
	 * ticket back so the chunk unloads. Call it every tick; it returns the mob's uuid once the mob is spawned, else null.
	 */
	private static UUID farMob(GameTestHelper c, ServerPlayer p, ChunkPos at, UUID[] made) {
		if (made[0] != null) {
			return made[0];
		}
		ServerLevel lv = c.getLevel();
		lv.getChunkSource().addTicketWithRadius(TicketType.PORTAL, at, 2);
		if (!lv.isPositionEntityTicking(at.getWorldPosition().offset(8, 80, 8))) {
			return null;
		}
		Zombie z = EntityTypes.ZOMBIE.create(lv, EntitySpawnReason.COMMAND);
		z.snapTo(at.getMiddleBlockX() + 0.5, 80, at.getMiddleBlockZ() + 0.5);
		z.setNoAi(true);
		c.assertTrue(lv.addFreshEntity(z), "the far mob was added");
		Grasp.tame(p, z, 20);
		Grasp.handle(p, Grasp.MODE, z.getUUID().toString(), 1); // Stay: a follower that loads would walk to its owner on its own
		lv.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, at, 2);
		made[0] = z.getUUID();
		return made[0];
	}

	@GameTest(maxTicks = 12000)
	public void aMobInAnUnloadedChunkComesWhenCalledAndTheTicketIsReleased(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		ServerLevel lv = c.getLevel();
		ChunkPos far = new ChunkPos(2000, 2000);
		Grasp.setCallTimeout(1200); // the test server ticks far faster than real time, so loading needs more ticks than in a game
		UUID[] made = new UUID[1];
		int[] phase = {0, 0};
		long t0 = System.nanoTime();
		c.onEachTick(() -> {
			phase[1]++;
			UUID id = farMob(c, p, far, made);
			if (id == null) {
				return;
			}
			if (phase[0] == 0 && phase[1] < 3) {
				return; // the ticket was just given back
			}
			if (phase[0] == 0 && anywhere(c, id) == null) {
				c.assertTrue(lv.getChunkSource().getChunkNow(far.x(), far.z()) == null, "the far chunk is unloaded");
				c.assertTrue(held(c, p) == 1, "the entry is kept while the mob is unloaded");
				Grasp.handle(p, Grasp.CALL, id.toString(), 0);
				phase[0] = 1;
				phase[1] = 0;
			} else if (phase[0] == 1 && anywhere(c, id) instanceof Zombie z && z.level() == p.level() && z.distanceTo(p) < 4) {
				c.assertTrue(z.distanceTo(p) < 4, "it came next to the owner, " + z.distanceTo(p));
				c.assertTrue(z.hasAttached(GraspedMob.ATTACHMENT) && held(c, p) == 1, "still grasped");
				phase[0] = 2;
				phase[1] = 0;
			} else if (phase[0] == 2 && !lv.isPositionEntityTicking(far.getWorldPosition().offset(8, 80, 8))) {
				cleanUpAny(c, p);
				c.succeed(); // the temporary ticket was given back: the far chunk no longer ticks
			} else if (phase[0] == 1 && (System.nanoTime() - t0) > 30_000_000_000L) {
				c.fail("the mob never came");
			} else if (phase[0] == 2 && phase[1] > 60) {
				c.fail("the far chunk keeps ticking: the ticket leaked");
			} else if (phase[0] == 0 && (System.nanoTime() - t0) > 30_000_000_000L) {
				c.fail("the far chunk never unloaded, so the test cannot start");
			}
		});
	}

	@GameTest(maxTicks = 12000)
	public void aMobThatIsNotWhereItWasSeenIsNotFoundAndNoCooldownIsSpent(GameTestHelper c) {
		ServerPlayer p = grasper(c, 20);
		ServerLevel lv = c.getLevel();
		ChunkPos real = new ChunkPos(2100, 2100), wrong = new ChunkPos(2200, 2200);
		Grasp.setCallTimeout(GraspCalls.TIMEOUT_TICKS);
		UUID[] made = new UUID[1];
		GraspData.Entry[] saved = new GraspData.Entry[1];
		GraspData data = GraspData.of(lv.getServer());
		int[] phase = {0, 0};
		long t0 = System.nanoTime();
		c.onEachTick(() -> {
			phase[1]++;
			UUID id = farMob(c, p, real, made);
			if (id == null) {
				return;
			}
			GraspData.Entry right = phase[0] == 0 ? data.find(p.getUUID(), id) : null;
			if (phase[0] == 0 && phase[1] < 3) {
				return; // the ticket was just given back
			}
			if (phase[0] == 0 && anywhere(c, id) == null) {
				saved[0] = right;
				data.replace(p.getUUID(), new GraspData.Entry(id, right.type(), right.number(), right.mode(), right.dim(), wrong.getMiddleBlockX(), 80, wrong.getMiddleBlockZ(),
					right.hp(), right.maxHp(), right.bonus(), right.armor(), right.damage(), right.armorGear(), right.weaponGear()));
				Grasp.handle(p, Grasp.CALL, id.toString(), 0);
				phase[0] = 1;
				phase[1] = 0;
			} else if (phase[0] == 1 && phase[1] == 130) {
				c.assertTrue(anywhere(c, id) == null, "the mob did not come (it is elsewhere)");
				c.assertTrue(held(c, p) == 1, "the slot is untouched");
				c.assertTrue(!lv.isPositionEntityTicking(wrong.getWorldPosition().offset(8, 80, 8)), "the wrong chunk was given back");
				data.replace(p.getUUID(), saved[0]); // seen again where it really is: calling works at once (no cooldown was spent)
				Grasp.handle(p, Grasp.CALL, id.toString(), 0);
				phase[0] = 2;
				phase[1] = 0;
			} else if (phase[0] == 2 && anywhere(c, id) instanceof Zombie z && z.level() == p.level() && z.distanceTo(p) < 4) {
				c.assertTrue(z.distanceTo(p) < 4, "it came once the right place was known");
				cleanUpAny(c, p);
				c.succeed();
			} else if (phase[0] == 2 && (System.nanoTime() - t0) > 30_000_000_000L) {
				c.fail("the second call never brought the mob: a cooldown was spent or the ticket is stuck");
			} else if (phase[0] == 0 && (System.nanoTime() - t0) > 30_000_000_000L) {
				c.fail("the far chunk never unloaded, so the test cannot start");
			}
		});
	}
}
