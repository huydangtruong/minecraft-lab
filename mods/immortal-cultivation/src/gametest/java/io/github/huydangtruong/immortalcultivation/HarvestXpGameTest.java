package io.github.huydangtruong.immortalcultivation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** P-055 (D140): a player breaking crops in their last two growth stages gets 1 vanilla XP each (no HeartXP). */
public class HarvestXpGameTest {
	private static void crop(GameTestHelper c, int x, int age) {
		c.setBlock(x, 0, 2, Blocks.FARMLAND);
		c.setBlock(x, 1, 2, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age));
	}

	private static void breakAt(GameTestHelper c, ServerPlayer p, int x) {
		p.gameMode.destroyBlock(c.absolutePos(new BlockPos(x, 1, 2)));
	}

	@GameTest(maxTicks = 60)
	public void breakingRipeWheatGivesOneXpUnripeGivesNone(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		crop(c, 1, 3);
		crop(c, 2, 6);
		crop(c, 3, 7);
		breakAt(c, p, 1);
		c.assertTrue(p.totalExperience == 0, "wheat at age 3 gives nothing, was " + p.totalExperience);
		breakAt(c, p, 2);
		c.assertTrue(p.totalExperience == 1, "age 6 (one stage short of ripe) gives 1, was " + p.totalExperience);
		breakAt(c, p, 3);
		c.assertTrue(p.totalExperience == 2, "age 7 gives 1 more, was " + p.totalExperience);
		c.assertTrue(HeartsAttachment.get(p).heartXp() == 0, "HeartXP is untouched");
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}
}
