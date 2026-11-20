package io.github.huydangtruong.immortalcultivation.technique;

/** Solar Lance numbers (D157, D159). Pure Java. */
public final class LanceRules {
	public static final double RANGE = 256;
	public static final int DURATION_TICKS = 240; // 12 s at most, a release stops it earlier (D159)
	public static final int DAMAGE_INTERVAL = 10;
	public static final int STAGE_TICKS = 60; // damage doubles every 3 s
	public static final int BREAK_TICKS = 6; // D161: the beam breaks the block it rests on after 0.3 s

	private LanceRules() {}

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

	/** D163: the beam looks wider the longer it fires: 1 at the start, MAX_WIDTH_FACTOR after the full 12 s, growing smoothly. */
	public static final float MAX_WIDTH_FACTOR = 2.5f;

	public static float widthFactor(int ticksLeft) {
		float firing = Math.max(0f, Math.min(1f, 1f - ticksLeft / (float) DURATION_TICKS));
		return 1f + (MAX_WIDTH_FACTOR - 1f) * firing;
	}

	/** Fraction of the bar left (1 = full, 0 = empty). */
	public static float barFraction(int ticksLeft) {
		return Math.max(0f, Math.min(1f, ticksLeft / (float) DURATION_TICKS));
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
