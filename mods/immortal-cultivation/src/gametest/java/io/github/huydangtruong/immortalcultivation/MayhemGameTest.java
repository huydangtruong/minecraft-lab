package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Mayhem;
import io.github.huydangtruong.immortalcultivation.technique.MayhemBlocks;
import io.github.huydangtruong.immortalcultivation.technique.MayhemRules;
import io.github.huydangtruong.immortalcultivation.technique.MayhemSavedData;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** P-064: Crystal Mayhem (D174) on the server. The caster looks along +Z from (5.5, 1, 0.5). */
public class MayhemGameTest {
	private static ServerPlayer caster(GameTestHelper c, int level) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 0.5, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.MAYHEM, level, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.MAYHEM));
		return p;
	}

	private static Zombie zombie(GameTestHelper c, double x, double z) {
		Zombie e = c.spawn(EntityTypes.ZOMBIE, new Vec3(x, 1, z));
		e.setPersistenceRequired();
		e.setNoAi(true);
		e.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.STONE_BUTTON)); // no sunburn
		return e;
	}

	private static long now(GameTestHelper c) {
		return c.getLevel().getServer().overworld().getGameTime();
	}

	private static MayhemSavedData data(GameTestHelper c) {
		return MayhemSavedData.of(c.getLevel().getServer());
	}

	private static long count(GameTestHelper c, ServerPlayer p, int kind) {
		return data(c).blocks.stream().filter(e -> e.kind == kind && e.owner.equals(p.getUUID().toString())).count();
	}

	private static void cluster(GameTestHelper c, ServerPlayer p, int x, int z) {
		Mayhem.placeCluster(p, c.getLevel(), c.absolutePos(new BlockPos(x, 1, z)), Direction.UP, now(c));
	}

	private static int blocksLeft(GameTestHelper c) {
		int n = 0;
		for (int x = -2; x < 14; x++) {
			for (int y = 0; y < 5; y++) {
				for (int z = -2; z < 14; z++) {
					var s = c.getBlockState(new BlockPos(x, y, z));
					if (s.is(MayhemBlocks.CLUSTER) || s.is(MayhemBlocks.BUDDING)) {
						n++;
					}
				}
			}
		}
		return n;
	}

	private static void finish(GameTestHelper c, ServerPlayer p) {
		Mayhem.end(p);
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 80)
	public void aShardHurtsByTheLightAtTheTargetAndLeavesACluster(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		Zombie z = zombie(c, 5.5, 4.5);
		c.runAfterDelay(5, () -> {
			var level = c.getLevel();
			BlockPos at = z.blockPosition();
			int sky = MayhemRules.skyLevel(level.getBrightness(LightLayer.SKY, at), level.getSkyDarken(), level.getOverworldClockTime());
			int block = level.getBrightness(LightLayer.BLOCK, at);
			double want = MayhemRules.shardDamage(1, sky, block);
			c.assertTrue(TechniqueNet.cast(p, 0, 0f) == null, "the left click fires a shard");
			c.assertTrue(Math.abs(20 - z.getHealth() - want) < 0.01, "the zombie lost " + (20 - z.getHealth()) + ", the light (sky " + sky + ", block " + block + ") says " + want);
			long t = level.getOverworldClockTime() % 24000;
			if (t < MayhemRules.NIGHT_FROM || t >= MayhemRules.NIGHT_TO) {
				c.assertTrue(sky == 0 || want > 2, "daylight adds damage when the sky is lit (sky " + sky + ")");
			}
			c.assertTrue(c.getBlockState(new BlockPos(5, 1, 4)).is(MayhemBlocks.CLUSTER), "a cluster stands under the zombie");
			c.assertTrue(Mayhem.shardsLeft(p) == 19, "one of 20 shards is used, left " + Mayhem.shardsLeft(p));
			finish(c, p);
		});
	}

	@GameTest(maxTicks = 90)
	public void aLightBlockAddsDamageAndTheDelayHoldsTheNextShard(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		Zombie z = zombie(c, 5.5, 4.5);
		c.setBlock(6, 1, 4, Blocks.SEA_LANTERN);
		c.runAfterDelay(10, () -> {
			var level = c.getLevel();
			int block = level.getBrightness(LightLayer.BLOCK, z.blockPosition());
			c.assertTrue(block >= 14, "the lantern lights the zombie's cell, block light " + block);
			Mayhem.input(p, 1, 0f);
			float lost = 20 - z.getHealth();
			Mayhem.input(p, 1, 0f); // too soon: the 1 s delay of level 1 holds it back
			c.assertTrue(Mayhem.shardsLeft(p) == 19, "the second click inside the delay fires nothing, left " + Mayhem.shardsLeft(p));
			c.assertTrue(lost >= MayhemRules.shardDamage(1, 0, block) - 0.01, "the lantern adds damage, lost " + lost);
			c.runAfterDelay(25, () -> {
				c.assertTrue(Mayhem.input(p, 1, 0f) == null && Mayhem.shardsLeft(p) == 18, "after 1 s the next shard flies");
				finish(c, p);
			});
		});
	}

	@GameTest(maxTicks = 260)
	public void aClusterGoesAfterTenSeconds(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		zombie(c, 5.5, 4.5);
		c.runAfterDelay(2, () -> {
			Mayhem.input(p, 1, 0f);
			c.assertTrue(c.getBlockState(new BlockPos(5, 1, 4)).is(MayhemBlocks.CLUSTER), "the cluster stands");
			c.assertTrue(count(c, p, MayhemSavedData.CLUSTER) == 1, "it is recorded");
		});
		c.runAfterDelay(150, () -> c.assertTrue(c.getBlockState(new BlockPos(5, 1, 4)).is(MayhemBlocks.CLUSTER), "still there after 7 s"));
		c.runAfterDelay(215, () -> {
			c.assertTrue(c.getBlockState(new BlockPos(5, 1, 4)).isAir(), "gone after 10 s");
			c.assertTrue(count(c, p, MayhemSavedData.CLUSTER) == 0, "no record is left");
			finish(c, p);
		});
	}

	@GameTest(maxTicks = 120)
	public void twentyShardsStartTheCooldownButTheChannelStaysOpenOnce(GameTestHelper c) {
		ServerPlayer p = caster(c, 20);
		for (int i = 0; i < 20; i++) {
			c.runAfterDelay(2L + i * 3L, () -> Mayhem.input(p, 20, 0f)); // level 20: a shard every 0.1 s (2 ticks)
		}
		c.runAfterDelay(80, () -> {
			c.assertTrue(Mayhem.shardsLeft(p) == 0, "all 20 shards are used, left " + Mayhem.shardsLeft(p));
			long left = TechniqueAttachment.get(p).cooldownEnd(Technique.MAYHEM) - now(c);
			c.assertTrue(left > 500 && left <= 600, "the 30 s cooldown runs, left " + left);
			c.assertTrue(Mayhem.input(p, 20, 0f) != null, "no shard is left to fire");
			c.assertTrue(Mayhem.input(p, 20, 1f) == null, "the channel still works during that cooldown");
			c.assertTrue(TechniqueAttachment.get(p).cooldownEnd(Technique.MAYHEM) == 0, "using it cancels the cooldown");
			c.assertTrue(Mayhem.shardsLeft(p) == 20 && Mayhem.windowActive(p), "20 shards again and the window runs");
			c.assertTrue(Mayhem.input(p, 20, 1f) != null, "a second channel in the same window is refused");
			finish(c, p);
		});
	}

	@GameTest(maxTicks = 60)
	public void theChannelTurnsTheNearestFiveClustersIntoLitBuddingsAndLeavesTheRest(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		FlightBladeGameTest.place(c, p, 3.5, 1, 0.5, 0f, 0f);
		for (int x = 0; x <= 6; x++) {
			cluster(c, p, x, 2);
		}
		c.assertTrue(count(c, p, MayhemSavedData.CLUSTER) == 7, "seven clusters stand, recorded " + count(c, p, MayhemSavedData.CLUSTER) + ", blocks " + blocksLeft(c) + ", at " + java.util.stream.IntStream.rangeClosed(0, 6).filter(x -> !c.getBlockState(new BlockPos(x, 1, 2)).is(MayhemBlocks.CLUSTER)).mapToObj(x -> x + "=" + c.getBlockState(new BlockPos(x, 1, 2))).toList());
		c.assertTrue(Mayhem.input(p, 1, 1f) == null, "the channel starts");
		c.runAfterDelay(3, () -> {
			c.assertTrue(count(c, p, MayhemSavedData.BUDDING) == 5, "4 + level 1 = 5 buddings, got " + count(c, p, MayhemSavedData.BUDDING));
			c.assertTrue(count(c, p, MayhemSavedData.CLUSTER) == 2, "the other two stay clusters, got " + count(c, p, MayhemSavedData.CLUSTER));
			c.assertTrue(count(c, p, MayhemSavedData.DECOR) >= 10, "buddings grow decoration, got " + count(c, p, MayhemSavedData.DECOR));
			int lit = 0;
			for (int x = 0; x <= 6; x++) {
				var s = c.getBlockState(new BlockPos(x, 1, 2));
				if (s.is(MayhemBlocks.BUDDING) && s.getValue(MayhemBlocks.Budding.LIT)) {
					lit++;
					c.assertTrue(s.getLightEmission() == 15, "an active budding gives light 15");
				}
			}
			c.assertTrue(lit == 5, "five lit buddings, got " + lit);
			c.assertTrue(Mayhem.shardsLeft(p) == 20, "20 shards refilled");
			Mayhem.input(p, 1, 0f);
			c.assertTrue(count(c, p, MayhemSavedData.CLUSTER) == 2, "a refill shard leaves no new cluster");
			finish(c, p);
		});
	}

	@GameTest(maxTicks = 120)
	public void aBuddingOutOfReachIsDarkAndHarmlessAndOverlapsBurnOnce(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		cluster(c, p, 4, 3);
		cluster(c, p, 6, 3);
		Zombie z = zombie(c, 5.5, 3.5); // within 3 blocks of both buddings
		c.assertTrue(Mayhem.input(p, 1, 1f) == null, "the channel starts");
		c.runAfterDelay(6, () -> {
			c.assertTrue(count(c, p, MayhemSavedData.BUDDING) == 2, "both clusters became buddings");
			float lost = 20 - z.getHealth();
			c.assertTrue(Math.abs(lost - 1f) < 0.01f, "two overlapping buddings burn once: 1 damage at level 1, lost " + lost);
			c.assertTrue(z.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "and slow it");
		});
		c.runAfterDelay(10, () -> FlightBladeGameTest.place(c, p, 0.5, 1, 7.5, 0f, 0f)); // 5.7+ blocks from both buddings: the links break
		c.runAfterDelay(20, () -> {
			var s = c.getBlockState(new BlockPos(4, 1, 3));
			c.assertTrue(s.is(MayhemBlocks.BUDDING) && !s.getValue(MayhemBlocks.Budding.LIT), "out of reach the budding stays but goes dark");
			c.assertTrue(s.getLightEmission() == 0, "no light");
			float at = z.getHealth();
			c.runAfterDelay(45, () -> {
				c.assertTrue(z.getHealth() == at, "no burn while no budding is linked to the player");
				finish(c, p);
			});
		});
	}

	@GameTest(maxTicks = 130)
	public void aChainOfLinksOfFiveBlocksReachesFartherThanTheCaster(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		FlightBladeGameTest.place(c, p, 0.5, 1, 0.5, 0f, 0f);
		cluster(c, p, 3, 2); // 3.6 from the player
		cluster(c, p, 6, 5); // 4.2 from the first, 7.8 from the player: reachable only through the chain
		c.assertTrue(Mayhem.input(p, 1, 1f) == null, "the channel starts");
		c.runAfterDelay(4, () -> {
			c.assertTrue(count(c, p, MayhemSavedData.BUDDING) == 2, "the chain converted both, got " + count(c, p, MayhemSavedData.BUDDING));
			var s = c.getBlockState(new BlockPos(6, 1, 5));
			c.assertTrue(s.is(MayhemBlocks.BUDDING) && s.getValue(MayhemBlocks.Budding.LIT), "the far one is linked, so lit");
			finish(c, p);
		});
	}

	@GameTest(maxTicks = 470)
	public void afterTwentySecondsEverythingGoesAndTheCooldownRuns(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		cluster(c, p, 4, 2);
		cluster(c, p, 6, 2);
		c.assertTrue(Mayhem.input(p, 1, 1f) == null, "the channel starts");
		c.runAfterDelay(10, () -> c.assertTrue(blocksLeft(c) >= 4, "buddings and decoration stand, " + blocksLeft(c) + " running " + Mayhem.running(p) + " rec " + data(c).blocks.size() + " b " + count(c, p, MayhemSavedData.BUDDING)));
		c.runAfterDelay(380, () -> {
			c.assertTrue(Mayhem.windowActive(p) && blocksLeft(c) >= 4, "still going after 19 s");
		});
		c.runAfterDelay(415, () -> {
			c.assertTrue(!Mayhem.running(p), "the run is over");
			c.assertTrue(blocksLeft(c) == 0, "no mod block is left, " + blocksLeft(c));
			c.assertTrue(data(c).blocks.stream().noneMatch(e -> e.owner.equals(p.getUUID().toString())), "no record is left");
			long left = TechniqueAttachment.get(p).cooldownEnd(Technique.MAYHEM) - now(c);
			c.assertTrue(left > 540 && left <= 600, "the 30 s cooldown runs, left " + left);
			c.assertTrue(TechniqueNet.cast(p, 0, 0f) != null, "no new shard during the cooldown");
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void aChannelWithNoClusterStillRefillsAndEndingEarlyCleansUp(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		c.assertTrue(Mayhem.input(p, 1, 1f) == null, "it starts with no cluster at all");
		c.assertTrue(Mayhem.windowActive(p) && Mayhem.shardsLeft(p) == 20, "it refills and counts the 20 s");
		cluster(c, p, 5, 3);
		c.runAfterDelay(8, () -> {
			c.assertTrue(count(c, p, MayhemSavedData.BUDDING) == 1, "a cluster made during the window is converted too");
			Mayhem.end(p); // death, dimension change and logout all end it like this
			c.assertTrue(blocksLeft(c) == 0, "everything is gone at once, " + blocksLeft(c));
			c.assertTrue(TechniqueAttachment.get(p).cooldownEnd(Technique.MAYHEM) - now(c) > 500, "the cooldown started");
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void leftOverBlocksOfAnOldRunAreSweptAway(GameTestHelper c) { // what a restart leaves behind
		ServerPlayer p = caster(c, 1);
		cluster(c, p, 5, 3);
		c.assertTrue(blocksLeft(c) == 1, "the cluster stands");
		data(c).blocks.stream().filter(e -> e.owner.equals(p.getUUID().toString())).forEach(e -> e.expiry = 0); // the server start sets this on every record
		c.runAfterDelay(12, () -> {
			c.assertTrue(blocksLeft(c) == 0, "swept, " + blocksLeft(c));
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void theCastCommandPathWorks(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		zombie(c, 5.5, 4.5);
		c.runAfterDelay(2, () -> {
			c.assertTrue(TechniqueCasts.cast(p, Technique.MAYHEM, 5, 0f), "/technique cast mayhem 5 0 fires a shard");
			c.assertTrue(Mayhem.shardsLeft(p) == 19, "a shard flew");
			finish(c, p);
		});
	}
}
