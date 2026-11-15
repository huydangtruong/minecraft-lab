package io.github.huydangtruong.permahearts.reward;

import io.github.huydangtruong.permahearts.ElixirStat;
import io.github.huydangtruong.permahearts.wanderer.QuestGenerator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Rolls the cards of a reward box (D76). All the numbers of the rules are constants here. No Minecraft imports. */
public final class RewardRoll {
	/** Each card is an elixir with this chance, otherwise an item. */
	public static final double ELIXIR_CHANCE = 0.5;
	/** An item card is a special with this chance (GOLD and DIAMOND only). */
	public static final double SPECIAL_CHANCE = 0.2;

	/** Base item pool, same for every tier; amounts come from the collect quests (QuestGenerator). */
	public static final List<String> BASE_ITEMS = List.of(
		"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald",
		"minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:ender_pearl", "minecraft:gunpowder");

	public static final List<RewardOption> GOLD_SPECIALS = List.of(
		new RewardOption("minecraft:enchanted_golden_apple", 4), new RewardOption("minecraft:emerald", 32));
	public static final List<RewardOption> DIAMOND_SPECIALS = List.of(
		new RewardOption("minecraft:enchanted_golden_apple", 4), new RewardOption("minecraft:netherite_ingot", 1),
		new RewardOption("minecraft:emerald", 32));

	private RewardRoll() {}

	public static List<RewardOption> specials(RewardTier tier) {
		return switch (tier) {
			case SILVER -> List.of();
			case GOLD -> GOLD_SPECIALS;
			case DIAMOND -> DIAMOND_SPECIALS;
		};
	}

	public static RewardOption elixir(ElixirStat stat) {
		return new RewardOption("permahearts:elixir_" + stat.id(), 1);
	}

	/** tier.options() cards. Elixirs may repeat; item cards never share an item id. */
	public static List<RewardOption> roll(RewardTier tier, RandomGenerator rng) {
		List<RewardOption> out = new ArrayList<>();
		Set<String> usedItems = new HashSet<>();
		ElixirStat[] stats = ElixirStat.values();
		List<RewardOption> specials = specials(tier);
		while (out.size() < tier.options()) {
			if (rng.nextDouble() < ELIXIR_CHANCE) {
				out.add(elixir(stats[rng.nextInt(stats.length)]));
				continue;
			}
			RewardOption pick = null;
			if (!specials.isEmpty() && rng.nextDouble() < SPECIAL_CHANCE) {
				List<RewardOption> free = specials.stream().filter(o -> !usedItems.contains(o.itemId())).toList();
				if (!free.isEmpty()) {
					pick = free.get(rng.nextInt(free.size()));
				}
			}
			if (pick == null) {
				List<String> free = BASE_ITEMS.stream().filter(id -> !usedItems.contains(id)).toList();
				String id = free.get(rng.nextInt(free.size()));
				pick = new RewardOption(id, QuestGenerator.collectAmount(id));
			}
			usedItems.add(pick.itemId());
			out.add(pick);
		}
		return List.copyOf(out);
	}
}
