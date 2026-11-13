package io.github.huydangtruong.permahearts.wanderer;

/**
 * The Blood Moon arena (D33): a 96 x 96 square centred on the Crimson Master. While the Blood Moon runs, players inside
 * cannot leave and players outside (for example after respawning far away) cannot come in. Pure Java for JUnit.
 */
public final class BloodMoonArena {
	public static final int HALF = 48;
	/** Players this close to an edge see it drawn with particles. */
	public static final int SHOW_EDGE = 8;

	private BloodMoonArena() {}

	/** cx, cz: the Crimson Master's block; the arena is centred on that block's middle. */
	public static boolean inside(double x, double z, int cx, int cz) {
		return Math.abs(x - (cx + 0.5)) <= HALF && Math.abs(z - (cz + 0.5)) <= HALF;
	}

	/** Distance from (x, z) to the nearest edge line of the arena (inside or outside). */
	public static double edgeDistance(double x, double z, int cx, int cz) {
		double dx = Math.abs(Math.abs(x - (cx + 0.5)) - HALF);
		double dz = Math.abs(Math.abs(z - (cz + 0.5)) - HALF);
		return Math.min(dx, dz);
	}
}
