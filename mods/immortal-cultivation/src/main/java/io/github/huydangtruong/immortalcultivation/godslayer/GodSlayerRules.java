package io.github.huydangtruong.immortalcultivation.godslayer;

import io.github.huydangtruong.immortalcultivation.reward.RewardTier;
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

	/**
	 * D184: from day 100 the boss takes the special-event slot after every 2nd Deadly Treasure (Crimson Master, Treasure,
	 * Crimson Master, Treasure, God Slayer), and hunts a player with at least 30 hearts.
	 */
	public static final int MIN_DAY = 100;
	public static final int TREASURES_PER_HUNT = 2;
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

	/** D286: every level is 30% stronger than the level before (compounding). */
	public static final double GROWTH = 1.3;

	/** Strength factor: 1 at level 1, x1.3 per level (Lv 10 = 1.3^9, about 10.6). Health and every damage use it. */
	public static double scale(int level) {
		return Math.pow(GROWTH, clampLevel(level) - 1);
	}

	/**
	 * Damage of a skill: its start value at the level it unlocks, then x1.3 per level above (D286, D287).
	 * Below the unlock level the start value is returned.
	 */
	public static float skillDamage(float start, int unlockLevel, int level) {
		return (float) (start * Math.pow(GROWTH, Math.max(0, clampLevel(level) - unlockLevel)));
	}

	/** D287: skill start values (at the unlock level) and shapes. */
	public static final float FIREBALL_START = 15f;
	public static final int FIREBALL_COUNT = 6;
	public static final float KATANA_START = 28f;
	public static final int KATANA_COUNT = 3;
	/** Angle between two neighbouring katanas of the fan, in degrees. */
	public static final double KATANA_FAN_STEP = 15;
	/** Flame breath: 3 x 1.6 (old Lv 4 value) x 1.5. */
	public static final float FLAME_START = 7.2f;
	public static final double FLAME_RANGE = 10;
	/** Half width of the flame cone, in degrees (was 30). */
	public static final double FLAME_HALF_ANGLE = 15;
	/** Eye beam: 4 x 1.8 (old Lv 5 value) x 2. */
	public static final float EYE_START = 14.4f;

	/** D288: Lv 6 blast. */
	public static final int BLAST_LEVEL = 6;
	public static final float BLAST_START = 50f;
	public static final double BLAST_RADIUS = 8;
	public static final int BLAST_WINDUP_TICKS = 30;
	public static final int BLIND_TICKS = 60;

	/** Is this block offset inside the blast sphere? */
	public static boolean inBlast(int dx, int dy, int dz) {
		return dx * dx + dy * dy + dz * dz <= BLAST_RADIUS * BLAST_RADIUS;
	}

	/** Opacity (0..1) of the red blindness screen: full, fading out over the last 10 ticks. */
	public static float blindAlpha(long ticksLeft) {
		if (ticksLeft <= 0) {
			return 0f;
		}
		return Math.min(1f, ticksLeft / 10f);
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

	/** D184: is this special-event slot the boss's? treasuresSince = Deadly Treasures started since the boss's last slot. */
	public static boolean isHuntSlot(long day, int treasuresSince) {
		return day >= MIN_DAY && treasuresSince >= TREASURES_PER_HUNT;
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

	/** D110: total levels for a kill = 50 + 50 per player who dealt damage. */
	public static int totalKillLevels(int damagers) {
		return 50 + 50 * damagers;
	}

	/** D110: each player's share of the total, by damage share. Players with no damage get 0. */
	public static java.util.Map<java.util.UUID, Integer> splitKillLevels(java.util.Map<java.util.UUID, Float> damage) {
		java.util.Map<java.util.UUID, Integer> out = new java.util.LinkedHashMap<>();
		double all = 0;
		int n = 0;
		for (float f : damage.values()) {
			if (f > 0) {
				all += f;
				n++;
			}
		}
		int total = totalKillLevels(n);
		for (java.util.Map.Entry<java.util.UUID, Float> e : damage.entrySet()) {
			if (e.getValue() > 0 && all > 0) {
				out.put(e.getKey(), (int) Math.round(total * e.getValue() / all));
			}
		}
		return out;
	}
}
