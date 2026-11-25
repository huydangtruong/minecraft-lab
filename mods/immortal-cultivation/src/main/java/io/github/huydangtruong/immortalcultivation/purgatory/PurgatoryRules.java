package io.github.huydangtruong.immortalcultivation.purgatory;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Numbers of the Purgatory (D234-D240), no Minecraft imports so JUnit can test them. Floors start at 1 and never end;
 * every number that makes a floor harder grows with the floor and never shrinks, and health and damage grow strictly.
 */
public final class PurgatoryRules {
	/** Outer size of the old bedrock box (D235, before P-082): only to clean up a run saved with one. */
	public static final int OLD_BOX = 64;
	/** Boxes stand this far apart on the x axis of the purgatory dimension. */
	public static final int SLOT_SPACING = 256;
	/** Lowest y of a dungeon (its bottom layer). */
	public static final int BOX_MIN_Y = 64;

	/** D255: the boss must die within 10 minutes. */
	public static final int RUN_TICKS = 10 * 60 * 20;
	/** A new floor every 3 minutes per mode (D234, D251, D254). */
	public static final int COOLDOWN_TICKS = 3 * 60 * 20;
	/** At most 50 mobs alive at once (D236). */
	public static final int MAX_ALIVE = 50;
	/** D261: runes per reward bag (a shulker box). */
	public static final int BAG_SIZE = 27;
	/** Team queue: start 10 s after the last player joined it (D237). */
	public static final int TEAM_COUNTDOWN_TICKS = 10 * 20;
	public static final int TEAM_MIN_PLAYERS = 2;
	/** Mobs never appear closer than this to a fighter. */
	public static final int SPAWN_MIN_DISTANCE = 8;

	/** Floors with a warden (a stronger mob that comes first). */
	public static final int WARDEN_EVERY = 10;
	/** Elites from this floor on. */
	public static final int ELITE_START = 10;

	private PurgatoryRules() {}

	/** D253: max health multiplier of a floor's mobs: x1 on floor 1, x2.5 on floor 10 (+1/6 per floor). */
	public static double healthMultiplier(int floor) {
		return 1.0 + (Math.max(1, floor) - 1) / 6.0;
	}

	/** D253: attack damage multiplier: x1 on floor 1, x2 on floor 10 (+1/9 per floor). */
	public static double damageMultiplier(int floor) {
		return 1.0 + (Math.max(1, floor) - 1) / 9.0;
	}

	/** Extra armour points: +1 every 3 floors. */
	public static int armorBonus(int floor) {
		return (Math.max(1, floor) - 1) / 3;
	}

	/** Extra movement speed (fraction of base): +1% per floor up to +30%. */
	public static double speedBonus(int floor) {
		return Math.min(0.30, 0.01 * (Math.max(1, floor) - 1));
	}

	/** Chance that a mob is an elite: none below floor 10, then +3% per floor up to 60%. */
	public static double eliteChance(int floor) {
		if (floor < ELITE_START) {
			return 0;
		}
		return Math.min(0.60, 0.05 + 0.03 * (floor - ELITE_START));
	}

	/** Team runs: mob health grows with the number of fighters (x1.5 per extra player). */
	public static double teamHealthFactor(int players) {
		return 1.0 + 0.5 * Math.max(0, players - 1);
	}

	/** Mob types that may appear on a floor (entity ids without namespace). New ones join, none ever leaves. */
	public static List<String> mobPool(int floor) {
		List<String> out = new ArrayList<>(List.of("zombie", "skeleton"));
		if (floor >= 3) {
			out.add("husk");
		}
		if (floor >= 5) {
			out.add("spider"); // D256: no creepers, they would blow up the rooms
		}
		if (floor >= 8) {
			out.add("stray");
			out.add("cave_spider");
		}
		if (floor >= 10) {
			out.add("witch");
			out.add("vindicator");
		}
		if (floor >= 15) {
			out.add("pillager");
			out.add("slime");
		}
		if (floor >= 20) {
			out.add("wither_skeleton");
			out.add("blaze");
		}
		if (floor >= 25) {
			out.add("piglin_brute");
			out.add("magma_cube");
		}
		if (floor >= 30) {
			out.add("evoker");
			out.add("ravager");
		}
		return out;
	}

	/** The boss of every 10th floor (a "Floor Warden", stronger than the other bosses). */
	public static String wardenType(int floor) {
		if (floor >= 30) {
			return "ravager";
		}
		if (floor >= 20) {
			return "wither_skeleton";
		}
		return "vindicator";
	}

	/** D240: levels for winning floor n. */
	public static int rewardLevels(int floor) {
		return 30 + floor;
	}

	/** D251: runes each winner gets for floor n: 3 per player in the run plus the floor number. */
	public static int rewardRunes(int floor, int players) {
		return 3 * Math.max(1, players) + Math.max(0, floor);
	}

	/** D264: highest reward rune level of floor n: 3 below floor 20, 4 from 20, 5 from 30, 6 from 50, 7 from 70, 8 from 100. */
	public static int maxRuneLevel(int floor) {
		return floor >= 100 ? 8 : floor >= 70 ? 7 : floor >= 50 ? 6 : floor >= 30 ? 5 : floor >= 20 ? 4 : 3;
	}

	/** A reward rune level of floor n, 1..{@link #maxRuneLevel}, each equally likely. */
	public static int rollRuneLevel(RandomGenerator rng, int floor) {
		return 1 + rng.nextInt(maxRuneLevel(floor));
	}

	/** Centre x of box slot i (z is always 0). */
	public static int slotCenterX(int slot) {
		return slot * SLOT_SPACING;
	}

	/** "m:ss" for a tick count. */
	public static String clock(int ticks) {
		int s = Math.max(0, (ticks + 19) / 20);
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}
}
