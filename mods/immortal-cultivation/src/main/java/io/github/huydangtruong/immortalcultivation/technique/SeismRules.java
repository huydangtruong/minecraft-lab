package io.github.huydangtruong.immortalcultivation.technique;

/**
 * Pure numbers of Seism (D142, P-056): Seismic Impact (a boulder where you aim) and Seismic Wave (three quake pulses
 * around the caster, from Lv 3). No Minecraft imports, JUnit-tested. Units: 1 heart = 2 HP.
 */
public final class SeismRules {
	/** Aim range of the boulder, in blocks. */
	public static final double AIM_RANGE = 48;
	/** The boulder appears this many blocks above the impact point. */
	public static final double FALL_HEIGHT = 5;
	/** Ticks the boulder falls (0.5 s, D154); the impact point follows the crosshair meanwhile. */
	public static final int FALL_TICKS = 10;
	/** Impact radius in blocks (fixed). */
	public static final double IMPACT_RADIUS = 3;
	/** First level with the Wave. */
	public static final int WAVE_MIN_LEVEL = 3;
	/** Pulse radii of the Wave, one per pulse. */
	public static final double[] WAVE_RADII = {3, 4, 5};
	/** Ticks between two pulses (1 s). */
	public static final int WAVE_PULSE_TICKS = 20;
	/** Ticks the caster stays slowed: from the first pulse to the last (3 s). */
	public static final int WAVE_SLOW_TICKS = 60;
	/** Movement speed modifier while the Wave runs (ADD_MULTIPLIED_TOTAL: -80% of the current speed). */
	public static final double WAVE_SLOW = -0.8;
	/** Horizontal knockback of a wave hit, in blocks. */
	public static final double WAVE_KNOCKBACK = 2;
	/** Hearts cap of one wave hit. */
	public static final int WAVE_MAX_HEARTS = 8;
	/** Vanilla living entity gravity and vertical drag per tick, used for the lift speed. */
	public static final double GRAVITY = 0.08;
	public static final double DRAG = 0.98;
	/** Shown when right click is held to the full charge at a level below the Wave. */
	public static final String WAVE_LOCKED_HINT = "Seismic Wave unlocks at Lv 3";

	private SeismRules() {}

	/** Impact damage in hearts: 5 at Lv 1, +1 at every even level (Lv 2 = 6, Lv 20 = 15). */
	public static int impactHearts(int level) {
		return 5 + level / 2;
	}

	/** True when a Wave may be cast: the level is high enough and the hold reached the full charge. */
	public static boolean waveCast(int level, float charge) {
		return level >= WAVE_MIN_LEVEL && charge >= 1f;
	}

	/** Extra steps above Lv 3: one per two levels (Lv 3-4 = 0, Lv 5-6 = 1 ...). */
	public static int steps(int level) {
		return Math.max(0, (level - 3) / 2);
	}

	/** Wave damage per pulse in hearts: 2 at Lv 3, +1 per step, capped at 8 (reached at Lv 15). */
	public static int waveHearts(int level) {
		return Math.min(WAVE_MAX_HEARTS, 2 + steps(level));
	}

	/** Wave lift in blocks: 5 at Lv 3, +1 per step (13 at Lv 19-20). */
	public static int waveLift(int level) {
		return 5 + steps(level);
	}

	/** Whole blocks a target stands above the caster (0 or less = level with or below). */
	public static int heightAbove(double targetY, double casterY) {
		return (int) Math.floor(targetY - casterY);
	}

	/** Hearts after the height rule: each block above the caster takes one heart (never below 0). */
	public static int heartsAt(int level, int dy) {
		return Math.max(0, waveHearts(level) - Math.max(0, dy));
	}

	/** Lift after the height rule: each block above the caster takes one block (never below 0). */
	public static int liftAt(int level, int dy) {
		return Math.max(0, waveLift(level) - Math.max(0, dy));
	}

	/** Radius of the n-th pulse (0..2). */
	public static double waveRadius(int pulse) {
		return WAVE_RADII[Math.max(0, Math.min(WAVE_RADII.length - 1, pulse))];
	}

	/** Height reached by a body launched with this vertical speed: each tick it moves by its speed, then speed = (speed - gravity) * drag. */
	public static double heightReached(double v0) {
		double v = v0;
		double y = 0;
		while (v > 0) {
			y += v;
			v = (v - GRAVITY) * DRAG;
		}
		return y;
	}

	/** Vertical speed (blocks per tick) that lifts a body about {@code blocks} high under vanilla gravity and drag. */
	public static double liftVelocity(double blocks) {
		if (blocks <= 0) {
			return 0;
		}
		double lo = 0;
		double hi = 5;
		for (int i = 0; i < 60; i++) {
			double mid = (lo + hi) / 2;
			if (heightReached(mid) < blocks) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		return (lo + hi) / 2;
	}

	/** Horizontal start speed so that a body slides about {@code blocks} in the air (drag 0.91 per tick). */
	public static double knockbackSpeed(double blocks) {
		return blocks * (1 - 0.91);
	}

	/** Boulder fall progress 0..1 at an age in ticks. */
	public static float fallProgress(int age) {
		return Math.max(0f, Math.min(1f, age / (float) FALL_TICKS));
	}

	/** Boulder size scale: grows from 25% to 100% while it falls. */
	public static float boulderScale(float progress) {
		return 0.25f + 0.75f * progress;
	}
}
