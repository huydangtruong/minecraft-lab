package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The stuck checker (P-082): walks a {@link Grid} the way a player can (walk, jump up one block, fall, swim or climb up,
 * jump a one-block gap; never into lava; sealed gates count as open, they always open in the end) and checks that from the
 * start every room can be reached and, wherever a player can get to, the boss can still be reached from there.
 */
public final class Walker {
	private final Grid g;

	private Walker(Grid g) {
		this.g = g;
	}

	private int index(int x, int y, int z) {
		return (y * g.sz + z) * g.sx + x;
	}

	private boolean pass(int x, int y, int z) {
		return g.mat(x, y, z).passable();
	}

	private boolean climb(int x, int y, int z) {
		Mat m = g.mat(x, y, z);
		return m.move == Mat.Move.WATER || m.move == Mat.Move.LADDER;
	}

	/** Feet may be here. */
	boolean stand(int x, int y, int z) {
		return g.in(x, y, z) && pass(x, y, z) && pass(x, y + 1, z) && (g.mat(x, y - 1, z).supports() || climb(x, y, z));
	}

	/** Every feet position reachable in one move. */
	private void moves(int x, int y, int z, List<int[]> out) {
		if (climb(x, y, z) && stand(x, y + 1, z)) {
			out.add(new int[] {x, y + 1, z});
		}
		boolean headroom = pass(x, y + 2, z);
		for (Dir d : Dir.values()) {
			int nx = x + d.dx;
			int nz = z + d.dz;
			if (pass(nx, y, nz) && pass(nx, y + 1, nz)) {
				if (stand(nx, y, nz)) {
					out.add(new int[] {nx, y, nz});
				} else {
					int fy = y;
					while (fy > 0 && !stand(nx, fy, nz) && pass(nx, fy - 1, nz)) {
						fy--;
					}
					if (stand(nx, fy, nz)) {
						out.add(new int[] {nx, fy, nz});
					}
					int gx = nx + d.dx;
					int gz = nz + d.dz;
					if (headroom && pass(nx, y + 2, nz) && stand(gx, y, gz)) {
						out.add(new int[] {gx, y, gz});
					}
				}
			}
			if (headroom && g.mat(nx, y, nz).move == Mat.Move.SOLID && stand(nx, y + 1, nz)) {
				out.add(new int[] {nx, y + 1, nz});
			}
		}
	}

	/** What a check found. */
	public final class Result {
		private final BitSet reach;
		private final String problem;

		Result(BitSet reach, String problem) {
			this.reach = reach;
			this.problem = problem;
		}

		public boolean ok() {
			return problem == null;
		}

		/** Why the floor is bad, or null. */
		public String problem() {
			return problem;
		}

		public boolean reachable(int x, int y, int z) {
			return g.in(x, y, z) && reach.get(index(x, y, z));
		}

		public int count() {
			return reach.cardinality();
		}

		/** The spot farthest from every door of the room (or {@code fallback} when there are no spots). */
		private int[] farFromDoors(Room r, int[] fallback, List<int[]> spots) {
			int[] best = fallback;
			double bestD = -1;
			for (int[] p : spots) {
				double near = Double.MAX_VALUE;
				for (Port port : r.ports) {
					int mid = port.slot() * DungeonRules.CELL + DungeonRules.CELL / 2;
					int dx = switch (port.side()) {
						case E -> r.x1() - 1;
						case W -> r.x0();
						default -> r.x0() + mid;
					};
					int dz = switch (port.side()) {
						case N -> r.z0();
						case S -> r.z1() - 1;
						default -> r.z0() + mid;
					};
					near = Math.min(near, (p[0] - dx) * (p[0] - dx) + (p[2] - dz) * (p[2] - dz));
				}
				if (near > bestD) {
					bestD = near;
					best = p;
				}
			}
			return best;
		}

		/** Spawn points (reachable, away from the walls and doors), the centre and the boss spot of a room. */
		void fillRoom(Room r, Random rng) {
			List<int[]> spots = new ArrayList<>();
			int[] best = null;
			double bestD = Double.MAX_VALUE;
			double mx = (r.x0() + r.x1()) / 2.0;
			double mz = (r.z0() + r.z1()) / 2.0;
			for (int x = r.x0() + 1; x < r.x1() - 1; x++) {
				for (int z = r.z0() + 1; z < r.z1() - 1; z++) {
					for (int y = r.y0() + 1; y < r.y1() - 1; y++) {
						if (g.mat(x, y, z) == Mat.LAVA) {
							r.lava = true; // D290
						}
						if (!reachable(x, y, z)) {
							continue;
						}
						int[] p = {x, y, z};
						double dist = (x - mx) * (x - mx) + (z - mz) * (z - mz) + (y - r.y0()) * 0.5;
						if (dist < bestD) {
							bestD = dist;
							best = p;
						}
						if (x >= r.x0() + 3 && x < r.x1() - 3 && z >= r.z0() + 3 && z < r.z1() - 3) {
							spots.add(p);
						}
					}
				}
			}
			Collections.shuffle(spots, rng);
			r.spawns.clear();
			r.spawns.addAll(spots.subList(0, Math.min(48, spots.size())));
			if (r.centre == null) {
				r.centre = best;
			}
			if (r.kind == RoomKind.PILLAR && (r.pillar == null || !reachable(r.pillar[0], r.pillar[1], r.pillar[2]))) {
				r.pillar = farFromDoors(r, spots.isEmpty() ? best : null, spots); // D265
			}
			if (r.kind == RoomKind.BOSS && (r.boss == null || !reachable(r.boss[0], r.boss[1], r.boss[2]))) {
				r.boss = best;
			}
		}
	}

	/** Check a floor: start and boss are feet positions in grid coordinates. */
	public static Result check(Grid g, List<Room> rooms, int[] start, Room boss) {
		Walker w = new Walker(g);
		return w.run(rooms, start, boss);
	}

	private Result run(List<Room> rooms, int[] start, Room boss) {
		BitSet reach = new BitSet();
		if (!stand(start[0], start[1], start[2])) {
			return new Result(reach, "the start is not a standing spot");
		}
		// forward search, keeping every move for the way back
		int cap = 1 << 16;
		int[] from = new int[cap];
		int[] to = new int[cap];
		int edges = 0;
		ArrayDeque q = new ArrayDeque();
		reach.set(index(start[0], start[1], start[2]));
		q.add(start[0], start[1], start[2]);
		List<int[]> out = new ArrayList<>();
		while (!q.isEmpty()) {
			int[] p = q.poll();
			int pi = index(p[0], p[1], p[2]);
			out.clear();
			moves(p[0], p[1], p[2], out);
			for (int[] n : out) {
				int ni = index(n[0], n[1], n[2]);
				if (edges == cap) {
					cap *= 2;
					from = java.util.Arrays.copyOf(from, cap);
					to = java.util.Arrays.copyOf(to, cap);
				}
				from[edges] = pi;
				to[edges] = ni;
				edges++;
				if (!reach.get(ni)) {
					reach.set(ni);
					q.add(n[0], n[1], n[2]);
				}
			}
		}
		Result res = new Result(reach, null);
		for (Room r : rooms) {
			boolean any = false;
			for (int x = r.x0() + 1; x < r.x1() - 1 && !any; x++) {
				for (int z = r.z0() + 1; z < r.z1() - 1 && !any; z++) {
					for (int y = r.y0() + 1; y < r.y1() - 1 && !any; y++) {
						any = reach.get(index(x, y, z));
					}
				}
			}
			if (!any) {
				return new Result(reach, "room " + r + " cannot be reached");
			}
		}
		// backward search from every reachable spot of the boss room
		int[] head = new int[g.sx * g.sy * g.sz + 1];
		for (int e = 0; e < edges; e++) {
			head[to[e] + 1]++;
		}
		for (int i = 1; i < head.length; i++) {
			head[i] += head[i - 1];
		}
		int[] fill = java.util.Arrays.copyOf(head, head.length);
		int[] back = new int[edges];
		for (int e = 0; e < edges; e++) {
			back[fill[to[e]]++] = from[e];
		}
		BitSet toBoss = new BitSet();
		java.util.ArrayDeque<Integer> bq = new java.util.ArrayDeque<>();
		for (int x = boss.x0() + 1; x < boss.x1() - 1; x++) {
			for (int z = boss.z0() + 1; z < boss.z1() - 1; z++) {
				for (int y = boss.y0() + 1; y < boss.y1() - 1; y++) {
					int i = index(x, y, z);
					if (reach.get(i)) {
						toBoss.set(i);
						bq.add(i);
					}
				}
			}
		}
		while (!bq.isEmpty()) {
			int i = bq.poll();
			for (int k = head[i]; k < head[i + 1]; k++) {
				int p = back[k];
				if (!toBoss.get(p)) {
					toBoss.set(p);
					bq.add(p);
				}
			}
		}
		BitSet stuck = (BitSet) reach.clone();
		stuck.andNot(toBoss);
		if (!stuck.isEmpty()) {
			int i = stuck.nextSetBit(0);
			int x = i % g.sx;
			int z = (i / g.sx) % g.sz;
			int y = i / (g.sx * g.sz);
			return new Result(reach, "stuck at " + x + " " + y + " " + z + " (" + stuck.cardinality() + " spots)");
		}
		return res;
	}

	/** A small queue of (x, y, z) without boxing. */
	private static final class ArrayDeque {
		private int[] a = new int[3 * 4096];
		private int head;
		private int tail;

		void add(int x, int y, int z) {
			if (tail + 3 > a.length) {
				if (head > 0) {
					System.arraycopy(a, head, a, 0, tail - head);
					tail -= head;
					head = 0;
				}
				if (tail + 3 > a.length) {
					a = java.util.Arrays.copyOf(a, a.length * 2);
				}
			}
			a[tail++] = x;
			a[tail++] = y;
			a[tail++] = z;
		}

		boolean isEmpty() {
			return head == tail;
		}

		int[] poll() {
			int[] p = {a[head], a[head + 1], a[head + 2]};
			head += 3;
			return p;
		}
	}
}
