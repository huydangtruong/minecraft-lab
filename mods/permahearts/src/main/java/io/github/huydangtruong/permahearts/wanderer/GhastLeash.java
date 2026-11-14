package io.github.huydangtruong.permahearts.wanderer;

/**
 * Rules for the Ghast boss (D35), pure Java. A vanilla ghast only floats to random points and never follows anyone; in
 * the open Overworld sky it drifts up and away. Once a second the boss is checked: too far from the Crimson Master, or
 * too high above the player, and it is sent back to a point hovering over the player.
 */
public final class GhastLeash {
	/** Horizontal distance from the Crimson Master the boss may float. */
	public static final double RADIUS = 24;
	/** Height above the player (or the ground, when the player is under it) the boss may float. */
	public static final double MAX_ABOVE = 16;
	/** Hover height above the player when it is pulled back. */
	public static final double HOVER_MIN = 8;
	public static final double HOVER_MAX = 12;
	/** Spawn height above the ground near the Crimson Master. */
	public static final int SPAWN_ABOVE = 6;
	/** Vanilla waits 40 ticks after a shot (60 ticks per shot with the 20 tick charge); 30 gives 50: 20% faster. */
	public static final int VANILLA_COOLDOWN = 40;
	public static final int BOSS_COOLDOWN = 30;

	private GhastLeash() {}

	/** True when the boss at (gx, gy, gz) must be pulled back. refY = the player's height, or the ground's if higher. */
	public static boolean outOfBounds(double gx, double gy, double gz, double anchorX, double anchorZ, double refY) {
		return Math.hypot(gx - anchorX, gz - anchorZ) > RADIUS || gy - refY > MAX_ABOVE;
	}

	/**
	 * Where to send the boss: above (px, pz) by hover (8..12) plus a small offset, moved inside the radius around the
	 * Crimson Master when the player stands outside it. Returns {x, y, z}.
	 */
	public static double[] returnPoint(double px, double pz, double refY, double anchorX, double anchorZ, double hover,
		double offsetX, double offsetZ) {
		double x = px + offsetX;
		double z = pz + offsetZ;
		double dx = x - anchorX;
		double dz = z - anchorZ;
		double d = Math.hypot(dx, dz);
		double max = RADIUS - 4; // a little inside, so the next random float does not leave at once
		if (d > max) {
			x = anchorX + dx / d * max;
			z = anchorZ + dz / d * max;
		}
		return new double[] {x, refY + Math.max(HOVER_MIN, Math.min(HOVER_MAX, hover)), z};
	}
}
