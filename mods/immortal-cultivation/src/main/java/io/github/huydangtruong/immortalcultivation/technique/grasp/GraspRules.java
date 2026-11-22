package io.github.huydangtruong.immortalcultivation.technique.grasp;

/**
 * Pure rules of Soul Grasp (D134, D135, D136): what a level can hold, charge time and the refusal reasons. No Minecraft
 * imports, JUnit-tested. The Minecraft glue lives in {@code technique.grasp}.
 */
public final class GraspRules {
	public static final int MAX_LEVEL = 20;
	/** Blocks within which a mob can be picked as the target. */
	public static final double SELECT_RANGE = 16;
	/** The tether breaks when the mob is farther than this (blocks). */
	public static final double BREAK_RANGE = 24;
	/** The tether breaks after this many ticks without line of sight. */
	public static final int NO_SIGHT_TICKS = 20;
	/** A following mob teleports next to its owner when farther than this (blocks). */
	public static final double FOLLOW_TELEPORT = 12;
	/** D165: a following mob keeps its own spot on a ring this far from its owner (blocks), so a crowd never stands on the owner. */
	public static final double FOLLOW_RING_MIN = 2.5, FOLLOW_RING_MAX = 3.5;
	/** D165: a follower closer than this to its owner (blocks, flat distance) is pushed out. */
	public static final double FOLLOW_KEEP_OUT = 2.2;
	/** Patrol radius of Guard mode (blocks). */
	public static final double GUARD_RADIUS = 8;
	/** Radius of Stay mode: it goes back to its post when pulled farther than this. */
	public static final double STAY_RADIUS = 2;
	/** A released mob ignores its ex-owner for this many ticks. */
	public static final int RELEASE_PEACE_TICKS = 60;
	/** Cooldown in seconds at every level, started only on success. */
	public static final int COOLDOWN_SECONDS = 120;
	/** Per-mob cooldown of Call (ticks, 10 s). */
	public static final int CALL_COOLDOWN_TICKS = 200;
	/** Damage a grasped villager deals when its type has no attack damage (D135). */
	public static final double VILLAGER_DAMAGE = 3;
	/** Damage a grasped passive animal or other non-fighter deals (D135). */
	public static final double PASSIVE_DAMAGE = 2;
	/** How far (blocks) a mob looks for hostile monsters, by mode. */
	public static final double SCAN_FOLLOW = 6, SCAN_STAY = 6, SCAN_GUARD = 16;

	/** Behaviour of a grasped mob. */
	public enum Mode {
		FOLLOW, STAY, GUARD;

		public static Mode of(int ordinal) {
			return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : FOLLOW;
		}
	}

	/** Why a mob cannot be grasped (OK = it can). */
	public enum Reason { OK, TOO_STRONG, FULL, NOT_TAMEABLE }

	/** Result of {@link #check}: levelNeeded is only meaningful for TOO_STRONG (21 = no level can hold it). */
	public record Check(Reason reason, int levelNeeded) {
		public boolean ok() {
			return reason == Reason.OK;
		}
	}

	/** Facts about the target the glue reads from the game. Any true flag makes it not tameable. */
	public record Facts(boolean player, boolean boss, boolean crimsonMaster, boolean ownedSummon, boolean illusionClone,
			boolean alreadyOwned, boolean wanderingTrader, boolean waterMob, boolean slime) {
		public boolean tameable() {
			return !(player || boss || crimsonMaster || ownedSummon || illusionClone || alreadyOwned || wanderingTrader || waterMob || slime);
		}
	}

	private GraspRules() {}

	/**
	 * D165: offset {dx, dz} of a following mob's spot around its owner, from a stable per-mob seed (the golden angle spreads
	 * seeds round the circle; even seeds on the inner ring, odd on the outer one).
	 */
	public static double[] followSpot(int seed) {
		double angle = seed * 2.399963229728653;
		double r = (seed & 1) == 0 ? FOLLOW_RING_MIN : FOLLOW_RING_MAX;
		return new double[] {Math.cos(angle) * r, Math.sin(angle) * r};
	}

	private static int clamp(int level) {
		return Math.max(1, Math.min(MAX_LEVEL, level));
	}

	/** Highest max health (HP) of a mob that can be grasped: 20 at Lv 1, 115 at Lv 20. */
	public static int maxTameHealth(int level) {
		return 20 + 5 * (clamp(level) - 1);
	}

	/** Extra max health (HP) a grasped mob gets: 5 at Lv 1, 43 at Lv 20. */
	public static int bonusHealth(int level) {
		return 5 + 2 * (clamp(level) - 1);
	}

	/** Mobs held at once: 1 at Lv 1, 2 at Lv 2 and 3, 11 at Lv 20. */
	public static int maxMobs(int level) {
		return 1 + clamp(level) / 2;
	}

	/** Seconds of charge: max hearts / 10, at least 1 and at most 5 (hp / 20). */
	public static double chargeSeconds(double mobMaxHp) {
		return Math.max(1, Math.min(5, mobMaxHp / 20));
	}

	public static int chargeTicks(double mobMaxHp) {
		return (int) Math.round(chargeSeconds(mobMaxHp) * 20);
	}

	/** Hearts shown in the list: hp / 2 rounded up. */
	public static int hearts(double hp) {
		return (int) Math.ceil(hp / 2);
	}

	/** Lowest level that can hold a mob with this max health; 21 when even Lv 20 is not enough. */
	public static int levelNeeded(double mobMaxHp) {
		for (int l = 1; l <= MAX_LEVEL; l++) {
			if (mobMaxHp <= maxTameHealth(l)) {
				return l;
			}
		}
		return MAX_LEVEL + 1;
	}

	/** Eligibility: not tameable first, then too strong, then no free slot. */
	public static Check check(Facts facts, double mobMaxHp, int level, int held) {
		if (!facts.tameable()) {
			return new Check(Reason.NOT_TAMEABLE, 0);
		}
		if (mobMaxHp > maxTameHealth(level)) {
			return new Check(Reason.TOO_STRONG, levelNeeded(mobMaxHp));
		}
		if (held >= maxMobs(level)) {
			return new Check(Reason.FULL, 0);
		}
		return new Check(Reason.OK, 0);
	}

	/** The overlay line for a refusal (English). */
	public static String message(Check c, int level, int held) {
		return switch (c.reason()) {
			case OK -> "";
			case TOO_STRONG -> c.levelNeeded() > MAX_LEVEL ? "Too strong" : "Too strong (needs Lv " + c.levelNeeded() + ")";
			case FULL -> "You already hold " + held + " mob" + (held == 1 ? "" : "s");
			case NOT_TAMEABLE -> "This mob cannot be tamed";
		};
	}

	/** Damage of a mob that has no attack damage attribute: villager 3, anything else 2. */
	public static double fallbackDamage(boolean villager) {
		return villager ? VILLAGER_DAMAGE : PASSIVE_DAMAGE;
	}

	/** Scan radius for hostile monsters by mode. */
	public static double scanRadius(Mode mode) {
		return switch (mode) {
			case FOLLOW -> SCAN_FOLLOW;
			case STAY -> SCAN_STAY;
			case GUARD -> SCAN_GUARD;
		};
	}

	/** The mob's name: "Skeleton 7". */
	public static String label(String typeName, int number) {
		return typeName + " " + number;
	}
}
