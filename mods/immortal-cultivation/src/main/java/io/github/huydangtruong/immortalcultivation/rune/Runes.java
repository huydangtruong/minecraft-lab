package io.github.huydangtruong.immortalcultivation.rune;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Runes (D241): a rune item strengthens one enchantment; inscribed on gear (Rune Forge, D242) it adds 2^(n-1) levels to that
 * enchantment, conflicts ignored. The gear keeps its runes in the {@link #INSCRIBED} component; {@link #withRunes} is what the
 * enchantment code sees (mixin), so the real enchantments of the item never change.
 */
public final class Runes {
	public static final int ORANGE = 0xFFA040;

	/** What a rune item is. */
	public static final DataComponentType<RuneEntry> RUNE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "rune"),
		DataComponentType.<RuneEntry>builder().persistent(RuneEntry.CODEC).networkSynchronized(RuneEntry.STREAM_CODEC).build());

	/** Runes inscribed on a piece of gear, in slot order. */
	public static final DataComponentType<List<RuneEntry>> INSCRIBED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "runes"),
		DataComponentType.<List<RuneEntry>>builder().persistent(RuneEntry.LIST_CODEC).networkSynchronized(RuneEntry.LIST_STREAM_CODEC).build());

	public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "rune"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new RuneItem(new Item.Properties().setId(ITEM_KEY).stacksTo(1).rarity(Rarity.RARE)));

	private Runes() {}

	public static void init() {
		RuneForge.init();
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("functional_blocks"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> out.accept(RuneForge.ITEM));
	}

	/** D241: enchantments a rune can be of: every one with levels (max level above 1), no curses. */
	public static boolean eligible(Holder<Enchantment> e) {
		return e.value().getMaxLevel() > 1 && !e.is(EnchantmentTags.CURSE);
	}

	public static List<Holder<Enchantment>> eligible(HolderLookup.Provider access) {
		List<Holder<Enchantment>> out = new ArrayList<>();
		access.lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(h -> {
			if (eligible(h)) {
				out.add(h);
			}
		});
		return out;
	}

	public static ItemStack rune(Holder<Enchantment> e, int level) {
		ItemStack s = new ItemStack(ITEM);
		s.set(RUNE, new RuneEntry(e, Math.max(1, level)));
		String kind = RuneRules.kind(e.unwrapKey().map(k -> k.identifier().getPath()).orElse(""));
		s.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
			new net.minecraft.world.item.component.CustomModelData(List.of(), List.of(), List.of(kind), List.of())); // D264: stone colour
		return s;
	}

	public static ItemStack rune(RuneEntry entry) {
		return rune(entry.enchantment(), entry.level());
	}

	/** D264: a reward rune of floor n: random kind, level 1 up to {@link io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryRules#maxRuneLevel}. */
	public static ItemStack randomReward(HolderLookup.Provider access, RandomSource rng, int floor) {
		List<Holder<Enchantment>> all = eligible(access);
		if (all.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return rune(all.get(rng.nextInt(all.size())), 1 + rng.nextInt(io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryRules.maxRuneLevel(floor)));
	}

	public static RuneEntry entryOf(ItemStack s) {
		return s.is(ITEM) ? s.get(RUNE) : null;
	}

	public static List<RuneEntry> inscribed(ItemStack s) {
		List<RuneEntry> l = s.get(INSCRIBED);
		return l == null ? List.of() : l;
	}

	public static void setInscribed(ItemStack s, List<RuneEntry> runes) {
		if (runes.isEmpty()) {
			s.remove(INSCRIBED);
		} else {
			s.set(INSCRIBED, List.copyOf(runes));
		}
	}

	/** D242: rune slots of a piece of gear. */
	public static int slots(ItemStack s) {
		if (s.isEmpty() || s.is(ITEM) || !s.isDamageableItem()) {
			return 0; // gear only: an iron ingot or a diamond block has no slots
		}
		return RuneRules.slots(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath());
	}

	/** A rune may go on gear whose kind the enchantment is meant for (a Sharpness rune on a sword or axe, not on boots). */
	public static boolean fits(ItemStack gear, RuneEntry rune) {
		return rune != null && slots(gear) > 0 && rune.enchantment().value().isSupportedItem(gear);
	}

	/** Bonus levels per enchantment from the runes on a stack. */
	public static Map<Holder<Enchantment>, Integer> bonuses(ItemStack s) {
		Map<Holder<Enchantment>, Integer> out = new LinkedHashMap<>();
		for (RuneEntry r : inscribed(s)) {
			out.merge(r.enchantment(), r.bonus(), Integer::sum);
		}
		return out;
	}

	/** The enchantments the game should use for a stack: its own plus the rune bonuses (D241). */
	public static ItemEnchantments withRunes(Object stack, ItemEnchantments base) {
		if (!(stack instanceof ItemStack s)) {
			return base;
		}
		List<RuneEntry> runes = s.get(INSCRIBED);
		if (runes == null || runes.isEmpty()) {
			return base;
		}
		ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(base == null ? ItemEnchantments.EMPTY : base);
		for (RuneEntry r : runes) {
			m.set(r.enchantment(), RuneRules.total(m.getLevel(r.enchantment()), r.bonus()));
		}
		return m.toImmutable();
	}

	/** Orange tooltip lines: one per enchantment, all its runes added up (D241). */
	public static List<Component> tooltip(ItemStack s) {
		List<Component> out = new ArrayList<>();
		RuneEntry self = entryOf(s);
		if (self != null) {
			out.add(Component.literal("+" + self.bonus() + " ").append(self.enchantment().value().description().copy())
				.withColor(ORANGE));
			out.add(Component.literal("Inscribe it at a Rune Forge").withStyle(ChatFormatting.GRAY));
			return out;
		}
		int slots = slots(s);
		if (slots == 0) {
			return out;
		}
		Map<Holder<Enchantment>, Integer> b = bonuses(s);
		for (Map.Entry<Holder<Enchantment>, Integer> e : b.entrySet()) {
			out.add(Component.literal("Rune: ").append(e.getKey().value().description().copy()).append(" +" + e.getValue()).withColor(ORANGE));
		}
		out.add(Component.literal("Rune slots " + inscribed(s).size() + " / " + slots).withStyle(ChatFormatting.GRAY));
		return out;
	}

	/** "Sharpness Rune III". */
	public static MutableComponent name(RuneEntry r) {
		return Component.empty().append(r.enchantment().value().description().copy()).append(" Rune " + RuneRules.roman(r.level()));
	}
}
