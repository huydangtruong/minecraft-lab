package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.ArrayList;
import java.util.List;

/** A generated floor: the voxels and its rooms. {@link #start} is where fighters arrive (feet, grid coordinates). */
public record Dungeon(long seed, int floor, Grid grid, List<Room> rooms, Room start, Room boss, int attempts) {
	/** The room covering a grid point, or null in a wall or outside. */
	public Room roomAt(int x, int y, int z) {
		for (Room r : rooms) {
			if (x > r.x0() && x < r.x1() - 1 && z > r.z0() && z < r.z1() - 1 && y > r.y0() && y < r.y1() - 1) {
				return r;
			}
		}
		return null;
	}

	/** D283: the rooms joined to this one by a door (same layer). */
	public List<Room> neighbours(Room room) {
		List<Room> out = new ArrayList<>();
		for (Port p : room.ports) {
			int cx;
			int cz;
			switch (p.side()) {
				case N -> {
					cx = room.cx + p.slot();
					cz = room.cz - 1;
				}
				case S -> {
					cx = room.cx + p.slot();
					cz = room.cz + room.cd;
				}
				case W -> {
					cx = room.cx - 1;
					cz = room.cz + p.slot();
				}
				default -> {
					cx = room.cx + room.cw;
					cz = room.cz + p.slot();
				}
			}
			for (Room r : rooms) {
				if (r != room && r.covers(cx, cz, room.layer) && !out.contains(r)) {
					out.add(r);
				}
			}
		}
		return out;
	}

	/**
	 * D269: skills may break the inside of a module but not its shell (walls, floor, ceiling, gates), nor anything in a
	 * staircase or Seal Pillar room (their steps are the only way up), nor anything outside the rooms (the bedrock skin).
	 */
	public boolean guarded(int x, int y, int z) {
		for (Room r : rooms) {
			if (x >= r.x0() && x < r.x1() && y >= r.y0() && y < r.y1() && z >= r.z0() && z < r.z1()) {
				return r.kind == RoomKind.STAIRS || r.kind == RoomKind.PILLAR
					|| x == r.x0() || x == r.x1() - 1 || z == r.z0() || z == r.z1() - 1 || y == r.y0() || y == r.y1() - 1;
			}
		}
		return true;
	}

	/** True when a grid point is part of some room's box (walls included). */
	public boolean built(int x, int y, int z) {
		for (Room r : rooms) {
			if (x >= r.x0() && x < r.x1() && y >= r.y0() && y < r.y1() && z >= r.z0() && z < r.z1()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * D259: the bedrock skin. Every point outside all rooms that touches a room's face (also one block past the grid), so a
	 * skill that breaks a wall only ever finds bedrock behind it, never the void.
	 */
	public List<int[]> skin() {
		int sx = grid.sx + 2;
		int sy = grid.sy + 2;
		int sz = grid.sz + 2;
		boolean[] in = new boolean[sx * sy * sz];
		for (Room r : rooms) {
			for (int x = r.x0(); x < r.x1(); x++) {
				for (int y = r.y0(); y < r.y1(); y++) {
					for (int z = r.z0(); z < r.z1(); z++) {
						in[((y + 1) * sz + z + 1) * sx + x + 1] = true;
					}
				}
			}
		}
		List<int[]> out = new ArrayList<>();
		int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int y = 0; y < sy; y++) {
			for (int z = 0; z < sz; z++) {
				for (int x = 0; x < sx; x++) {
					if (in[(y * sz + z) * sx + x]) {
						continue;
					}
					for (int[] f : faces) {
						int nx = x + f[0];
						int ny = y + f[1];
						int nz = z + f[2];
						if (nx >= 0 && ny >= 0 && nz >= 0 && nx < sx && ny < sy && nz < sz && in[(ny * sz + nz) * sx + nx]) {
							out.add(new int[] {x - 1, y - 1, z - 1});
							break;
						}
					}
				}
			}
		}
		return out;
	}
}
