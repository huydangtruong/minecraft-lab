package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/** Horizontal sides of a cell, x grows east, z grows south (like Minecraft). */
public enum Dir {
	N(0, -1), E(1, 0), S(0, 1), W(-1, 0);

	public final int dx;
	public final int dz;

	Dir(int dx, int dz) {
		this.dx = dx;
		this.dz = dz;
	}

	public Dir opposite() {
		return values()[(ordinal() + 2) % 4];
	}

	public Dir cw() {
		return values()[(ordinal() + 1) % 4];
	}
}
