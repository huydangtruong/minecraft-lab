package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

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
}
