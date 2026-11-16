package io.github.huydangtruong.permahearts.technique;

import java.util.List;

/**
 * Pure rules for techniques (D43-D45): XP costs, refunds, emerald slot thresholds, per-level numbers and the skill
 * key cycle. No Minecraft imports, JUnit-tested.
 */
public final class TechniqueRules {
	public static final int MAX_LEVEL = 20;
	public static final int MAX_SLOTS = 4;
	/** Emeralds for slot 1, 2, 3, 4 (each on top of the previous). */
	public static final int[] SLOT_STEPS = {64, 128, 256, 512}; // D77: was 64, 256, 512, 1024
	public static final int MAX_EMERALDS = 64 + 128 + 256 + 512;
	/** Seconds a charged throw needs to reach full power (like a bow). */
	public static final float FULL_CHARGE_SECONDS = 1f;

	/** D79: Cosmic Warping places a portal only after right click is held this long (no portal from a misclick). */
	public static final float WARP_PLACE_SECONDS = 1f;

	/** Seconds of holding right click to reach full charge: Thousand Mile Step takes 3 times as long as the orbs (D58). */
	public static float fullChargeSeconds(Technique t) {
		return switch (t) {
			case TELEPORT -> 3 * FULL_CHARGE_SECONDS;
			case WARP -> WARP_PLACE_SECONDS;
			default -> FULL_CHARGE_SECONDS;
		};
	}

	/** D79: true when a Cosmic Warping charge is full, so the portal may be placed. */
	public static boolean warpPlaceReady(float charge) {
		return charge >= 1f;
	}

	private TechniqueRules() {}

	// ---- XP ----

	/** Experience levels the upgrade to toLevel costs: round(5.8 + 0.4 * toLevel) = 6, 7, 7 ... 14. */
	public static int upgradeCost(int toLevel) {
		if (toLevel < 1 || toLevel > MAX_LEVEL) {
			throw new IllegalArgumentException("level " + toLevel);
		}
		return (63 + 4 * toLevel) / 10;
	}

	/** Levels spent in total to go from 0 to level (80 for 10, 200 for 20). */
	public static int totalCost(int level) {
		int sum = 0;
		for (int l = 1; l <= level; l++) {
			sum += upgradeCost(l);
		}
		return sum;
	}

	/** Vanilla Player#getXpNeededForNextLevel for a level. */
	public static int xpNeededForNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
	}

	/** Experience points a player at this level and progress (0..1) holds. */
	public static int points(int level, float progress) {
		int sum = 0;
		for (int l = 0; l < level; l++) {
			sum += xpNeededForNextLevel(l);
		}
		return sum + Math.round(progress * xpNeededForNextLevel(level));
	}

	/** Reset gives back half of the points spent on the technique. */
	public static int refund(int spentPoints) {
		return Math.max(0, spentPoints) / 2;
	}

	// ---- slots ----

	/** Slots open for this many emeralds deposited in total (0..4). */
	public static int slotsFor(int deposited) {
		int slots = 0;
		int need = 0;
		for (int step : SLOT_STEPS) {
			need += step;
			if (deposited >= need) {
				slots++;
			}
		}
		return slots;
	}

	/** Emeralds still missing for the next slot (0 when all 4 are open). */
	public static int missingForNextSlot(int deposited) {
		int need = 0;
		for (int step : SLOT_STEPS) {
			need += step;
			if (deposited < need) {
				return need - deposited;
			}
		}
		return 0;
	}

	/** Emeralds the next slot costs in all (its step), 0 when all are open. */
	public static int nextSlotStep(int deposited) {
		int slots = slotsFor(deposited);
		return slots >= MAX_SLOTS ? 0 : SLOT_STEPS[slots];
	}

	// ---- skill key ----

	/**
	 * Next selection when the skill key is tapped: -1 is the empty (✕) selection, 0..3 a slot. Goes ✕ → each filled slot
	 * in order → ✕, skipping empty slots.
	 */
	public static int nextSelection(int current, boolean[] filled) {
		for (int i = current + 1; i < filled.length; i++) {
			if (filled[i]) {
				return i;
			}
		}
		return -1;
	}

	// ---- numbers per level ----

	/** base at level 1, + step per level up to 10, + step / 2 per level after 10 (D45). */
	public static double scaled(double base, double step, int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return base + step * (Math.min(l, 10) - 1) + step / 2 * Math.max(0, l - 10);
	}

	public static double fireRange(int level) {
		return scaled(24, 2, level); // D72: was 16 at level 1
	}

	public static double fireImpactDamage(int level) {
		return scaled(4, 1, level);
	}

	public static double fireFieldRadius(int level) {
		return scaled(3, 0.2, level); // D72: was 2 + 0.1 per level
	}

	public static double fireFieldSeconds(int level) {
		return scaled(4, 0.2, level);
	}

	/** D94: burn damage per second while on fire because of the technique: 2 at level 1, 5 at level 20, linear (no D45 halving). */
	public static double fireBurnPerSecond(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return 2 + 3.0 * (l - 1) / (MAX_LEVEL - 1);
	}

	/** D94: seconds an entity keeps burning after it leaves the field: 3 at level 1, +0.2 per level (6.8 at level 20). */
	public static double fireAfterburnSeconds(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return 3 + 0.2 * (l - 1);
	}

	public static double iceRange(int level) {
		return scaled(24, 2, level); // D72: was 16 at level 1
	}

	public static double iceImpactDamage(int level) {
		return 2;
	}

	public static double iceRadius(int level) {
		return scaled(3, 0.2, level);
	}

	public static double iceAreaSeconds(int level) {
		return 8; // D54: was 6
	}

	/** Move speed taken away inside the ice area (D54): 40% at level 1 up to 96% at 20. */
	public static double iceSlow(int level) {
		return linear(level, 0.40, 0.96); // D54: 40% at level 1, 96% at 20
	}

	/** Attack speed taken away inside the ice area. */
	public static double iceAttackSlow(int level) {
		return linear(level, 0.40, 0.96); // D54: same as the move slow
	}

	/** Frostbite lasts this long after leaving the area. */
	public static double iceLingerSeconds(int level) {
		return 4; // D54: was 3
	}

	public static double healingRadius(int level) {
		return 4;
	}

	public static double healingSeconds(int level) {
		return 10;
	}

	/** Health (half hearts) healed over the circle's whole duration (D80): 12 hearts at level 1 up to 48 at level 20, straight line. */
	public static double healingTotal(int level) {
		return linear(level, 24, 96);
	}

	public static double phaseSeconds(int level) {
		return scaled(10, 0.5, level);
	}

	/** How far a phasing player sees through blocks (D51): 8 blocks at level 1 up to 48 at level 20, whole blocks. */
	public static int phaseSightRange(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return (int) Math.round(8 + 40.0 * (l - 1) / (MAX_LEVEL - 1));
	}

	/** Distance where the in-rock fog of Phase Walk starts: 60% of the sight range. */
	public static float phaseFogStart(int level) {
		return phaseSightRange(level) * 0.6f;
	}

	public static double repulseRadius(int level) {
		return scaled(4, 0.2, level);
	}

	/** Knockback strength (horizontal push, a little upward too). */
	public static double repulsePush(int level) {
		return scaled(1.5, 0.25, level); // D72: was 1.0 + 0.15
	}

	/** D99: seconds a non-player stays stunned after Heaven Blast: 1.0 at level 1, +0.1 per level (2.9 at level 20). */
	public static double repulseStunSeconds(int level) {
		return 1.0 + 0.1 * (Math.max(1, Math.min(MAX_LEVEL, level)) - 1);
	}

	public static double repulseDamage(int level) {
		return scaled(5, 0.5, level); // D72: was 2 at level 1
	}

	/** Share of the caster's current health given up (D46): 80% at level 1, 50% at 10, 30% at 20. */
	public static double sacrificeCost(int level) {
		return piecewise(level, 0.80, 0.50, 0.30);
	}

	/** Seconds of invulnerability (D72): 8 at level 1, +0.5 per level, 17.5 at 20. D46 had 5 / 8 / 10. */
	public static double sacrificeSeconds(int level) {
		return 8 + 0.5 * (Math.max(1, Math.min(MAX_LEVEL, level)) - 1);
	}

	/** A straight line from (1, at1) to (20, at20). */
	public static double linear(int level, double at1, double at20) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return at1 + (at20 - at1) * (l - 1) / (MAX_LEVEL - 1.0);
	}

	/** Straight lines through (1, at1), (10, at10), (20, at20). */
	public static double piecewise(int level, double at1, double at10, double at20) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return l <= 10 ? at1 + (at10 - at1) * (l - 1) / 9.0 : at10 + (at20 - at10) * (l - 10) / 10.0;
	}

	/** Seconds of creative-like flight: 15 at level 1 up to 40 at 20 (D72; D54 had 10 to 30). */
	public static double flightSeconds(int level) {
		return linear(level, 15, 40);
	}

	/** Flight speed on top of the creative flying speed (D54): -20% (80% of creative) at level 1 up to +100% at 20. */
	public static double flightSpeedBonus(int level) {
		return linear(level, -0.20, 1.0); // D54: 80% of creative at level 1, 200% at 20
	}

	/** Life Steal Blade damage on top of the player's base attack (D52, D60): 8 at level 1, +0.5 per level (17.5 at 20). */
	public static double bladeDamage(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return 8 + 0.5 * (l - 1);
	}

	/** Blocks a Life Steal Blade hit throws the target back on flat ground (D60): 2 at level 1 up to 6 at 20. */
	public static double bladeKnockback(int level) {
		return linear(level, 2, 6);
	}

	/** Seconds the Life Steal Blade makes the target bleed (D60), 1 damage per second. */
	public static final int BLEED_SECONDS = 4;

	/** Violet Guardian (D60): golems summoned, 1 at levels 1-9, 2 at 10-14, 3 at 15-19, 4 at 20. */
	public static int golemCount(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return l >= 20 ? 4 : l >= 15 ? 3 : l >= 10 ? 2 : 1;
	}

	/** Multiple of a vanilla iron golem's health and damage (D60): 0.5 at level 1 up to 2.0 at 20. */
	public static double golemPower(int level) {
		return linear(level, 0.5, 2.0);
	}

	/** Vanilla iron golem health and attack damage (checked with javap: 100 and 15). */
	public static final double GOLEM_BASE_HEALTH = 100, GOLEM_BASE_DAMAGE = 15;

	public static double golemHealth(int level) {
		return GOLEM_BASE_HEALTH * golemPower(level);
	}

	public static double golemDamage(int level) {
		return GOLEM_BASE_DAMAGE * golemPower(level);
	}

	/** Seconds the guardians stay (D60). */
	public static final int GOLEM_SECONDS = 90;

	/** Warrior Angel (D63): angels summoned, 2 at levels 1-4, 3 at 5-9, 4 at 10-14, 5 at 15-19, 6 at 20. */
	public static int angelCount(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return l >= 20 ? 6 : l >= 15 ? 5 : l >= 10 ? 4 : l >= 5 ? 3 : 2;
	}

	/** Angel health (D72): 16 at level 1 up to 30 at 20. D63 used a multiple of a vanilla vex's 14. */
	public static double angelHealth(int level) {
		return linear(level, 16, 30);
	}

	/** Angel attack damage (D72): 4 at level 1, +1 per level, 23 at 20. */
	public static double angelDamage(int level) {
		return 4 + (Math.max(1, Math.min(MAX_LEVEL, level)) - 1);
	}

	/** Seconds the angels stay (D63). */
	public static final int ANGEL_SECONDS = 60;

	// ---- Illusion (D71) ----

	/** Clones created: 1 at levels 1-9, 2 at 10-19, 3 at 20. */
	public static int illusionCount(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return l >= 20 ? 3 : l >= 10 ? 2 : 1;
	}

	/** The share of the owner's damage, health and armour a clone has: 20% at level 1 up to 40% at 20. */
	public static double illusionFactor(int level) {
		return linear(level, 0.20, 0.40);
	}

	/** Seconds the clones stay (D73): 30 at level 1 up to 50 at 20. */
	public static double illusionSeconds(int level) {
		return linear(level, 30, 50);
	}

	/** Work area radius in blocks around the cast point (D73): 8 at level 1, +4 every 5 levels, 24 at 20 (D109). */
	public static double illusionWorkRadius(int level) {
		return 8 + 4 * (Math.max(1, Math.min(20, level)) / 5);
	}

	/** Ticks between two work actions (D73): 20 at level 1 down to 8 at 20. A block still takes its break time on top. */
	public static int illusionWorkTicks(int level) {
		return (int) Math.round(linear(level, 20, 8));
	}

	/** Stacks a working clone carries. */
	public static final int ILLUSION_CARRY_STACKS = 9;

	/** The chest search reaches this many blocks beyond the work area. */
	public static final int ILLUSION_CHEST_EXTRA = 8;

	/** Blocks within which a working clone fights hostile mobs near itself or its owner. */
	public static final double ILLUSION_WORK_FIGHT_RANGE = 6;

	/** Ticks between two clone hits, from the owner's attack speed attribute (4 = bare hand, 1.6 = sword): a player's cooldown rhythm. */
	public static int illusionAttackTicks(double attackSpeed) {
		return (int) Math.max(4, Math.round(20 / Math.max(0.1, attackSpeed)));
	}

	/** Ticks until a clone's mood changes: 2 to 5 s. {@code r} is a random number in [0, 1). */
	public static int illusionMoodTicks(double r) {
		return 40 + (int) Math.floor(Math.max(0, Math.min(0.999999, r)) * 61);
	}

	/** Chance (0..1) that a new fighting mood is "back off": 20%, or 55% below 40% health. */
	public static double illusionBackOffChance(double healthFraction) {
		return healthFraction < 0.4 ? 0.55 : 0.2;
	}

	// ---- Aqua Marine (D64) ----

	/** Seconds Aqua Marine lasts: 30 at level 1 up to 70 at level 20 (rounded to ticks where it is used, like flightSeconds). */
	public static double aquaSeconds(int level) {
		return linear(level, 30, 70);
	}

	/** Underwater mining speed attribute value: 1.0 (Aqua Affinity) at level 1, 2.0 at level 20. Vanilla base is 0.2. */
	public static double aquaMiningSpeed(int level) {
		return linear(level, 1.0, 2.0);
	}

	/** Vanilla submerged mining speed attribute without Aqua Affinity (Attributes.SUBMERGED_MINING_SPEED default). */
	public static final double SUBMERGED_MINING_BASE = 0.2;

	// Vanilla swimming physics (LivingEntity.travelInWater): each tick v = (v + accel) * drag and the player moves by
	// v + accel, so the steady distance per tick is accel / (1 - drag). Plain swimming drag is 0.8 (0.9 while
	// sprinting); Dolphin's Grace sets it to 0.96.
	public static final double WATER_ACCEL = 0.02;
	public static final double DRAG_PLAIN = 0.8;
	public static final double DRAG_SPRINT = 0.9;
	public static final double DRAG_DOLPHIN = 0.96;
	/** Walking speed attribute and the sprint bonus (+30%): the base of the water movement efficiency blend. */
	public static final double WALK_SPEED = 0.1;
	public static final double SPRINT_FACTOR = 1.3;

	/** Steady distance per tick (blocks) with this acceleration and drag. */
	public static double steadySpeed(double accel, double drag) {
		return accel / (1 - drag);
	}

	/** Steady swim speed in vanilla: plain or sprint-swimming, without or with Dolphin's Grace. */
	public static double swimSpeed(boolean sprinting, boolean dolphin) {
		return steadySpeed(WATER_ACCEL, dolphin ? DRAG_DOLPHIN : sprinting ? DRAG_SPRINT : DRAG_PLAIN);
	}

	/** Steady swim speed at level 20 (D69), blocks per tick: sprint-swimming and plain swimming. */
	public static final double AQUA_SPRINT_MAX = 1.2;
	public static final double AQUA_SWIM_MAX = 1.0;

	/** The steady speed Aqua Marine aims for: Dolphin's Grace speed (0.5) at level 1, AQUA_SPRINT_MAX / AQUA_SWIM_MAX at 20. */
	public static double aquaTargetSpeed(int level, boolean sprinting) {
		return linear(level, swimSpeed(sprinting, true), sprinting ? AQUA_SPRINT_MAX : AQUA_SWIM_MAX);
	}

	/**
	 * Value to add to the WATER_MOVEMENT_EFFICIENCY attribute (on top of the real Dolphin's Grace effect, which fixes
	 * the drag at 0.96) so that the steady swim speed reaches aquaTargetSpeed. Vanilla acceleration in water is
	 * 0.02 + (movementSpeed - 0.02) * efficiency, and the efficiency is halved off the ground (open water). 0 at level 1.
	 */
	public static double aquaWaterEfficiency(int level, boolean sprinting, boolean onGround) {
		double accel = aquaTargetSpeed(level, sprinting) * (1 - DRAG_DOLPHIN);
		double speed = WALK_SPEED * (sprinting ? SPRINT_FACTOR : 1);
		return Math.max(0, (accel - WATER_ACCEL) / ((speed - WATER_ACCEL) * (onGround ? 1 : 0.5)));
	}

	/** Unused since the blade got a 12 s cooldown (D52); kept for reference. */
	public static final double BLADE_ATTACK_SPEED = 1.6;

	/** Vanilla creative flying speed (Abilities default); the flight speed bonus is applied on top of it. */
	public static final float CREATIVE_FLYING_SPEED = 0.05f;

	/** Abilities flying speed while Flight is on. */
	public static float flyingSpeed(int level) {
		return (float) (CREATIVE_FLYING_SPEED * (1 + flightSpeedBonus(level)));
	}

	/** Slow falling after Flight ends, in ticks (5 s). */
	public static final int FLIGHT_SLOW_FALL_TICKS = 100;

	/** About how far a vanilla ender pearl flies on flat ground at full strength (simulated: 1.5 speed, 0.03 gravity). */
	public static final double PEARL_RANGE = 50;

	/** Thousand Mile Step range as a multiple of an ender pearl's (D58): 3x at level 1 up to 50x at level 20. */
	public static double teleportPearls(int level) {
		return linear(level, 3, 50);
	}

	/** Thousand Mile Step distance in blocks at full charge: 150 at level 1, 2500 at level 20. */
	public static double teleportRange(int level) {
		return PEARL_RANGE * teleportPearls(level);
	}

	// ---- Thunder Strike (D86) ----

	/** Reach of the aim ray, in blocks. */
	public static final double THUNDER_RANGE = 128;
	/** Ticks between the cast (warning starts) and the strike. */
	public static final int THUNDER_WARN_TICKS = 20;

	/** Damage at the centre of the blast: 14 at level 1, +2 per level (52 at level 20). Half of it at the edge. */
	public static double thunderDamage(int level) {
		return 14 + 2 * (level - 1);
	}

	/** Explosion power: 3 (a creeper) at level 1, +5% of that per level (5.85 at level 20). */
	public static double thunderPower(int level) {
		return 3.0 * (1 + 0.05 * (level - 1));
	}

	/** Blocks from the centre inside which entities are hurt, and the radius of the warning ring. */
	public static double thunderRadius(int level) {
		return thunderPower(level) * 1.2;
	}

	/** Damage at a distance from the centre: full at 0, half at the radius, none beyond it. */
	public static double thunderDamageAt(int level, double distance) {
		double r = thunderRadius(level);
		if (distance > r) {
			return 0;
		}
		return thunderDamage(level) * (1 - 0.5 * Math.max(0, distance) / r);
	}

	// ---- Cosmic Warping (D75) ----

	/** Portals one player may own: 2 at level 1 up to 20 at level 20, a straight line rounded down. */
	public static int warpMaxPortals(int level) {
		int l = Math.max(1, Math.min(MAX_LEVEL, level));
		return 2 + (l - 1) * 18 / 19;
	}

	/** Seconds of cooldown after a warp (D80, D81): 90 at level 1 down to 40 at level 20, straight line, rounded. */
	public static int warpCooldownSeconds(int level) {
		return (int) Math.round(linear(level, 90, 40));
	}

	/** Cooldown of a technique at a level in seconds: the enum value, except Cosmic Warping which falls with the level. */
	public static int cooldownSeconds(Technique t, int level) {
		if (t == Technique.WARP) return warpCooldownSeconds(level);
		if (t == Technique.TELEPORT) return teleportCooldownSeconds(level); // D121
		return t.cooldownSeconds();
	}

	/** D121: Quantum Leap cooldown, 40 s at Lv 1, 1 s less per level (Lv 20 = 21 s). */
	public static int teleportCooldownSeconds(int level) {
		return 40 - (Math.max(1, level) - 1);
	}

	public static int cooldownTicks(Technique t, int level) {
		return cooldownSeconds(t, level) * 20;
	}

	/** One line of the detail page: label and the value at a level, e.g. ("Range", "16 blocks"). */
	public record Stat(String label, String value) {}

	/** The numbers the Techniques tab shows for a technique at a level (level >= 1). */
	public static List<Stat> stats(Technique t, int level) {
		return switch (t) {
			case FIRE -> List.of(
				new Stat("Range", num(fireRange(level)) + " blocks"),
				new Stat("Impact", num(fireImpactDamage(level)) + " damage"),
				new Stat("Fire field", num(fireFieldRadius(level)) + " radius, " + num(fireFieldSeconds(level)) + " s"),
				new Stat("Burn", num(fireBurnPerSecond(level)) + " / s"),
				new Stat("Afterburn", num(fireAfterburnSeconds(level)) + " s"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case ICE -> List.of(
				new Stat("Range", num(iceRange(level)) + " blocks"),
				new Stat("Area", num(iceRadius(level)) + " radius, " + num(iceAreaSeconds(level)) + " s"),
				new Stat("Slow", "-" + pct(iceSlow(level)) + " speed"),
				new Stat("Frostbite", "-" + pct(iceAttackSlow(level)) + " attack speed"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case HEALING -> List.of(
				new Stat("Circle", num(healingRadius(level)) + " radius, " + num(healingSeconds(level)) + " s"),
				new Stat("Heals", num(healingTotal(level) / 2) + " hearts"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case PHASE -> List.of(
				new Stat("Duration", num(phaseSeconds(level)) + " s"),
				new Stat("Sight", phaseSightRange(level) + " blocks through walls"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case REPULSE -> List.of(
				new Stat("Radius", num(repulseRadius(level)) + " blocks"),
				new Stat("Push", num(repulsePush(level))),
				new Stat("Damage", num(repulseDamage(level))),
				new Stat("Stun", String.format(java.util.Locale.ROOT, "%.1f s", repulseStunSeconds(level))),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case FLIGHT -> List.of(
				new Stat("Flight", num(flightSeconds(level)) + " s"),
				new Stat("Speed", pct(1 + flightSpeedBonus(level)) + " of creative"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case BLADE -> List.of(
				new Stat("Damage", num(bladeDamage(level))), // D57: just the number
				new Stat("Bleeding", BLEED_SECONDS + " s"),
				new Stat("Knockback", num(bladeKnockback(level)) + " blocks"),
				new Stat("Life steal", "all damage dealt"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case TELEPORT -> List.of(
				new Stat("Range", num(teleportRange(level)) + " blocks"),
				new Stat("Ender pearls", num(teleportPearls(level)) + "x"),
				new Stat("Charge", num(fullChargeSeconds(t)) + " s"),
				new Stat("Cooldown", cooldownSeconds(t, level) + " s"));
			case GOLEM -> List.of(
				new Stat("Guardians", String.valueOf(golemCount(level))),
				new Stat("Health", num(golemHealth(level))),
				new Stat("Damage", num(golemDamage(level))),
				new Stat("Duration", GOLEM_SECONDS + " s"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case ANGEL -> List.of(
				new Stat("Angels", String.valueOf(angelCount(level))),
				new Stat("Health", num(angelHealth(level))),
				new Stat("Damage", num(angelDamage(level))),
				new Stat("Lasts", ANGEL_SECONDS + " s"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case ILLUSION -> List.of(
				new Stat("Clones", String.valueOf(illusionCount(level))),
				new Stat("Damage", pct(illusionFactor(level))),
				new Stat("Health", pct(illusionFactor(level))),
				new Stat("Lasts", num(illusionSeconds(level)) + " s"),
				new Stat("Work area", num(illusionWorkRadius(level)) + " blocks"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case AQUA -> List.of(
				new Stat("Lasts", num(aquaSeconds(level)) + " s"),
				new Stat("Swim speed", num(aquaTargetSpeed(level, false) * 20) + " blocks/s"),
				new Stat("Sprint swim", num(aquaTargetSpeed(level, true) * 20) + " blocks/s"),
				new Stat("Underwater mining", "\u00d7" + num(aquaMiningSpeed(level)) + " aqua affinity"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case WARP -> List.of(
				new Stat("Portals", String.valueOf(warpMaxPortals(level))),
				new Stat("Cooldown", warpCooldownSeconds(level) + " s"));
			case THUNDER -> List.of(
				new Stat("Damage", num(thunderDamage(level))),
				new Stat("Blast size", pct(thunderPower(level) / 3.0) + " of a creeper"),
				new Stat("Range", num(THUNDER_RANGE) + " blocks"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
			case SACRIFICE -> List.of(
				new Stat("Costs", pct(sacrificeCost(level)) + " of current health"),
				new Stat("Invulnerable", num(sacrificeSeconds(level)) + " s"),
				new Stat("Cooldown", t.cooldownSeconds() + " s"));
		};
	}

	/** "16", "2.9", "0.25" (no trailing zeros). */
	public static String num(double v) {
		double r = Math.round(v * 100) / 100.0;
		if (r == Math.rint(r)) {
			return String.valueOf((long) r);
		}
		String s = String.valueOf(r);
		return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
	}

	private static String pct(double fraction) {
		return Math.round(fraction * 100) + "%";
	}
}
