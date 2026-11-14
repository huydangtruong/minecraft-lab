package io.github.huydangtruong.permahearts.technique;

/**
 * Launch speed of the Flame Burst / Frost Orb so that a full-charge throw lands as far as the technique's range on flat
 * ground. Pure Java: models a vanilla thrown projectile (move, then drag 0.99, then gravity 0.03 per tick).
 */
public final class OrbBallistics {
	public static final double GRAVITY = 0.03;
	public static final double DRAG = 0.99;
	/** The lowest share of the range a throw reaches (a tap is never a dud). */
	public static final float MIN_CHARGE = 0.2f;

	private OrbBallistics() {}

	/** Horizontal distance a level throw at speed (blocks per tick) travels before it drops eyeHeight blocks. */
	public static double flatRange(double speed, double eyeHeight) {
		double x = 0;
		double y = 0;
		double vx = speed;
		double vy = 0;
		for (int tick = 0; tick < 5000; tick++) {
			double nx = x + vx;
			double ny = y + vy;
			if (ny <= -eyeHeight) {
				double t = (-eyeHeight - y) / (ny - y);
				return x + (nx - x) * t;
			}
			x = nx;
			y = ny;
			vx *= DRAG;
			vy = vy * DRAG - GRAVITY;
		}
		return x;
	}

	/** Speed (blocks per tick) whose level throw reaches the given range. */
	public static double speedForRange(double range, double eyeHeight) {
		double lo = 0.01;
		double hi = 20;
		for (int i = 0; i < 60; i++) {
			double mid = (lo + hi) / 2;
			if (flatRange(mid, eyeHeight) < range) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		return (lo + hi) / 2;
	}

	/** Range actually aimed for at a charge (0..1). */
	public static double chargedRange(double fullRange, float charge) {
		return fullRange * Math.max(MIN_CHARGE, Math.min(1f, charge));
	}
}
