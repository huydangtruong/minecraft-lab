package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/** Numbers of the Purgatory dungeon (P-082), no Minecraft imports. */
public final class DungeonRules {
	/** A cell is CELL x CELL blocks and CELL_H high (floor and ceiling included). */
	public static final int CELL = 15;
	public static final int CELL_H = 12;
	public static final int DOOR_H = 4;
	/** Cells per side of the floor plan and the most layers. */
	public static final int COLS = 6;
	public static final int ROWS = 6;
	public static final int MAX_LAYERS = 2;
	public static final int SIZE_X = COLS * CELL;
	public static final int SIZE_Y = MAX_LAYERS * CELL_H;
	public static final int SIZE_Z = ROWS * CELL;
	/** A second layer from this floor. */
	public static final int TWO_LAYERS_FROM = 5;
	/** Floors with this many runs of the rest shrine. */
	public static final int REST_FROM = 3;
	/** D262: a floor has up to this many rooms more than {@link #rooms} (random, 0 = the standard length). */
	public static final int EXTRA_ROOMS_MAX = 6;
	/** D262: chance that growing the floor may give a room a third door (a fork); otherwise rooms keep two. */
	public static final double BRANCH_CHANCE = 0.12;
	/** Chance that two neighbouring dead ends get a door between them (loops). */
	public static final double LOOP_CHANCE = 0.3;
	/** Time limit of a floor; running out is a loss. */
	public static final int TIME_TICKS = 10 * 60 * 20;
	/** A sealed room whose mobs are not dead: they glow after this, the gates open after the second number. */
	public static final int GLOW_AFTER_TICKS = 60 * 20;
	public static final int OPEN_AFTER_TICKS = 150 * 20;
	/** Boss health on top of the floor multiplier, and how often it calls adds. */
	public static final double BOSS_HEALTH = 8.0;
	/** D261: bosses are twice the normal size and hit 20% harder from floor 1. */
	public static final double BOSS_SCALE = 2.0;
	public static final double BOSS_DAMAGE = 1.2;
	public static final int BOSS_ADDS_EVERY_TICKS = 20 * 20;

	private DungeonRules() {}

	public static int layers(int floor) {
		return floor >= TWO_LAYERS_FROM ? 2 : 1;
	}

	/** Rooms besides the boss room (the stairs count). */
	public static int rooms(int floor) {
		return Math.min(20, 10 + (Math.max(1, floor) - 1) / 2); // D259: floors 1-2 took about a minute with 6
	}

	/** Mobs that come when a room wakes. */
	public static int wave(RoomKind kind, int floor, int players) {
		int f = Math.max(1, floor);
		int extra = Math.max(0, players - 1);
		return switch (kind) {
			case LOCKED -> Math.min(14, 5 + f / 3 + 2 * extra);
			case TREASURE -> Math.min(16, 7 + f / 3 + 2 * extra);
			case BOSS -> Math.min(8, 2 + f / 8 + extra);
			case OPEN, DARK -> Math.min(10, 5 + f / 5 + extra); // D261: the path rooms had too few
			case CORRIDOR, HAZARD, TRAP -> Math.min(9, 4 + f / 6 + extra);
			case STAIRS -> Math.min(6, 2 + f / 8 + extra / 2);
			default -> 0;
		};
	}

	/** Waves a sealed room sends (D259): the second comes when the first is dead. */
	public static int waves(RoomKind kind) {
		return kind == RoomKind.LOCKED || kind == RoomKind.TREASURE ? 2 : 1;
	}

	/** After the boss dies the fighters stay this long (D260), or leave at once with Come back now. */
	public static final int VICTORY_TICKS = 30 * 20;

	/** Mobs a boss calls every {@link #BOSS_ADDS_EVERY_TICKS}. */
	public static int bossAdds(int floor, int players) {
		return Math.min(5, 1 + Math.max(1, floor) / 10 + Math.max(0, players - 1) / 2);
	}

	/** Boss mob id by floor (the warden floors are decided by PurgatoryRules). */
	public static String boss(int floor, int roll) {
		int f = Math.max(1, floor);
		String[] tier;
		if (f < 5) {
			tier = new String[] {"minecraft:zombie", "minecraft:husk"};
		} else if (f < 10) {
			tier = new String[] {"minecraft:vindicator"};
		} else if (f < 20) {
			tier = new String[] {"minecraft:wither_skeleton", "minecraft:ravager"};
		} else if (f < 30) {
			tier = new String[] {"minecraft:ravager", "minecraft:piglin_brute"};
		} else {
			tier = new String[] {"minecraft:evoker", "minecraft:ravager", "minecraft:piglin_brute"};
		}
		return tier[Math.floorMod(roll, tier.length)];
	}
}
