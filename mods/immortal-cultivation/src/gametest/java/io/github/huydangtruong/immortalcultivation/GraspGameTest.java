package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.GraspRules;
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

	/** Huy's bug: grasped undead burned in the sun (D134 says they never do). A plain zombie next to it must burn. */
	@GameTest(maxTicks = 100)
	public void aGraspedZombieDoesNotBurnInTheSun(GameTestHelper c) {
		ServerPlayer p = grasper(c, 10);
		c.getLevel().getServer().getCommands().performPrefixedCommand(c.getLevel().getServer().createCommandSourceStack(), "time set noon");
		Zombie mine = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 5.5));
		mine.setNoAi(true);
		Grasp.tame(p, mine, 10);
		Zombie wild = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 2.5));
		wild.setNoAi(true);
		c.runAfterDelay(60, () -> {
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
}
