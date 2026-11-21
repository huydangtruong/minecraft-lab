package io.github.huydangtruong.immortalcultivation.technique;

/** Life Stealer numbers (D158, D159, D180, D181). Pure Java. */
public final class StealerRules {
	public static final double RANGE = 16;
	public static final int BLEED_SECONDS = 2;
	public static final int SLOW_SECONDS = 2;
	public static final double SLOW = 0.8; // -80% movement speed
	public static final int PULSES = 10; // D180: 10 drain pulses
	public static final int PULSE_TICKS = 4; // one every 0.2 s: the drain lasts 2 s
	public static final int FLY_IN_TICKS = 10;
	public static final int LIFT_TICKS = 5;
	public static final int FLY_OUT_TICKS = 10;
	public static final double LIFT_HEIGHT = 2;
	public static final double BAT_FAR = 3; // blocks from the target when the bats appear and when they vanish
	public static final double BAT_NEAR = 0.9; // blocks from the target while they bite

	private StealerRules() {}

	private static int clamp(int level) {
		return Math.max(1, Math.min(TechniqueRules.MAX_LEVEL, level));
	}

	/** Share of the target's max health drained: D181: 36% at Lv 1, +2% per level (74% at Lv 20); was 30% (D164). */
	public static double pct(int level) {
		return 0.36 + 0.02 * (clamp(level) - 1);
	}

	/** Most health points one cast drains: 7 at Lv 1, +3 per level (64 at Lv 20). */
	public static double cap(int level) {
		return 7 + 3 * (clamp(level) - 1);
	}

	public static double amount(int level, double targetMaxHealth) {
		return Math.max(0, Math.min(pct(level) * targetMaxHealth, cap(level)));
	}

	/** One pulse: an equal part of the total amount. */
	public static double pulse(int level, double targetMaxHealth) {
		return amount(level, targetMaxHealth) / PULSES;
	}

	/** Length of the drain: 40 ticks (2 s). */
	public static int drainTicks() {
		return PULSES * PULSE_TICKS;
	}

	/** Share (0..1) of the lift done {@code ticksSinceArrival} ticks after the bats arrived. */
	public static double liftProgress(int ticksSinceArrival) {
		return Math.max(0, Math.min(1, ticksSinceArrival / (double) LIFT_TICKS));
	}

	/** Distance of the bats from the target: far to near while flying in (tick 0..FLY_IN_TICKS), near while biting, near to far while flying out. */
	public static double batDistance(int flyTick, boolean out) {
		double t = Math.max(0, Math.min(1, flyTick / (double) (out ? FLY_OUT_TICKS : FLY_IN_TICKS)));
		return out ? BAT_NEAR + (BAT_FAR - BAT_NEAR) * t : BAT_FAR + (BAT_NEAR - BAT_FAR) * t;
	}
}
