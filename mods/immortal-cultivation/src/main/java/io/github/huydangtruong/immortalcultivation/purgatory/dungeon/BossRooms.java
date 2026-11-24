package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.function.Consumer;

/**
 * Boss rooms (P-082): the last room of a floor, one door. Each is drawn with its door on the local south wall, so the altar,
 * throne or forge faces whoever comes in.
 */
public enum BossRooms {
	COPPER_VAULT(2, BossRooms::copperVault, Style.TRIAL),
	SCULK_CATHEDRAL(2, BossRooms::sculkCathedral, Style.DEEP),
	PURPUR_THRONE(2, BossRooms::purpurThrone, Style.END),
	NETHER_FORGE(2, BossRooms::netherForge, Style.NETHER),
	FLOODED_TEMPLE(2, BossRooms::floodedTemple, Style.OCEAN),
	MINE_CRYPT(1, BossRooms::mineCrypt, Style.MINE);

	/** Cells per side. */
	public final int size;
	public final Style style;
	private final Consumer<Canvas> draw;

	BossRooms(int size, Consumer<Canvas> draw, Style style) {
		this.size = size;
		this.draw = draw;
		this.style = style;
	}

	public String id() {
		return "boss_" + name().toLowerCase(java.util.Locale.ROOT);
	}

	void draw(Canvas c) {
		draw.accept(c.facing(c.room.ports.get(0).side().opposite()));
	}

	private static final int TOP = DungeonRules.CELL_H - 2;
	private static final int CEIL = DungeonRules.CELL_H - 1;

	private static void column(Canvas c, int x, int z, int size, Mat m) {
		c.fill(x, 1, z, x + size - 1, TOP, z + size - 1, m);
	}

	private static void copperVault(Canvas c) {
		c.shell();
		int m = c.w / 2; // 15
		for (int[] p : new int[][] {{5, 5}, {5, 14}, {5, 23}, {23, 5}, {23, 14}, {23, 23}, {14, 5}}) {
			column(c, p[0], p[1], 2, Mat.ACCENT);
			c.fill(p[0], 6, p[1], p[0] + 1, 6, p[1] + 1, Mat.LIGHT);
		}
		c.fill(10, 1, 10, 19, 1, 19, Mat.FLOOR2);
		for (int a = 10; a <= 19; a++) {
			c.set(a, 1, 9, Mat.STAIR, Dir.N);
			c.set(a, 1, 20, Mat.STAIR, Dir.S);
			c.set(9, 1, a, Mat.STAIR, Dir.W);
			c.set(20, 1, a, Mat.STAIR, Dir.E);
		}
		c.fill(m - 1, 1, m - 1, m, 1, m, Mat.LIGHT);
		for (int x = 4; x < c.w - 4; x += 5) {
			for (int z = 4; z < c.d - 4; z += 5) {
				c.set(x, CEIL, z, Mat.LIGHT);
			}
		}
		c.bossSpot(m, 2, m - 3);
	}

	private static void sculkCathedral(Canvas c) {
		c.shell();
		for (int z = 5; z <= 23; z += 6) {
			column(c, 8, z, 2, Mat.PILLAR);
			column(c, 20, z, 2, Mat.PILLAR);
			c.set(8, 7, z, Mat.LIGHT);
			c.set(21, 7, z + 1, Mat.LIGHT);
		}
		for (int x = 1; x < c.w - 1; x++) {
			for (int z = 1; z < c.d - 1; z++) {
				if (c.rng.nextInt(10) < 3) {
					c.set(x, 0, z, Mat.SCULK);
				}
			}
		}
		// the frame at the far end, like an ancient city's portal
		c.fill(10, 1, 1, 19, 8, 2, Mat.DECOR);
		c.fill(12, 1, 1, 17, 6, 2, Mat.AIR);
		c.fill(12, 0, 1, 17, 0, 2, Mat.SCULK);
		c.fill(11, 9, 1, 18, 9, 2, Mat.WALL2);
		c.set(14, TOP, 12, Mat.LANTERN);
		c.set(15, TOP, 18, Mat.LANTERN);
		c.bossSpot(15, 1, 7);
	}

	private static void purpurThrone(Canvas c) {
		c.shell();
		c.fill(6, 1, 1, 23, 1, 8, Mat.FLOOR2);
		c.fill(8, 2, 1, 21, 2, 6, Mat.FLOOR2);
		c.fill(10, 3, 1, 19, 3, 4, Mat.FLOOR2);
		c.fill(14, 4, 1, 15, 4, 1, Mat.STAIR, Dir.N);
		for (int[] p : new int[][] {{3, 3}, {3, 14}, {3, 25}, {26, 3}, {26, 14}, {26, 25}}) {
			column(c, p[0], p[1], 1, Mat.PILLAR);
		}
		for (int x = 6; x <= 24; x += 6) {
			c.set(x, TOP, 12, Mat.LANTERN);
			c.set(x, TOP, 21, Mat.LANTERN);
		}
		for (int x = 2; x < c.w - 2; x++) {
			for (int z = 10; z < c.d - 2; z++) {
				if ((x + z) % 2 == 0) {
					c.set(x, 0, z, Mat.FLOOR2);
				}
			}
		}
		c.bossSpot(15, 4, 4);
	}

	private static void netherForge(Canvas c) {
		c.shell();
		c.fill(1, 1, 1, c.w - 2, 1, c.d - 2, Mat.FLOOR2);
		for (int[] p : new int[][] {{4, 4}, {20, 4}, {4, 20}, {20, 20}}) {
			c.fill(p[0], 1, p[1], p[0] + 5, 1, p[1] + 5, Mat.LAVA);
		}
		for (int[] p : new int[][] {{12, 12}, {17, 12}, {12, 17}, {17, 17}}) {
			column(c, p[0], p[1], 1, Mat.PILLAR);
		}
		for (int x = 2; x < c.w - 2; x++) {
			for (int z = 2; z < c.d - 2; z++) {
				if (c.get(x, 1, z) == Mat.FLOOR2 && c.rng.nextInt(100) < 8) {
					c.set(x, 1, z, Mat.MAGMA);
				}
			}
		}
		c.fill(13, 2, 3, 16, 3, 4, Mat.DECOR);
		for (int x = 6; x < c.w; x += 8) {
			for (int z = 6; z < c.d; z += 8) {
				c.set(x, CEIL, z, Mat.LIGHT);
			}
		}
		c.bossSpot(15, 2, 14);
	}

	private static void floodedTemple(Canvas c) {
		c.shell();
		c.fill(1, 1, 1, c.w - 2, 1, c.d - 2, Mat.FLOOR2);
		c.fill(4, 1, 4, c.w - 5, 1, c.d - 5, Mat.WATER);
		c.fill(10, 1, 10, 19, 1, 19, Mat.FLOOR2);
		c.fill(14, 1, 4, 16, 1, 25, Mat.FLOOR2);
		c.fill(4, 1, 14, 25, 1, 16, Mat.FLOOR2);
		for (int[] p : new int[][] {{6, 6}, {22, 6}, {6, 22}, {22, 22}}) {
			column(c, p[0], p[1], 2, Mat.PILLAR);
			c.fill(p[0], 5, p[1], p[0] + 1, 5, p[1] + 1, Mat.LIGHT);
		}
		for (int x = 5; x < c.w; x += 6) {
			for (int z = 5; z < c.d; z += 6) {
				c.set(x, CEIL, z, Mat.LIGHT);
			}
		}
		c.fill(13, 2, 13, 16, 2, 13, Mat.DECOR);
		c.bossSpot(15, 2, 16);
	}

	private static void mineCrypt(Canvas c) {
		c.shell();
		for (int[] p : new int[][] {{3, 3}, {11, 3}, {3, 11}, {11, 11}}) {
			column(c, p[0], p[1], 1, Mat.PILLAR);
			c.set(p[0], 2, p[1], Mat.COBWEB);
		}
		c.fill(3, TOP, 3, 11, TOP, 3, Mat.WALL2);
		c.fill(3, TOP, 11, 11, TOP, 11, Mat.WALL2);
		c.fill(6, 1, 1, 8, 1, 2, Mat.DECOR);
		c.set(1, 1, 1, Mat.COBWEB);
		c.set(13, 1, 1, Mat.COBWEB);
		c.set(4, TOP, 7, Mat.LANTERN);
		c.set(10, TOP, 7, Mat.LANTERN);
		c.bossSpot(7, 1, 6);
	}
}
