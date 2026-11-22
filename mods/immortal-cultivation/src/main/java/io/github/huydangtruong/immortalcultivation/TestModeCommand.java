package io.github.huydangtruong.immortalcultivation;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * /testmode full | reset (D92): a full enchanted netherite kit, 50 hearts, 10 of every elixir, 128 golden carrots, 256 emeralds
 * and 200 XP levels (D126), with a one-step way back. Ops only.
 */
public final class TestModeCommand {
	private TestModeCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("testmode")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("full").executes(ctx -> {
				full(ctx.getSource().getPlayerOrException());
				ctx.getSource().sendSuccess(() -> Component.literal("Test mode on. /testmode reset to go back."), false);
				return 1;
			}))
			.then(Commands.literal("netherite").executes(ctx -> {
				netherite(ctx.getSource().getPlayerOrException());
				ctx.getSource().sendSuccess(() -> Component.literal("Full netherite kit given."), false);
				return 1;
			}))
			.then(Commands.literal("reset").executes(ctx -> {
				boolean ok = reset(ctx.getSource().getPlayerOrException());
				ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Test mode off." : "Test mode is not on."), false);
				return ok ? 1 : 0;
			})));
	}

	private static Holder<Enchantment> enchant(ServerPlayer p, String id) {
		ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(id));
		return p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	public static void full(ServerPlayer p) {
		if (p.getAttached(HeartsAttachment.TEST_SNAPSHOT) == null) { // a second full keeps the first snapshot
			List<ItemStack> items = new ArrayList<>();
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				items.add(p.getInventory().getItem(i).copy());
			}
			HeartsAttachment.setTestSnapshot(p, new TestSnapshot(HeartsAttachment.get(p), HeartsAttachment.getCultivation(p), items,
				p.experienceLevel, p.experienceProgress, p.totalExperience));
		}
		netherite(p);
		giveStacks(p, Items.GOLDEN_CARROT, TestModeRules.GOLDEN_CARROTS);
		giveStacks(p, Items.EMERALD, TestModeRules.EMERALDS);
		p.giveExperienceLevels(TestModeRules.XP_LEVELS);
		HeartsAttachment.set(p, HeartRules.withHearts(HeartsAttachment.get(p), TestModeRules.HEARTS));
		CultivationData c = HeartsAttachment.getCultivation(p);
		for (ElixirStat s : ElixirStat.values()) {
			c = c.plus(s, TestModeRules.ELIXIRS_EACH);
		}
		HeartsAttachment.setCultivation(p, c);
		HealthApplier.apply(p, true);
	}

	/** D186: the enchanted netherite kit of test mode alone (armor worn, the old armor goes to the inventory). No snapshot, nothing else. */
	public static void netherite(ServerPlayer p) {
		EquipmentSlot[] armor = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		int armorIndex = 0;
		for (TestModeRules.Gear g : TestModeRules.GEAR) {
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(g.item())));
			for (TestModeRules.Ench e : g.enchants()) {
				stack.enchant(enchant(p, e.id()), e.level());
			}
			if (g.armor()) {
				EquipmentSlot slot = armor[armorIndex++];
				ItemStack old = p.getItemBySlot(slot).copy();
				p.setItemSlot(slot, stack);
				give(p, old);
			} else {
				give(p, stack);
			}
		}
	}

	/** Puts the saved state back and deletes the snapshot. False when test mode is not on. */
	public static boolean reset(ServerPlayer p) {
		TestSnapshot s = p.getAttached(HeartsAttachment.TEST_SNAPSHOT);
		if (s == null) {
			return false;
		}
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			p.getInventory().setItem(i, i < s.items().size() ? s.items().get(i).copy() : ItemStack.EMPTY);
		}
		HeartsAttachment.set(p, s.hearts());
		HeartsAttachment.setCultivation(p, s.cultivation());
		if (s.xpLevel() >= 0) {
			p.experienceProgress = s.xpProgress();
			p.totalExperience = s.xpTotal();
			p.setExperienceLevels(s.xpLevel()); // also makes the server resend the XP bar
		}
		HeartsAttachment.setTestSnapshot(p, null);
		HealthApplier.apply(p, true);
		return true;
	}

	/** Gives count of an item in full stacks. */
	private static void giveStacks(ServerPlayer p, Item item, int count) {
		ItemStack proto = new ItemStack(item);
		for (int left = count; left > 0; left -= proto.getMaxStackSize()) {
			give(p, new ItemStack(item, Math.min(left, proto.getMaxStackSize())));
		}
	}

	private static void give(ServerPlayer p, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		if (!p.getInventory().add(stack) && !stack.isEmpty()) {
			p.spawnAtLocation(p.level(), stack);
		}
	}
}
