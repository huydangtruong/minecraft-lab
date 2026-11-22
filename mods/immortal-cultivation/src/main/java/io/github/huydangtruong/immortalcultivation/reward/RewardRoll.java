package io.github.huydangtruong.immortalcultivation.reward;

import io.github.huydangtruong.immortalcultivation.ElixirStat;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Rolls the cards of a reward box (D76). All the numbers of the rules are constants here. No Minecraft imports. */
public final class RewardRoll {
	/** Each card is an elixir with this chance, otherwise an item (D153: 60/40). */
	public static final double ELIXIR_CHANCE = 0.6;
	/** Base item pool, same for every tier; amounts come from the collect quests (QuestGenerator). Iron, redstone, lapis
	 * and gunpowder moved to the Deadly Treasure (D150). */
	public static final List<String> BASE_ITEMS = List.of(
		"minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald", "minecraft:ender_pearl");

	/** A special with a base item id replaces that base card's amount (D211: bigger diamond, emerald, pearl cards in the higher boxes). */
	public static final List<RewardOption> GOLD_SPECIALS = List.of(
		new RewardOption("minecraft:enchanted_golden_apple", 4), new RewardOption("minecraft:emerald", 32),
		new RewardOption("minecraft:diamond", 32));
	public static final List<RewardOption> DIAMOND_SPECIALS = List.of(
		new RewardOption("minecraft:enchanted_golden_apple", 4), new RewardOption("minecraft:netherite_ingot", 1),
		new RewardOption("minecraft:emerald", 32), new RewardOption("minecraft:diamond", 32), new RewardOption("minecraft:ender_pearl", 48));

	public static final List<RewardOption> EMERALD_SPECIALS = List.of(
		new RewardOption("minecraft:totem_of_undying", 1), new RewardOption("permahearts:enlightenment_bottle", 1),
		RewardOption.book("minecraft:mending", 1), RewardOption.book("minecraft:sharpness", 5),
		RewardOption.book("minecraft:protection", 5), RewardOption.book("minecraft:unbreaking", 3),
		new RewardOption("minecraft:diamond", 48), new RewardOption("minecraft:emerald", 64), new RewardOption("minecraft:ender_pearl", 48));

	private RewardRoll() {}

	public static List<RewardOption> specials(RewardTier tier) {
		return switch (tier) {
			case SILVER -> List.of();
			case GOLD -> GOLD_SPECIALS;
			case DIAMOND -> DIAMOND_SPECIALS;
			case EMERALD -> EMERALD_SPECIALS;
		};
	}

	public static RewardOption elixir(ElixirStat stat) {
		return new RewardOption("permahearts:elixir_" + stat.id(), 1);
	}

	public static final String HEART_ELIXIR = "permahearts:elixir_heart";

	/** All item cards of a tier (D108): base items (amounts from collect quests), the emerald card is x32 from gold up, plus the specials. */
	public static List<RewardOption> itemPool(RewardTier tier) {
		List<RewardOption> pool = new ArrayList<>();
		List<RewardOption> specials = specials(tier);
		for (String id : BASE_ITEMS) {
			RewardOption special = specials.stream().filter(o -> o.itemId().equals(id)).findFirst().orElse(null);
			pool.add(special != null ? special : new RewardOption(id, QuestGenerator.collectAmount(id)));
		}
		for (RewardOption o : specials) {
			if (!pool.contains(o)) {
				pool.add(o);
			}
		}
		return pool;
	}

	/** tier.options() cards: each 60% elixir (uniform, may repeat) or item (uniform, no repeats). */
	public static List<RewardOption> roll(RewardTier tier, RandomGenerator rng) {
		List<RewardOption> elixirs = new ArrayList<>();
		for (ElixirStat s : ElixirStat.values()) {
			elixirs.add(elixir(s));
		}
		if (tier != RewardTier.SILVER) {
			elixirs.add(new RewardOption(HEART_ELIXIR, 1));
		}
		List<RewardOption> free = new ArrayList<>(itemPool(tier));
		List<RewardOption> out = new ArrayList<>();
		while (out.size() < tier.options()) {
			if (rng.nextDouble() < ELIXIR_CHANCE) {
				out.add(elixirs.get(rng.nextInt(elixirs.size())));
			} else {
				out.add(free.remove(rng.nextInt(free.size())));
			}
		}
		return List.copyOf(out);
	}
}
