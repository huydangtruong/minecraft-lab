package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * One "focus" item per technique (D46): never in a creative tab, not craftable. The client draws it in the main hand of a
 * player who has that technique selected; nobody's inventory ever holds it.
 */
public final class FocusItems {
	private static final Map<Technique, Item> ITEMS = new EnumMap<>(Technique.class);

	static {
		for (Technique t : Technique.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "focus_" + t.id()));
			ITEMS.put(t, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.EPIC))));
		}
	}

	private FocusItems() {}

	/** Touches the class so the items are registered. */
	public static void init() {}

	public static Item get(Technique t) {
		return ITEMS.get(t);
	}

	public static ItemStack stack(Technique t) {
		return new ItemStack(ITEMS.get(t));
	}
}
