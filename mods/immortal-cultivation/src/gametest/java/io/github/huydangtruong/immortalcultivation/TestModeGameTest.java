package io.github.huydangtruong.immortalcultivation;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/** P-031 / D92: /testmode full and reset. */
public class TestModeGameTest {
	private static void run(GameTestHelper c, CommandSourceStack src, String cmd) {
		try {
			c.getLevel().getServer().getCommands().getDispatcher().execute(cmd, src);
		} catch (CommandSyntaxException e) {
			throw new AssertionError(cmd + " failed: " + e.getMessage());
		}
	}

	private static int lvl(ServerPlayer p, ItemStack s, String id) {
		var holder = p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(ResourceKey.<Enchantment>create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(id)));
		return s.getEnchantments().getLevel(holder);
	}

	private static boolean same(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
	}

	@GameTest(maxTicks = 100)
	public void fullAndResetRoundTrip(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		MinecraftServer server = c.getLevel().getServer();
		CommandSourceStack admin = server.createCommandSourceStack().withEntity(p);
		HeartsAttachment.set(p, new HeartsData(2, 100, 5, 1));
		HeartsAttachment.setCultivation(p, CultivationData.DEFAULT.plus(ElixirStat.STRENGTH, 3));
		p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		p.getInventory().setItem(0, new ItemStack(Items.APPLE, 7));
		p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
		HealthApplier.apply(p, true);
		p.setExperienceLevels(7);
		int size = p.getInventory().getContainerSize();
		ItemStack[] before = new ItemStack[size];
		for (int i = 0; i < size; i++) {
			before[i] = p.getInventory().getItem(i).copy();
		}

		run(c, admin, "testmode full");
		run(c, admin, "testmode full"); // the second keeps the first snapshot
		String[] armor = {"helmet", "chestplate", "leggings", "boots"};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			ItemStack s = p.getItemBySlot(slots[i]);
			c.assertTrue(s.getItem().toString().contains("netherite_" + armor[i]), "worn " + armor[i] + " is netherite, was " + s);
			c.assertTrue(lvl(p, s, "protection") == 4 && lvl(p, s, "unbreaking") == 3, armor[i] + " enchants");
		}
		boolean[] tool = new boolean[5];
		for (int i = 0; i < size; i++) {
			ItemStack s = p.getInventory().getItem(i);
			if (s.is(Items.NETHERITE_SWORD) && lvl(p, s, "sharpness") == 5 && lvl(p, s, "fire_aspect") == 2) tool[0] = true;
			if (s.is(Items.NETHERITE_PICKAXE) && lvl(p, s, "efficiency") == 5 && lvl(p, s, "fortune") == 3) tool[1] = true;
			if (s.is(Items.NETHERITE_AXE) && lvl(p, s, "sharpness") == 5 && lvl(p, s, "efficiency") == 5) tool[2] = true;
			if (s.is(Items.NETHERITE_SHOVEL) && lvl(p, s, "efficiency") == 5 && lvl(p, s, "unbreaking") == 3) tool[3] = true;
			if (s.is(Items.NETHERITE_HOE) && lvl(p, s, "efficiency") == 5 && lvl(p, s, "fortune") == 3) tool[4] = true;
		}
		for (int i = 0; i < 5; i++) {
			c.assertTrue(tool[i], "enchanted tool " + i + " is in the inventory");
		}
		c.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 50, "50 hearts");
		c.assertTrue(p.getMaxHealth() == 100f, "max health 100, was " + p.getMaxHealth());
		CultivationData cd = HeartsAttachment.getCultivation(p);
		for (ElixirStat s : ElixirStat.values()) {
			int want = (s == ElixirStat.STRENGTH ? 3 : 0) + 2 * TestModeRules.ELIXIRS_EACH; // strength had 3 before; each full adds 10 (two fulls)
			c.assertTrue(cd.level(s) == want, s.id() + " level " + cd.level(s) + ", want " + want);
		}

		c.assertTrue(count(p, Items.GOLDEN_CARROT) == 2 * TestModeRules.GOLDEN_CARROTS, "128 golden carrots per full, had " + count(p, Items.GOLDEN_CARROT));
		c.assertTrue(count(p, Items.EMERALD) == 2 * TestModeRules.EMERALDS, "256 emeralds per full, had " + count(p, Items.EMERALD));
		c.assertTrue(p.experienceLevel == 7 + TestModeRules.XP_LEVELS * 2, "XP levels after two fulls, was " + p.experienceLevel);

		run(c, admin, "testmode reset");
		c.assertTrue(p.experienceLevel == 7, "XP level restored, was " + p.experienceLevel);
		for (int i = 0; i < size; i++) {
			c.assertTrue(same(before[i], p.getInventory().getItem(i)), "slot " + i + " restored: " + before[i] + " vs " + p.getInventory().getItem(i));
		}
		HeartsData h = HeartsAttachment.get(p);
		c.assertTrue(h.bonusHearts() == 2 && h.heartXp() == 100 && h.totalHostileKills() == 5 && h.totalDeaths() == 1, "hearts restored: " + h);
		CultivationData after = HeartsAttachment.getCultivation(p);
		c.assertTrue(after.level(ElixirStat.STRENGTH) == 3 && after.level(ElixirStat.FURY) == 0, "elixirs restored");
		c.assertTrue(p.getMaxHealth() == 24f, "max health back to 24, was " + p.getMaxHealth());
		c.assertTrue(p.getAttached(HeartsAttachment.TEST_SNAPSHOT) == null, "snapshot deleted");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void nonOpCannotRunIt(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		boolean rejected = false;
		try {
			c.getLevel().getServer().getCommands().getDispatcher().execute("testmode full", p.createCommandSourceStack());
		} catch (CommandSyntaxException e) {
			rejected = true;
		}
		c.assertTrue(rejected, "a non-op must not run /testmode");
		c.succeed();
	}

	private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack s = p.getInventory().getItem(i);
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		return n;
	}
}
