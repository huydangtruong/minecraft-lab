package io.github.huydangtruong.permahearts.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One card of a reward box: an item id (e.g. "minecraft:diamond" or "permahearts:elixir_fury") and how many. An enchanted
 * book card also carries the enchantment it stores (id like "minecraft:mending" and level); both are empty / 0 otherwise.
 * Boxes saved before the emerald box have no such fields and load with the defaults.
 */
public record RewardOption(String itemId, int count, String enchant, int enchantLevel) {
	public static final Codec<RewardOption> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("item").forGetter(RewardOption::itemId),
		Codec.INT.fieldOf("count").forGetter(RewardOption::count),
		Codec.STRING.optionalFieldOf("enchant", "").forGetter(RewardOption::enchant),
		Codec.INT.optionalFieldOf("level", 0).forGetter(RewardOption::enchantLevel)
	).apply(i, RewardOption::new));

	public RewardOption(String itemId, int count) {
		this(itemId, count, "", 0);
	}

	/** An enchanted book storing one enchantment. */
	public static RewardOption book(String enchant, int level) {
		return new RewardOption("minecraft:enchanted_book", 1, enchant, level);
	}

	public boolean hasEnchant() {
		return !enchant.isEmpty() && enchantLevel > 0;
	}

	/** Two cards with the same key are "the same item" for the no-duplicate rule (books differ by enchantment). */
	public String key() {
		return hasEnchant() ? itemId + "#" + enchant + "@" + enchantLevel : itemId;
	}
}
