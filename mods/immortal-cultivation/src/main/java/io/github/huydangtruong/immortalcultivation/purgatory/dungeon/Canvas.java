package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.Random;

/**
 * Where a module draws: a room's box in the {@link Grid}, in local coordinates (x along the width, z along the depth, y up;
 * floor at y 0, ceiling at y {@link DungeonRules#CELL_H}-1, walls at x/z 0 and size-1). A rotated canvas ({@link #facing})
 * turns local north towards another side, so a one-cell module can be drawn against any wall.
 */
public final class Canvas {
	public final Grid grid;
	public final Room room;
	public final Random rng;
	public final int w;
	public final int d;
	public final int h = DungeonRules.CELL_H;
	private final Dir frame;
	private Style style;

	Canvas(Grid grid, Room room, Random rng) {
		this(grid, room, rng, Dir.N, room.style);
	}

	private Canvas(Grid grid, Room room, Random rng, Dir frame, Style style) {
		this.grid = grid;
		this.room = room;
		this.rng = rng;
		this.w = room.cw * DungeonRules.CELL;
		this.d = room.cd * DungeonRules.CELL;
		this.frame = frame;
		this.style = style;
	}

	/** The same box with local north turned to {@code side} (square boxes only). */
	public Canvas facing(Dir side) {
		return new Canvas(grid, room, rng, side, style);
	}

	public Canvas style(Style s) {
		style = s;
		return this;
	}

	public Style style() {
		return style;
	}

	/** Grid coordinates of a local point. */
	public int[] at(int x, int y, int z) {
		int u = x;
		int v = z;
		int lx;
		int lz;
		switch (frame) {
			case E -> {
				lx = w - 1 - v;
				lz = u;
			}
			case S -> {
				lx = w - 1 - u;
				lz = d - 1 - v;
			}
			case W -> {
				lx = v;
				lz = d - 1 - u;
			}
			default -> {
				lx = u;
				lz = v;
			}
		}
		return new int[] {room.x0() + lx, room.y0() + y, room.z0() + lz};
	}

	/** A local direction as a real one. */
	public Dir turn(Dir local) {
		return Dir.values()[(local.ordinal() + frame.ordinal()) % 4];
	}

	public boolean inside(int x, int y, int z) {
		return x >= 0 && z >= 0 && y >= 0 && x < w && z < d && y < h;
	}

	public void set(int x, int y, int z, Mat m) {
		set(x, y, z, m, Dir.N);
	}

	public void set(int x, int y, int z, Mat m, Dir localFacing) {
		if (inside(x, y, z)) {
			int[] p = at(x, y, z);
			grid.set(p[0], p[1], p[2], m, style, turn(localFacing));
		}
	}

	public Mat get(int x, int y, int z) {
		if (!inside(x, y, z)) {
			return Mat.SHELL;
		}
		int[] p = at(x, y, z);
		return grid.mat(p[0], p[1], p[2]);
	}

	/** Fill a box (corners in any order, inclusive). */
	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Mat m) {
		fill(x0, y0, z0, x1, y1, z1, m, Dir.N);
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Mat m, Dir localFacing) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					set(x, y, z, m, localFacing);
				}
			}
		}
	}

	/** Replace only voxels that are {@code from}. */
	public void replace(int x0, int y0, int z0, int x1, int y1, int z1, Mat from, Mat to) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					if (get(x, y, z) == from) {
						set(x, y, z, to);
					}
				}
			}
		}
	}

	/** Floor, four walls, ceiling, air inside. */
	public void shell() {
		fill(0, 0, 0, w - 1, h - 1, d - 1, Mat.WALL);
		fill(1, 1, 1, w - 2, h - 2, d - 2, Mat.AIR);
		fill(0, 0, 0, w - 1, 0, d - 1, Mat.FLOOR);
		fill(0, h - 1, 0, w - 1, h - 1, d - 1, Mat.CEILING);
	}

	public void chest(int x, int y, int z, Dir localFacing) {
		set(x, y, z, Mat.CHEST, localFacing);
		int[] p = at(x, y, z);
		room.chests.add(p);
	}

	public void plate(int x, int y, int z) {
		set(x, y, z, Mat.PLATE);
		room.plates.add(at(x, y, z));
	}

	public void dispenser(int x, int y, int z, Dir localFacing) {
		set(x, y, z, Mat.DISPENSER, localFacing);
		int[] p = at(x, y, z);
		room.dispensers.add(new int[] {p[0], p[1], p[2], turn(localFacing).ordinal()});
	}

	/** D265: the Seal Pillar stands here (feet, local). */
	public void pillarSpot(int x, int y, int z) {
		room.pillar = at(x, y, z);
	}

	/** The boss stands here (feet, local). */
	public void bossSpot(int x, int y, int z) {
		room.boss = at(x, y, z);
	}

	/**
	 * Cut every port of the room through its wall (3 wide, 4 high, gates when the room seals) and clear an apron two blocks deep
	 * behind it on solid floor, so no module can block its own doors. Works in real (unrotated) coordinates.
	 */
	void cutDoors() {
		Canvas c = frame == Dir.N ? this : facing(Dir.N);
		Mat door = room.kind.locks ? Mat.GATE : Mat.AIR;
		for (Port p : room.ports) {
			int mid = p.slot() * DungeonRules.CELL + DungeonRules.CELL / 2;
			for (int a = mid - 1; a <= mid + 1; a++) {
				for (int depth = 0; depth <= 2; depth++) {
					int x;
					int z;
					switch (p.side()) {
						case N -> {
							x = a;
							z = depth;
						}
						case S -> {
							x = a;
							z = d - 1 - depth;
						}
						case W -> {
							x = depth;
							z = a;
						}
						default -> {
							x = w - 1 - depth;
							z = a;
						}
					}
					c.set(x, 0, z, depth == 0 ? Mat.FLOOR : Mat.FLOOR2 == c.get(x, 0, z) ? Mat.FLOOR2 : Mat.FLOOR);
					for (int y = 1; y <= DungeonRules.DOOR_H; y++) {
						c.set(x, y, z, depth == 0 ? door : Mat.AIR);
						if (depth == 0 && door == Mat.GATE) {
							room.gates.add(c.at(x, y, z));
						}
					}
				}
			}
		}
	}
}
