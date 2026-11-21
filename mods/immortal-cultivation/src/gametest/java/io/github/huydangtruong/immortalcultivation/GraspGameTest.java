package io.github.huydangtruong.immortalcultivation;

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
import net.minecraft.server.level.ServerPlayer;
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
}
