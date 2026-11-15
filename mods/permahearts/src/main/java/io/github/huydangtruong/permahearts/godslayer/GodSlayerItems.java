package io.github.huydangtruong.permahearts.godslayer;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** The God Slayer's katana: only the boss holds it. Not in a creative tab, not craftable, not a loot drop. */
public final class GodSlayerItems {
	public static final ResourceKey<Item> KATANA_KEY = ResourceKey.create(Registries.ITEM,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "katana"));
	public static final Item KATANA = Registry.register(BuiltInRegistries.ITEM, KATANA_KEY,
		new Item(new Item.Properties().setId(KATANA_KEY).stacksTo(1).rarity(Rarity.EPIC)));

	private GodSlayerItems() {}

	/** Touches the class so the item is registered. */
	public static void init() {}
}
