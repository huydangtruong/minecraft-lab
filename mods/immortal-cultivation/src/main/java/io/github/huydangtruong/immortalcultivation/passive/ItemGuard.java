package io.github.huydangtruong.immortalcultivation.passive;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * Item guard glue (D176, P-065). A broken stack keeps full damage and a {@code permahearts:item_guard_debt} component
 * (the debt in durability points). The rules are in {@link ItemGuardRules}; the mixins call the methods here.
 */
public final class ItemGuard {
	/** Debt of a broken stack in durability points; present = broken. Saved and synced. */
	public static final DataComponentType<Integer> DEBT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "item_guard_debt"),
		DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** Set while Mending spends XP: enchantments of a broken stack count there so Mending can repair it. */
	public static boolean mending;

	private ItemGuard() {}

	public static void init() {
		DEBT.toString(); // touch the class so the component is registered
	}

	public static boolean isBroken(ItemStack s) {
		return !s.isEmpty() && s.has(DEBT);
	}

	public static int debt(ItemStack s) {
		return s.getOrDefault(DEBT, 0);
	}

	/** Item guard level of a player in use: 0 when off, mod off or not learned. */
	public static int level(ServerPlayer p) {
		if (!ImmortalCultivation.enabled(p.level().getServer())) {
			return 0;
		}
		return PassiveAttachment.get(p).activeLevel(Passive.GUARD);
	}

	/**
	 * D179: makes a stack that would break broken instead, at full damage. The debt starts at 0; wear past the last
	 * durability point (newDamage above the max) already counts, up to the cap of the level.
	 */
	public static void breakGuarded(ItemStack s, int level, int newDamage) {
		int max = s.getMaxDamage();
		s.set(DataComponents.DAMAGE, max);
		s.set(DEBT, ItemGuardRules.grow(0, newDamage - max, PassiveRules.debt(level, max)));
	}

	/** D179: wear on a stack that is already broken grows its debt, up to the cap of the player's level now. */
	public static void wearBroken(ItemStack s, int level, int newDamage) {
		int max = s.getMaxDamage();
		s.set(DEBT, ItemGuardRules.grow(debt(s), newDamage - max, PassiveRules.debt(level, max)));
	}

	/** Called when damage of a broken stack goes down (Mending, anvil, ...): pays the debt first. Returns true when handled. */
	public static boolean repair(ItemStack s, int newDamage) {
		int damage = s.getOrDefault(DataComponents.DAMAGE, 0);
		if (newDamage >= damage) {
			return false;
		}
		ItemGuardRules.Repaired r = ItemGuardRules.repairTo(debt(s), damage, newDamage);
		s.set(DataComponents.DAMAGE, r.damage());
		if (r.broken(s.getMaxDamage())) {
			s.set(DEBT, r.debt());
		} else {
			s.remove(DEBT);
		}
		return true;
	}

	public static Component tooltip(ItemStack s) {
		int debt = debt(s);
		return Component.literal(debt > 0 ? "Broken: -" + debt + " durability" : "Broken: repair it")
			.withStyle(net.minecraft.ChatFormatting.RED);
	}
}
