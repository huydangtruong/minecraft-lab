package io.github.huydangtruong.immortalcultivation;

import com.mojang.serialization.JsonOps;
import io.github.huydangtruong.immortalcultivation.passive.ItemGuard;
import io.github.huydangtruong.immortalcultivation.passive.Passive;
import io.github.huydangtruong.immortalcultivation.passive.PassiveAttachment;
import io.github.huydangtruong.immortalcultivation.passive.PassiveData;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

/** P-065 (D176): Item guard. Items that would break stay as broken stacks with a durability debt. */
public class ItemGuardGameTest {
	private static ServerPlayer player(GameTestHelper c, int level) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(ImmortalCultivation.ENABLED, true, s);
		ServerPlayer p = GameTestKit.survivor(c); // Creative never loses durability
		PassiveAttachment.set(p, PassiveData.DEFAULT.withLevel(Passive.GUARD, level));
		return p;
	}

	private static ItemStack nearlyBroken(Item item) {
		ItemStack s = new ItemStack(item);
		s.setDamageValue(s.getMaxDamage() - 1);
		return s;
	}

	/** Breaks the stack under the player and returns how many times the vanilla break callback ran. */
	private static int wear(ServerPlayer p, ItemStack s) {
		AtomicInteger broke = new AtomicInteger();
		s.hurtAndBreak(1, p.level(), p, x -> broke.incrementAndGet());
		return broke.get();
	}

	/** Broken, then worn until the debt reaches the cap of the player's level (D179). */
	private static ItemStack broken(ServerPlayer p, Item item) {
		ItemStack s = nearlyBroken(item);
		wear(p, s);
		wearMore(p, s, 100_000);
		return s;
	}

	private static void wearMore(ServerPlayer p, ItemStack s, int amount) {
		s.hurtAndBreak(amount, p.level(), p, x -> {});
	}

	@GameTest(maxTicks = 100)
	public void itemGuardLevelOneDebtGrowsFromZeroToFull(GameTestHelper c) {
		ServerPlayer p = player(c, 1);
		ItemStack s = nearlyBroken(Items.DIAMOND_PICKAXE);
		int callbacks = wear(p, s);
		c.assertTrue(callbacks == 0 && s.getCount() == 1, "the pickaxe must not break");
		c.assertTrue(s.getDamageValue() == 1561 && ItemGuard.isBroken(s), "full damage and broken");
		c.assertTrue(ItemGuard.debt(s) == 0, "D179: no debt when it just reaches 0, was " + ItemGuard.debt(s));
		c.assertTrue(wear(p, s) == 0 && ItemGuard.debt(s) == 1, "more wear builds debt: 1, was " + ItemGuard.debt(s));
		wearMore(p, s, 500);
		c.assertTrue(ItemGuard.debt(s) == 501 && s.getDamageValue() == 1561, "debt 501 at full damage, was " + ItemGuard.debt(s));
		wearMore(p, s, 5000);
		c.assertTrue(ItemGuard.debt(s) == 1561 && s.getCount() == 1, "Lv 1 cap 100% = 1561, never destroyed, was " + ItemGuard.debt(s));
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardLevelTenDebtCapIsTenPercent(GameTestHelper c) {
		ServerPlayer p = player(c, 10);
		ItemStack s = broken(p, Items.DIAMOND_PICKAXE);
		c.assertTrue(ItemGuard.debt(s) == 157, "Lv 10 debt cap 10% rounded up = 157, was " + ItemGuard.debt(s));
		PassiveAttachment.set(p, PassiveAttachment.get(p).withOn(Passive.GUARD, false));
		wearMore(p, s, 50);
		c.assertTrue(ItemGuard.debt(s) == 157 && s.getCount() == 1, "switched off: a broken item gains no debt and is not destroyed");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardWithoutThePassiveItemsBreakLikeVanilla(GameTestHelper c) {
		ServerPlayer p = player(c, 0);
		ItemStack s = nearlyBroken(Items.DIAMOND_PICKAXE);
		c.assertTrue(wear(p, s) == 1 && s.isEmpty(), "no passive: vanilla break");
		PassiveAttachment.set(p, PassiveData.DEFAULT.withLevel(Passive.GUARD, 5).withOn(Passive.GUARD, false));
		s = nearlyBroken(Items.DIAMOND_PICKAXE);
		c.assertTrue(wear(p, s) == 1 && s.isEmpty(), "switched off: vanilla break");
		PassiveAttachment.set(p, PassiveData.DEFAULT.withLevel(Passive.GUARD, 5));
		MinecraftServer srv = c.getLevel().getServer();
		srv.getGameRules().set(ImmortalCultivation.ENABLED, false, srv);
		s = nearlyBroken(Items.DIAMOND_PICKAXE);
		c.assertTrue(wear(p, s) == 1 && s.isEmpty(), "mod off in this world: vanilla break");
		srv.getGameRules().set(ImmortalCultivation.ENABLED, true, srv);
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardBrokenToolsActLikeAnEmptyHand(GameTestHelper c) {
		ServerPlayer p = player(c, 1);
		var stone = Blocks.STONE.defaultBlockState();
		var pick = new ItemStack(Items.DIAMOND_PICKAXE);
		c.assertTrue(pick.getDestroySpeed(stone) > 1.0f && pick.isCorrectToolForDrops(stone), "a whole pickaxe mines");
		ItemStack s = broken(p, Items.DIAMOND_PICKAXE);
		c.assertTrue(s.getDestroySpeed(stone) == 1.0f, "no mining speed, was " + s.getDestroySpeed(stone));
		c.assertTrue(!s.isCorrectToolForDrops(stone), "stone drops nothing with it");
		c.assertTrue(s.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()), "dirt still drops, like a hand");
		ItemStack sword = broken(p, Items.DIAMOND_SWORD);
		AtomicInteger mods = new AtomicInteger();
		sword.forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> mods.incrementAndGet());
		c.assertTrue(mods.get() == 0, "a broken sword adds no attack damage");
		AtomicInteger whole = new AtomicInteger();
		new ItemStack(Items.DIAMOND_SWORD).forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> whole.incrementAndGet());
		c.assertTrue(whole.get() > 0, "a whole sword does");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardBrokenArmorGivesNothingAndGearCannotBeUsed(GameTestHelper c) {
		ServerPlayer p = player(c, 1);
		ItemStack chest = broken(p, Items.DIAMOND_CHESTPLATE);
		AtomicInteger mods = new AtomicInteger();
		chest.forEachModifier(EquipmentSlot.CHEST, (a, m) -> mods.incrementAndGet());
		c.assertTrue(mods.get() == 0, "broken chestplate gives no armor");
		AtomicInteger whole = new AtomicInteger();
		new ItemStack(Items.DIAMOND_CHESTPLATE).forEachModifier(EquipmentSlot.CHEST, (a, m) -> whole.incrementAndGet());
		c.assertTrue(whole.get() > 0, "a whole one does");
		ItemStack elytra = broken(p, Items.ELYTRA);
		c.assertTrue(!LivingEntity.canGlideUsing(elytra, EquipmentSlot.CHEST), "broken elytra cannot glide");
		c.assertTrue(LivingEntity.canGlideUsing(new ItemStack(Items.ELYTRA), EquipmentSlot.CHEST), "whole elytra can");
		ItemStack bow = broken(p, Items.BOW);
		c.assertTrue(bow.use(c.getLevel(), p, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "broken bow cannot be drawn");
		ItemStack flint = broken(p, Items.FLINT_AND_STEEL);
		c.assertTrue(flint.use(c.getLevel(), p, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "broken flint and steel does nothing");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardBrokenTooltipAndBar(GameTestHelper c) {
		ServerPlayer p = player(c, 1);
		ItemStack s = broken(p, Items.DIAMOND_PICKAXE);
		var lines = s.getTooltipLines(Item.TooltipContext.of(c.getLevel()), null, TooltipFlag.NORMAL);
		boolean found = lines.stream().anyMatch(l -> l.getString().equals("Broken: -1561 durability"));
		c.assertTrue(found, "red tooltip line, lines were " + lines);
		c.assertTrue(s.isBarVisible() && s.getBarColor() == 0xFFE02020, "the bar is red");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardDebtSurvivesSaving(GameTestHelper c) {
		ServerPlayer p = player(c, 3);
		ItemStack s = broken(p, Items.IRON_PICKAXE);
		int debt = ItemGuard.debt(s);
		RegistryOps<com.google.gson.JsonElement> ops = c.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var json = ItemStack.CODEC.encodeStart(ops, s).getOrThrow();
		ItemStack back = ItemStack.CODEC.parse(ops, json).getOrThrow();
		c.assertTrue(ItemGuard.isBroken(back) && ItemGuard.debt(back) == debt && debt == 200, "Lv 3 iron pickaxe: 80% of 250 = 200, was " + debt);
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardAnvilMaterialsPayTheDebtFirst(GameTestHelper c) {
		ServerPlayer p = player(c, 10); // debt 157
		ItemStack s = broken(p, Items.DIAMOND_PICKAXE);
		AnvilMenu menu = new AnvilMenu(0, p.getInventory(), ContainerLevelAccess.create(c.getLevel(), BlockPos.ZERO));
		menu.getSlot(0).set(s);
		menu.getSlot(1).set(new ItemStack(Items.DIAMOND, 1)); // one diamond = 390 points
		ItemStack out = menu.getSlot(2).getItem();
		c.assertTrue(!out.isEmpty(), "the anvil gives a result");
		c.assertTrue(!ItemGuard.isBroken(out), "157 debt paid, 233 left over: usable again");
		c.assertTrue(out.getDamageValue() == 1561 - 233, "damage 1328, was " + out.getDamageValue());
		// Lv 1: 1561 debt, one diamond (390) only reduces the debt
		PassiveAttachment.set(p, PassiveData.DEFAULT.withLevel(Passive.GUARD, 1));
		ItemStack s1 = broken(p, Items.DIAMOND_PICKAXE);
		AnvilMenu menu1 = new AnvilMenu(0, p.getInventory(), ContainerLevelAccess.create(c.getLevel(), BlockPos.ZERO));
		menu1.getSlot(0).set(s1);
		menu1.getSlot(1).set(new ItemStack(Items.DIAMOND, 1));
		ItemStack out1 = menu1.getSlot(2).getItem();
		c.assertTrue(ItemGuard.isBroken(out1) && ItemGuard.debt(out1) == 1171 && out1.getDamageValue() == 1561,
			"debt 1561 - 390 = 1171 and still broken, debt was " + ItemGuard.debt(out1));
		c.assertTrue(ItemGuard.debt(s1) == 1561, "the input stays as it was until taken");
		// combining two pickaxes at Lv 1: the whole one's 1561 points (vanilla caps at the damage) only pay the debt
		AnvilMenu menu2 = new AnvilMenu(0, p.getInventory(), ContainerLevelAccess.create(c.getLevel(), BlockPos.ZERO));
		menu2.getSlot(0).set(broken(p, Items.DIAMOND_PICKAXE));
		menu2.getSlot(1).set(new ItemStack(Items.DIAMOND_PICKAXE));
		ItemStack out2 = menu2.getSlot(2).getItem();
		c.assertTrue(ItemGuard.isBroken(out2) && ItemGuard.debt(out2) == 0 && out2.getDamageValue() == 1561, "Lv 1 combine pays the debt only: " + ItemGuard.debt(out2));
		// Lv 10: debt 157, so 1404 durability is left
		PassiveAttachment.set(p, PassiveData.DEFAULT.withLevel(Passive.GUARD, 10));
		AnvilMenu menu3 = new AnvilMenu(0, p.getInventory(), ContainerLevelAccess.create(c.getLevel(), BlockPos.ZERO));
		menu3.getSlot(0).set(broken(p, Items.DIAMOND_PICKAXE));
		menu3.getSlot(1).set(new ItemStack(Items.DIAMOND_PICKAXE));
		ItemStack out3 = menu3.getSlot(2).getItem();
		c.assertTrue(!out3.isEmpty() && !ItemGuard.isBroken(out3) && out3.getDamageValue() == 157, "Lv 10 combine repairs it: " + out3.getDamageValue());
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void itemGuardMendingPaysDebtThenRepairs(GameTestHelper c) {
		ServerPlayer p = player(c, 10); // debt 157
		ItemStack s = nearlyBroken(Items.DIAMOND_PICKAXE);
		var enchants = c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		s.enchant(enchants.getOrThrow(Enchantments.MENDING), 1);
		wear(p, s);
		wearMore(p, s, 100_000);
		c.assertTrue(ItemGuard.isBroken(s) && ItemGuard.debt(s) == 157, "broke and wore to the 157 cap");
		p.setItemInHand(InteractionHand.MAIN_HAND, s);
		ExperienceOrb orb = new ExperienceOrb(c.getLevel(), p.getX(), p.getY(), p.getZ(), 100);
		c.getLevel().addFreshEntity(orb);
		orb.playerTouch(p); // 100 xp = 200 points: 157 debt + 43 durability
		ItemStack now = p.getItemInHand(InteractionHand.MAIN_HAND);
		c.assertTrue(!ItemGuard.isBroken(now), "mending paid the debt and gave durability");
		c.assertTrue(now.getDamageValue() == 1561 - 43, "damage 1518, was " + now.getDamageValue());
		c.assertTrue(now.getOrDefault(DataComponents.DAMAGE, 0) == now.getDamageValue(), "component in sync");
		GameTestKit.drop(c, p);
		c.succeed();
	}
}
