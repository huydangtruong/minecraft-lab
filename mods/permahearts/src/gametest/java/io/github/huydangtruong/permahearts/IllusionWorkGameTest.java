package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.Illusion;
import io.github.huydangtruong.permahearts.technique.IllusionTargets;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.illusion.FishTask;
import io.github.huydangtruong.permahearts.technique.illusion.IllusionCarry;
import io.github.huydangtruong.permahearts.technique.illusion.IllusionJob;
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
		return c.getBlockState(new BlockPos(x, 1, z)).is(Blocks.WHEAT) && c.getBlockState(new BlockPos(x, 1, z)).getValue(CropBlock.AGE) == 0;
	}

	@GameTest(maxTicks = 1200)
	public void farmHarvestsReplantsAndFillsTheChestWithoutXp(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.IRON_HOE));
		Container chest = chest(c, 2, 1, 5);
		ripeWheat(c, 9, 2);
		ripeWheat(c, 10, 2);
		ripeWheat(c, 9, 3);
		int xp = p.totalExperience;
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.FARM && a.getMainHandItem().is(Items.IRON_HOE), "the hoe sets the FARM job");
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) >= 3, "wheat in the chest, was " + count(chest, s -> s.is(Items.WHEAT)) + dbg(a));
			c.assertTrue(replanted(c, 9, 2) && replanted(c, 10, 2) && replanted(c, 9, 3), "the three crops are replanted");
			c.assertTrue(p.totalExperience == xp && c.getLevel().getEntitiesOfClass(ExperienceOrb.class, arena(c)).isEmpty(), "no XP from farming");
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
		c.setBlock(8, 4, 5, leaf);
		c.setBlock(6, 4, 5, leaf);
		// a built wall: logs touching player-placed (persistent) leaves
		var kept = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		c.setBlock(4, 1, 9, Blocks.OAK_LOG);
		c.setBlock(4, 2, 9, Blocks.OAK_LOG);
		c.setBlock(5, 2, 9, kept);
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.CHOP, "an axe sets the CHOP job");
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.OAK_LOG)) >= 4, "the four trunk logs are in the chest, was " + count(chest, s -> s.is(Items.OAK_LOG)));
			c.assertTrue(c.getBlockState(new BlockPos(4, 1, 9)).is(Blocks.OAK_LOG) && c.getBlockState(new BlockPos(4, 2, 9)).is(Blocks.OAK_LOG), "the built wall stays");
			FlightBladeGameTest.drop(c, p);
		});
	}

	@GameTest(maxTicks = 1200)
	public void shearTakesTheWoolOfASheepToTheChest(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.SHEARS));
		Container chest = chest(c, 2, 1, 5);
		var sheep = c.spawn(EntityTypes.SHEEP, new Vec3(7.5, 1, 7.5));
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
		c.spawn(EntityTypes.COW, new Vec3(7.5, 1, 7.5));
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.MILK, "a bucket sets the MILK job");
		c.succeedWhen(() -> {
			c.assertTrue(count(chest, s -> s.is(Items.MILK_BUCKET)) == 1, "one milk bucket in the chest");
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
	public void breedSplitsTheOwnersFoodUsesTheChestFirstAndGivesNoXp(GameTestHelper c) {
		ServerPlayer p = owner(c, new ItemStack(Items.WHEAT, 64));
		Container chest = chest(c, 2, 1, 5);
		chest.setItem(0, new ItemStack(Items.WHEAT, 2));
		Cow one = c.spawn(EntityTypes.COW, new Vec3(7.5, 1, 7.5));
		Cow two = c.spawn(EntityTypes.COW, new Vec3(6.5, 1, 8.5));
		Illusion a = castOne(c, p);
		c.assertTrue(a.job() == IllusionJob.BREED, "wheat sets the BREED job");
		c.assertTrue(p.getMainHandItem().getCount() == 32 && a.carry().count(s -> s.is(Items.WHEAT)) == 32, "64 wheat is split 32 for the owner and 32 for the clone");
		boolean[] checked = {false};
		c.succeedWhen(() -> {
			if (!checked[0]) {
				c.assertTrue(one.isInLove() && two.isInLove(), "both cows in love" + dbg(a) + " c1=" + one.isInLove() + " c2=" + two.isInLove() + " carry=" + a.carry().count(x -> true));
				c.assertTrue(count(chest, s -> s.is(Items.WHEAT)) == 0 && a.carry().count(s -> s.is(Items.WHEAT)) == 32, "the 2 chest wheat were used first, the clone still has its 32");
				checked[0] = true;
			}
			List<Cow> cows = c.getLevel().getEntitiesOfClass(Cow.class, arena(c), Cow::isBaby);
			c.assertTrue(!cows.isEmpty(), "a baby cow was born");
			c.assertTrue(c.getLevel().getEntitiesOfClass(ExperienceOrb.class, arena(c)).isEmpty(), "no breeding XP orb");
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
		ripeWheat(c, 10, 2);
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
			c.assertTrue(replanted(c, 10, 2) && count(chest, s -> s.is(Items.WHEAT)) >= 1, "back at work: the crop is harvested and replanted" + dbg(a));
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
