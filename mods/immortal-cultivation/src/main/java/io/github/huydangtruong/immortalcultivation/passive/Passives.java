package io.github.huydangtruong.immortalcultivation.passive;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Server glue of the passives (D148). The numbers live in {@link PassiveRules}. */
public final class Passives {
	private Passives() {}

	private static boolean on(ServerPlayer p) {
		return ImmortalCultivation.enabled(p.level().getServer());
	}

	private static int level(ServerPlayer p, Passive passive) {
		return on(p) ? PassiveAttachment.get(p).level(passive) : 0;
	}

	/** Reduce cooldown: every technique cooldown goes through here. */
	public static long cooldown(ServerPlayer p, long baseTicks) {
		return PassiveRules.reducedCooldown(baseTicks, level(p, Passive.COOLDOWN));
	}

	/** Hunger sustain: called after FoodData#tick. */
	public static void holdFood(ServerPlayer p) {
		int floor = PassiveRules.hungerFloor(level(p, Passive.HUNGER));
		if (floor > 0 && p.getFoodData().getFoodLevel() < floor) {
			p.getFoodData().setFoodLevel(floor);
		}
	}

	/** XP gain bonus: vanilla XP from an orb, a crop harvest, or a clone's breeding (D148). */
	public static int bonusXp(ServerPlayer p, int amount) {
		return PassiveRules.boostedXp(amount, level(p, Passive.XP), p.getRandom().nextDouble());
	}

	/**
	 * Inventory protect, around Inventory#dropAll in Player#dropEquipment (vanishing items are already gone): takes the
	 * protected stacks out, lets vanilla drop the rest, then puts them back. They stay in the dead player's inventory
	 * and COPY_FROM hands them to the respawned player ({@link #restore}).
	 */
	public static List<ItemStack> takeProtected(ServerPlayer p) {
		int level = level(p, Passive.INVENTORY);
		Inventory inv = p.getInventory();
		List<Integer> slots = PassiveRules.protectedSlots(level, inv.getSelectedSlot());
		ItemStack[] kept = new ItemStack[inv.getContainerSize()];
		java.util.Arrays.fill(kept, ItemStack.EMPTY);
		for (int s : slots) {
			if (s < kept.length) {
				kept[s] = inv.removeItemNoUpdate(s);
			}
		}
		return List.of(kept);
	}

	public static void putBack(ServerPlayer p, List<ItemStack> kept) {
		Inventory inv = p.getInventory();
		for (int s = 0; s < kept.size(); s++) {
			if (!kept.get(s).isEmpty()) {
				inv.setItem(s, kept.get(s));
			}
		}
	}

	/** Respawn after a normal death: the new player gets what the old one kept. */
	public static void restore(ServerPlayer old, ServerPlayer p) {
		if (level(old, Passive.INVENTORY) < 1) {
			return;
		}
		Inventory from = old.getInventory();
		Inventory to = p.getInventory();
		for (int s = 0; s < from.getContainerSize() && s < to.getContainerSize(); s++) {
			ItemStack st = from.getItem(s);
			if (!st.isEmpty() && to.getItem(s).isEmpty()) {
				to.setItem(s, st.copy());
				from.setItem(s, ItemStack.EMPTY);
			}
		}
		to.setSelectedSlot(from.getSelectedSlot());
	}
}
