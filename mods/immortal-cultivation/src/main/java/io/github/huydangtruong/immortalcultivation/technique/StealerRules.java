package io.github.huydangtruong.immortalcultivation.technique;

/** Life Stealer numbers (D158, D159). Pure Java. */
public final class StealerRules {
	public static final double RANGE = 16;
	public static final int BLEED_SECONDS = 2;
	public static final int SLOW_SECONDS = 2;
	public static final double SLOW = 0.8; // -80% movement speed

	private StealerRules() {}

	private static int clamp(int level) {
		return Math.max(1, Math.min(TechniqueRules.MAX_LEVEL, level));
	}

	/** Share of the target's max health drained: D164: 30% at Lv 1, +2% per level (68% at Lv 20). */
	public static double pct(int level) {
		return 0.30 + 0.02 * (clamp(level) - 1);
	}

	/** Most health points one cast drains: 7 at Lv 1, +3 per level (64 at Lv 20). */
	public static double cap(int level) {
		return 7 + 3 * (clamp(level) - 1);
	}

	public static double amount(int level, double targetMaxHealth) {
		return Math.max(0, Math.min(pct(level) * targetMaxHealth, cap(level)));
	}
}
