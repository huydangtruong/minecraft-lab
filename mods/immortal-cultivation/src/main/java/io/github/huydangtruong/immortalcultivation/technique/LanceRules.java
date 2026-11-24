package io.github.huydangtruong.immortalcultivation.technique;

/** Solar Lance numbers (D157, D159). Pure Java. */
public final class LanceRules {
	public static final double RANGE = 256;
	/** D243: firing time grows with the level, 12 s at Lv 1 up to 24 s at Lv 20; a release stops it earlier (D159). */
	public static final int BASE_DURATION_TICKS = 240;
	public static final int MAX_DURATION_TICKS = 480;
	private static final int TOP_LEVEL = 20;

	public static int durationTicks(int level) {
		int l = Math.max(1, Math.min(TOP_LEVEL, level));
		return BASE_DURATION_TICKS + (MAX_DURATION_TICKS - BASE_DURATION_TICKS) * (l - 1) / (TOP_LEVEL - 1);
	}
	public static final int DAMAGE_INTERVAL = 10;
	public static final int STAGE_TICKS = 60; // damage doubles every 3 s
	public static final int BREAK_TICKS = 6; // D161: the beam breaks the block it rests on after 0.3 s

	/** D201: every beam hit slows what it crosses: Slowness II for 1.5 s, refreshed by the next hit, so it ends soon after the beam leaves. */
	public static final int SLOW_AMPLIFIER = 1;
	public static final int SLOW_TICKS = 30;
	/** D201: and pushes it a little along the beam, like heat vision pushing a person back: blocks per tick of speed added by one hit (about 1 block of travel on flat ground). */
	public static final double PUSH = 0.35;
	/** A tiny lift so ground friction does not eat the push. */
	public static final double PUSH_LIFT = 0.1;

	private LanceRules() {}

	/** Knockback resistance 0..1 (bosses have 1) scales the push: 1 = nothing left. */
	public static double pushScale(double knockbackResistance) {
		return 1 - Math.max(0, Math.min(1, knockbackResistance));
	}

	/** Horizontal push along (dirX, dirZ), the beam direction; {0, 0} when the direction is flat zero. Result is {x, z}. */
	public static double[] push(double dirX, double dirZ, double knockbackResistance) {
		double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
		if (len < 1e-6) {
			return new double[] {0, 0};
		}
		double k = PUSH * pushScale(knockbackResistance) / len;
		return new double[] {dirX * k, dirZ * k};
	}

	/** Lift that goes with the push (zero when the target resists everything). */
	public static double pushLift(double knockbackResistance) {
		return PUSH_LIFT * pushScale(knockbackResistance);
	}

	/** Damage per second before doubling: 1 at Lv 1, +0.5 per level (10.5 at Lv 20). */
	public static double baseDamagePerSecond(int level) {
		return 1 + 0.5 * (Math.max(1, Math.min(TechniqueRules.MAX_LEVEL, level)) - 1);
	}

	/** Stage 0..3 after the given number of ticks of firing. */
	public static int stage(int ticksFiring) {
		return Math.max(0, ticksFiring) / STAGE_TICKS;
	}

	public static double damagePerSecond(int level, int ticksFiring) {
		return baseDamagePerSecond(level) * (1 << stage(ticksFiring));
	}

	/** Damage of one hit (applied every DAMAGE_INTERVAL ticks): half of the per-second value. */
	public static double damagePerHit(int level, int ticksFiring) {
		return damagePerSecond(level, ticksFiring) * DAMAGE_INTERVAL / 20.0;
	}

	/** D163: the beam looks wider the longer it fires: 1 at the start, MAX_WIDTH_FACTOR at the end of its full time, growing smoothly. */
	public static final float MAX_WIDTH_FACTOR = 2.5f;

	public static float widthFactor(int ticksLeft, int total) {
		float firing = Math.max(0f, Math.min(1f, 1f - ticksLeft / (float) Math.max(1, total)));
		return 1f + (MAX_WIDTH_FACTOR - 1f) * firing;
	}

	/** Fraction of the bar left (1 = full, 0 = empty). */
	public static float barFraction(int ticksLeft, int total) {
		return Math.max(0f, Math.min(1f, ticksLeft / (float) Math.max(1, total)));
	}

	/** True when the beam has rested on one block for enough ticks to break it. */
	public static boolean breaksBlock(int ticksOnBlock) {
		return ticksOnBlock >= BREAK_TICKS;
	}

	/** Vanilla crack stage 0..9 for the given ticks on a block. */
	public static int crackStage(int ticksOnBlock) {
		return Math.max(0, Math.min(9, ticksOnBlock * 10 / BREAK_TICKS));
	}

	/** Only blocks that can be mined at all (destroy speed 0 or more) break; bedrock, barriers and the like never do. */
	public static boolean breakable(float destroySpeed) {
		return destroySpeed >= 0;
	}
}
