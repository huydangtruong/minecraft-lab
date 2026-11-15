package io.github.huydangtruong.permahearts.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One card of a reward box: an item id (e.g. "minecraft:diamond" or "permahearts:elixir_fury") and how many. */
public record RewardOption(String itemId, int count) {
	public static final Codec<RewardOption> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("item").forGetter(RewardOption::itemId),
		Codec.INT.fieldOf("count").forGetter(RewardOption::count)
	).apply(i, RewardOption::new));
}
