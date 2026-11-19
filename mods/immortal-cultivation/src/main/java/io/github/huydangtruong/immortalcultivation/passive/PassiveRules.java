package io.github.huydangtruong.immortalcultivation.passive;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Numbers of the passives (D148). Pure Java so JUnit can test it. Level 0 = not learned, 1..MAX_LEVEL. */
public final class PassiveRules {
	public static final int MAX_LEVEL = 10;
	/** Vanilla XP levels paid for each upgrade. */
	public static final int COST_LEVELS = 5;

	/** Inventory slot indices (Inventory: 0-8 hotbar, 9-35 rows, 36 feet, 37 legs, 38 chest, 39 head, 40 offhand). */
	public static final int FEET = 36;
	public static final int LEGS = 37;
	public static final int CHEST = 38;
	public static final int HEAD = 39;
	public static final int OFFHAND = 40;

	private PassiveRules() {}

	public static int clamp(int level) {
		return Math.max(0, Math.min(MAX_LEVEL, level));
	}

	/** Reduce cooldown: 3% at level 1, +1.5% per level, 16.5% at level 10. */
	public static double cooldownPercent(int level) {
		level = clamp(level);
		return level == 0 ? 0 : 3 + 1.5 * (level - 1);
	}

	/** A cooldown of base ticks after the reduction (rounded, never below 1 tick when base > 0). */
	public static long reducedCooldown(long base, int level) {
		if (base <= 0) {
			return base;
		}
		return Math.max(1, Math.round(base * (1 - cooldownPercent(level) / 100.0)));
	}

	/** Increase vision: night vision strength, 0.2 at level 1 up to 1.2 at level 10 (+1/9 per level). */
	public static double vision(int level) {
		level = clamp(level);
		return level == 0 ? 0 : 0.2 + (level - 1) / 9.0;
	}

	/** Hunger sustain: the food bar never drops below this many points (2 points = 1 drumstick). 5 at level 1, 14 at 10. */
	public static int hungerFloor(int level) {
		level = clamp(level);
		return level == 0 ? 0 : 4 + level;
	}

	/** XP gain bonus: +3% per level, 30% at level 10. Vanilla XP orbs only. */
	public static int xpPercent(int level) {
		return 3 * clamp(level);
	}

	/** XP actually given for amount picked up. roll in [0,1) rounds the fractional bonus up with that probability. */
	public static int boostedXp(int amount, int level, double roll) {
		if (amount <= 0) {
			return amount;
		}
		double bonus = amount * xpPercent(level) / 100.0;
		int whole = (int) Math.floor(bonus);
		return amount + whole + (roll < bonus - whole ? 1 : 0);
	}

	/** Crop growth (P-058): +20% growth per level, +200% at level 10 (3 times as fast). */
	public static int growthPercent(int level) {
		return 20 * clamp(level);
	}

	/** Extra growth attempts for one random tick of an owned crop; roll in [0,1) rounds the fraction by chance. */
	public static int extraGrowth(int level, double roll) {
		int pct = growthPercent(level);
		return pct / 100 + (roll < (pct % 100) / 100.0 ? 1 : 0);
	}

	/** Name of the slot group a level adds (1-based), shown in the tab. */
	public static String inventoryGroup(int level) {
		return switch (level) {
			case 1 -> "Main hand";
			case 2 -> "Offhand";
			case 3 -> "Helmet";
			case 4 -> "Boots";
			case 5 -> "Chest";
			case 6 -> "Legs";
			case 7 -> "Hotbar";
			case 8 -> "Row 1";
			case 9 -> "Row 2";
			case 10 -> "Row 3";
			default -> "Nothing";
		};
	}

	/** Inventory slots kept on death at this level; selected = the hotbar slot in the main hand (0..8). */
	public static List<Integer> protectedSlots(int level, int selected) {
		level = clamp(level);
		List<Integer> out = new ArrayList<>();
		if (level >= 1 && selected >= 0 && selected < 9) {
			out.add(selected);
		}
		int[] single = {OFFHAND, HEAD, FEET, CHEST, LEGS};
		for (int i = 0; i < single.length && level >= i + 2; i++) {
			out.add(single[i]);
		}
		for (int row = 0; row < 4 && level >= row + 7; row++) {
			for (int s = row * 9; s < row * 9 + 9; s++) {
				if (!out.contains(s)) {
					out.add(s);
				}
			}
		}
		return out;
	}

	/** The value shown for a passive at a level, e.g. "7.5%", "2.5", "Helmet". */
	public static String value(Passive p, int level) {
		return switch (p) {
			case COOLDOWN -> pct(cooldownPercent(level));
			case VISION -> pct(vision(level) * 100);
			case HUNGER -> num(hungerFloor(level) / 2.0);
			case XP -> xpPercent(level) + "%";
			case GROWTH -> "+" + growthPercent(level) + "%";
			case INVENTORY -> protectedSlots(level, 0).size() + " slots";
		};
	}

	/** Row title: name and current value, e.g. "Reduce cooldown 7.5%", "Inventory protect 14 slots". */
	public static String title(Passive p, int level) {
		return p.displayName() + " " + value(p, clamp(level));
	}

	/** What a level shows in the detail table: the value, or for Inventory protect the slot group it adds. */
	public static String tableValue(Passive p, int level) {
		return p == Passive.INVENTORY ? "+" + inventoryGroup(level) : value(p, level);
	}

	static String pct(double v) {
		return num(Math.round(v * 10) / 10.0) + "%";
	}

	static String num(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
	}
}
