package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.rune.RuneEntry;
import io.github.huydangtruong.immortalcultivation.rune.RuneForgeMenu;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** D241-D242: rune levels count as enchantment levels (past conflicts); the forge inscribes for levels and merges for free. */
public class RuneGameTest {
	private static Holder<Enchantment> ench(GameTestHelper c, ResourceKey<Enchantment> k) {
		return c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(k);
	}

	@GameTest(maxTicks = 20)
	public void runesAddLevelsAndIgnoreConflicts(GameTestHelper c) {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(ench(c, Enchantments.SHARPNESS), 3);
		Runes.setInscribed(sword, List.of(new RuneEntry(ench(c, Enchantments.SHARPNESS), 3),
			new RuneEntry(ench(c, Enchantments.SMITE), 1)));
		c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(ench(c, Enchantments.SHARPNESS), sword) == 7, "3 + 4");
		c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(ench(c, Enchantments.SMITE), sword) == 1, "Smite beside Sharpness");
		c.assertTrue(Runes.slots(sword) == 2 && Runes.slots(new ItemStack(Items.IRON_INGOT)) == 0
			&& Runes.slots(new ItemStack(Items.NETHERITE_BOOTS)) == 4, "slots by material");
		c.assertTrue(!Runes.fits(sword, new RuneEntry(ench(c, Enchantments.PROTECTION), 1)), "no armor rune on a sword");
		c.assertTrue(!Runes.eligible(ench(c, Enchantments.MENDING)) && !Runes.eligible(ench(c, Enchantments.SILK_TOUCH))
			&& Runes.eligible(ench(c, Enchantments.EFFICIENCY)), "one-level enchantments have no rune");
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void theForgeChargesToInscribeAndNothingToTakeOut(GameTestHelper c) {
		GameTestKit.enable(c);
		ServerPlayer p = GameTestKit.survivor(c);
		p.giveExperienceLevels(20);
		RuneForgeMenu m = new RuneForgeMenu(1, p.getInventory(), ContainerLevelAccess.NULL);
		m.getSlot(RuneForgeMenu.GEAR).setByPlayer(new ItemStack(Items.DIAMOND_PICKAXE));
		c.assertTrue(m.gearSlots() == 2, "2 slots");
		c.assertTrue(!m.getSlot(RuneForgeMenu.RUNE0 + 2).isActive(), "third slot locked");
		ItemStack rune = Runes.rune(ench(c, Enchantments.EFFICIENCY), 3);
		c.assertTrue(m.getSlot(RuneForgeMenu.RUNE0).mayPlace(rune), "fits");
		c.assertTrue(!m.getSlot(RuneForgeMenu.RUNE0).mayPlace(Runes.rune(ench(c, Enchantments.SHARPNESS), 1)), "Sharpness not on a pickaxe");
		m.getSlot(RuneForgeMenu.RUNE0).setByPlayer(rune);
		c.assertTrue(p.experienceLevel == 5, "15 levels paid, left " + p.experienceLevel);
		ItemStack gear = m.gearStack();
		c.assertTrue(Runes.inscribed(gear).size() == 1, "inscribed");
		c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(ench(c, Enchantments.EFFICIENCY), gear) == 4, "Efficiency +4");
		c.assertTrue(!m.getSlot(RuneForgeMenu.RUNE0 + 1).mayPlace(Runes.rune(ench(c, Enchantments.EFFICIENCY), 2)), "10 levels too dear");
		m.getSlot(RuneForgeMenu.RUNE0).remove(1);
		c.assertTrue(p.experienceLevel == 5 && Runes.inscribed(m.gearStack()).isEmpty(), "taken out for free");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void twoEqualRunesMergeIntoOneLevelHigher(GameTestHelper c) {
		GameTestKit.enable(c);
		ServerPlayer p = GameTestKit.survivor(c);
		RuneForgeMenu m = new RuneForgeMenu(1, p.getInventory(), ContainerLevelAccess.NULL);
		m.clickMenuButton(p, RuneForgeMenu.TAB_MERGE);
		m.getSlot(RuneForgeMenu.MERGE_A).setByPlayer(Runes.rune(ench(c, Enchantments.UNBREAKING), 2));
		m.getSlot(RuneForgeMenu.MERGE_B).setByPlayer(Runes.rune(ench(c, Enchantments.UNBREAKING), 1));
		c.assertTrue(m.getSlot(RuneForgeMenu.RESULT).getItem().isEmpty(), "different levels do not merge");
		m.getSlot(RuneForgeMenu.MERGE_B).setByPlayer(Runes.rune(ench(c, Enchantments.UNBREAKING), 2));
		ItemStack out = m.getSlot(RuneForgeMenu.RESULT).getItem();
		RuneEntry e = Runes.entryOf(out);
		c.assertTrue(e != null && e.level() == 3 && e.bonus() == 4, "level 3 (+4)");
		ItemStack taken = m.getSlot(RuneForgeMenu.RESULT).remove(1);
		m.getSlot(RuneForgeMenu.RESULT).onTake(p, taken);
		c.assertTrue(m.getSlot(RuneForgeMenu.MERGE_A).getItem().isEmpty() && m.getSlot(RuneForgeMenu.MERGE_B).getItem().isEmpty(),
			"both used up");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void theRuneForgeRecipeUnlocksInTheRecipeBook(GameTestHelper c) {
		var server = c.getLevel().getServer();
		var key = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("permahearts", "rune_forge"));
		c.assertTrue(server.getRecipeManager().byKey(key).isPresent(), "recipe loaded");
		var adv = server.getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("permahearts", "recipes/decorations/rune_forge"));
		c.assertTrue(adv != null, "unlock advancement loaded");
		ServerPlayer p = GameTestKit.survivor(c);
		c.assertTrue(!p.getRecipeBook().contains(key), "locked at first");
		p.getAdvancements().award(adv, "has_iron_ingot");
		c.assertTrue(p.getRecipeBook().contains(key), "in the recipe book after picking up iron");
		GameTestKit.drop(c, p);
		c.succeed();
	}
}
