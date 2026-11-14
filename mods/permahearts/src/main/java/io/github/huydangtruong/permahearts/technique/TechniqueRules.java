package io.github.huydangtruong.permahearts.technique;

import java.util.List;

/**
 * Pure rules for techniques (D43-D45): XP costs, refunds, emerald slot thresholds, per-level numbers and the skill
 * key cycle. No Minecraft imports, JUnit-tested.
 */
public final class TechniqueRules {
	public static final int MAX_LEVEL = 20;
	public static final int MAX_SLOTS = 4;
	/** Emeralds for slot 1, 2, 3, 4 (each on top of the previous). */
	public static final int[] SLOT_STEPS = {64, 256, 512, 1024};
	public static final int MAX_EMERALDS = 64 + 256 + 512 + 1024;
	/** Seconds a charged throw needs to reach full power (like a bow). */
	public static final float FULL_CHARGE_SECONDS = 1f;

	private TechniqueRules() {}

	// ---- XP ----

	/** Experience levels the upgrade to toLevel costs: round(5.8 + 0.4 * toLevel) = 6, 7, 7 ... 14. */
	public static int upgradeCost(int toLevel) {
		if (toLevel < 1 || toLevel > MAX_LEVEL) {
			throw new IllegalArgumentException("level " + toLevel);
		}
		return (63 + 4 * toLevel) / 10;
	}

	/** Levels spent in total to go from 0 to level (80 for 10, 200 for 20). */
	public static int totalCost(int level) {
		int sum = 0;
		for (int l = 1; l <= level; l++) {
			sum += upgradeCost(l);
		}
		return sum;
	}

	/** Vanilla Player#getXpNeededForNextLevel for a level. */
	public static int xpNeededForNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
	}

	/** Experience points a player at this level and progress (0..1) holds. */
	public static int points(int level, float progress) {
		int sum = 0;
		for (int l = 0; l < level; l++) {
			sum += xpNeededForNextLevel(l);
		}
		return sum + Math.round(progress * xpNeededForNextLevel(level));
	}

	/** Reset gives back half of the points spent on the technique. */
	public static int refund(int spentPoints) {
		return Math.max(0, spentPoints) / 2;
	}

	// ---- slots ----

	/** Slots open for this many emeralds deposited in total (0..4). */
	public static int slotsFor(int deposited) {
		int slots = 0;
		int need = 0;
		for (int step : SLOT_STEPS) {
			need += step;
			if (deposited >= need) {
				slots++;
			}
		}
		return slots;
	}

	/** Emeralds still missing for the next slot (0 when all 4 are open). */
	public static int missingForNextSlot(int deposited) {
		int need = 0;
		for (int step : SLOT_STEPS) {
			need += step;
			if (deposited < need) {
				return need - deposited;
			}
		}
		return 0;
	}

	/** Emeralds the next slot costs in all (its step), 0 when all are open. */
	public static int nextSlotStep(int deposited) {
		int slots = slotsFor(deposited);
		return slots >= MAX_SLOTS ? 0 : SLOT_STEPS[slots];
	}

	// ---- skill key ----

	/**
	 * Next selection when the skill key is tapped: -1 is the empty (✕) selection, 0..3 a slot. Goes ✕ → each filled slot
	 * in order → ✕, skipping empty slots.
	 */
	public static int nextSelection(int current, boolean[] filled) {
		for (int i = current + 1; i < filled.length; i++) {
			if (filled[i]) {
				return i;
			}
		}
		return -1;
	}

	// ---- numbers per level ----

	/** base at level 1, + step per level up to 10, + step / 2 per level after 10 (D45). */
	public static double scaled(double base, double step, int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return base + step * (Math.min(l, 10) - 1) + step / 2 * Math.max(0, l - 10);
	}

	public static double fireRange(int level) {
		return scaled(16, 2, level);
	}

	public static double fireImpactDamage(int level) {
		return scaled(4, 1, level);
	}

	public static double fireFieldRadius(int level) {
		return scaled(2, 0.1, level);
	}

	public static double fireFieldSeconds(int level) {
		return scaled(4, 0.2, level);
	}

	/** Burn damage per second in the field: like normal fire, stronger from level 6 (the colour stays the same). */
	public static double fireBurnPerSecond(int level) {
		return level >= 6 ? 2 : 1;
	}

	public static double iceRange(int level) {
		return scaled(16, 2, level);
	}

	public static double iceImpactDamage(int level) {
		return 2;
	}

	public static double iceRadius(int level) {
		return scaled(3, 0.2, level);
	}

	public static double iceAreaSeconds(int level) {
		return 8; // D54: was 6
	}

	/** Move speed taken away inside the ice area (D54): 40% at level 1 up to 96% at 20. */
	public static double iceSlow(int level) {
		return linear(level, 0.40, 0.96); // D54: 40% at level 1, 96% at 20
	}

	/** Attack speed taken away inside the ice area. */
	public static double iceAttackSlow(int level) {
		return linear(level, 0.40, 0.96); // D54: same as the move slow
	}

	/** Frostbite lasts this long after leaving the area. */
	public static double iceLingerSeconds(int level) {
		return 4; // D54: was 3
	}

	public static double healingRadius(int level) {
		return 4;
	}

	public static double healingSeconds(int level) {
		return 10;
	}

	/** Health (half hearts) healed over the circle's whole duration: 20 = 10 hearts at level 1. */
	public static double healingTotal(int level) {
		return scaled(20, 4, level);
	}

	public static double phaseSeconds(int level) {
		return scaled(10, 0.5, level);
	}

	/** How far a phasing player sees through blocks (D51): 8 blocks at level 1 up to 48 at level 20, whole blocks. */
	public static int phaseSightRange(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return (int) Math.round(8 + 40.0 * (l - 1) / (MAX_LEVEL - 1));
	}

	/** Distance where the in-rock fog of Phase Walk starts: 60% of the sight range. */
	public static float phaseFogStart(int level) {
		return phaseSightRange(level) * 0.6f;
	}

	public static double repulseRadius(int level) {
		return scaled(4, 0.2, level);
	}

	/** Knockback strength (horizontal push, a little upward too). */
	public static double repulsePush(int level) {
		return scaled(1.0, 0.15, level);
	}

	public static double repulseDamage(int level) {
		return scaled(2, 0.5, level);
	}

	/** Share of the caster's current health given up (D46): 80% at level 1, 50% at 10, 30% at 20. */
	public static double sacrificeCost(int level) {
		return piecewise(level, 0.80, 0.50, 0.30);
	}

	/** Seconds of invulnerability (D46): 5 at level 1, 8 at 10, 10 at 20. */
	public static double sacrificeSeconds(int level) {
		return piecewise(level, 5, 8, 10);
	}

	/** A straight line from (1, at1) to (20, at20). */
	public static double linear(int level, double at1, double at20) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return at1 + (at20 - at1) * (l - 1) / (MAX_LEVEL - 1.0);
	}

	/** Straight lines through (1, at1), (10, at10), (20, at20). */
	public static double piecewise(int level, double at1, double at10, double at20) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return l <= 10 ? at1 + (at10 - at1) * (l - 1) / 9.0 : at10 + (at20 - at10) * (l - 10) / 10.0;
	}

	/** Seconds of creative-like flight (D54): 10 at level 1 up to 30 at 20. */
	public static double flightSeconds(int level) {
		return linear(level, 10, 30); // D54: 10 s at level 1, 30 s at 20
	}

	/** Flight speed on top of the creative flying speed (D54): -20% (80% of creative) at level 1 up to +100% at 20. */
	public static double flightSpeedBonus(int level) {
		return linear(level, -0.20, 1.0); // D54: 80% of creative at level 1, 200% at 20
	}

	/** Life Steal Blade damage on top of the player's base attack (D52): an iron sword's (+5) at level 1, +0.5 per level. */
	public static double bladeDamage(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return 5 + 0.5 * (l - 1);
	}

	/** Unused since the blade got a 12 s cooldown (D52); kept for reference. */
	public static final double BLADE_ATTACK_SPEED = 1.6;

	/** Vanilla creative flying speed (Abilities default); the flight speed bonus is applied on top of it. */
	public static final float CREATIVE_FLYING_SPEED = 0.05f;

	/** Abilities flying speed while Flight is on. */
	public static float flyingSpeed(int level) {
		return (float) (CREATIVE_FLYING_SPEED * (1 + flightSpeedBonus(level)));
	}

	/** Slow falling after Flight ends, in ticks (5 s). */
	public static final int FLIGHT_SLOW_FALL_TICKS = 100;

	/** About how far a vanilla ender pearl flies on flat ground at full strength (simulated: 1.5 speed, 0.03 gravity). */
	public static final double PEARL_RANGE = 50;

	/** Thousand Mile Step range as a multiple of an ender pearl's (D58): 3x at level 1 up to 50x at level 20. */
	public static double teleportPearls(int level) {
		return linear(level, 3, 50);
	}

	/** Thousand Mile Step distance in blocks at full charge: 150 at level 1, 2500 at level 20. */
	public static double teleportRange(int level) {
		return PEARL_RANGE * teleportPearls(level);
	}

	/** One line of the detail page: label and the value at a level, e.g. ("Range", "16 blocks"). */
	public record Stat(String label, String value) {}

	/** The numbers the Techniques tab shows for a technique at a level (level >= 1). */
	public static List<Stat> stats(Technique t, int level) {
		return switch (t) {
			case FIRE -> List.of(
				new Stat("Range", num(fireRange(level)) + " blocks"),
				new Stat("Impact", num(fireImpactDamage(level)) + " damage"),
				new Stat("Fire field", num(fireFieldRadius(level)) + " radius, " + num(fireFieldSeconds(level)) + " s"),
				new Stat("Burn", num(fireBurnPerSecond(level)) + " / s"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case ICE -> List.of(
				new Stat("Range", num(iceRange(level)) + " blocks"),
				new Stat("Area", num(iceRadius(level)) + " radius, " + num(iceAreaSeconds(level)) + " s"),
				new Stat("Slow", "-" + pct(iceSlow(level)) + " speed"),
				new Stat("Frostbite", "-" + pct(iceAttackSlow(level)) + " attack speed"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case HEALING -> List.of(
				new Stat("Circle", num(healingRadius(level)) + " radius, " + num(healingSeconds(level)) + " s"),
				new Stat("Heals", num(healingTotal(level) / 2) + " hearts"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case PHASE -> List.of(
				new Stat("Duration", num(phaseSeconds(level)) + " s"),
				new Stat("Sight", phaseSightRange(level) + " blocks through walls"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case REPULSE -> List.of(
				new Stat("Radius", num(repulseRadius(level)) + " blocks"),
				new Stat("Push", num(repulsePush(level))),
				new Stat("Damage", num(repulseDamage(level))),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case FLIGHT -> List.of(
				new Stat("Flight", num(flightSeconds(level)) + " s"),
				new Stat("Speed", pct(1 + flightSpeedBonus(level)) + " of creative"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case BLADE -> List.of(
				new Stat("Damage", num(bladeDamage(level))), // D57: just the number
				new Stat("Life steal", "all damage dealt"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case TELEPORT -> List.of(
				new Stat("Range", num(teleportRange(level)) + " blocks"),
				new Stat("Ender pearls", num(teleportPearls(level)) + "x"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case SACRIFICE -> List.of(
				new Stat("Costs", pct(sacrificeCost(level)) + " of current health"),
				new Stat("Invulnerable", num(sacrificeSeconds(level)) + " s"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
		};
	}

	/** "16", "2.9", "0.25" (no trailing zeros). */
	public static String num(double v) {
		double r = Math.round(v * 100) / 100.0;
		if (r == Math.rint(r)) {
			return String.valueOf((long) r);
		}
		String s = String.valueOf(r);
		return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
	}

	private static String pct(double fraction) {
		return Math.round(fraction * 100) + "%";
	}
}
