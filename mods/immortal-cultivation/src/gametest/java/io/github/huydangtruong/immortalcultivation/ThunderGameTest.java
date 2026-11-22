package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.Thunder;
import io.github.huydangtruong.immortalcultivation.technique.VioletGuardian;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** P-029: Thunder Strike on the server (D86): the 1 s warning, the strike, the damage rules, mobGriefing, the sky aim. */
public class ThunderGameTest {
	private static ServerPlayer caster(GameTestHelper c, int level) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 5.5, 1, 0.5, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.THUNDER, level, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.THUNDER));
		return p;
	}

	private static Vec3 abs(GameTestHelper c, double x, double y, double z) {
		return c.absoluteVec(new Vec3(x, y, z));
	}

	private static void griefing(GameTestHelper c, boolean on) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(GameRules.MOB_GRIEFING, on, s);
	}

	@GameTest(maxTicks = 100)
	public void aStrikeAtAZombieWaitsOneSecondThenHurtsItAndBreaksBlocks(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		griefing(c, true);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(5.5, 1, 10.5));
		z.setPersistenceRequired();
		c.setBlock(5, 1, 9, Blocks.DIRT);
		float before = z.getHealth();
		Thunder.cast(p, 1, abs(c, 5.5, 2.0, 10.0));
		c.runAfterDelay(15, () -> {
			c.assertTrue(z.getHealth() >= before - 2f, "nothing hurts it during the warning, was " + z.getHealth());
			c.assertTrue(c.getBlockState(new BlockPos(5, 1, 9)).is(Blocks.DIRT), "the block is still there in the warning");
		});
		c.runAfterDelay(30, () -> {
			c.assertTrue(z.getHealth() <= before - 5f || !z.isAlive(), "the zombie took damage, health " + z.getHealth());
			c.assertTrue(c.getBlockState(new BlockPos(5, 1, 9)).isAir(), "the dirt block near the point is gone");
			c.assertTrue(p.getHealth() == p.getMaxHealth(), "the caster is not hurt");
			// mobGriefing off (the rule is global, so it is only off for the ticks around this strike)
			c.setBlock(5, 1, 9, Blocks.DIRT);
			Thunder.cast(p, 1, abs(c, 5.5, 2.6, 10.2));
			c.runAfterDelay(19, () -> griefing(c, false));
			c.runAfterDelay(24, () -> {
				griefing(c, true);
				c.assertTrue(c.getBlockState(new BlockPos(5, 1, 9)).is(Blocks.DIRT), "no block breaks with mobGriefing off");
				GameTestKit.drop(c, p);
				c.succeed();
			});
		});
	}

	@GameTest(maxTicks = 60)
	public void theCasterAtTheEdgeAndTheirGuardianAreNotHurt(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		var guardians = VioletGuardian.summon(p, 1);
		for (var g : guardians) {
			g.teleportTo(abs(c, 7.5, 1, 5.5).x, abs(c, 7.5, 1, 5.5).y, abs(c, 7.5, 1, 5.5).z);
		}
		float gHealth = guardians.get(0).getHealth();
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(5.5, 1, 7.5));
		z.setPersistenceRequired();
		z.setNoAi(true);
		float zBefore = z.getHealth();
		Thunder.cast(p, 1, abs(c, 5.5, 1, 6.5)); // 1 block from the caster: well inside the blast
		float[] atStrike = new float[1];
		c.runAfterDelay(19, () -> atStrike[0] = guardians.get(0).getHealth());
		c.runAfterDelay(30, () -> {
			c.assertTrue(p.getHealth() == p.getMaxHealth(), "the caster is never hurt, health " + p.getHealth());
			c.assertTrue(guardians.get(0).getHealth() >= atStrike[0] - 4f, "the caster's guardian is never hurt, health " + guardians.get(0).getHealth());
			c.assertTrue(z.getHealth() < zBefore || !z.isAlive(), "control: the zombie in the blast is hurt");
			guardians.forEach(g -> g.discard());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void aPigInTheBlastBecomesAZombifiedPiglin(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		Pig pig = c.spawn(EntityTypes.PIG, new Vec3(5.5, 1, 9.5));
		pig.setPersistenceRequired();
		Thunder.cast(p, 1, abs(c, 5.5, 1, 11.0)); // 1.5 blocks from the pig, not under the bolt
		c.runAfterDelay(30, () -> {
			c.assertTrue(!pig.isAlive(), "the pig is gone");
			var piglins = c.getLevel().getEntitiesOfClass(ZombifiedPiglin.class, c.getBounds().inflate(4));
			c.assertTrue(!piglins.isEmpty(), "a zombified piglin took its place");
			piglins.forEach(e -> e.discard());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void aLevelTwentyStrikeBreaksALargerCraterThanLevelOne(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		griefing(c, true);
		int[] small = new int[1];
		fill(c);
		Thunder.cast(p, 1, abs(c, 6.5, 2.5, 6.5));
		c.runAfterDelay(25, () -> {
			small[0] = air(c);
			fill(c);
			Thunder.cast(p, 20, abs(c, 6.5, 2.5, 6.5));
			c.runAfterDelay(25, () -> {
				int big = air(c);
				c.assertTrue(small[0] > 5, "level 1 breaks blocks, was " + small[0]);
				c.assertTrue(big > small[0], "level 20 breaks more than level 1: " + big + " vs " + small[0]);
				GameTestKit.drop(c, p);
				c.succeed();
			});
		});
	}

	private static void fill(GameTestHelper c) {
		for (int x = 1; x <= 11; x++) {
			for (int z = 1; z <= 11; z++) {
				for (int y = 1; y <= 5; y++) {
					c.setBlock(x, y, z, Blocks.DIRT);
				}
			}
		}
	}

	private static int air(GameTestHelper c) {
		int n = 0;
		for (int x = 1; x <= 11; x++) {
			for (int z = 1; z <= 11; z++) {
				for (int y = 1; y <= 5; y++) {
					if (c.getBlockState(new BlockPos(x, y, z)).isAir()) {
						n++;
					}
				}
			}
		}
		return n;
	}

	@GameTest(maxTicks = 40)
	public void aCastAlwaysWorksAndStartsTheCooldown(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		GameTestKit.place(c, p, 5.5, 1, 0.5, 0f, -5f);
		c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the cast is accepted, never refused");
		c.assertTrue(TechniqueAttachment.get(p).onCooldown(Technique.THUNDER, c.getLevel().getServer().overworld().getGameTime()), "the cooldown started at the cast");
		c.assertTrue(TechniqueNet.cast(p, 0, 1f) != null, "a second cast is on cooldown");
		GameTestKit.drop(c, p);
		c.succeed();
	}
}
