package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionJob;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import org.junit.jupiter.api.Test;

class TechniqueRulesTest {
	@Test
	void xpCostsMatchHuysTotals() {
		assertEquals(6, TechniqueRules.upgradeCost(1));
		assertEquals(10, TechniqueRules.upgradeCost(10));
		assertEquals(14, TechniqueRules.upgradeCost(20));
		assertEquals(80, TechniqueRules.totalCost(10));
		assertEquals(200, TechniqueRules.totalCost(20));
		for (int l = 2; l <= 20; l++) {
			assertEquals(true, TechniqueRules.upgradeCost(l) >= TechniqueRules.upgradeCost(l - 1), "never cheaper at level " + l);
		}
	}

	@Test
	void vanillaXpPoints() {
		assertEquals(0, TechniqueRules.points(0, 0f));
		assertEquals(7, TechniqueRules.points(1, 0f));
		assertEquals(352, TechniqueRules.points(16, 0f)); // vanilla: 16 levels = 352 points
		assertEquals(1395, TechniqueRules.points(30, 0f)); // vanilla: 30 levels = 1395 points
		assertEquals(50, TechniqueRules.refund(101));
	}

	@Test
	void emeraldSlots() {
		assertEquals(0, TechniqueRules.slotsFor(63));
		assertEquals(1, TechniqueRules.slotsFor(64));
		assertEquals(1, TechniqueRules.slotsFor(191));
		assertEquals(2, TechniqueRules.slotsFor(192));
		assertEquals(3, TechniqueRules.slotsFor(448));
		assertEquals(4, TechniqueRules.slotsFor(960));
		assertEquals(960, TechniqueRules.MAX_EMERALDS);
		assertEquals(10, TechniqueRules.missingForNextSlot(54));
		assertEquals(128, TechniqueRules.nextSlotStep(100));
		assertEquals(0, TechniqueRules.missingForNextSlot(960));
	}

	@Test
	void skillKeyCycleSkipsEmptySlots() {
		boolean[] filled = {true, false, true, false};
		assertEquals(0, TechniqueRules.nextSelection(-1, filled));
		assertEquals(2, TechniqueRules.nextSelection(0, filled));
		assertEquals(-1, TechniqueRules.nextSelection(2, filled));
		assertEquals(-1, TechniqueRules.nextSelection(-1, new boolean[] {false, false, false, false}));
	}

	@Test
	void heavenBlastStatsShowTheStun() {
		assertEquals("Stun", TechniqueRules.stats(Technique.REPULSE, 1).get(3).label());
		assertEquals("1.0 s", TechniqueRules.stats(Technique.REPULSE, 1).get(3).value());
		assertEquals("2.9 s", TechniqueRules.stats(Technique.REPULSE, 20).get(3).value());
	}

	@Test
	void numbersAtLevels1_10_20() {
		assertArrayEquals(new double[] {24, 42, 52}, at(TechniqueRules::fireRange)); // D72
		assertArrayEquals(new double[] {24, 42, 52}, at(TechniqueRules::iceRange));
		assertArrayEquals(new double[] {3, 4.8, 5.8}, at(TechniqueRules::fireFieldRadius), 1e-9);
		assertArrayEquals(new double[] {2, 2 + 3.0 * 9 / 19, 5}, at(TechniqueRules::fireBurnPerSecond), 1e-9); // D94
		assertArrayEquals(new double[] {3, 4.8, 6.8}, at(TechniqueRules::fireAfterburnSeconds), 1e-9);
		assertArrayEquals(new double[] {1.5, 3.75, 5.0}, at(TechniqueRules::repulsePush), 1e-9);
		assertArrayEquals(new double[] {4, 13, 18}, at(TechniqueRules::fireImpactDamage));
		assertArrayEquals(new double[] {0.40, 0.40 + 0.56 * 9 / 19, 0.96}, at(TechniqueRules::iceSlow), 1e-9);
		assertArrayEquals(new double[] {0.40, 0.40 + 0.56 * 9 / 19, 0.96}, at(TechniqueRules::iceAttackSlow), 1e-9);
		assertEquals(8, TechniqueRules.iceAreaSeconds(5));
		assertEquals(4, TechniqueRules.iceLingerSeconds(5));
		assertArrayEquals(new double[] {24, 24 + 72.0 * 9 / 19, 96}, at(TechniqueRules::healingTotal)); // D80: 12 -> 48 hearts
		assertArrayEquals(new double[] {10, 14.5, 17}, at(TechniqueRules::phaseSeconds), 1e-9);
		assertArrayEquals(new double[] {5, 9.5, 12}, at(TechniqueRules::repulseDamage), 1e-9);
		assertArrayEquals(new double[] {1.0, 1.9, 2.9}, at(TechniqueRules::repulseStunSeconds), 1e-9); // D99
		assertArrayEquals(new double[] {0.80, 0.50, 0.30}, at(TechniqueRules::sacrificeCost), 1e-9);
		assertArrayEquals(new double[] {8, 12.5, 17.5}, at(TechniqueRules::sacrificeSeconds), 1e-9);
		assertEquals(45, Technique.SACRIFICE.cooldownSeconds());
		assertEquals(16, TechniqueRules.phaseSightRange(1));
		assertEquals(19, TechniqueRules.phaseSightRange(2));
		assertEquals(39, TechniqueRules.phaseSightRange(10));
		assertEquals(64, TechniqueRules.phaseSightRange(20));
		assertArrayEquals(new double[] {15, 15 + 25.0 * 9 / 19, 40}, at(TechniqueRules::flightSeconds), 1e-9);
		assertArrayEquals(new double[] {-0.20, -0.20 + 1.2 * 9 / 19, 1.0}, at(TechniqueRules::flightSpeedBonus), 1e-9);
		assertArrayEquals(new double[] {8, 12.5, 17.5}, at(TechniqueRules::bladeDamage), 1e-9);
		assertEquals(0.04f, TechniqueRules.flyingSpeed(1), 1e-6f); // 80% of creative (0.05)
		assertEquals(0.1f, TechniqueRules.flyingSpeed(20), 1e-6f); // 200% of creative
		assertArrayEquals(new double[] {150, 50 * (3 + 47.0 * 9 / 19), 2500}, at(TechniqueRules::teleportRange), 1e-9);
		assertEquals(40, TechniqueRules.cooldownSeconds(Technique.TELEPORT, 1)); // D121
		assertEquals(21, TechniqueRules.cooldownSeconds(Technique.TELEPORT, 20));
		assertEquals(3 * TechniqueRules.fullChargeSeconds(Technique.FIRE), TechniqueRules.fullChargeSeconds(Technique.TELEPORT), 1e-9);
		assertEquals(3f, TechniqueRules.fullChargeSeconds(Technique.TELEPORT), 1e-9);
		assertEquals("3 s", TechniqueRules.stats(Technique.TELEPORT, 1).get(2).value());
		assertEquals(0.5f, io.github.huydangtruong.immortalcultivation.technique.SkillBarLogic.charge(30, 3f), 1e-6);
		assertEquals(80, Technique.FLIGHT.cooldownSeconds());
		assertEquals(12, Technique.BLADE.cooldownSeconds());
		assertArrayEquals(new double[] {2, 2 + 4 * 9 / 19.0, 6}, at(TechniqueRules::bladeKnockback), 1e-9);
		assertEquals(120, Technique.GOLEM.cooldownSeconds());
		assertEquals(1, TechniqueRules.golemCount(1));
		assertEquals(1, TechniqueRules.golemCount(9));
		assertEquals(2, TechniqueRules.golemCount(10));
		assertEquals(2, TechniqueRules.golemCount(14));
		assertEquals(3, TechniqueRules.golemCount(15));
		assertEquals(3, TechniqueRules.golemCount(19));
		assertEquals(4, TechniqueRules.golemCount(20));
		assertEquals(0.5, TechniqueRules.golemPower(1), 1e-9);
		assertEquals(2.0, TechniqueRules.golemPower(20), 1e-9);
		assertEquals(50, TechniqueRules.golemHealth(1), 1e-9);
		assertEquals(30, TechniqueRules.golemDamage(20), 1e-9);
		assertEquals("4", TechniqueRules.stats(Technique.GOLEM, 20).get(0).value());
		assertEquals(120, Technique.ANGEL.cooldownSeconds());
		assertEquals("Warrior Angel", Technique.ANGEL.displayName());
		int[][] angels = {{1, 2}, {4, 2}, {5, 3}, {9, 3}, {10, 4}, {14, 4}, {15, 5}, {19, 5}, {20, 6}};
		for (int[] k : angels) {
			assertEquals(k[1], TechniqueRules.angelCount(k[0]), "angels at level " + k[0]);
		}
		assertEquals(16, TechniqueRules.angelHealth(1), 1e-9); // D72
		assertEquals(30, TechniqueRules.angelHealth(20), 1e-9);
		assertEquals(4, TechniqueRules.angelDamage(1), 1e-9);
		assertEquals(13, TechniqueRules.angelDamage(10), 1e-9);
		assertEquals(23, TechniqueRules.angelDamage(20), 1e-9);
		assertEquals("8.5 s", TechniqueRules.stats(Technique.SACRIFICE, 2).get(1).value());
		assertEquals(60, TechniqueRules.ANGEL_SECONDS);
		assertEquals("6", TechniqueRules.stats(Technique.ANGEL, 20).get(0).value());
		assertEquals("60 s", TechniqueRules.stats(Technique.ANGEL, 1).get(3).value());
		assertEquals(100, Technique.AQUA.cooldownSeconds());
		assertEquals("Aqua Marine", Technique.AQUA.displayName());
		assertEquals("aqua", Technique.AQUA.id());
		assertArrayEquals(new double[] {30, 30 + 40.0 * 9 / 19, 70}, at(TechniqueRules::aquaSeconds), 1e-9);
		assertArrayEquals(new double[] {1.0, 1.0 + 9 / 19.0, 2.0}, at(TechniqueRules::aquaMiningSpeed), 1e-9);
		// swim physics: level 1 = Dolphin's Grace (no extra efficiency), level 20 = 1.2 sprinting / 1.0 not (D69)
		for (boolean sprint : new boolean[] {false, true}) {
			double dolphin = TechniqueRules.swimSpeed(sprint, true);
			assertEquals(dolphin, TechniqueRules.aquaTargetSpeed(1, sprint), 1e-9);
			assertEquals(sprint ? 1.2 : 1.0, TechniqueRules.aquaTargetSpeed(20, sprint), 1e-9);
			assertEquals(dolphin + ((sprint ? 1.2 : 1.0) - dolphin) * 9 / 19, TechniqueRules.aquaTargetSpeed(10, sprint), 1e-9);
			for (boolean ground : new boolean[] {false, true}) {
				assertEquals(0, TechniqueRules.aquaWaterEfficiency(1, sprint, ground), 1e-9);
				assertTrue(TechniqueRules.aquaWaterEfficiency(20, sprint, ground) > TechniqueRules.aquaWaterEfficiency(10, sprint, ground));
				assertTrue(TechniqueRules.aquaWaterEfficiency(20, sprint, ground) < 1);
			}
		}
		assertEquals(0.5, TechniqueRules.swimSpeed(false, true), 1e-9);
		assertEquals(0.2, TechniqueRules.swimSpeed(true, false), 1e-9);
		assertEquals("30 s", TechniqueRules.stats(Technique.AQUA, 1).get(0).value());
		assertEquals("70 s", TechniqueRules.stats(Technique.AQUA, 20).get(0).value());
		assertEquals("10 blocks/s", TechniqueRules.stats(Technique.AQUA, 1).get(1).value());
		assertEquals("10 blocks/s", TechniqueRules.stats(Technique.AQUA, 1).get(2).value());
		assertEquals("20 blocks/s", TechniqueRules.stats(Technique.AQUA, 20).get(1).value());
		assertEquals("24 blocks/s", TechniqueRules.stats(Technique.AQUA, 20).get(2).value());
		assertEquals("\u00d72 aqua affinity", TechniqueRules.stats(Technique.AQUA, 20).get(3).value());
		assertEquals(16, Technique.FIRE.cooldownSeconds());
		assertEquals(18, Technique.ICE.cooldownSeconds());
		assertEquals(40, Technique.HEALING.cooldownSeconds());
		assertEquals(60, Technique.PHASE.cooldownSeconds());
		assertEquals("Healing Domain", Technique.HEALING.displayName());
	}

	@Test
	void illusionNumbers() {
		assertEquals("Illusion", Technique.ILLUSION.displayName());
		assertEquals(110, Technique.ILLUSION.cooldownSeconds());
		int[][] clones = {{1, 1}, {9, 1}, {10, 2}, {19, 2}, {20, 3}};
		for (int[] k : clones) {
			assertEquals(k[1], TechniqueRules.illusionCount(k[0]), "clones at level " + k[0]);
		}
		assertEquals(0.20, TechniqueRules.illusionFactor(1), 1e-9);
		assertEquals(0.40, TechniqueRules.illusionFactor(20), 1e-9);
		assertEquals(30, TechniqueRules.illusionSeconds(1), 1e-9);
		assertEquals(50, TechniqueRules.illusionSeconds(20), 1e-9);
		assertEquals(8, TechniqueRules.illusionWorkRadius(1), 1e-9);
		assertEquals(8, TechniqueRules.illusionWorkRadius(4), 1e-9);
		assertEquals(12, TechniqueRules.illusionWorkRadius(5), 1e-9);
		assertEquals(24, TechniqueRules.illusionWorkRadius(20), 1e-9);
		assertEquals(20, TechniqueRules.illusionWorkTicks(1));
		assertEquals(8, TechniqueRules.illusionWorkTicks(20));
		assertTrue(TechniqueRules.illusionWorkTicks(10) < 20 && TechniqueRules.illusionWorkTicks(10) > 8);
		assertEquals(9, TechniqueRules.ILLUSION_CARRY_STACKS);
		// a player's hit rhythm: bare hand 4 attack speed = 5 ticks, sword 1.6 = 13 ticks (about), axe 0.8 = 25
		assertEquals(5, TechniqueRules.illusionAttackTicks(4.0));
		assertEquals(13, TechniqueRules.illusionAttackTicks(1.6));
		assertEquals(25, TechniqueRules.illusionAttackTicks(0.8));
		assertEquals(4, TechniqueRules.illusionAttackTicks(100));
		assertEquals(40, TechniqueRules.illusionMoodTicks(0.0)); // 2 s
		assertEquals(100, TechniqueRules.illusionMoodTicks(0.999999)); // 5 s
		assertTrue(TechniqueRules.illusionBackOffChance(0.2) > TechniqueRules.illusionBackOffChance(0.9));
		assertEquals("3", TechniqueRules.stats(Technique.ILLUSION, 20).get(0).value());
		assertEquals("50 s", TechniqueRules.stats(Technique.ILLUSION, 20).get(3).value());
		assertEquals("Work area", TechniqueRules.stats(Technique.ILLUSION, 20).get(4).label());
		assertEquals("24 blocks", TechniqueRules.stats(Technique.ILLUSION, 20).get(4).value());
	}

	@Test
	void illusionJobFromItemId() {
		String[][] rows = {
			{"minecraft:wooden_hoe", "FARM"}, {"minecraft:netherite_hoe", "FARM"}, {"minecraft:diamond_hoe", "FARM"},
			{"minecraft:fishing_rod", "FISH"},
			{"minecraft:iron_axe", "CHOP"}, {"minecraft:stone_axe", "CHOP"},
			{"minecraft:shears", "SHEAR"},
			{"minecraft:bucket", "MILK"},
			{"minecraft:wheat", "BREED"}, {"minecraft:carrot", "BREED"}, {"minecraft:potato", "BREED"},
			{"minecraft:diamond_sword", "FIGHT"}, {"minecraft:diamond_pickaxe", "FIGHT"}, {"minecraft:iron_shovel", "FIGHT"},
			{"minecraft:wheat_seeds", "FIGHT"}, {"minecraft:water_bucket", "FIGHT"}, {"minecraft:air", "FIGHT"},
			{"minecraft:bow", "FIGHT"}, {"other:shiny_axe", "FIGHT"}, {"", "FIGHT"}};
		for (String[] r : rows) {
			assertEquals(IllusionJob.valueOf(r[1]), IllusionJob.fromItemId(r[0]), r[0]);
		}
		assertEquals(IllusionJob.FIGHT, IllusionJob.fromItemId(null));
		assertFalse(IllusionJob.FIGHT.works());
		assertTrue(IllusionJob.BREED.works());
	}

	@Test
	void statLines() {
		assertEquals("24 blocks", TechniqueRules.stats(Technique.FIRE, 1).get(0).value());
		assertEquals("12 hearts", TechniqueRules.stats(Technique.HEALING, 1).get(1).value());
		assertEquals("2.9", TechniqueRules.num(2.9000001));
	}

	private static double[] at(java.util.function.IntToDoubleFunction f) {
		return new double[] {f.applyAsDouble(1), f.applyAsDouble(10), f.applyAsDouble(20)};
	}
}
