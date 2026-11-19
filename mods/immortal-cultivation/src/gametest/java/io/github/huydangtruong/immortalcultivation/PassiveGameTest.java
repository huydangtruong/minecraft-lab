package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.passive.CropGrowth;
import io.github.huydangtruong.immortalcultivation.passive.Passive;
import io.github.huydangtruong.immortalcultivation.passive.PassiveAttachment;
import io.github.huydangtruong.immortalcultivation.passive.PassiveData;
import io.github.huydangtruong.immortalcultivation.passive.PassiveNet;
import io.github.huydangtruong.immortalcultivation.passive.PassiveRules;
import io.github.huydangtruong.immortalcultivation.passive.Passives;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server side of the Passives tab (P-057, D148). */
public class PassiveGameTest {
	private static ServerPlayer player(GameTestHelper c) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(ImmortalCultivation.ENABLED, true, s);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		PassiveAttachment.set(p, PassiveData.DEFAULT);
		p.experienceLevel = 0;
		p.experienceProgress = 0f;
		p.totalExperience = 0;
		return p;
	}

	private static void set(ServerPlayer p, Passive passive, int level) {
		PassiveAttachment.set(p, PassiveAttachment.get(p).withLevel(passive, level));
	}

	@GameTest(maxTicks = 100)
	public void upgradeCostsFiveLevelsAndStopsAtTen(GameTestHelper c) {
		ServerPlayer p = player(c);
		p.giveExperienceLevels(7);
		c.assertTrue(PassiveNet.apply(p, PassiveNet.UPGRADE, "cooldown") == null, "upgrade with 7 levels should work");
		c.assertTrue(PassiveAttachment.get(p).level(Passive.COOLDOWN) == 1, "cooldown should be Lv 1");
		c.assertTrue(p.experienceLevel == 2, "5 levels paid, 2 left, was " + p.experienceLevel);
		c.assertTrue(PassiveNet.apply(p, PassiveNet.UPGRADE, "cooldown") != null, "2 levels are not enough");
		c.assertTrue(PassiveAttachment.get(p).level(Passive.COOLDOWN) == 1 && p.experienceLevel == 2, "a refusal changes nothing");
		p.giveExperienceLevels(50);
		set(p, Passive.XP, PassiveRules.MAX_LEVEL);
		c.assertTrue(PassiveNet.apply(p, PassiveNet.UPGRADE, "xp") != null, "Lv 11 must be refused");
		c.assertTrue(PassiveNet.apply(p, PassiveNet.UPGRADE, "nope") != null, "unknown id must be refused");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void visionTogglesOnlyWhenLearned(GameTestHelper c) {
		ServerPlayer p = player(c);
		c.assertTrue(PassiveNet.apply(p, PassiveNet.TOGGLE_VISION, "vision") != null, "nothing to toggle at Lv 0");
		set(p, Passive.VISION, 3);
		c.assertTrue(PassiveAttachment.get(p).visionOn(), "on by default");
		c.assertTrue(PassiveNet.apply(p, PassiveNet.TOGGLE_VISION, "vision") == null, "toggle works");
		c.assertTrue(!PassiveAttachment.get(p).visionOn(), "now off");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void cooldownIsShortened(GameTestHelper c) {
		ServerPlayer p = player(c);
		c.assertTrue(Passives.cooldown(p, 200) == 200, "no passive, no change");
		set(p, Passive.COOLDOWN, 10);
		c.assertTrue(Passives.cooldown(p, 200) == 167, "Lv 10 takes 16.5% off, was " + Passives.cooldown(p, 200));
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void foodNeverDropsBelowTheFloor(GameTestHelper c) {
		ServerPlayer p = player(c);
		set(p, Passive.HUNGER, 10);
		p.getFoodData().setFoodLevel(0);
		p.getFoodData().tick(p);
		c.assertTrue(p.getFoodData().getFoodLevel() == 14, "Lv 10 holds 7 drumsticks, was " + p.getFoodData().getFoodLevel());
		p.getFoodData().setFoodLevel(18);
		p.getFoodData().tick(p);
		c.assertTrue(p.getFoodData().getFoodLevel() == 18, "above the floor nothing changes");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void orbsGiveMoreXp(GameTestHelper c) {
		ServerPlayer p = player(c);
		set(p, Passive.XP, 10);
		ExperienceOrb orb = new ExperienceOrb(c.getLevel(), p.getX(), p.getY(), p.getZ(), 100);
		c.getLevel().addFreshEntity(orb);
		orb.playerTouch(p);
		c.assertTrue(p.totalExperience == 130, "100 XP +30% = 130, was " + p.totalExperience);
		p.giveExperiencePoints(100); // not from an orb: no bonus
		c.assertTrue(p.totalExperience == 230, "commands and refunds get no bonus, was " + p.totalExperience);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void cropHarvestXpGetsTheBonus(GameTestHelper c) {
		player(c); // switches the mod on
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c); // harvest XP skips Creative
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		set(p, Passive.XP, 10);
		ImmortalCultivation.onCropHarvest(p, 100);
		c.assertTrue(p.totalExperience == 130, "100 crops x 1 XP +30% = 130, was " + p.totalExperience);
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void protectedSlotsSurviveDeath(GameTestHelper c) {
		ServerPlayer p = player(c);
		set(p, Passive.INVENTORY, 7); // main hand, offhand, 4 armor, hotbar
		var inv = p.getInventory();
		inv.setSelectedSlot(2);
		inv.setItem(2, new ItemStack(Items.DIAMOND_SWORD));
		inv.setItem(5, new ItemStack(Items.BREAD, 7));
		inv.setItem(40, new ItemStack(Items.SHIELD));
		inv.setItem(39, new ItemStack(Items.IRON_HELMET));
		inv.setItem(20, new ItemStack(Items.DIRT, 64)); // row 2: not protected at Lv 7
		p.setHealth(0f);
		p.die(c.getLevel().damageSources().generic());
		ServerPlayer r = c.getLevel().getServer().getPlayerList().respawn(p, false, Entity.RemovalReason.KILLED);
		var ri = r.getInventory();
		c.assertTrue(ri.getItem(2).is(Items.DIAMOND_SWORD), "main hand kept");
		c.assertTrue(ri.getItem(5).is(Items.BREAD) && ri.getItem(5).getCount() == 7, "hotbar kept");
		c.assertTrue(ri.getItem(40).is(Items.SHIELD), "offhand kept");
		c.assertTrue(ri.getItem(39).is(Items.IRON_HELMET), "helmet kept");
		c.assertTrue(ri.getItem(20).isEmpty(), "row 2 dropped");
		c.assertTrue(PassiveAttachment.get(r).level(Passive.INVENTORY) == 7, "passive levels survive death");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void plantedCropsGrowFasterForTheOwner(GameTestHelper c) {
		ServerPlayer p = player(c);
		BlockPos soil = c.absolutePos(new BlockPos(1, 1, 1));
		BlockPos crop = soil.above();
		c.getLevel().setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
		c.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
		RandomSource r = RandomSource.create(1);
		set(p, Passive.GROWTH, 10);
		CropGrowth.remember(p);
		c.assertTrue(CropGrowth.extraTicks(c.getLevel(), crop, r) == 0, "nobody planted it: no bonus");
		CropGrowth.planted(c.getLevel(), crop, p.getUUID());
		c.assertTrue(CropGrowth.extraTicks(c.getLevel(), crop, r) == 2, "Lv 10 = +200% = two extra attempts");
		c.assertTrue(PassiveNet.apply(p, PassiveNet.TOGGLE, "growth") == null, "growth can be switched");
		c.assertTrue(CropGrowth.extraTicks(c.getLevel(), crop, r) == 0, "switched off: no bonus");
		PassiveNet.apply(p, PassiveNet.TOGGLE, "growth");
		c.assertTrue(CropGrowth.extraTicks(c.getLevel(), crop, r) == 2, "back on");
		CropGrowth.planted(c.getLevel(), soil, p.getUUID());
		c.assertTrue(CropGrowth.extraTicks(c.getLevel(), soil, r) == 0, "farmland is not a crop");
		p.gameMode.destroyBlock(crop);
		c.assertTrue(CropGrowth.extraTicks(c.getLevel(), crop, r) == 0, "breaking the crop forgets the owner");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void extraAttemptsGrowTheCrop(GameTestHelper c) {
		ServerPlayer p = player(c);
		BlockPos soil = c.absolutePos(new BlockPos(1, 1, 1));
		BlockPos crop = soil.above();
		c.getLevel().setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
		c.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
		set(p, Passive.GROWTH, 10);
		CropGrowth.planted(c.getLevel(), crop, p.getUUID());
		// count how often vanilla random ticks advance the crop, with and without the bonus
		RandomSource r = RandomSource.create(7);
		int grown = 0;
		for (int i = 0; i < 400; i++) {
			c.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
			c.getLevel().getBlockState(crop).randomTick(c.getLevel(), crop, r);
			if (c.getLevel().getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE) > 0) {
				grown++;
			}
		}
		set(p, Passive.GROWTH, 0);
		int plain = 0;
		for (int i = 0; i < 400; i++) {
			c.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
			c.getLevel().getBlockState(crop).randomTick(c.getLevel(), crop, r);
			if (c.getLevel().getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE) > 0) {
				plain++;
			}
		}
		c.assertTrue(grown > plain, "Lv 10 should grow more often: " + grown + " vs " + plain);
		c.succeed();
	}
}
