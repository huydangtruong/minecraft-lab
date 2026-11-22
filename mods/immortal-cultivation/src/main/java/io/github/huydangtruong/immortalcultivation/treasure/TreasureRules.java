package io.github.huydangtruong.immortalcultivation.treasure;

import io.github.huydangtruong.immortalcultivation.reward.RewardOption;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Deadly Treasure numbers (P-059, D150). No Minecraft imports. */
public final class TreasureRules {
	private TreasureRules() {}

	/** The event lasts 2 Minecraft days from the announcement (D173, was 1). */
	public static final long DURATION_TICKS = 2 * 24000L;
	/** D187: the "vanishes soon" warning comes this long before the end (2 real minutes, like the Crimson Master). */
	public static final long WARN_TICKS = 2400L;
	/** Horizontal distance from the chosen player. */
	public static final int DISTANCE = 1000;
	/** The chest is placed once a player comes this close (chunks loaded), like the Crimson Master. */
	public static final double PLACE_NEAR = 96.0;
	public static final double CAVE_CHANCE = 0.6;
	/** Guards stay within this radius of the chest and attack players this close. */
	public static final int HOME_RADIUS = 12;
	public static final double AGGRO_RANGE = 24.0;
	/** Mega look: 1.3x bigger, half knockback. */
	public static final double SCALE = 1.3;
	public static final double KNOCKBACK_RESISTANCE = 0.5;
	/** Random stacks besides the bottle, the apples and the carrots: always 8 (D172, was 6..8). */
	public static final int MIN_RANDOM = 8;
	public static final int MAX_RANDOM = 8;

	/** entity: minecraft id; species: glow texture name; mult: health and damage multiplier. */
	public record Guard(String entity, String species, int count, double mult) {}

	public record Group(String id, String name, boolean water, List<Guard> guards) {
		public int size() {
			return guards.stream().mapToInt(Guard::count).sum();
		}
	}

	private static Guard g(String id, int count, double mult) {
		return new Guard("minecraft:" + id, id, count, mult);
	}

	public static final List<Group> GROUPS = List.of(
		new Group("brutes", "Mega Piglin Brutes", false, List.of(g("piglin_brute", 6, 3))),
		new Group("blazes", "Mega Blazes", false, List.of(g("blaze", 6, 4))),
		new Group("wither_skeletons", "Mega Wither Skeletons", false, List.of(g("wither_skeleton", 6, 4))),
		new Group("breezes", "Mega Breezes", false, List.of(g("breeze", 6, 4))),
		new Group("vindicators", "Mega Vindicators", false, List.of(g("vindicator", 6, 4))),
		new Group("hoglins", "Mega Hoglins", false, List.of(g("hoglin", 3, 3), g("zoglin", 2, 3))),
		new Group("endermen", "Mega Endermen", false, List.of(g("enderman", 4, 3))),
		new Group("witches", "Mega Witches", false, List.of(g("witch", 4, 4))),
		new Group("archers", "Mega Strays and Boggeds", false, List.of(g("stray", 3, 4), g("bogged", 3, 4))),
		new Group("illagers", "Mega Illagers", false, List.of(g("vindicator", 3, 3), g("evoker", 2, 3), g("ravager", 1, 2))),
		new Group("drowned", "Mega Drowned", true, List.of(g("drowned", 6, 4))),
		new Group("guardians", "Mega Guardians", true, List.of(g("guardian", 5, 3))),
		new Group("elder_guardians", "Mega Elder Guardians", true, List.of(g("elder_guardian", 2, 2))));

	public static Group group(String id) {
		return GROUPS.stream().filter(gr -> gr.id().equals(id)).findFirst().orElse(null);
	}

	/** A random group for a land (cave or surface) or an underwater chest. */
	public static Group pickGroup(boolean water, RandomGenerator rng) {
		List<Group> pool = GROUPS.stream().filter(gr -> gr.water() == water).toList();
		return pool.get(rng.nextInt(pool.size()));
	}

	public record Loot(RewardOption option, int weight) {}

	private static Loot item(String id, int count, int weight) {
		return new Loot(new RewardOption("minecraft:" + id, count), weight);
	}

	private static final int COMMON = 10;
	private static final int RARE = 4;
	private static final int VERY_RARE = 1;

	/** Always in the chest. */
	public static final List<RewardOption> ALWAYS = List.of(
		new RewardOption("permahearts:enlightenment_bottle", 1), new RewardOption("minecraft:enchanted_golden_apple", 2), // D166: two apples (was one)
		new RewardOption("minecraft:golden_carrot", 32)); // D172

	/** Random part (no netherite ingot, D150; D214). Weights: common 10, rare 4, very rare 1. */
	public static final List<Loot> POOL = List.of(
		item("diamond", 32, COMMON), item("emerald", 48, COMMON), item("gunpowder", 64, COMMON), item("string", 64, COMMON),
		item("cooked_beef", 32, COMMON), item("carrot", 64, COMMON), item("iron_ingot", 64, COMMON), item("redstone", 64, COMMON),
		item("lapis_lazuli", 64, COMMON), item("experience_bottle", 32, COMMON), item("coal", 64, COMMON),
		item("netherite_upgrade_smithing_template", 1, RARE), item("totem_of_undying", 1, RARE), item("ender_pearl", 32, RARE),
		item("name_tag", 3, RARE), item("shulker_shell", 4, RARE), item("echo_shard", 8, RARE),
		item("wither_skeleton_skull", 3, RARE), item("trident", 1, RARE), item("heart_of_the_sea", 1, RARE),
		item("elytra", 1, VERY_RARE), item("heavy_core", 1, VERY_RARE), item("sniffer_egg", 1, VERY_RARE),
		new Loot(RewardOption.book("minecraft:looting", 3), VERY_RARE), new Loot(RewardOption.book("minecraft:fortune", 3), VERY_RARE),
		new Loot(RewardOption.book("minecraft:silk_touch", 1), VERY_RARE),
		new Loot(RewardOption.book("minecraft:blast_protection", 4), VERY_RARE)); // D214: coal, 32 bottles and pearls, Blast Protection IV

	/** The chest content: the bottle, two enchanted golden apples (D166), 32 golden carrots, then 8 different random stacks by weight (D172). */
	public static List<RewardOption> loot(RandomGenerator rng) {
		List<RewardOption> out = new ArrayList<>(ALWAYS);
		List<Loot> free = new ArrayList<>(POOL);
		int n = MIN_RANDOM + rng.nextInt(MAX_RANDOM - MIN_RANDOM + 1);
		for (int i = 0; i < n; i++) {
			int total = free.stream().mapToInt(Loot::weight).sum();
			int roll = rng.nextInt(total);
			for (int j = 0; j < free.size(); j++) {
				roll -= free.get(j).weight();
				if (roll < 0) {
					out.add(free.remove(j).option());
					break;
				}
			}
		}
		return out;
	}

	/** Phase of the event. */
	public enum Phase { IDLE, PENDING, ACTIVE }

	/**
	 * D172: the treasure takes every other Crimson Master visit. When the Crimson Master leaves, the treasure waits for
	 * the next visit day (nextDay) and starts at its beginning, like the Crimson Master would.
	 */
	public static long slotStart(long nextDay) {
		return nextDay * 24000L;
	}

	/** D172: on its visit day the Crimson Master stays away while the treasure waits for that day or runs. */
	public static boolean crimsonSkips(Phase phase) {
		return phase != Phase.IDLE;
	}
}
