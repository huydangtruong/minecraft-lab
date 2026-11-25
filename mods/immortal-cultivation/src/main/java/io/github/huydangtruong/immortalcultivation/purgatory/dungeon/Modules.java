package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.Random;
import java.util.function.Consumer;

/**
 * The one-cell rooms a floor is made of (P-082). Each draws itself for any set of doors: {@link Canvas#cutDoors} runs after
 * it, so a module only has to keep its middle walkable. Looks borrow from vanilla structures: mineshafts, trial chambers,
 * end cities, ancient cities, nether fortresses, ocean monuments, strongholds.
 */
public enum Modules {
	// corridors
	STONE_HALL(RoomKind.CORRIDOR, 10, Modules::stoneHall, Style.BASE),
	MINE_TUNNEL(RoomKind.CORRIDOR, 10, Modules::mineTunnel, Style.MINE),
	COPPER_CROSSING(RoomKind.CORRIDOR, 8, Modules::copperCrossing, Style.TRIAL),
	SCULK_CORRIDOR(RoomKind.CORRIDOR, 8, Modules::sculkCorridor, Style.DEEP),
	TRAP_CORRIDOR(RoomKind.TRAP, 6, Modules::trapCorridor, Style.TRIAL, Style.BASE),
	// fights
	PILLAR_HALL(RoomKind.OPEN, 8, Modules::pillarHall, Style.BASE, Style.DEEP, Style.TRIAL),
	PURPUR_GALLERY(RoomKind.OPEN, 7, Modules::purpurGallery, Style.END),
	ARENA(RoomKind.LOCKED, 8, Modules::arena, Style.TRIAL),
	BALCONY_HALL(RoomKind.LOCKED, 7, Modules::balconyHall, Style.END, Style.LIBRARY),
	CRYPT(RoomKind.LOCKED, 7, Modules::crypt, Style.DEEP, Style.BASE),
	COPPER_LAB(RoomKind.LOCKED, 7, Modules::copperLab, Style.TRIAL),
	NETHER_HALL(RoomKind.LOCKED, 7, Modules::netherHall, Style.NETHER),
	// hazards
	LIBRARY_MAZE(RoomKind.HAZARD, 5, c -> maze(c, Mat.SHELF), Style.LIBRARY),
	HEDGE_MAZE(RoomKind.HAZARD, 5, c -> maze(c, Mat.LEAVES), Style.GARDEN),
	LAVA_BRIDGE(RoomKind.HAZARD, 5, Modules::lavaBridge, Style.NETHER),
	FLOODED_HALL(RoomKind.HAZARD, 5, Modules::floodedHall, Style.OCEAN),
	DARK_ROOM(RoomKind.DARK, 5, Modules::darkRoom, Style.DEEP),
	// special (placed by the layout, weight 0)
	REST_SHRINE(RoomKind.REST, 0, Modules::restShrine, Style.GARDEN),
	TREASURE_VAULT(RoomKind.TREASURE, 0, Modules::treasureVault, Style.TRIAL, Style.BASE),
	START(RoomKind.START, 0, Modules::start, Style.BASE),
	STAIR_UP(RoomKind.STAIRS, 0, Modules::stairUp, Style.BASE, Style.MINE, Style.TRIAL),
	STAIR_DOWN(RoomKind.STAIRS, 0, Modules::stairDown, Style.BASE, Style.MINE, Style.TRIAL);

	public final RoomKind kind;
	public final int weight;
	private final Consumer<Canvas> draw;
	private final Style[] styles;

	Modules(RoomKind kind, int weight, Consumer<Canvas> draw, Style... styles) {
		this.kind = kind;
		this.weight = weight;
		this.draw = draw;
		this.styles = styles;
	}

	public Style pickStyle(Random rng) {
		return styles[rng.nextInt(styles.length)];
	}

	public void draw(Canvas c) {
		draw.accept(c);
	}

	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	// ---------------------------------------------------------------- helpers

	static final int MID = DungeonRules.CELL / 2; // 7
	static final int IN0 = 1;
	static final int IN1 = DungeonRules.CELL - 2; // 13
	static final int TOP = DungeonRules.CELL_H - 2; // 10

	private static boolean has(Canvas c, Dir side) {
		for (Port p : c.room.ports) {
			if (p.side() == side) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Solid rock with tunnels from the middle to every door: {@code half} blocks each side of the centre line, {@code height}
	 * high.
	 */
	private static void passages(Canvas c, int half, int height, boolean lights) {
		c.shell();
		c.fill(IN0, 1, IN0, IN1, TOP, IN1, Mat.WALL);
		c.fill(MID - half, 1, MID - half, MID + half, height, MID + half, Mat.AIR);
		for (Dir s : Dir.values()) {
			if (has(c, s)) {
				arm(c, s, half, height, Mat.AIR);
			}
		}
		if (c.room.ports.size() == 1) { // a dead end gets a little chamber
			c.fill(MID - 3, 1, MID - 3, MID + 3, height, MID + 3, Mat.AIR);
		}
		if (lights) {
			c.set(MID, height, MID, Mat.LANTERN);
		}
	}

	/** Fill the arm towards a side (local = real, unrotated canvas). */
	private static void arm(Canvas c, Dir s, int half, int height, Mat m) {
		switch (s) {
			case N -> c.fill(MID - half, 1, IN0, MID + half, height, MID, m);
			case S -> c.fill(MID - half, 1, MID, MID + half, height, IN1, m);
			case W -> c.fill(IN0, 1, MID - half, MID, height, MID + half, m);
			default -> c.fill(MID, 1, MID - half, IN1, height, MID + half, m);
		}
	}

	/** Positions along an arm, from near the middle to the wall: {along, across-axis is x?}. */
	private static int[][] armSteps(Dir s, int every) {
		int n = 0;
		int[][] out = new int[8][];
		for (int k = MID - every; k >= IN0 + 2; k -= every) {
			int a = switch (s) {
				case N, W -> k;
				default -> DungeonRules.CELL - 1 - k;
			};
			out[n++] = new int[] {a};
		}
		return java.util.Arrays.copyOf(out, n);
	}

	private static void pillar(Canvas c, int x, int z, int size, Mat m) {
		c.fill(x, 1, z, x + size - 1, TOP, z + size - 1, m);
	}

	// ---------------------------------------------------------------- set pieces (D263)

	/** A column with a base and a capital. */
	private static void column(Canvas c, int x, int z, int top, Mat body) {
		c.fill(x, 1, z, x, top, z, body);
		c.set(x, 1, z, Mat.ACCENT);
		c.set(x, top, z, Mat.ACCENT);
	}

	/** Stairs rising towards local north: {@code n} steps, x0..x1 wide, the first (height 1) at z0, solid underneath. */
	private static void stepsNorth(Canvas f, int x0, int x1, int z0, int n) {
		for (int k = 1; k <= n; k++) {
			int z = z0 - k + 1;
			if (k > 1) {
				f.fill(x0, 1, z, x1, k - 1, z, Mat.WALL2);
			}
			f.fill(x0, k, z, x1, k, z, Mat.STAIR, Dir.N);
		}
	}

	/**
	 * A mezzanine along the local north wall (z 1..depth) at height y: columns under its edge, a rail on it and stairs up at
	 * its west end (x 3..4). Doorways (4 high) pass under it.
	 */
	private static void gallery(Canvas f, int y, int depth, Mat deck) {
		f.fill(IN0, y, IN0, IN1, y, depth, deck);
		for (int x = 5; x <= IN1; x++) {
			f.set(x, y + 1, depth, Mat.FENCE);
		}
		for (int x : new int[] {5, 10}) {
			f.fill(x, 1, depth, x, y - 1, depth, Mat.PILLAR);
		}
		stepsNorth(f, 3, 4, depth + y, y);
		f.set(8, y - 1, 2, Mat.LANTERN);
	}

	/** A floating slab with a lantern hanging under it. */
	private static void platform(Canvas c, int x0, int z0, int x1, int z1, int y, Mat m) {
		c.fill(x0, y, z0, x1, y, z1, m);
		c.set((x0 + x1) / 2, y - 1, (z0 + z1) / 2, Mat.LANTERN);
	}

	/** A one-wide bridge at height y (along x when {@code alongX}), with rails on both sides. */
	private static void bridge(Canvas c, int a0, int a1, int across, int y, boolean alongX, boolean rails) {
		for (int a = a0; a <= a1; a++) {
			int x = alongX ? a : across;
			int z = alongX ? across : a;
			c.set(x, y, z, Mat.FLOOR2);
			if (rails) {
				c.set(alongX ? x : x - 1, y + 1, alongX ? z - 1 : z, Mat.FENCE);
				c.set(alongX ? x : x + 1, y + 1, alongX ? z + 1 : z, Mat.FENCE);
			}
		}
	}

	/** A low wall to hide behind ("temporary wall"), topped with the accent block. */
	private static void cover(Canvas c, int x0, int z0, int x1, int z1, int h) {
		c.fill(x0, 1, z0, x1, h - 1, z1, Mat.WALL2);
		c.fill(x0, h, z0, x1, h, z1, Mat.ACCENT);
	}

	// ---------------------------------------------------------------- corridors

	/** Three-wide tunnels opening into a vaulted crossing with four columns and ribs on the ceiling. */
	private static void stoneHall(Canvas c) {
		passages(c, 1, 4, false);
		c.fill(3, 1, 3, 11, 7, 11, Mat.AIR);
		for (int[] p : new int[][] {{3, 3}, {11, 3}, {3, 11}, {11, 11}}) {
			column(c, p[0], p[1], 7, Mat.PILLAR);
		}
		for (int a = 4; a <= 10; a++) {
			c.set(a, 7, 3, Mat.ACCENT);
			c.set(a, 7, 11, Mat.ACCENT);
			c.set(3, 7, a, Mat.ACCENT);
			c.set(11, 7, a, Mat.ACCENT);
		}
		c.fill(5, 0, 5, 9, 0, 9, Mat.FLOOR2);
		c.set(MID, 6, MID, Mat.LANTERN);
		cover(c, 4, 4, 5, 4, 2);
		cover(c, 9, 10, 10, 10, 2);
		for (Dir s : Dir.values()) {
			if (has(c, s)) {
				for (int[] st : armSteps(s, 3)) {
					if (s == Dir.N || s == Dir.S) {
						c.fill(MID - 1, 4, st[0], MID + 1, 4, st[0], Mat.ACCENT);
					} else {
						c.fill(st[0], 4, MID - 1, st[0], 4, MID + 1, Mat.ACCENT);
					}
				}
			}
		}
	}

	private static void mineTunnel(Canvas c) {
		passages(c, 1, 3, false);
		for (Dir s : Dir.values()) {
			if (!has(c, s)) {
				continue;
			}
			boolean alongZ = s == Dir.N || s == Dir.S;
			for (int[] st : armSteps(s, 3)) {
				int a = st[0];
				if (alongZ) {
					c.set(MID - 1, 1, a, Mat.FENCE);
					c.set(MID - 1, 2, a, Mat.FENCE);
					c.set(MID + 1, 1, a, Mat.FENCE);
					c.set(MID + 1, 2, a, Mat.FENCE);
					c.fill(MID - 1, 3, a, MID + 1, 3, a, Mat.WALL2);
				} else {
					c.set(a, 1, MID - 1, Mat.FENCE);
					c.set(a, 2, MID - 1, Mat.FENCE);
					c.set(a, 1, MID + 1, Mat.FENCE);
					c.set(a, 2, MID + 1, Mat.FENCE);
					c.fill(a, 3, MID - 1, a, 3, MID + 1, Mat.WALL2);
				}
			}
			if (alongZ) {
				c.replace(MID, 1, s == Dir.N ? IN0 : MID, MID, 1, s == Dir.N ? MID : IN1, Mat.AIR, Mat.RAIL);
			} else {
				c.replace(s == Dir.W ? IN0 : MID, 1, MID, s == Dir.W ? MID : IN1, 1, MID, Mat.AIR, Mat.RAIL);
			}
		}
		for (int i = 0; i < 6; i++) {
			int x = MID - 1 + c.rng.nextInt(3);
			int z = MID - 1 + c.rng.nextInt(3);
			if (c.get(x, 3, z) == Mat.AIR && (x != MID || z != MID)) {
				c.set(x, 3, z, Mat.COBWEB);
			}
		}
		c.set(MID, 3, MID, Mat.LANTERN);
		c.replace(IN0, 0, IN0, IN1, 0, IN1, Mat.FLOOR, Mat.FLOOR2);
	}

	/** Trial-chamber crossing: copper partition walls in a pinwheel and two walkways crossing overhead, stairs up to them. */
	private static void copperCrossing(Canvas c) {
		Canvas f = c.facing(Dir.values()[c.rng.nextInt(4)]);
		f.shell();
		cover(f, 3, 3, 5, 3, 3);
		cover(f, 11, 3, 11, 5, 3);
		cover(f, 9, 11, 11, 11, 3);
		cover(f, 3, 9, 3, 11, 3);
		bridge(f, IN0, IN1, MID, 6, true, false);
		bridge(f, IN0, IN1, MID, 6, false, false);
		stepsNorth(f, 10, 11, 13, 6);
		for (int[] p : new int[][] {{4, 4}, {10, 4}, {4, 10}, {10, 10}}) {
			f.set(p[0], DungeonRules.CELL_H - 1, p[1], Mat.LIGHT);
		}
		f.set(MID, 5, MID, Mat.LANTERN);
		f.fill(5, 0, 5, 9, 0, 9, Mat.FLOOR2);
		f.replace(IN0, 1, IN0, IN1, 1, IN1, Mat.WALL, Mat.ACCENT);
	}

	private static void sculkCorridor(Canvas c) {
		passages(c, 1, 4, true);
		for (int x = IN0; x <= IN1; x++) {
			for (int z = IN0; z <= IN1; z++) {
				if (c.rng.nextInt(10) < 4) {
					c.set(x, 0, z, Mat.SCULK);
				}
				if (c.get(x, 1, z) == Mat.WALL && c.rng.nextInt(10) < 2) {
					c.set(x, 1, z, Mat.SCULK);
				}
			}
		}
		c.set(MID, 0, MID, Mat.LIGHT);
	}

	private static void trapCorridor(Canvas c) {
		passages(c, 1, 4, true);
		for (Dir s : Dir.values()) {
			if (!has(c, s)) {
				continue;
			}
			for (int[] st : armSteps(s, 2)) {
				int a = st[0];
				if (s == Dir.N || s == Dir.S) {
					c.plate(MID, 1, a);
					c.dispenser(MID - 2, 2, a, Dir.E);
					c.dispenser(MID + 2, 2, a, Dir.W);
				} else {
					c.plate(a, 1, MID);
					c.dispenser(a, 2, MID - 2, Dir.S);
					c.dispenser(a, 2, MID + 2, Dir.N);
				}
			}
		}
	}

	// ---------------------------------------------------------------- fights

	/** A hall with a mezzanine on one side, columns and low walls to hide behind. */
	private static void pillarHall(Canvas c) {
		Canvas f = c.facing(Dir.values()[c.rng.nextInt(4)]);
		f.shell();
		gallery(f, 5, 3, Mat.FLOOR2);
		for (int[] p : new int[][] {{4, 10}, {10, 10}, {MID, 12}}) {
			column(f, p[0], p[1], TOP, Mat.PILLAR);
			f.set(p[0], 6, p[1], Mat.LIGHT);
		}
		cover(f, 9, 7, 11, 7, 2);
		cover(f, 2, 10, 2, 12, 2);
		f.fill(6, 0, 6, 8, 0, 10, Mat.FLOOR2);
		f.set(MID, TOP, 8, Mat.LANTERN);
	}

	/** End-city gallery: floating purpur platforms joined by railed bridges, stairs up from the floor. */
	private static void purpurGallery(Canvas c) {
		Canvas f = c.facing(Dir.values()[c.rng.nextInt(4)]);
		f.shell();
		for (int x = IN0; x <= IN1; x++) {
			for (int z = IN0; z <= IN1; z++) {
				if ((x + z) % 2 == 0) {
					f.set(x, 0, z, Mat.FLOOR2);
				}
			}
		}
		platform(f, 2, 2, 4, 4, 4, Mat.FLOOR2);
		platform(f, 10, 2, 12, 4, 4, Mat.FLOOR2);
		platform(f, 6, 9, 8, 11, 4, Mat.FLOOR2);
		bridge(f, 5, 9, 3, 4, true, true);
		bridge(f, 4, 8, MID, 4, false, true);
		f.set(MID, 5, 2, Mat.AIR);
		f.set(MID, 5, 4, Mat.AIR);
		stepsNorth(f, 3, 4, 8, 4);
		for (int[] p : new int[][] {{IN0, IN1}, {IN1, IN1}, {IN1, 6}}) {
			column(f, p[0], p[1], TOP, Mat.PILLAR);
		}
		f.set(MID, 5, 10, Mat.DECOR);
		f.set(3, TOP, 9, Mat.LANTERN);
		f.set(11, TOP, 9, Mat.LANTERN);
		f.set(MID, TOP, 3, Mat.LANTERN);
	}

	private static void arena(Canvas c) {
		c.shell();
		c.fill(IN0, 1, IN0, IN1, 1, IN1, Mat.FLOOR2); // raised ring
		c.fill(4, 1, 4, 10, 1, 10, Mat.AIR); // sunken pit
		for (int a = 4; a <= 10; a++) {
			c.set(a, 1, 3, Mat.STAIR, Dir.N);
			c.set(a, 1, 11, Mat.STAIR, Dir.S);
			c.set(3, 1, a, Mat.STAIR, Dir.W);
			c.set(11, 1, a, Mat.STAIR, Dir.E);
		}
		for (int[] p : new int[][] {{IN0, IN0}, {IN1 - 1, IN0}, {IN0, IN1 - 1}, {IN1 - 1, IN1 - 1}}) {
			pillar(c, p[0], p[1], 2, Mat.ACCENT);
		}
		c.set(MID, 0, MID, Mat.LIGHT);
		c.fill(6, 0, 6, 8, 0, 8, Mat.FLOOR2);
		c.set(MID, 0, MID, Mat.LIGHT);
		for (int[] p : new int[][] {{4, 4}, {10, 4}, {4, 10}, {10, 10}}) {
			c.set(p[0], DungeonRules.CELL_H - 1, p[1], Mat.LIGHT);
		}
		for (int[] p : new int[][] {{3, 3}, {11, 3}, {3, 11}, {11, 11}}) { // columns at the corners of the pit
			column(c, p[0], p[1], TOP, Mat.PILLAR);
			c.set(p[0], 7, p[1], Mat.LIGHT);
		}
		platform(c, 6, 6, 8, 8, TOP - 1, Mat.ACCENT); // a chandelier over the pit
		c.fill(6, TOP, 6, 8, TOP, 8, Mat.AIR);
		c.set(MID, TOP, MID, Mat.PILLAR);
	}

	/** Mezzanines on two opposite walls joined by a railed bridge over the middle of the hall. */
	private static void balconyHall(Canvas c) {
		Dir side = Dir.values()[c.rng.nextInt(4)];
		Canvas f = c.facing(side);
		f.shell();
		gallery(f, 5, 3, Mat.FLOOR2);
		gallery(c.facing(side.opposite()), 5, 3, Mat.FLOOR2);
		bridge(f, 4, 10, MID, 5, false, true);
		f.set(MID, 6, 3, Mat.AIR);
		f.set(MID, 6, 11, Mat.AIR);
		f.set(MID, 4, MID, Mat.LANTERN);
		if (c.style() == Style.LIBRARY) {
			for (int x : new int[] {IN0, IN1}) {
				f.fill(x, 1, 6, x, 4, 8, Mat.SHELF);
			}
		}
		f.set(3, TOP, MID, Mat.LANTERN);
		f.set(11, TOP, MID, Mat.LANTERN);
		f.fill(5, 0, 5, 9, 0, 9, Mat.FLOOR2);
	}

	/** Rows of tombs between low walls, an arched ceiling and cobwebbed corners. */
	private static void crypt(Canvas c) {
		c.shell();
		for (int z : new int[] {3, 6, 9}) {
			c.fill(3, 1, z, 4, 1, z, Mat.WALL2);
			c.fill(10, 1, z, 11, 1, z, Mat.WALL2);
		}
		for (int[] p : new int[][] {{IN0, IN0}, {IN1, IN0}, {IN0, IN1}, {IN1, IN1}}) {
			c.set(p[0], 1, p[1], Mat.COBWEB);
			c.set(p[0], 2, p[1], Mat.COBWEB);
			c.set(p[0], TOP, p[1], Mat.COBWEB);
		}
		cover(c, 5, 4, 5, 5, 2);
		cover(c, 9, 9, 9, 10, 2);
		for (int[] p : new int[][] {{MID, 4}, {MID, 10}, {3, MID}, {11, MID}}) {
			column(c, p[0], p[1], TOP, Mat.PILLAR);
			c.set(p[0], 5, p[1], Mat.LIGHT);
		}
		for (int a = IN0; a <= IN1; a++) { // ribs of the vault
			c.set(a, TOP, 4, Mat.ACCENT);
			c.set(a, TOP, 10, Mat.ACCENT);
			c.set(4, TOP, a, Mat.ACCENT);
			c.set(10, TOP, a, Mat.ACCENT);
		}
		c.set(3, TOP - 1, 3, Mat.LANTERN);
		c.set(11, TOP - 1, 11, Mat.LANTERN);
	}

	/** Trial-chamber lab: four floating copper platforms in a ring of bridges, a wide stair up from the middle. */
	private static void copperLab(Canvas c) {
		Canvas f = c.facing(Dir.values()[c.rng.nextInt(4)]);
		f.shell();
		for (int[] p : new int[][] {{2, 2}, {10, 2}, {2, 10}, {10, 10}}) {
			platform(f, p[0], p[1], p[0] + 2, p[1] + 2, 4, Mat.ACCENT);
			f.set(p[0] + 1, DungeonRules.CELL_H - 1, p[1] + 1, Mat.LIGHT);
		}
		bridge(f, 5, 9, 3, 4, true, false);
		bridge(f, 5, 9, 11, 4, true, false);
		bridge(f, 5, 9, 3, 4, false, false);
		bridge(f, 5, 9, 11, 4, false, false);
		stepsNorth(f, 6, 8, 7, 4);
		f.fill(5, 0, 8, 9, 0, 12, Mat.FLOOR2);
		f.set(MID, 1, 10, Mat.LIGHT);
		for (int x = 2; x <= 12; x += 2) {
			f.fill(x, DungeonRules.CELL_H - 1, 6, x, DungeonRules.CELL_H - 1, 8, Mat.ACCENT);
		}
	}

	/** Nether fortress hall: magma floor, braziers, and a railed fortress bridge across it on arches. */
	private static void netherHall(Canvas c) {
		Canvas f = c.facing(Dir.values()[c.rng.nextInt(4)]);
		f.shell();
		for (int x = 3; x <= 11; x++) {
			for (int z = 3; z <= 11; z++) {
				if (f.rng.nextInt(100) < 15) {
					f.set(x, 0, z, Mat.MAGMA);
				}
			}
		}
		for (int[] p : new int[][] {{IN0 + 1, IN1 - 1}, {IN1 - 1, IN0 + 1}}) { // braziers
			f.fill(p[0] - 1, 1, p[1] - 1, p[0] + 1, 1, p[1] + 1, Mat.WALL2);
			f.set(p[0], 1, p[1], Mat.LAVA);
		}
		bridge(f, IN0, IN1, MID, 5, true, true);
		f.set(9, 6, 8, Mat.AIR);
		f.set(10, 6, 8, Mat.AIR);
		stepsNorth(f, 9, 10, 12, 5);
		for (int x : new int[] {4, 7}) { // arches under the bridge
			f.fill(x, 1, MID, x, 4, MID, Mat.PILLAR);
		}
		for (int[] p : new int[][] {{4, 4}, {10, 4}, {4, 10}}) {
			f.fill(p[0], 1, p[1], p[0], TOP, p[1], Mat.FENCE);
		}
		f.set(4, DungeonRules.CELL_H - 1, 10, Mat.LIGHT);
		f.set(10, DungeonRules.CELL_H - 1, 4, Mat.LIGHT);
		f.set(MID, DungeonRules.CELL_H - 1, MID, Mat.LIGHT);
		f.set(2, 4, MID, Mat.LANTERN);
	}

	// ---------------------------------------------------------------- hazards

	/** A perfect maze of 7 x 7 cells, walls 3 high: no way over, always a way through. */
	private static void maze(Canvas c, Mat wall) {
		c.shell();
		c.fill(IN0, 1, IN0, IN1, 3, IN1, wall);
		int n = 7;
		boolean[][] seen = new boolean[n][n];
		java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
		stack.push(new int[] {n / 2, n / 2});
		seen[n / 2][n / 2] = true;
		c.fill(1 + 2 * (n / 2), 1, 1 + 2 * (n / 2), 1 + 2 * (n / 2), 3, 1 + 2 * (n / 2), Mat.AIR);
		while (!stack.isEmpty()) {
			int[] at = stack.peek();
			java.util.List<Dir> open = new java.util.ArrayList<>();
			for (Dir dd : Dir.values()) {
				int i = at[0] + dd.dx;
				int j = at[1] + dd.dz;
				if (i >= 0 && j >= 0 && i < n && j < n && !seen[i][j]) {
					open.add(dd);
				}
			}
			if (open.isEmpty()) {
				stack.pop();
				continue;
			}
			Dir go = open.get(c.rng.nextInt(open.size()));
			int i = at[0] + go.dx;
			int j = at[1] + go.dz;
			seen[i][j] = true;
			c.fill(1 + 2 * at[0] + go.dx, 1, 1 + 2 * at[1] + go.dz, 1 + 2 * i, 3, 1 + 2 * j, Mat.AIR);
			stack.push(new int[] {i, j});
		}
		for (int x = 3; x <= 11; x += 4) {
			for (int z = 3; z <= 11; z += 4) {
				c.set(x, DungeonRules.CELL_H - 1, z, Mat.LIGHT);
			}
		}
		for (int x = IN0; x <= IN1; x++) {
			for (int z = IN0; z <= IN1; z++) {
				if (c.get(x, 1, z) == Mat.AIR && c.rng.nextInt(10) < 3) {
					c.set(x, 1, z, Mat.CARPET);
				}
			}
		}
	}

	/** Raised floor at y 1 with a pool (lava or water) at the same height, at least 3 blocks from the walls. */
	private static void pool(Canvas c, Mat liquid) {
		c.shell();
		c.fill(IN0, 1, IN0, IN1, 1, IN1, Mat.FLOOR2);
		c.fill(4, 1, 4, 10, 1, 10, liquid);
	}

	/** A lava pool with a floating island over it, reached by four short bridges from steps on the rim. */
	private static void lavaBridge(Canvas c) {
		pool(c, Mat.LAVA);
		platform(c, 6, 6, 8, 8, 3, Mat.FLOOR2);
		c.set(MID, 2, MID, Mat.AIR); // no lantern hanging into the lava
		bridge(c, 4, 5, MID, 3, false, false);
		bridge(c, 9, 10, MID, 3, false, false);
		bridge(c, 4, 5, MID, 3, true, false);
		bridge(c, 9, 10, MID, 3, true, false);
		for (int[] p : new int[][] {{MID, 3}, {MID, 11}, {3, MID}, {11, MID}}) {
			c.set(p[0], 2, p[1], Mat.FLOOR2);
		}
		for (int[] p : new int[][] {{2, 2}, {12, 2}, {2, 12}, {12, 12}}) {
			column(c, p[0], p[1], TOP, Mat.PILLAR);
			c.set(p[0], 2, p[1], Mat.ACCENT);
		}
		c.set(6, 4, 6, Mat.LANTERN);
		c.set(8, 4, 8, Mat.LANTERN);
		c.set(MID, DungeonRules.CELL_H - 1, MID, Mat.LIGHT);
	}

	/** A flooded hall: broken arches on columns, an island and stepping stones. */
	private static void floodedHall(Canvas c) {
		pool(c, Mat.WATER);
		for (int[] p : new int[][] {{5, 5}, {9, 5}, {5, 9}, {9, 9}}) {
			c.fill(p[0], 1, p[1], p[0], TOP, p[1], Mat.PILLAR);
			c.set(p[0], 5, p[1], Mat.LIGHT);
		}
		c.fill(5, 6, 5, 9, 6, 5, Mat.WALL2); // arches between the columns (one broken)
		c.fill(5, 6, 9, 9, 6, 9, Mat.WALL2);
		c.fill(5, 6, 5, 5, 6, 9, Mat.WALL2);
		c.fill(9, 6, 6, 9, 6, 7, Mat.WALL2);
		c.fill(6, 1, 6, 8, 1, 8, Mat.FLOOR2); // an island
		c.set(MID, 2, MID, Mat.DECOR);
		for (int i = 0; i < 4; i++) {
			c.set(4 + c.rng.nextInt(7), 1, 4 + c.rng.nextInt(7), Mat.FLOOR2);
		}
		c.set(MID, DungeonRules.CELL_H - 1, MID, Mat.LIGHT);
	}

	/** Pitch dark: broken pillars and low walls at random among patches of sculk. */
	private static void darkRoom(Canvas c) {
		c.shell();
		for (int i = 0; i < 7; i++) {
			int x = 4 + c.rng.nextInt(7);
			int z = 4 + c.rng.nextInt(7);
			c.fill(x, 1, z, x, 2 + c.rng.nextInt(TOP - 1), z, Mat.PILLAR);
		}
		for (int i = 0; i < 3; i++) {
			int x = 3 + c.rng.nextInt(7);
			int z = 3 + c.rng.nextInt(7);
			if (c.rng.nextBoolean()) {
				cover(c, x, z, x + 2, z, 2);
			} else {
				cover(c, x, z, x, z + 2, 2);
			}
		}
		for (int x = IN0; x <= IN1; x++) {
			for (int z = IN0; z <= IN1; z++) {
				if (c.rng.nextInt(10) < 3) {
					c.set(x, 0, z, Mat.SCULK);
				}
			}
		}
	}

	// ---------------------------------------------------------------- special

	private static void restShrine(Canvas c) {
		c.shell();
		for (int[] p : new int[][] {{IN0, IN0}, {IN1 - 1, IN0}, {IN0, IN1 - 1}, {IN1 - 1, IN1 - 1}}) {
			c.fill(p[0], 1, p[1], p[0] + 1, 2, p[1] + 1, Mat.LEAVES);
		}
		for (int x = 3; x <= 11; x++) {
			for (int z = 3; z <= 11; z++) {
				if (c.rng.nextInt(10) < 4) {
					c.set(x, 1, z, Mat.CARPET);
				}
			}
		}
		c.fill(6, 0, 6, 8, 0, 8, Mat.FLOOR2);
		c.set(MID, 1, MID, Mat.CAMPFIRE);
		for (int[] p : new int[][] {{4, 4}, {10, 4}, {4, 10}, {10, 10}}) {
			c.set(p[0], TOP, p[1], Mat.LANTERN);
		}
	}

	private static void treasureVault(Canvas c) {
		c.shell();
		c.fill(IN0, 0, IN0, IN1, 0, IN1, Mat.FLOOR2);
		c.fill(3, 0, 3, 11, 0, 11, Mat.FLOOR);
		c.fill(5, 1, 5, 9, 1, 9, Mat.FLOOR2); // a dais with steps
		for (int a = 6; a <= 8; a++) {
			c.set(a, 1, 4, Mat.STAIR, Dir.S);
			c.set(a, 1, 10, Mat.STAIR, Dir.N);
			c.set(4, 1, a, Mat.STAIR, Dir.E);
			c.set(10, 1, a, Mat.STAIR, Dir.W);
		}
		c.chest(MID, 2, MID, Dir.S);
		for (int[] p : new int[][] {{5, 5}, {9, 5}, {5, 9}, {9, 9}}) {
			c.fill(p[0], 2, p[1], p[0], 4, p[1], Mat.ACCENT);
			c.set(p[0], 5, p[1], Mat.LIGHT);
		}
		for (int[] p : new int[][] {{2, 2}, {12, 2}, {2, 12}, {12, 12}}) {
			column(c, p[0], p[1], TOP, Mat.PILLAR);
		}
		c.set(MID, TOP, MID, Mat.LANTERN);
	}

	private static void start(Canvas c) {
		c.shell();
		c.fill(5, 0, 5, 9, 0, 9, Mat.FLOOR2);
		c.set(MID, 0, MID, Mat.LIGHT);
		for (int[] p : new int[][] {{4, 4}, {10, 4}, {4, 10}, {10, 10}}) {
			column(c, p[0], p[1], TOP, Mat.PILLAR);
			c.set(p[0], 5, p[1], Mat.LIGHT);
		}
		c.set(MID, TOP, MID, Mat.LANTERN);
	}

	/** Straight stairs along the wall the layout chose ({@link Room#stairSide}), up through the ceiling. */
	private static void stairUp(Canvas c) {
		Canvas f = c.facing(c.room.stairSide);
		f.shell();
		f.fill(8, DungeonRules.CELL_H - 1, IN0, 11, DungeonRules.CELL_H - 1, 3, Mat.AIR);
		for (int i = 1; i <= 11; i++) {
			f.fill(i, 1, IN0, i, i - 1, 3, Mat.WALL2);
			f.fill(i, i, IN0, i, i, 3, Mat.STAIR, Dir.E);
		}
		f.set(MID, TOP, 10, Mat.LANTERN);
	}

	/** The top of {@link #stairUp}: a hole in the floor fenced on its open sides. */
	private static void stairDown(Canvas c) {
		Canvas f = c.facing(c.room.stairSide);
		f.shell();
		f.fill(9, 0, IN0, 11, 0, 3, Mat.AIR);
		for (int x = 8; x <= 11; x++) {
			f.set(x, 1, 4, Mat.FENCE);
		}
		for (int z = IN0; z <= 3; z++) {
			f.set(8, 1, z, Mat.FENCE);
		}
		f.set(MID, TOP, 10, Mat.LANTERN);
	}
}
