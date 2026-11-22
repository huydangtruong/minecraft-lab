package io.github.huydangtruong.immortalcultivation.danger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * P-054 (D137, D138, D206): the numbers of the world danger level 0..50. Pure Java, no Minecraft imports.
 * D206 stretched the scale from 30 to 50 levels: the new level 50 is the old level 30, a new level L is the old level
 * L x 3/5 (continuous curves) and every old threshold T became round(T x 5/3).
 */
public final class DangerRules {
	public static final int MAX_LEVEL = 50;
	/** The level the scale had before D206 (a stored level of that scale is converted once on load). */
	public static final int OLD_MAX_LEVEL = 30;
	/** Ticks between a Crimson Master leaving and the level going up: 1/5 of a day (4 minutes). */
	public static final long GROWTH_DELAY_TICKS = 4800L;
	public static final int MIN_ELITE_LEVEL = 33;
	public static final int GEAR_START_LEVEL = 35;
	/** Rain lets hostile mobs spawn from this level (old 6). */
	public static final int RAIN_LEVEL = 10;
	/** Daylight spawns: none below DAY_START_LEVEL (old 8), 15% there, always from DAY_FULL_LEVEL (old 20). */
	public static final int DAY_START_LEVEL = 13;
	public static final int DAY_FULL_LEVEL = 33;
	/** Level of the first extra mob type (Pillager, old 4). */
	public static final int FIRST_TYPE_LEVEL = 7;
	/** Elites with a 3rd affix from this level (old 30), a 2nd from AFFIX2_LEVEL (old 25). */
	public static final int AFFIX2_LEVEL = 42;
	public static final int AFFIX3_LEVEL = 50;
	/** Elite armor may carry Protection IV instead of III from this level (old 26). */
	public static final int PROTECTION_4_LEVEL = 43;

	private DangerRules() {}

	public static int clamp(int level) {
		return Math.max(0, Math.min(MAX_LEVEL, level));
	}

	/** The level on the pre-D206 scale (0..30) that has the same strength as this level; may be fractional. */
	private static double old(int level) {
		return clamp(level) * 3 / 5.0;
	}

	/** Levels taken off when an old save is converted (D209: Huy wants old worlds a bit gentler, 30 becomes 45). */
	public static final int OLD_SCALE_DISCOUNT = 5;

	/** Converts a level stored on the old 0..30 scale to the new 0..50 scale: x5/3, then minus 5, not below 0 (30 becomes 45). */
	public static int fromOldScale(int oldLevel) {
		int o = Math.max(0, Math.min(OLD_MAX_LEVEL, oldLevel));
		return clamp((int) Math.round(o * 5 / 3.0) - OLD_SCALE_DISCOUNT);
	}

	/** Extra hostile mob cap: +6% per level to 33.3 (+200%), then +12% per level, flat at +300% from 41.7 (old curve at L x 3/5). */
	public static double spawnBonus(int level) {
		double o = old(level);
		if (o <= 20) {
			return 0.10 * o;
		}
		return Math.min(3.0, 2.0 + 0.20 * (o - 20));
	}

	/** Extra max health and attack damage of hostile mobs (ADD_MULTIPLIED_BASE). */
	public static double strengthBonus(int level) {
		double o = old(level);
		return o <= 20 ? 0.075 * o : 1.5 + 0.15 * (o - 20);
	}

	/** Chance that a spawn that would be blocked by daylight is allowed: 0 below 13, 0.15 at 13, 1.0 from 33. */
	public static double daySpawnFactor(int level) {
		int l = clamp(level);
		if (l < DAY_START_LEVEL) {
			return 0;
		}
		if (l >= DAY_FULL_LEVEL) {
			return 1.0;
		}
		return 0.15 + 0.85 * (l - DAY_START_LEVEL) / (double) (DAY_FULL_LEVEL - DAY_START_LEVEL);
	}

	public static boolean rainSpawns(int level) {
		return clamp(level) >= RAIN_LEVEL;
	}

	// ---- XP (D207) ----

	/** XP of a danger-boosted mob: the vanilla XP times its health ratio, (1 + strength bonus) x (2 for an elite). */
	public static double xpFactor(double healthBonus, boolean elite) {
		return (1.0 + Math.max(0, healthBonus)) * (elite ? 2.0 : 1.0);
	}

	/** Scales a vanilla XP reward by the factor, rounded; never below the vanilla amount, 0 stays 0. */
	public static int scaledXp(int baseXp, double factor) {
		if (baseXp <= 0 || factor <= 1.0) {
			return baseXp;
		}
		return (int) Math.round(baseXp * factor);
	}

	/** Mob types that join the Overworld monster spawns, with the level that unlocks them. */
	public enum Type {
		PILLAGER(7, 5, false), VINDICATOR(10, 5, false), BLAZE(17, 3, true), WITHER_SKELETON(17, 3, true),
		EVOKER(20, 2, false), PIGLIN_BRUTE(23, 3, false), RAVAGER(27, 1, false);

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

	/** Per-slot chance of extra armor: none below 35, 20% at 35 rising to 70% at 50. */
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

	/** D139: 0 below level 33; 5% at 33 rising linearly to 15% at 50. */
	public static double eliteChance(int level) {
		int l = clamp(level);
		if (l < MIN_ELITE_LEVEL) {
			return 0;
		}
		return 0.05 + 0.10 * (l - MIN_ELITE_LEVEL) / (double) (MAX_LEVEL - MIN_ELITE_LEVEL);
	}

	public static int maxAffixes(int level) {
		int l = clamp(level);
		if (l < MIN_ELITE_LEVEL) {
			return 0;
		}
		return l < AFFIX2_LEVEL ? 1 : l < AFFIX3_LEVEL ? 2 : 3;
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

	/** Rolls 1..maxAffixes distinct affixes. Empty below MIN_ELITE_LEVEL. */
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
			case 7 -> "Pillagers appear";
			case 10 -> "Vindicators appear, rain spawns";
			case 13 -> "day spawns";
			case 17 -> "Blazes and Wither Skeletons appear";
			case 20 -> "Evokers appear";
			case 23 -> "Piglin Brutes appear";
			case 27 -> "Ravagers appear";
			case 33 -> "elites appear, day as busy as night";
			case 35 -> "more armor";
			case 50 -> "netherite armor";
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
