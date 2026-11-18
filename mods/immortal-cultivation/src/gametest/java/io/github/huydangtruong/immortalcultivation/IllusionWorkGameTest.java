package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.IllusionTargets;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.illusion.FishTask;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionCarry;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionJob;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * P-025 (D73): Illusion work modes. The AI decides when things happen, so every check waits with succeedWhen and a generous
 * limit instead of looking once at a fixed tick.
 */
public class IllusionWorkGameTest {
	private static ServerPlayer owner(GameTestHelper c, ItemStack held) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		p.setHealth(20f); // a mock player starts at 0 health: it would not count as alive
		p.setItemInHand(InteractionHand.MAIN_HAND, held);
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withLevel(Technique.ILLUSION, 1, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.ILLUSION));
		return p;
	}

	private static Illusion castOne(GameTestHelper c, ServerPlayer p) {
		TechniqueCasts.cast(p, Technique.ILLUSION, 1, 1f);
		return Illusion.clonesOf(c.getLevel(), p.getUUID()).get(0);
	}

	private static Container chest(GameTestHelper c, int x, int y, int z) {
		c.setBlock(x, y, z, Blocks.CHEST);
		return (Container) c.getLevel().getBlockEntity(c.absolutePos(new BlockPos(x, y, z)));
	}

	private static int count(Container chest, Predicate<ItemStack> test) {
		return IllusionCarry.countIn(chest, test);
	}

	private static String dbg(Illusion a) {
		return " [clone alive=" + a.isAlive() + " pos=" + a.blockPosition() + " target=" + a.getTarget() + " carry=" + a.carry().count(x -> true) + " nav=" + a.getNavigation().isDone() + "]";
	}

	private static AABB arena(GameTestHelper c) {
		Vec3 a = c.absoluteVec(Vec3.ZERO);
		return new AABB(a.x - 2, a.y - 2, a.z - 2, a.x + 14, a.y + 8, a.z + 14);
	}

	private static void ripeWheat(GameTestHelper c, int x, int z) {
		c.setBlock(x, 0, z, Blocks.FARMLAND);
		c.setBlock(x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
	}

	private static boolean replanted(GameTestHelper c, int x, int z) {
		return c.getBlockState(new BlockPos(x, 1, z)).is(Blocks.WHEAT) && c.getBlockState(new BlockPos(x, 1, z)).getValue(CropBlock.AGE) < 7; // a new crop may grow a stage while the test waits
	}

	@GameTest(maxTicks = 1200)
	public void farmHarvestsReplantsFillsTheChestAndGivesTheOwnerOneXpPerCrop(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Container chest = chest(c, 2, 1, 5);
		ripeWheat(c, 6, 2); // everything inside x/z 0..7: row 8 is the barrier wall
		ripeWheat(c, 7, 2);
		ripeWheat(c, 6, 3);
		int xp = p.totalExperience;
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.FARM && a.getMainHandItem().is(Items.IRON_HOE), "the hoe sets the FARM job");
		c.succeedWhen(() -> {
			c.assertTrue(p.totalExperience >= 3 * HeartRules.CROP_XP && HeartsAttachment.get(p).heartXp() == 0, "D140: the owner got 1 vanilla XP per harvested crop and no HeartXP, was " + p.totalExperience);
			c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) >= 3, "wheat in the chest, was " + count(chest, s -> s.is(Items.WHEAT)) + dbg(a));
			c.assertTrue(replanted(c, 6, 2) && replanted(c, 7, 2) && replanted(c, 6, 3), "the three crops are replanted" + dbg(a));
			c.assertTrue(c.getLevel().getEntitiesOfClass(ExperienceOrb.class, arena(c)).isEmpty(), "no XP orb from farming");
			FlightBladeGameTest.drop(c, p);
		});
	}

	/** A field for the D84 tests: 3 empty farmland blocks, dirt and grass next to them and next to water, one ripe wheat. */
	private static void field(GameTestHelper c) {
		c.setBlock(6, 0, 2, Blocks.FARMLAND);
		c.setBlock(7, 0, 2, Blocks.FARMLAND);
		c.setBlock(6, 0, 3, Blocks.FARMLAND);
		c.setBlock(5, 0, 3, Blocks.WATER);
		c.setBlock(5, 0, 2, Blocks.DIRT);
		c.setBlock(7, 0, 3, Blocks.GRASS_BLOCK);
		c.setBlock(4, 0, 2, Blocks.DIRT);
		ripeWheat(c, 7, 4);
	}

	private static boolean field(GameTestHelper c, boolean planted) {
		boolean untilled = c.getBlockState(new BlockPos(5, 0, 2)).is(Blocks.DIRT) && c.getBlockState(new BlockPos(7, 0, 3)).is(Blocks.GRASS_BLOCK)
			&& c.getBlockState(new BlockPos(4, 0, 2)).is(Blocks.DIRT);
		return untilled && (!planted || c.getBlockState(new BlockPos(6, 1, 2)).is(Blocks.WHEAT) && c.getBlockState(new BlockPos(7, 1, 2)).is(Blocks.WHEAT)
			&& c.getBlockState(new BlockPos(6, 1, 3)).is(Blocks.WHEAT));
	}

	/** D84: the farmer never makes farmland. It plants the empty farmland it has, harvests, and takes the rest to the chest. */
	@GameTest(maxTicks = 1200)
	public void farmNeverTillsPlantsEmptyFarmlandAndDeliversTheRestToTheChest(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Container chest = chest(c, 2, 1, 5);
		field(c);
		Illusion a = castOne(c, p);
		a.carry().add(new ItemStack(Items.WHEAT_SEEDS, 10));
		c.succeedWhen(() -> {
			c.assertTrue(field(c, true), "empty farmland planted, dirt and grass untouched" + dbg(a));
			c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) >= 1, "the ripe wheat is in the chest" + dbg(a));
			c.assertTrue(count(chest, s -> s.is(Items.WHEAT_SEEDS)) >= 5 && a.carry().isEmpty(), "leftover seeds delivered, was " + count(chest, s -> s.is(Items.WHEAT_SEEDS)) + dbg(a));
			FlightBladeGameTest.drop(c, p);
		});
	}

	/** D120: no seeds in hand, empty farmland and a chest with seeds: the clone fetches them and plants. */
	@GameTest(maxTicks = 1200)
	public void farmTakesSeedsFromTheChestAndPlantsEmptyFarmland(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Container chest = chest(c, 2, 1, 5);
		chest.setItem(0, new ItemStack(Items.STICK, 3)); // other items stay
		chest.setItem(1, new ItemStack(Items.WHEAT_SEEDS, 16));
		c.setBlock(6, 0, 2, Blocks.FARMLAND);
		c.setBlock(7, 0, 2, Blocks.FARMLAND);
		c.setBlock(6, 0, 3, Blocks.FARMLAND);
		Illusion a = castOne(c, p);
		c.succeedWhen(() -> {
			c.assertTrue(c.getBlockState(new BlockPos(6, 1, 2)).is(Blocks.WHEAT) && c.getBlockState(new BlockPos(7, 1, 2)).is(Blocks.WHEAT)
				&& c.getBlockState(new BlockPos(6, 1, 3)).is(Blocks.WHEAT), "seeds fetched from the chest and planted, chest seeds=" + count(chest, x -> x.is(Items.WHEAT_SEEDS)) + " planted=" + c.getBlockState(new BlockPos(6, 1, 2)).is(Blocks.WHEAT) + c.getBlockState(new BlockPos(7, 1, 2)).is(Blocks.WHEAT) + c.getBlockState(new BlockPos(6, 1, 3)).is(Blocks.WHEAT) + dbg(a));
			c.assertTrue(count(chest, s -> s.is(Items.STICK)) == 3, "only seeds were taken");
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 1200)
	public void farmWithoutAChestDropsTheLeftoversAndNeverTills(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		field(c);
		Illusion a = castOne(c, p);
		a.carry().add(new ItemStack(Items.WHEAT_SEEDS, 10));
		c.succeedWhen(() -> {
			c.assertTrue(field(c, true), "empty farmland planted, dirt and grass untouched" + dbg(a));
			c.assertTrue(a.carry().isEmpty(), "nothing is kept" + dbg(a));
			int seeds = logItems(c, p, Items.WHEAT_SEEDS);
			c.assertTrue(seeds >= 5, "leftover seeds dropped on the ground, found " + seeds);
			c.assertTrue(logItems(c, p, Items.WHEAT) >= 1, "the wheat dropped too");
			FlightBladeGameTest.drop(c, p);
		});
	}

	/** D123: wheat one stage short of ripe (6) and beetroot at 2 are harvested with the ripe drops (wheat, not only seeds). */
	@GameTest(maxTicks = 900)
	public void farmHarvestsTheLastTwoStagesWithRipeDrops(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Container chest = chest(c, 2, 1, 5);
		c.setBlock(6, 0, 2, Blocks.FARMLAND);
		c.setBlock(6, 1, 2, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 6));
		c.setBlock(6, 0, 3, Blocks.FARMLAND);
		c.setBlock(6, 1, 3, Blocks.BEETROOTS.defaultBlockState().setValue(net.minecraft.world.level.block.BeetrootBlock.AGE, 2));
		c.setBlock(7, 0, 2, Blocks.FARMLAND);
		c.setBlock(7, 1, 2, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 5)); // too young: stays
		Illusion a = castOne(c, p);
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) >= 1, "wheat from the stage-6 crop" + dbg(a));
			c.assertTrue(count(chest, s -> s.is(Items.BEETROOT)) >= 1, "beetroot from the stage-2 crop" + dbg(a));
			c.assertTrue(c.getBlockState(new BlockPos(7, 1, 2)).getValue(CropBlock.AGE) >= 5, "the young crop is left alone");
			FlightBladeGameTest.drop(c, p);
		});
	}

	/** P-044: a ripe field the clone cannot reach (on a pillar, nearer than the free crop) must not keep it standing still. */
	@GameTest(maxTicks = 900)
	public void farmSkipsAnUnreachableFieldAndHarvestsTheRest(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Container chest = chest(c, 2, 1, 5);
		for (int x = 6; x <= 7; x++) {
			for (int z = 1; z <= 4; z++) {
				for (int y = 0; y <= 4; y++) {
					c.setBlock(x, y, z, Blocks.STONE);
				}
				c.setBlock(x, 5, z, Blocks.FARMLAND);
				c.setBlock(x, 6, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
			}
		}
		ripeWheat(c, 0, 0);
		Illusion a = castOne(c, p);
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) >= 1, "the reachable wheat reached the chest, was " + count(chest, s -> s.is(Items.WHEAT)) + " free=" + c.getBlockState(new BlockPos(0, 1, 0)) + " pillar=" + c.getBlockState(new BlockPos(6, 6, 2)) + dbg(a));
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 1200)
	public void chopBreaksANaturalTreeAndLeavesABuiltWall(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_AXE));
		Container chest = chest(c, 2, 1, 5);
		c.setBlock(7, 0, 5, Blocks.DIRT);
		for (int y = 1; y <= 4; y++) {
			c.setBlock(7, y, 5, Blocks.OAK_LOG);
		}
		var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
		c.setBlock(7, 5, 5, leaf);
		c.setBlock(7, 4, 4, leaf);
		c.setBlock(6, 4, 5, leaf);
		// a built wall: logs touching player-placed (persistent) leaves
		var kept = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		c.setBlock(3, 1, 7, Blocks.OAK_LOG);
		c.setBlock(3, 2, 7, Blocks.OAK_LOG);
		c.setBlock(4, 2, 7, kept);
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.CHOP, "an axe sets the CHOP job");
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.OAK_LOG)) >= 4, "the four trunk logs are in the chest, was " + count(chest, s -> s.is(Items.OAK_LOG)));
			c.assertTrue(c.getBlockState(new BlockPos(3, 1, 7)).is(Blocks.OAK_LOG) && c.getBlockState(new BlockPos(3, 2, 7)).is(Blocks.OAK_LOG), "the built wall stays");
			FlightBladeGameTest.drop(c, p);
		});
	}

	private static void tree(GameTestHelper c, int x, int z, int logs, net.minecraft.world.level.block.Block log) {
		c.setBlock(x, 0, z, Blocks.DIRT);
		for (int y = 1; y <= logs; y++) {
			c.setBlock(x, y, z, log);
		}
		var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
		c.setBlock(x, logs + 1, z, leaf);
		for (int y = Math.max(2, logs - 2); y <= logs; y++) { // natural leaves all round the upper logs: they block the way to them
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 0 || dz != 0) {
						c.setBlock(x + dx, y, z + dz, leaf);
					}
				}
			}
		}
	}

	private static int logsLeft(GameTestHelper c, int x, int z, int logs, net.minecraft.world.level.block.Block log) {
		int n = 0;
		for (int y = 1; y <= logs; y++) {
			if (c.getBlockState(new BlockPos(x, y, z)).is(log)) {
				n++;
			}
		}
		return n;
	}

	private static int logItems(GameTestHelper c, ServerPlayer p, net.minecraft.world.item.Item item) {
		int n = c.getLevel().getEntitiesOfClass(ItemEntity.class, arena(c), e -> e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			if (p.getInventory().getItem(i).is(item)) {
				n += p.getInventory().getItem(i).getCount();
			}
		}
		return n;
	}

	/** P-028 (D82): a level-1 clone fells a 6-log oak in 12 s, and with no chest every log ends up on the ground or with the owner. */
	@GameTest(maxTicks = 900)
	public void chopFellsASixLogOakWithin12SecondsAndDropsTheLogsWithoutAChest(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_AXE));
		tree(c, 6, 2, 6, Blocks.OAK_LOG);
		Illusion a = castOne(c, p);
		long start = c.getLevel().getGameTime();
		long[] felled = {-1};
		c.succeedWhen(() -> {
			if (felled[0] < 0 && logsLeft(c, 6, 2, 6, Blocks.OAK_LOG) == 0) {
				felled[0] = c.getLevel().getGameTime() - start;
				System.out.println("CHOPTIME oak6 ticks=" + felled[0]);
			}
			c.assertTrue(felled[0] >= 0, "the oak is felled" + dbg(a) + " left=" + logsLeft(c, 6, 2, 6, Blocks.OAK_LOG));
			c.assertTrue(felled[0] <= 240, "felled within 12 s, took " + felled[0] + " ticks");
			c.assertTrue(!a.isAlive(), "wait until the clone is gone" + dbg(a));
			int n = logItems(c, p, Items.OAK_LOG);
			c.assertTrue(n == 6, "all 6 logs dropped or picked up by the owner, found " + n);
			FlightBladeGameTest.drop(c, p);
		});
	}

	/** P-028 (D82): a tall tree is felled completely, no log left floating. */
	@GameTest(maxTicks = 900)
	public void chopFellsATallBirchCompletely(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_AXE));
		tree(c, 6, 2, 7, Blocks.BIRCH_LOG);
		var kept = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		c.setBlock(3, 1, 4, kept); // a player-placed leaf block in the work area must stay
		Illusion a = castOne(c, p);
		long start = c.getLevel().getGameTime();
		c.succeedWhen(() -> {
			int left = logsLeft(c, 6, 2, 7, Blocks.BIRCH_LOG);
			if (left == 0) {
				System.out.println("CHOPTIME birch7 ticks=" + (c.getLevel().getGameTime() - start));
			}
			c.assertTrue(left == 0, "the birch is felled, " + left + " logs left" + dbg(a));
			c.assertTrue(c.getBlockState(new BlockPos(3, 1, 4)).is(Blocks.OAK_LEAVES), "player-placed leaves are never broken");
			int leaves = 0;
			for (int y = 5; y <= 7; y++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						leaves += c.getBlockState(new BlockPos(6 + dx, y, 2 + dz)).is(Blocks.OAK_LEAVES) ? 1 : 0;
					}
				}
			}
			c.assertTrue(leaves >= 12, "only the leaves in the way are broken, not the canopy: " + leaves + " of 24 left");
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 1200)
	public void shearTakesTheWoolOfASheepToTheChest(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.SHEARS));
		Container chest = chest(c, 2, 1, 5);
		var sheep = c.spawn(EntityTypes.SHEEP, new Vec3(6.5, 1, 6.5));
		sheep.setNoAi(true); // a sheep that wanders into the corner by the barrier wall made this test flaky
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.SHEAR, "shears set the SHEAR job");
		c.succeedWhen(() -> {
			c.assertTrue(sheep.isSheared(), "the sheep was sheared" + dbg(a) + " ready=" + sheep.readyForShearing());
			c.assertTrue(count(chest, s -> s.is(ItemTags.WOOL)) >= 1, "wool in the chest");
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 1200)
	public void milkTurnsAChestBucketIntoAMilkBucket(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.BUCKET));
		Container chest = chest(c, 2, 1, 5);
		chest.setItem(0, new ItemStack(Items.BUCKET));
		Cow cow = c.spawn(EntityTypes.COW, new Vec3(6.5, 1, 6.5));
		cow.setNoAi(true); // a cow that wanders into the corner by the barrier wall made this test flaky
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.MILK, "a bucket sets the MILK job");
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.MILK_BUCKET)) == 1, "one milk bucket in the chest" + dbg(a));
			c.assertTrue(count(chest, s -> s.is(Items.BUCKET)) == 0, "the empty bucket was used up");
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 300)
	public void milkWithoutABucketInTheChestDoesNothing(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.BUCKET));
		Container chest = chest(c, 2, 1, 5);
		c.spawn(EntityTypes.COW, new Vec3(8.5, 1, 7.5));
		Illusion a = castOne(c, p);
		c.runAfterDelay(200, () -> {
			c.assertTrue(chest.isEmpty() && a.carry().isEmpty(), "no bucket, so no milk and nothing carried");
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 1500)
	public void breedSplitsTheOwnersFoodUsesTheChestFirstAndGivesTheBirthXpToTheOwnerNoOrb(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.WHEAT, 64));
		Container chest = chest(c, 2, 1, 5);
		chest.setItem(0, new ItemStack(Items.WHEAT, 2));
		Cow one = c.spawn(EntityTypes.COW, new Vec3(7.5, 1, 7.5));
		Cow two = c.spawn(EntityTypes.COW, new Vec3(5.5, 1, 7.5)); // inside x/z 0..7: row 8 is the barrier wall
		int xpBefore = p.totalExperience;
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.BREED, "wheat sets the BREED job");
		c.assertTrue(p.getMainHandItem().getCount() == 0 && a.carry().count(s -> s.is(Items.WHEAT)) == 64, "D109: all 64 wheat goes to the clone");
		boolean[] checked = {false};
		c.succeedWhen(() -> {
			if (!checked[0]) {
				c.assertTrue(one.isInLove() && two.isInLove(), "both cows in love" + dbg(a) + " c1=" + one.isInLove() + " c2=" + two.isInLove() + " carry=" + a.carry().count(x -> true));
				c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) == 0 && a.carry().count(s -> s.is(Items.WHEAT)) == 64, "the 2 chest wheat were used first, the clone still has its 64");
				checked[0] = true;
			}
			List<Cow> cows = c.getLevel().getEntitiesOfClass(Cow.class, arena(c), Cow::isBaby);
			c.assertTrue(!cows.isEmpty(), "a baby cow was born");
			c.assertTrue(c.getLevel().getEntitiesOfClass(ExperienceOrb.class, arena(c)).isEmpty(), "no breeding XP orb");
			c.assertTrue(p.totalExperience > xpBefore, "D140: the birth XP went straight to the owner");
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 1200)
	public void fishFillsTheChestWithoutXp(GameTestHelper c) {
		FishTask.testWaitTicks = 20;
		ServerPlayer p = owner(c, new ItemStack(Items.FISHING_ROD));
		Container chest = chest(c, 2, 1, 5);
		for (int x = 6; x <= 7; x++) {
			for (int z = 6; z <= 7; z++) {
				c.setBlock(x, -1, z, Blocks.BARRIER);
				c.setBlock(x, 0, z, Blocks.WATER);
			}
		}
		int xp = p.totalExperience;
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.FISH, "a rod sets the FISH job");
		c.succeedWhen(() -> {
			c.assertTrue(!chest.isEmpty(), "something caught is in the chest" + dbg(a));
			c.assertTrue(p.totalExperience == xp && c.getLevel().getEntitiesOfClass(ExperienceOrb.class, arena(c)).isEmpty(), "no XP from fishing");
			FishTask.testWaitTicks = -1;
			FlightBladeGameTest.drop(c, p);
		});
	}

	/** D85: the job comes from the first hotbar slot, not from the main hand; an empty first slot means FIGHT. */
	@GameTest(maxTicks = 60)
	public void theJobComesFromTheFirstHotbarSlotNotTheMainHand(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.AIR));
		p.getInventory().setItem(0, new ItemStack(Items.IRON_AXE));
		p.getInventory().setItem(3, new ItemStack(Items.DIAMOND_SWORD));
		p.getInventory().setSelectedSlot(3);
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.CHOP && a.getMainHandItem().is(Items.IRON_AXE), "slot 0 axe gives CHOP, was " + a.job());
		c.assertTrue(p.getInventory().getItem(0).is(Items.IRON_AXE) && p.getInventory().getItem(0).getCount() == 1, "the owner's item is untouched");
		p.getInventory().setItem(0, ItemStack.EMPTY);
		Illusion b = castOne(c, p);
		c.assertTrue(b.job() == IllusionJob.FIGHT, "an empty slot 0 gives FIGHT even with a sword in hand, was " + b.job());
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 120)
	public void theJobStaysLockedWhenTheOwnerSwitchesItems(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Illusion a = castOne(c, p);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		c.runAfterDelay(60, () -> { // the once-a-second gear refresh has run by now
			c.assertTrue(a.job() == IllusionJob.FARM && a.getMainHandItem().is(Items.IRON_HOE), "still a farmer with the hoe, has " + a.getMainHandItem());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 1500)
	public void aWorkingCloneFightsWhoHitsTheOwnerThenGoesBackToWork(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		ServerPlayer q = FlightBladeGameTest.survivor(c);
		MinecraftServer server = c.getLevel().getServer();
		boolean pvp = server.getGameRules().get(GameRules.PVP);
		server.getGameRules().set(GameRules.PVP, false, server);
		Container chest = chest(c, 2, 1, 5);
		ripeWheat(c, 7, 2);
		FlightBladeGameTest.place(c, q, 3.5, 1, 7.5, 0f, 0f);
		q.setHealth(20f);
		Illusion a = castOne(c, p);
		p.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(q), 2f); // refused, but the attacker is noted
		boolean[] fought = {false};
		c.succeedWhen(() -> {
			if (!fought[0]) {
				c.assertTrue(a.getTarget() == q, "the working clone targets the second player, was " + a.getTarget());
				fought[0] = true;
				server.getGameRules().set(GameRules.PVP, pvp, server);
				FlightBladeGameTest.drop(c, q);
			}
			c.assertTrue(replanted(c, 7, 2) && count(chest, s -> s.is(Items.WHEAT)) >= 1, "back at work: the crop is harvested and replanted" + dbg(a));
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 60)
	public void aCloneThatVanishesDropsWhatItCarries(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Illusion a = castOne(c, p);
		a.carry().add(new ItemStack(Items.DIAMOND, 3));
		Illusion.dismissAll(p);
		c.runAfterDelay(3, () -> {
			int dropped = c.getLevel().getEntitiesOfClass(ItemEntity.class, arena(c), e -> e.getItem().is(Items.DIAMOND)).stream().mapToInt(e -> e.getItem().getCount()).sum();
			c.assertTrue(dropped == 3, "the 3 diamonds were dropped, found " + dropped);
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void hostileMobsAlsoTargetTheClones(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		p.setHealth(20f);
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withLevel(Technique.ILLUSION, 20, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.ILLUSION));
		TechniqueCasts.cast(p, Technique.ILLUSION, 20, 1f);
		List<Illusion> mine = Illusion.clonesOf(c.getLevel(), p.getUUID());
		c.assertTrue(mine.size() == 3, "three clones");
		mine.forEach(m -> m.setNoAi(true));
		// A husk without AI, so nothing else moves or fights; the vanilla "nearest player" goal is asked again and again, as it
		// is while a mob has no target. Every pick is one of the 4 (owner + 3 clones): 40+ picks never all landing on the owner is (3/4)^... certain.
		var husk = c.spawn(EntityTypes.HUSK, new Vec3(7.5, 1, 5.5));
		husk.setNoAi(true);
		var goal = new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(husk, net.minecraft.world.entity.player.Player.class, true);
		int clonePicks = 0;
		int ownerPicks = 0;
		for (int i = 0; i < 1500; i++) {
			husk.setTarget(null);
			if (goal.canUse()) {
				goal.start();
				if (husk.getTarget() instanceof Illusion) {
					clonePicks++;
				} else if (husk.getTarget() == p) {
					ownerPicks++;
				}
			}
		}
		husk.discard();
		FlightBladeGameTest.drop(c, p);
		c.assertTrue(clonePicks > 0 && ownerPicks > 0, "the husk picks both the clones and the owner, clones " + clonePicks + " owner " + ownerPicks);
		c.assertTrue(clonePicks > ownerPicks, "with 3 clones to 1 owner the clones are picked more often, clones " + clonePicks + " owner " + ownerPicks);
		c.succeed();
	}
}
