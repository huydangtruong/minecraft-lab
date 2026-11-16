package io.github.huydangtruong.permahearts.godslayer;

import io.github.huydangtruong.permahearts.reward.RewardTier;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Pure numbers and decisions of the God Slayer boss (P-032, D96, D97). No Minecraft imports, so JUnit can test them. */
public final class GodSlayerRules {
	private GodSlayerRules() {}

	public static final int MAX_LEVEL = 10;
	public static final float BASE_HEALTH = 500f;
	public static final float BASE_SLASH_DAMAGE = 30f;
	/** No single hit takes more than this from the boss (anti-cheese). */
	public static final float MAX_DAMAGE_PER_HIT = 40f;

	/** A hunt lasts 10 minutes; the warning before it 60 seconds. */
	public static final int HUNT_TICKS = 12000;
	public static final int WARNING_TICKS = 1200;

	/** The boss can come from day 100, on every 4th Crimson Master visit, to players with at least 30 hearts. */
	public static final int MIN_DAY = 100;
	public static final int VISIT_PERIOD = 4;
	public static final int MIN_HEARTS = 30;

	/** Beyond this distance (or in another dimension, or stuck this long) the boss teleports next to its target (D100). */
	public static final double TELEPORT_RANGE = 64;
	/** 8 s without a path to the target, or without moving, while not in reach. A target that merely keeps its distance never counts. */
	public static final int STUCK_TICKS = 160;

	public static final double SLASH_REACH = 3.5;
	public static final int SLASH_COOLDOWN = 20;

	/** Speeds in blocks per tick. A dolphin swims about 0.5; the boss swims three times as fast. */
	public static final double DOLPHIN_SWIM = 0.5;
	public static final double WATER_SPEED = DOLPHIN_SWIM * 3;
	public static final double AIR_SPEED = 0.9;
	public static final double GROUND_SPEED_ATTRIBUTE = 0.4;

	/** Frostbite slow and Repulse push work at this share on the boss. */
	public static final double CONTROL_RESIST = 0.1;

	/** A boss that nobody hit for this long, or that cannot be reached, heals 2% of its max health per second. */
	public static final int IDLE_REGEN_TICKS = 600;
	public static final int REACH_REGEN_WINDOW = 100;
	public static final int UNREACHABLE_TICKS = 40;
	public static final double REGEN_PERCENT_PER_SECOND = 0.02;

	public static final int KILL_LEVELS = 100;
	public static final int REWARD_BOXES = 5;

	public enum Mode { GROUND, AIR, WATER }

	public static int clampLevel(int level) {
		return Math.max(1, Math.min(MAX_LEVEL, level));
	}

	/** Body scale: 1 at level 1, +0.2 per level. */
	public static double scale(int level) {
		return 1 + 0.2 * (clampLevel(level) - 1);
	}

	public static float maxHealth(int level) {
		return (float) (BASE_HEALTH * scale(level));
	}

	public static float slashDamage(int level) {
		return (float) (BASE_SLASH_DAMAGE * scale(level));
	}

	public static float clampDamage(float amount) {
		return Math.min(amount, MAX_DAMAGE_PER_HIT);
	}

	/** A box tier by the equal 25% odds of the boss reward. */
	public static RewardTier rollTier(RandomGenerator rng) {
		return RewardTier.values()[rng.nextInt(RewardTier.values().length)];
	}

	public static List<RewardTier> rewardBoxes(RandomGenerator rng) {
		List<RewardTier> out = new ArrayList<>();
		for (int i = 0; i < REWARD_BOXES; i++) {
			out.add(rollTier(rng));
		}
		return out;
	}

	/** Teleport to the target: another dimension, farther than 64 blocks, or no path / not moving for 8 s while out of reach. */
	public static boolean shouldTeleport(double distance, boolean sameDimension, int ticksStuck) {
		return !sameDimension || distance > TELEPORT_RANGE || ticksStuck >= STUCK_TICKS;
	}

	/**
	 * Movement mode: water wins; then the air, when the target stands high above the ground or the ground is blocked
	 * and the target is above; the ground otherwise. The air mode is kept while the target stays above, so it does not flip.
	 */
	public static Mode chooseMode(boolean inWater, double targetHeightAboveGround, double dy, int stuckTicks, Mode current) {
		if (inWater) {
			return Mode.WATER;
		}
		if (targetHeightAboveGround >= 3 || (stuckTicks >= UNREACHABLE_TICKS && dy >= 2.5)) {
			return Mode.AIR;
		}
		if (current == Mode.AIR && (targetHeightAboveGround >= 1.5 || dy >= 2.5)) {
			return Mode.AIR;
		}
		return Mode.GROUND;
	}

	/** The boss heals while nobody can hurt it: hit lately but out of reach, or left alone for 30 s. */
	public static boolean regenActive(int ticksSinceHit, int stuckTicks, boolean inReach) {
		if (ticksSinceHit >= IDLE_REGEN_TICKS) {
			return true;
		}
		return ticksSinceHit <= REACH_REGEN_WINDOW && stuckTicks >= UNREACHABLE_TICKS && !inReach;
	}

	/** Health gained per second by regeneration. */
	public static float regenAmount(float maxHealth) {
		return (float) (maxHealth * REGEN_PERCENT_PER_SECOND);
	}

	/** Frostbite slow percent on the boss: a tenth, at least 1%. */
	public static int frostPercent(int percent) {
		return Math.max(1, (int) Math.round(percent * CONTROL_RESIST));
	}

	/** Stun ticks on the boss: a tenth, at least 1 tick (P-034, D99). */
	public static int stunTicks(int ticks) {
		return Math.max(1, (int) Math.round(ticks * CONTROL_RESIST));
	}

	/** Is this Crimson Master visit one that brings the boss: day 100 or later, every 4th visit. */
	public static boolean isHuntVisit(long day, int visitCount) {
		return day >= MIN_DAY && visitCount > 0 && visitCount % VISIT_PERIOD == 0;
	}

	public static boolean heartsEnough(int hearts) {
		return hearts >= MIN_HEARTS;
	}

	/** Pulse of the red edge, 0.85..1: peaks on the tick the drum plays (ticksLeft a multiple of 20), fades until the next one. */
	public static float edgePulse(long ticksLeft) {
		int age = (int) Math.floorMod(20 - Math.floorMod(ticksLeft, 20L), 20L);
		float k = 1f - age / 20f;
		return 0.85f + 0.15f * k * k;
	}

	/** Thickness of the red screen edge as a share of the shorter screen side: fixed, it does not grow (P-037). */
	public static final float EDGE_THICKNESS = 0.06f;
	/** The red edge is never more opaque than this at its outer rim. */
	public static final float EDGE_MAX_ALPHA = 0.35f;

	/**
	 * Opacity, 0..EDGE_MAX_ALPHA, of the 1 px ring number i of n (0 = outermost) of the red edge: fades smoothly to fully
	 * transparent at the inner side. strength = edgeStrength, pulse = about 0.85..1.
	 */
	public static float edgeLayerAlpha(float strength, float pulse, int i, int n) {
		if (n <= 0 || i < 0 || i >= n) {
			return 0f;
		}
		float k = 1f - i / (float) n;
		return Math.min(EDGE_MAX_ALPHA, Math.max(0f, strength) * Math.max(0f, pulse) * EDGE_MAX_ALPHA * k * k);
	}

	/** Strength of the red screen edge, 0..1: nothing at the start of the warning, full when it ends. */
	public static float edgeStrength(long ticksLeft) {
		if (ticksLeft <= 0) {
			return 0f;
		}
		double f = 1.0 - Math.min(1.0, ticksLeft / (double) WARNING_TICKS);
		return (float) Math.max(0.05, f * f);
	}

	/** Seconds shown in the warning countdown (rounded up). */
	public static int secondsLeft(long ticksLeft) {
		return (int) Math.max(0, (ticksLeft + 19) / 20);
	}
}
