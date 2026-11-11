package io.github.huydangtruong.permahearts;

import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;

public final class ElixirItems {
	private static final Map<ElixirStat, Item> ITEMS = new EnumMap<>(ElixirStat.class);

	static {
		for (ElixirStat stat : ElixirStat.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "elixir_" + stat.id()));
			Item.Properties props = new Item.Properties()
				.setId(key)
				.stacksTo(16)
				.rarity(Rarity.EPIC)
				.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
				.usingConvertsTo(Items.GLASS_BOTTLE)
				.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			ITEMS.put(stat, Registry.register(BuiltInRegistries.ITEM, key, new ElixirItem(props, stat)));
		}
	}

	private ElixirItems() {}

	public static Item get(ElixirStat stat) {
		return ITEMS.get(stat);
	}

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
			Identifier.withDefaultNamespace("food_and_drinks"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> {
			for (ElixirStat stat : ElixirStat.values()) {
				out.accept(get(stat));
			}
		});
	}
}
