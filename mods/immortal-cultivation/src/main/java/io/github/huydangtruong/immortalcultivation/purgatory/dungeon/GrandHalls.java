package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/**
 * D285: the big 2x2-cell halls (30 x 30, like the big rooms of a trial chamber): galleries along the walls at height
 * {@link #DECK}, stairs up to them, columns, floating platforms joined by bridges, a top tier at {@link #HIGH}. Every hall
 * keeps the eight places a door can be cut free at walking height ({@link Canvas#cutDoors} runs after), and puts two
 * treasure chests (one up high). Rails are added last by {@link #rails}: a fence on every deck edge with a drop behind it.
 */
final class GrandHalls {
	/** Galleries, bridges and platforms stand on this height (doorways are 4 high, so they pass under). */
	static final int DECK = 5;
	/** The top tier. */
	static final int HIGH = 8;
	private static final int IN1 = 2 * DungeonRules.CELL - 2; // 28
	private static final int CEIL = DungeonRules.CELL_H - 1;

	private GrandHalls() {}

	// ---------------------------------------------------------------- pieces

	/**
	 * Steps rising towards {@code up}: the first (height base+1) covers x0..x1 / z0..z1, each next one is a block further
	 * along {@code up} and a block higher, solid underneath down to {@code base}+1.
	 */
	static void flight(Canvas c, int x0, int z0, int x1, int z1, Dir up, int base, int n) {
		for (int k = 1; k <= n; k++) {
			int ox = up.dx * (k - 1);
			int oz = up.dz * (k - 1);
			if (k > 1) {
				c.fill(x0 + ox, base + 1, z0 + oz, x1 + ox, base + k - 1, z1 + oz, Mat.WALL2);
			}
			c.fill(x0 + ox, base + k, z0 + oz, x1 + ox, base + k, z1 + oz, Mat.STAIR, up);
		}
	}

	/** A gallery along the north wall of {@code f}: deck z 1..depth at {@link #DECK}, columns under its edge, lanterns. */
	private static void gallery(Canvas f, int depth, Mat deck) {
		f.fill(1, DECK, 1, IN1, DECK, depth, deck);
		for (int x : new int[] {4, 12, 17, 25}) {
			f.fill(x, 1, depth, x, DECK - 1, depth, Mat.PILLAR);
		}
		f.set(14, DECK - 1, 2, Mat.LANTERN);
		f.set(3, DECK - 1, 2, Mat.LANTERN);
		f.set(26, DECK - 1, 2, Mat.LANTERN);
	}

	/** Stairs up to the gallery of {@code f} (2 wide at x 10..11, rising north to its edge). */
	private static void galleryStairs(Canvas f, int depth) {
		flight(f, 10, depth + DECK, 11, depth + DECK, Dir.N, 0, DECK);
	}

	/** A floating deck with a lantern hanging under its middle. */
	private static void platform(Canvas c, int x0, int z0, int x1, int z1, int y, Mat m) {
		c.fill(x0, y, z0, x1, y, z1, m);
		c.set((x0 + x1) / 2, y - 1, (z0 + z1) / 2, Mat.LANTERN);
	}

	/** Lights in the ceiling every {@code every} blocks. */
	private static void ceiling(Canvas c, int every) {
		for (int x = 4; x < c.w - 2; x += every) {
			for (int z = 4; z < c.d - 2; z += every) {
				c.set(x, CEIL, z, Mat.LIGHT);
			}
		}
	}

	/** A full-height column with a light at {@code light}. */
	private static void column(Canvas c, int x, int z, Mat m, int light) {
		c.fill(x, 1, z, x, CEIL - 1, z, m);
		c.set(x, light, z, Mat.LIGHT);
	}

	/**
	 * Rails at height y: a fence on every deck block (not a step) whose side has a drop of two or more, unless something
	 * already stands on it. Bridges are 4 wide, so 2 stay free between their rails.
	 */
	static void rails(Canvas c, int y) {
		boolean[][] edge = new boolean[c.w][c.d];
		for (int x = 1; x <= IN1; x++) {
			for (int z = 1; z <= IN1; z++) {
				Mat m = c.get(x, y, z);
				if (m.move != Mat.Move.SOLID || m == Mat.STAIR || m == Mat.CHEST || c.get(x, y + 1, z) != Mat.AIR) {
					continue;
				}
				for (Dir d : Dir.values()) {
					int nx = x + d.dx;
					int nz = z + d.dz;
					if (c.get(nx, y, nz) == Mat.AIR && c.get(nx, y - 1, nz) == Mat.AIR) {
						edge[x][z] = true;
					}
				}
			}
		}
		for (int x = 1; x <= IN1; x++) {
			for (int z = 1; z <= IN1; z++) {
				if (edge[x][z]) {
					c.set(x, y + 1, z, Mat.FENCE);
				}
			}
		}
	}

	private static Dir pair(Canvas c) {
		return c.rng.nextBoolean() ? Dir.N : Dir.E;
	}

	// ---------------------------------------------------------------- halls

	/**
	 * Trial chamber atrium: galleries on all four walls, stairs up two of them, bridges from the other two to a floating
	 * platform in the middle with a top tier above it; four copper columns.
	 */
	static void copperAtrium(Canvas c) {
		c.shell();
		for (int x = 2; x <= 27; x++) {
			for (int z = 2; z <= 27; z++) {
				if ((x / 3 + z / 3) % 2 == 0) {
					c.set(x, 0, z, Mat.FLOOR2);
				}
			}
		}
		Dir stairs = pair(c);
		for (Dir s : Dir.values()) {
			gallery(c.facing(s), 4, Mat.FLOOR2);
		}
		platform(c, 11, 11, 18, 18, DECK, Mat.FLOOR2);
		for (Dir s : Dir.values()) {
			Canvas f = c.facing(s);
			if (s == stairs || s == stairs.opposite()) {
				galleryStairs(f, 4);
			} else {
				f.fill(13, DECK, 5, 16, DECK, 10, Mat.FLOOR2);
			}
		}
		flight(c, 13, 16, 14, 16, Dir.N, DECK, 2); // approached from the platform's inside, not its railed edge
		platform(c, 11, 11, 16, 14, HIGH, Mat.ACCENT);
		for (int[] p : new int[][] {{8, 8}, {21, 8}, {8, 21}, {21, 21}}) {
			column(c, p[0], p[1], Mat.ACCENT, 7);
		}
		ceiling(c, 6);
		c.chest(14, HIGH + 1, 12, Dir.S);
		c.chest(14, 1, 14, Dir.N);
		rails(c, DECK);
		rails(c, HIGH);
	}

	/**
	 * Nether bastion: deep galleries on two walls with stairs, a floating platform over a lava pool joined to both, magma in
	 * the floor and blackstone pillars on the other walls.
	 */
	static void netherBastion(Canvas c) {
		c.shell();
		c.fill(1, 0, 1, IN1, 0, IN1, Mat.FLOOR2);
		for (int x = 2; x <= 27; x++) {
			for (int z = 2; z <= 27; z++) {
				if (c.rng.nextInt(100) < 7) {
					c.set(x, 0, z, Mat.MAGMA);
				}
			}
		}
		Dir s = pair(c);
		c.fill(11, 1, 11, 18, 1, 18, Mat.LAVA);
		platform(c, 11, 11, 18, 18, DECK, Mat.WALL2);
		for (Dir side : new Dir[] {s, s.opposite()}) {
			Canvas f = c.facing(side);
			gallery(f, 5, Mat.WALL2);
			galleryStairs(f, 5);
			f.fill(13, DECK, 6, 16, DECK, 10, Mat.WALL2);
		}
		for (Dir side : new Dir[] {s.cw(), s.cw().opposite()}) {
			Canvas f = c.facing(side);
			f.fill(14, 1, 5, 15, CEIL - 1, 5, Mat.PILLAR);
			f.set(14, 6, 6, Mat.LANTERN);
		}
		ceiling(c, 8);
		c.facing(s).chest(2, DECK + 1, 2, Dir.S);
		c.chest(14, DECK + 1, 14, Dir.S);
		rails(c, DECK);
	}

	/**
	 * End city spires: galleries on two walls, a purpur tower in the middle ringed by a floating deck, bridges from both
	 * galleries, and steps from the ring up to a high platform in a corner.
	 */
	static void endSpires(Canvas c) {
		c.shell();
		for (int x = 2; x <= 27; x++) {
			for (int z = 2; z <= 27; z++) {
				if ((x + z) % 2 == 0) {
					c.set(x, 0, z, Mat.FLOOR2);
				}
			}
		}
		Dir s = pair(c);
		for (Dir side : new Dir[] {s, s.opposite()}) {
			Canvas f = c.facing(side);
			gallery(f, 4, Mat.FLOOR2);
			galleryStairs(f, 4);
			f.fill(13, DECK, 5, 16, DECK, 9, Mat.FLOOR2);
		}
		platform(c, 10, 10, 19, 19, DECK, Mat.FLOOR2);
		c.fill(13, 1, 13, 16, CEIL - 1, 16, Mat.PILLAR);
		for (int y : new int[] {3, 7}) {
			c.set(13, y, 14, Mat.LIGHT);
			c.set(16, y, 15, Mat.LIGHT);
			c.set(14, y, 16, Mat.LIGHT);
			c.set(15, y, 13, Mat.LIGHT);
		}
		flight(c, 18, 12, 19, 12, Dir.N, DECK, 3);
		platform(c, 17, 3, 23, 9, HIGH, Mat.FLOOR2);
		ceiling(c, 6);
		c.chest(21, HIGH + 1, 5, Dir.S);
		c.chest(14, 1, 11, Dir.N);
		rails(c, DECK);
		rails(c, HIGH);
	}

	/**
	 * Ancient city citadel: galleries on two walls joined by a walkway across the hall, over a raised dais with two portal
	 * frames; two platforms hang off the walkway. Sculk in the floor.
	 */
	static void deepCitadel(Canvas c) {
		c.shell();
		for (int x = 1; x <= IN1; x++) {
			for (int z = 1; z <= IN1; z++) {
				if (c.rng.nextInt(10) < 3) {
					c.set(x, 0, z, Mat.SCULK);
				}
			}
		}
		Dir s = pair(c);
		Canvas f = c.facing(s);
		for (int a = 9; a <= 20; a++) {
			f.set(a, 1, 9, Mat.STAIR, Dir.S);
			f.set(a, 1, 20, Mat.STAIR, Dir.N);
			f.set(9, 1, a, Mat.STAIR, Dir.E);
			f.set(20, 1, a, Mat.STAIR, Dir.W);
		}
		f.fill(10, 1, 10, 19, 2, 19, Mat.FLOOR2);
		for (Dir side : new Dir[] {s, s.opposite()}) {
			Canvas g = c.facing(side);
			gallery(g, 4, Mat.WALL2);
			galleryStairs(g, 4);
		}
		for (int x : new int[] {11, 18}) {
			f.fill(x, 3, 12, x, 8, 17, Mat.DECOR);
			f.fill(x, 3, 13, x, 6, 16, Mat.AIR);
		}
		f.fill(13, DECK, 5, 16, DECK, 24, Mat.WALL2);
		platform(f, 17, 7, 22, 10, DECK, Mat.WALL2);
		platform(f, 7, 19, 12, 22, DECK, Mat.WALL2);
		f.set(14, CEIL - 1, 9, Mat.LANTERN);
		f.set(15, CEIL - 1, 20, Mat.LANTERN);
		ceiling(c, 8);
		f.chest(14, 3, 11, Dir.S);
		f.chest(20, DECK + 1, 8, Dir.W);
		rails(c, DECK);
	}

	/**
	 * Ocean monument hall: four pools in the floor between dry cross walks, galleries on two walls with wide stairs, a
	 * floating platform joined to both by bridges over the water, a top tier, prismarine columns in the pools.
	 */
	static void oceanHall(Canvas c) {
		c.shell();
		c.fill(1, 0, 1, IN1, 0, IN1, Mat.FLOOR2);
		for (int[] p : new int[][] {{4, 4}, {17, 4}, {4, 17}, {17, 17}}) {
			c.fill(p[0], 0, p[1], p[0] + 8, 0, p[1] + 8, Mat.WATER);
		}
		Dir s = pair(c);
		platform(c, 11, 11, 18, 18, DECK, Mat.FLOOR2);
		for (Dir side : new Dir[] {s, s.opposite()}) {
			Canvas f = c.facing(side);
			gallery(f, 3, Mat.FLOOR2);
			flight(f, 12, 3 + DECK, 14, 3 + DECK, Dir.N, 0, DECK);
			f.fill(15, DECK, 4, 18, DECK, 10, Mat.FLOOR2);
		}
		for (int[] p : new int[][] {{8, 8}, {21, 8}, {8, 21}, {21, 21}}) {
			c.fill(p[0], 0, p[1], p[0], CEIL - 1, p[1], Mat.PILLAR);
			c.set(p[0], 3, p[1], Mat.LIGHT);
			c.set(p[0], 8, p[1], Mat.LIGHT);
		}
		Canvas f = c.facing(s);
		flight(f, 15, 16, 16, 16, Dir.N, DECK, 2);
		platform(f, 13, 11, 18, 14, HIGH, Mat.ACCENT);
		ceiling(c, 6);
		f.chest(17, HIGH + 1, 12, Dir.S);
		f.chest(2, DECK + 1, 2, Dir.S);
		rails(c, DECK);
		rails(c, HIGH);
	}

	/**
	 * Two-storey library: galleries on all four walls with bookshelves above them, stairs up two, shelf stacks on the floor,
	 * steps on a gallery up to a high bridge and a reading platform under the chandelier.
	 */
	static void libraryAtrium(Canvas c) {
		c.shell();
		Dir s = pair(c);
		for (Dir side : Dir.values()) {
			Canvas f = c.facing(side);
			f.fill(1, DECK + 1, 0, IN1, CEIL - 2, 0, Mat.SHELF);
			gallery(f, 4, Mat.FLOOR2);
		}
		Canvas f = c.facing(s);
		galleryStairs(f, 4);
		galleryStairs(c.facing(s.opposite()), 4);
		for (int[] row : new int[][] {{9, 12}, {17, 12}, {9, 17}, {17, 17}}) {
			f.fill(row[0], 1, row[1], row[0] + 3, 3, row[1], Mat.SHELF);
		}
		f.fill(12, 0, 12, 17, 0, 17, Mat.FLOOR2);
		Canvas q = c.facing(s.cw());
		flight(q, 12, 1, 12, 2, Dir.E, DECK, 3);
		q.fill(15, HIGH, 1, 18, HIGH, 4, Mat.FLOOR2);
		q.fill(15, HIGH, 5, 18, HIGH, 11, Mat.FLOOR2);
		platform(q, 11, 12, 18, 17, HIGH, Mat.FLOOR2);
		q.set(14, CEIL - 1, 14, Mat.LANTERN);
		ceiling(c, 7);
		q.chest(14, HIGH + 1, 15, Dir.N);
		f.chest(14, 1, 14, Dir.S);
		rails(c, DECK);
		rails(c, HIGH);
	}
}
