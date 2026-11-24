package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Builds a floor (P-082) on a {@link DungeonRules#COLS} x {@link DungeonRules#ROWS} grid of cells: a random tree of rooms
 * grown from the start room (from floor 5 half of it a layer higher, joined by one staircase), a few extra doors for loops,
 * and the boss room hung as a leaf off the room farthest from the start. A floor that {@link Walker} finds a stuck spot in
 * is thrown away and the next attempt of the same seed is used, so a seed always gives the same floor.
 */
public final class Layout {
	private static final int ATTEMPTS = 60;

	private final Random rng;
	private final int floor;
	private final int layers;
	private final int[][][] owner = new int[DungeonRules.COLS][DungeonRules.ROWS][DungeonRules.MAX_LAYERS];
	private final boolean[][][][] door = new boolean[DungeonRules.COLS][DungeonRules.ROWS][DungeonRules.MAX_LAYERS][4];
	private final boolean[][][][] blocked = new boolean[DungeonRules.COLS][DungeonRules.ROWS][DungeonRules.MAX_LAYERS][4];
	private final List<int[]> cells = new ArrayList<>();
	private int[] stairLow;
	private int[] bossOrigin;
	private int bossCell = -1;
	private Dir bossSide;
	private BossRooms bossKind;

	private Layout(Random rng, int floor) {
		this.rng = rng;
		this.floor = Math.max(1, floor);
		this.layers = DungeonRules.layers(this.floor);
		for (int[][] a : owner) {
			for (int[] b : a) {
				java.util.Arrays.fill(b, -1);
			}
		}
	}

	/** The floor for a seed (always the same for the same seed and floor number). */
	public static Dungeon generate(long seed, int floor) {
		for (int a = 0; a < ATTEMPTS; a++) {
			Random rng = new Random(seed * 1_000_003L + a * 7_919L + floor);
			Dungeon d = new Layout(rng, floor).attempt(seed, a + 1);
			if (d != null) {
				return d;
			}
		}
		throw new IllegalStateException("no floor for seed " + seed + " floor " + floor);
	}

	private boolean inGrid(int c, int r) {
		return c >= 0 && r >= 0 && c < DungeonRules.COLS && r < DungeonRules.ROWS;
	}

	private int add(int c, int r, int l) {
		owner[c][r][l] = cells.size();
		cells.add(new int[] {c, r, l});
		return cells.size() - 1;
	}

	private void link(int c, int r, int l, Dir d) {
		door[c][r][l][d.ordinal()] = true;
		door[c + d.dx][r + d.dz][l][d.opposite().ordinal()] = true;
	}

	/** Grow {@code count} new cells on a layer from a cell already in it. */
	private boolean grow(int[] from, int count) {
		List<int[]> mine = new ArrayList<>();
		mine.add(from);
		int added = 0;
		for (int tries = 0; added < count && tries < 3000; tries++) {
			int[] at = rng.nextInt(10) < 6 ? mine.get(mine.size() - 1) : mine.get(rng.nextInt(mine.size()));
			Dir d = Dir.values()[rng.nextInt(4)];
			int c = at[0] + d.dx;
			int r = at[1] + d.dz;
			if (!inGrid(c, r) || owner[c][r][at[2]] >= 0 || blocked[at[0]][at[1]][at[2]][d.ordinal()]) {
				continue;
			}
			mine.add(cells.get(add(c, r, at[2])));
			link(at[0], at[1], at[2], d);
			added++;
		}
		return added == count;
	}

	/** Steps from the start to every cell (through doors and the staircase). */
	private int[] distances() {
		int[] dist = new int[cells.size()];
		java.util.Arrays.fill(dist, -1);
		ArrayDeque<Integer> q = new ArrayDeque<>();
		dist[0] = 0;
		q.add(0);
		while (!q.isEmpty()) {
			int i = q.poll();
			int[] at = cells.get(i);
			List<Integer> next = new ArrayList<>();
			for (Dir d : Dir.values()) {
				if (door[at[0]][at[1]][at[2]][d.ordinal()]) {
					next.add(owner[at[0] + d.dx][at[1] + d.dz][at[2]]);
				}
			}
			if (stairLow != null && at[0] == stairLow[0] && at[1] == stairLow[1]) {
				next.add(owner[at[0]][at[1]][1 - at[2]]);
			}
			for (int n : next) {
				if (n >= 0 && dist[n] < 0) {
					dist[n] = dist[i] + 1;
					q.add(n);
				}
			}
		}
		return dist;
	}

	private Dungeon attempt(long seed, int attempts) {
		int target = DungeonRules.rooms(floor);
		add(rng.nextInt(2), rng.nextInt(2), 0);
		int lower = layers == 2 ? target / 2 : target;
		if (!grow(cells.get(0), lower - 1)) {
			return null;
		}
		if (layers == 2 && !placeStairs(target - lower)) {
			return null;
		}
		if (!placeBoss()) {
			return null;
		}
		addLoops();
		return build(seed, attempts);
	}

	private boolean placeStairs(int upper) {
		int[] dist = distances();
		List<Integer> order = new ArrayList<>();
		for (int i = 1; i < cells.size(); i++) {
			order.add(i);
		}
		Collections.shuffle(order, rng);
		order.sort(Comparator.comparingInt(i -> -dist[i]));
		for (int i : order) {
			int[] at = cells.get(i);
			List<Dir> free = new ArrayList<>();
			for (Dir d : Dir.values()) {
				if (!door[at[0]][at[1]][0][d.ordinal()]) {
					free.add(d);
				}
			}
			if (free.isEmpty()) {
				continue;
			}
			Dir side = free.get(rng.nextInt(free.size()));
			blocked[at[0]][at[1]][0][side.ordinal()] = true;
			stairLow = new int[] {at[0], at[1], side.ordinal()};
			int top = add(at[0], at[1], 1);
			return grow(cells.get(top), upper - 1);
		}
		return false;
	}

	private boolean placeBoss() {
		int[] dist = distances();
		int last = layers - 1;
		List<Integer> order = new ArrayList<>();
		for (int i = 1; i < cells.size(); i++) {
			if (cells.get(i)[2] == last && dist[i] >= 0) {
				order.add(i);
			}
		}
		Collections.shuffle(order, rng);
		order.sort(Comparator.comparingInt(i -> -dist[i]));
		List<BossRooms> kinds = new ArrayList<>(List.of(BossRooms.values()));
		Collections.shuffle(kinds, rng);
		kinds.sort(Comparator.comparingInt(k -> -k.size)); // try the big rooms first, the 1-cell crypt only as a fallback
		if (rng.nextInt(6) == 0) {
			kinds.add(0, BossRooms.MINE_CRYPT); // the crypt also comes up on its own sometimes
		}
		for (BossRooms kind : kinds) {
			int s = kind.size;
			for (int i : order) {
				int[] at = cells.get(i);
				List<Dir> dirs = new ArrayList<>(List.of(Dir.values()));
				Collections.shuffle(dirs, rng);
				for (Dir d : dirs) {
					if (blocked[at[0]][at[1]][at[2]][d.ordinal()]) {
						continue;
					}
					int along = rng.nextInt(s);
					for (int k = 0; k < s; k++) {
						int off = (along + k) % s;
						int fx;
						int fz;
						switch (d) {
							case E -> {
								fx = at[0] + 1;
								fz = at[1] - off;
							}
							case W -> {
								fx = at[0] - s;
								fz = at[1] - off;
							}
							case S -> {
								fx = at[0] - off;
								fz = at[1] + 1;
							}
							default -> {
								fx = at[0] - off;
								fz = at[1] - s;
							}
						}
						if (free(fx, fz, s, at[2])) {
							bossKind = kind;
							bossOrigin = new int[] {fx, fz, at[2], off};
							bossCell = i;
							bossSide = d;
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean free(int fx, int fz, int s, int l) {
		for (int c = fx; c < fx + s; c++) {
			for (int r = fz; r < fz + s; r++) {
				if (!inGrid(c, r) || owner[c][r][l] >= 0) {
					return false;
				}
			}
		}
		return true;
	}

	private void addLoops() {
		for (int[] at : cells) {
			for (Dir d : new Dir[] {Dir.E, Dir.S}) {
				int c = at[0] + d.dx;
				int r = at[1] + d.dz;
				if (inGrid(c, r) && owner[c][r][at[2]] >= 0 && !door[at[0]][at[1]][at[2]][d.ordinal()]
					&& !blocked[at[0]][at[1]][at[2]][d.ordinal()] && !blocked[c][r][at[2]][d.opposite().ordinal()]
					&& rng.nextDouble() < DungeonRules.LOOP_CHANCE) {
					link(at[0], at[1], at[2], d);
				}
			}
		}
	}

	private boolean isStair(int[] at) {
		return stairLow != null && at[0] == stairLow[0] && at[1] == stairLow[1];
	}

	private Dungeon build(long seed, int attempts) {
		Grid grid = new Grid(DungeonRules.SIZE_X, DungeonRules.SIZE_Y, DungeonRules.SIZE_Z);
		// which module each cell gets
		Modules[] pick = new Modules[cells.size()];
		pick[0] = Modules.START;
		List<Integer> plain = new ArrayList<>();
		for (int i = 1; i < cells.size(); i++) {
			int[] at = cells.get(i);
			if (isStair(at)) {
				pick[i] = at[2] == 0 ? Modules.STAIR_UP : Modules.STAIR_DOWN;
			} else {
				plain.add(i);
			}
		}
		Collections.shuffle(plain, rng);
		if (plain.size() >= 3) {
			int leaf = plain.get(0);
			for (int i : plain) {
				if (i != bossCell && degree(cells.get(i)) == 1) {
					leaf = i;
					break;
				}
			}
			pick[leaf] = Modules.TREASURE_VAULT;
			plain.remove(Integer.valueOf(leaf));
		}
		if (floor >= DungeonRules.REST_FROM && plain.size() >= 4) {
			pick[plain.remove(plain.size() - 1)] = Modules.REST_SHRINE;
		}
		Map<Modules, Integer> uses = new EnumMap<>(Modules.class);
		for (int i : plain) {
			pick[i] = weighted(uses, null);
		}
		int locked = 0;
		for (int i : plain) {
			locked += pick[i].kind == RoomKind.LOCKED ? 1 : 0;
		}
		for (int i : plain) {
			if (locked >= Math.max(1, plain.size() / 3)) {
				break;
			}
			if (pick[i].kind != RoomKind.LOCKED) {
				pick[i] = weighted(uses, RoomKind.LOCKED);
				locked++;
			}
		}

		List<Room> rooms = new ArrayList<>();
		for (int i = 0; i < cells.size(); i++) {
			int[] at = cells.get(i);
			Modules m = pick[i];
			Room room = new Room(i, m.kind, m.id(), m.pickStyle(rng), at[0], at[1], at[2], 1, 1);
			for (Dir d : Dir.values()) {
				if (door[at[0]][at[1]][at[2]][d.ordinal()]) {
					room.ports.add(new Port(d, 0));
				}
			}
			if (i == bossCell) {
				room.ports.add(new Port(bossSide, 0));
			}
			if (isStair(at)) {
				room.stairSide = Dir.values()[stairLow[2]];
			}
			rooms.add(room);
		}
		Room boss = new Room(cells.size(), RoomKind.BOSS, bossKind.id(), bossKind.style, bossOrigin[0], bossOrigin[1],
			bossOrigin[2], bossKind.size, bossKind.size);
		boss.ports.add(new Port(bossSide.opposite(), bossOrigin[3]));
		rooms.add(boss);

		for (int i = 0; i < cells.size(); i++) {
			pick[i].draw(new Canvas(grid, rooms.get(i), rng));
		}
		bossKind.draw(new Canvas(grid, boss, rng));
		for (Room r : rooms) {
			new Canvas(grid, r, rng).cutDoors();
		}

		Room start = rooms.get(0);
		start.centre = new int[] {start.x0() + DungeonRules.CELL / 2, start.y0() + 1, start.z0() + DungeonRules.CELL / 2};
		Walker.Result res = Walker.check(grid, rooms, start.centre, boss);
		if (!res.ok()) {
			return null;
		}
		for (Room r : rooms) {
			res.fillRoom(r, rng);
		}
		return new Dungeon(seed, floor, grid, List.copyOf(rooms), start, boss, attempts);
	}

	private int degree(int[] at) {
		int n = 0;
		for (Dir d : Dir.values()) {
			n += door[at[0]][at[1]][at[2]][d.ordinal()] ? 1 : 0;
		}
		return n;
	}

	/** A module by weight, less likely the more it was used already. */
	private Modules weighted(Map<Modules, Integer> uses, RoomKind only) {
		double total = 0;
		List<Modules> pool = new ArrayList<>();
		for (Modules m : Modules.values()) {
			if (m.weight > 0 && (only == null || m.kind == only)) {
				pool.add(m);
				total += m.weight / (1.0 + 2 * uses.getOrDefault(m, 0));
			}
		}
		double roll = rng.nextDouble() * total;
		for (Modules m : pool) {
			roll -= m.weight / (1.0 + 2 * uses.getOrDefault(m, 0));
			if (roll <= 0) {
				uses.merge(m, 1, Integer::sum);
				return m;
			}
		}
		Modules m = pool.get(pool.size() - 1);
		uses.merge(m, 1, Integer::sum);
		return m;
	}
}
