package io.github.huydangtruong.immortalcultivation.danger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

/** P-054 (D137, D138): the numbers of the world danger level 0..30. Pure Java, no Minecraft imports. */
public final class DangerRules {
	public static final int MAX_LEVEL = 30;
	/** Ticks between a Crimson Master leaving and the level going up: 1/5 of a day (4 minutes). */
	public static final long GROWTH_DELAY_TICKS = 4800L;
	public static final int MIN_ELITE_LEVEL = 10;
	public static final int GEAR_START_LEVEL = 21;

	private DangerRules() {}

	public static int clamp(int level) {
		return Math.max(0, Math.min(MAX_LEVEL, level));
	}

	/** Extra hostile mob cap: +10% per level to 20, +20% per level to 25 (+300%), then flat. */
	public static double spawnBonus(int level) {
		int l = clamp(level);
		if (l <= 20) {
			return 0.10 * l;
		}
		return Math.min(3.0, 2.0 + 0.20 * (l - 20));
	}

	/** Extra max health and attack damage of hostile mobs (ADD_MULTIPLIED_BASE). */
	public static double strengthBonus(int level) {
		int l = clamp(level);
		return l <= 20 ? 0.075 * l : 1.5 + 0.15 * (l - 20);
	}

	/** Chance that a spawn that would be blocked by daylight is allowed: 0 below 8, 0.15 at 8, 1.0 at 20. */
	public static double daySpawnFactor(int level) {
		int l = clamp(level);
		if (l < 8) {
			return 0;
		}
		if (l >= 20) {
			return 1.0;
		}
		return 0.15 + 0.85 * (l - 8) / 12.0;
	}

	public static boolean rainSpawns(int level) {
		return clamp(level) >= 6;
	}

	/** Mob types that join the Overworld monster spawns, with the level that unlocks them. */
	public enum Type {
		PILLAGER(4, 5, false), VINDICATOR(6, 5, false), BLAZE(10, 3, true), WITHER_SKELETON(10, 3, true),
		EVOKER(12, 2, false), PIGLIN_BRUTE(14, 3, false), RAVAGER(16, 1, false);

		public final int unlockLevel;
		public final int weight;
		/** Spawns only where the light is dark (at most 7). */
		public final boolean darkOnly;

		Type(int unlockLevel, int weight, boolean darkOnly) {
			this.unlockLevel = unlockLevel;
			this.weight = weight;
			this.darkOnly = darkOnly;
		}
	}

	public static List<Type> unlockedTypes(int level) {
		List<Type> out = new ArrayList<>();
		for (Type t : Type.values()) {
			if (clamp(level) >= t.unlockLevel) {
				out.add(t);
			}
		}
		return out;
	}

	// ---- Gear ----

	public enum Tier { NONE, IRON, DIAMOND, NETHERITE }

	/** Armor for head, chest, legs, feet. enchanted[i] says whether slot i gets a Protection enchantment. */
	public record GearPlan(Tier[] armor, boolean[] enchanted) {
		public boolean hasNetherite() {
			return Arrays.asList(armor).contains(Tier.NETHERITE);
		}
	}

	/** Per-slot chance of extra armor: none up to level 20, 20% at 21 rising to 70% at 30. */
	public static double gearChance(int level) {
		int l = clamp(level);
		if (l < GEAR_START_LEVEL) {
			return 0;
		}
		return 0.20 + 0.50 * (l - GEAR_START_LEVEL) / (double) (MAX_LEVEL - GEAR_START_LEVEL);
	}

	public static double enchantChance(int level) {
		int l = clamp(level);
		if (l < GEAR_START_LEVEL) {
			return 0;
		}
		return 0.10 + 0.60 * (l - GEAR_START_LEVEL) / (double) (MAX_LEVEL - GEAR_START_LEVEL);
	}

	public static GearPlan gearRoll(int level, RandomGenerator rng) {
		Tier[] armor = new Tier[4];
		boolean[] enchanted = new boolean[4];
		Arrays.fill(armor, Tier.NONE);
		int l = clamp(level);
		if (l < GEAR_START_LEVEL) {
			return new GearPlan(armor, enchanted);
		}
		double p = (l - GEAR_START_LEVEL) / (double) (MAX_LEVEL - GEAR_START_LEVEL);
		double iron = 1.1 - p;
		double diamond = 0.6;
		double netherite = p * p;
		double total = iron + diamond + netherite;
		for (int i = 0; i < 4; i++) {
			if (rng.nextDouble() >= gearChance(l)) {
				continue;
			}
			double r = rng.nextDouble() * total;
			armor[i] = r < iron ? Tier.IRON : r < iron + diamond ? Tier.DIAMOND : Tier.NETHERITE;
			enchanted[i] = rng.nextDouble() < enchantChance(l);
		}
		GearPlan plan = new GearPlan(armor, enchanted);
		if (l >= MAX_LEVEL && !plan.hasNetherite()) {
			armor[rng.nextInt(4)] = Tier.NETHERITE;
		}
		return plan;
	}

	// ---- Elites ----

	/** 0 below level 10; 2% at 10, 10% at 20, 15% at 30 (linear in each part). */
	public static double eliteChance(int level) {
		int l = clamp(level);
		if (l < MIN_ELITE_LEVEL) {
			return 0;
		}
		if (l <= 20) {
			return 0.02 + 0.08 * (l - 10) / 10.0;
		}
		return 0.10 + 0.05 * (l - 20) / 10.0;
	}

	public static int maxAffixes(int level) {
		int l = clamp(level);
		if (l < MIN_ELITE_LEVEL) {
			return 0;
		}
		return l < 20 ? 1 : l < 30 ? 2 : 3;
	}

	public enum Affix {
		SWIFT("Swift"), IRONHIDE("Ironhide"), REGENERATING("Regenerating"), BLAZING("Blazing"), VENOMOUS("Venomous");

		public final String label;

		Affix(String label) {
			this.label = label;
		}

		public static Affix byName(String name) {
			for (Affix a : values()) {
				if (a.name().equalsIgnoreCase(name)) {
					return a;
				}
			}
			return null;
		}
	}

	/** Rolls 1..maxAffixes distinct affixes. Empty below level 10. */
	public static List<Affix> rollAffixes(int level, RandomGenerator rng) {
		int max = maxAffixes(level);
		if (max == 0) {
			return List.of();
		}
		List<Affix> pool = new ArrayList<>(List.of(Affix.values()));
		int n = 1 + rng.nextInt(max);
		List<Affix> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			out.add(pool.remove(rng.nextInt(pool.size())));
		}
		return out;
	}

	/** Ironhide: the elite takes this share of the damage (40% less). */
	public static final double IRONHIDE_DAMAGE_TAKEN = 0.6;
	public static final double SWIFT_SPEED_BONUS = 0.4;
	public static final double REGEN_FRACTION_PER_SECOND = 0.01;
	public static final int BLAZING_FIRE_SECONDS = 4;
	public static final int VENOMOUS_POISON_SECONDS = 5;

	// ---- Notice ----

	public record Notice(String title, String subtitle) {}

	private static int pct(double x) {
		return (int) Math.round(x * 100 + 1e-9);
	}

	/** What a level unlocks, short. Empty when the level adds nothing new. */
	public static String unlocks(int level) {
		return switch (clamp(level)) {
			case 4 -> "Pillagers appear";
			case 6 -> "Vindicators appear, rain spawns";
			case 8 -> "day spawns";
			case 10 -> "elites appear, Blazes and Wither Skeletons appear";
			case 12 -> "Evokers appear";
			case 14 -> "Piglin Brutes appear";
			case 16 -> "Ravagers appear";
			case 20 -> "day as busy as night";
			case 21 -> "more armor";
			case 30 -> "netherite armor";
			default -> "";
		};
	}

	public static Notice notice(int level) {
		int l = clamp(level);
		String sub = "+" + pct(spawnBonus(l)) + "% mobs, +" + pct(strengthBonus(l)) + "% strength";
		String extra = unlocks(l);
		if (!extra.isEmpty()) {
			sub += ", " + extra;
		}
		return new Notice("Danger level " + l, sub);
	}
}
