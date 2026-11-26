package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

import java.util.ArrayList;
import java.util.List;

/** One room of a floor: which cells it covers, how it was drawn and the points the game needs (all in grid coordinates). */
public final class Room {
	public final int index;
	public final RoomKind kind;
	public final String module;
	public final Style style;
	/** First cell (column, row, layer) and size in cells. */
	public final int cx;
	public final int cz;
	public final int layer;
	public final int cw;
	public final int cd;
	public final List<Port> ports = new ArrayList<>();
	/** Feet positions where mobs may appear. */
	public final List<int[]> spawns = new ArrayList<>();
	/** Doorway voxels that become bars while the room is sealed. */
	public final List<int[]> gates = new ArrayList<>();
	public final List<int[]> chests = new ArrayList<>();
	/** D263: wooden gift chests (a smaller loot table than the treasure chest). */
	public final List<int[]> gifts = new ArrayList<>();
	public final List<int[]> plates = new ArrayList<>();
	/** x, y, z, facing ordinal. */
	public final List<int[]> dispensers = new ArrayList<>();
	/** D265: where the Seal Pillar stands (feet; pillar rooms). */
	public int[] pillar;
	/** Boss feet position (boss rooms). */
	public int[] boss;
	/** Stair rooms: the wall the stairs run along (no door there below). */
	public Dir stairSide = Dir.N;
	/** A standing spot near the middle (feet). */
	public int[] centre;

	Room(int index, RoomKind kind, String module, Style style, int cx, int cz, int layer, int cw, int cd) {
		this.index = index;
		this.kind = kind;
		this.module = module;
		this.style = style;
		this.cx = cx;
		this.cz = cz;
		this.layer = layer;
		this.cw = cw;
		this.cd = cd;
	}

	public int x0() {
		return cx * DungeonRules.CELL;
	}

	public int y0() {
		return layer * DungeonRules.CELL_H;
	}

	public int z0() {
		return cz * DungeonRules.CELL;
	}

	/** Exclusive ends. */
	public int x1() {
		return x0() + cw * DungeonRules.CELL;
	}

	public int y1() {
		return y0() + DungeonRules.CELL_H;
	}

	public int z1() {
		return z0() + cd * DungeonRules.CELL;
	}

	public boolean covers(int cellX, int cellZ, int cellLayer) {
		return cellLayer == layer && cellX >= cx && cellX < cx + cw && cellZ >= cz && cellZ < cz + cd;
	}

	@Override
	public String toString() {
		return module + "#" + index + "(" + kind + " " + cx + "," + cz + "," + layer + ")";
	}
}
