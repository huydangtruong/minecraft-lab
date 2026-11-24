package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/** A box of voxels: material, style and facing. Outside the box everything is {@link Mat#SHELL}. */
public final class Grid {
	public final int sx;
	public final int sy;
	public final int sz;
	private final byte[] mat;
	private final byte[] style;
	private final byte[] facing;

	public Grid(int sx, int sy, int sz) {
		this.sx = sx;
		this.sy = sy;
		this.sz = sz;
		mat = new byte[sx * sy * sz]; // 0 = SHELL
		style = new byte[mat.length];
		facing = new byte[mat.length];
	}

	public boolean in(int x, int y, int z) {
		return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
	}

	private int i(int x, int y, int z) {
		return (y * sz + z) * sx + x;
	}

	public Mat mat(int x, int y, int z) {
		return in(x, y, z) ? Mat.VALUES[mat[i(x, y, z)]] : Mat.SHELL;
	}

	public Style style(int x, int y, int z) {
		return in(x, y, z) ? Style.VALUES[style[i(x, y, z)]] : Style.BASE;
	}

	public Dir facing(int x, int y, int z) {
		return in(x, y, z) ? Dir.values()[facing[i(x, y, z)]] : Dir.N;
	}

	public void set(int x, int y, int z, Mat m, Style s, Dir f) {
		if (in(x, y, z)) {
			int k = i(x, y, z);
			mat[k] = (byte) m.ordinal();
			style[k] = (byte) s.ordinal();
			facing[k] = (byte) f.ordinal();
		}
	}
}
