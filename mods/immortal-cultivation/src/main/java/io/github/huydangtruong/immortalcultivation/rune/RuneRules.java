package io.github.huydangtruong.immortalcultivation.rune;

/** Numbers of runes and the Rune Forge (D241, D242), no Minecraft imports so JUnit can test them. */
public final class RuneRules {
	/** Vanilla never lets an enchantment level go above this. */
	public static final int MAX_ENCHANT_LEVEL = 255;
	/** Inscribing a rune costs this many levels per rune level. */
	public static final int COST_PER_LEVEL = 5;

	private RuneRules() {}

	/** D292: each rune level is 1.5 times the level before. */
	public static final double GROWTH = 1.5;

	/**
	 * D292 (was D242's 2^(n-1)): enchantment levels a rune adds: ceil(1.5^(n-1)), capped at 255. Rounding up keeps every
	 * level stronger than the one before: 1, 2, 3, 4, 6, 8, 12, 18, 26, 39, 58, 87, 130, 195, 255 ...
	 */
	public static int bonus(int runeLevel) {
		int n = Math.max(1, runeLevel);
		if (n > 15) {
			return MAX_ENCHANT_LEVEL;
		}
		return (int) Math.min(MAX_ENCHANT_LEVEL, Math.ceil(Math.pow(GROWTH, n - 1) - 1e-9));
	}

	/** Base level plus every rune bonus, capped like vanilla. */
	public static int total(int base, int runeBonus) {
		return Math.min(MAX_ENCHANT_LEVEL, Math.max(0, base) + Math.max(0, runeBonus));
	}

	/** D242: experience levels to put a rune in a slot (taking it out is free). */
	public static int inscribeCost(int runeLevel) {
		return COST_PER_LEVEL * Math.max(1, runeLevel);
	}

	/**
	 * D242: rune slots of an item, from its id path: netherite 4, diamond 2, iron 1; mace and trident 4, bow and crossbow 2,
	 * fishing rod 1 (manager choice); anything else 0.
	 */
	public static int slots(String itemPath) {
		if (itemPath.startsWith("netherite_") || itemPath.equals("mace") || itemPath.equals("trident")) {
			return 4;
		}
		if (itemPath.startsWith("diamond_") || itemPath.equals("bow") || itemPath.equals("crossbow")) {
			return 2;
		}
		if (itemPath.startsWith("iron_") || itemPath.equals("fishing_rod")) {
			return 1;
		}
		return 0;
	}

	/** D264: the look of a rune's stone by what it is for: attack (red), armor (blue), ranged (green), tool (teal), other (grey). */
	public static String kind(String enchantmentPath) {
		return switch (enchantmentPath) {
			case "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge", "density",
				"breach", "wind_burst" -> "attack";
			case "protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "feather_falling",
				"respiration", "depth_strider", "frost_walker", "soul_speed", "swift_sneak" -> "armor";
			case "power", "punch", "quick_charge", "piercing", "loyalty", "riptide", "impaling", "lunge", "throw_power" -> "ranged";
			case "efficiency", "fortune", "unbreaking", "lure", "luck_of_the_sea" -> "tool";
			default -> "other";
		};
	}

	/** Highest level the number on a rune shows (D278). */
	public static final int MAX_SHOWN_LEVEL = 99;

	/**
	 * D278: the two number layers of a rune texture: a lone digit is centred ("c3", ""), two digits are tens and ones
	 * ("t1", "o2"). Levels above 99 show 99.
	 */
	public static String[] numberLayers(int level) {
		int n = Math.max(1, Math.min(MAX_SHOWN_LEVEL, level));
		return n < 10 ? new String[]{"c" + n, ""} : new String[]{"t" + n / 10, "o" + n % 10};
	}

	/** "I", "II" ... up to 10, then plain numbers. */
	public static String roman(int n) {
		String[] r = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		return n >= 1 && n <= r.length ? r[n - 1] : String.valueOf(n);
	}
}
