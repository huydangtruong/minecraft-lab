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
