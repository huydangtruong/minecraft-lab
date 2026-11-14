package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
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
		assertEquals(1, TechniqueRules.slotsFor(319));
		assertEquals(2, TechniqueRules.slotsFor(320));
		assertEquals(3, TechniqueRules.slotsFor(832));
		assertEquals(4, TechniqueRules.slotsFor(1856));
		assertEquals(1856, TechniqueRules.MAX_EMERALDS);
		assertEquals(10, TechniqueRules.missingForNextSlot(54));
		assertEquals(256, TechniqueRules.nextSlotStep(100));
		assertEquals(0, TechniqueRules.missingForNextSlot(1856));
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
	void numbersAtLevels1_10_20() {
		assertArrayEquals(new double[] {16, 34, 44}, at(TechniqueRules::fireRange));
		assertArrayEquals(new double[] {4, 13, 18}, at(TechniqueRules::fireImpactDamage));
		assertEquals(1, TechniqueRules.fireBurnPerSecond(5));
		assertEquals(2, TechniqueRules.fireBurnPerSecond(6));
		assertArrayEquals(new double[] {0.20, 0.56, 0.86}, at(TechniqueRules::iceSlow), 1e-9);
		assertArrayEquals(new double[] {20, 56, 76}, at(TechniqueRules::healingTotal));
		assertArrayEquals(new double[] {10, 14.5, 17}, at(TechniqueRules::phaseSeconds), 1e-9);
		assertArrayEquals(new double[] {2, 6.5, 9}, at(TechniqueRules::repulseDamage), 1e-9);
		assertArrayEquals(new double[] {0.80, 0.50, 0.30}, at(TechniqueRules::sacrificeCost), 1e-9);
		assertArrayEquals(new double[] {5, 8, 10}, at(TechniqueRules::sacrificeSeconds), 1e-9);
		assertEquals(45, Technique.SACRIFICE.cooldownSeconds());
		assertEquals(8, TechniqueRules.phaseSightRange(1));
		assertEquals(10, TechniqueRules.phaseSightRange(2));
		assertEquals(27, TechniqueRules.phaseSightRange(10));
		assertEquals(48, TechniqueRules.phaseSightRange(20));
		assertArrayEquals(new double[] {20, 40, 60}, at(TechniqueRules::flightSeconds), 1e-9);
		assertArrayEquals(new double[] {0, 0.5, 1.0}, at(TechniqueRules::flightSpeedBonus), 1e-9);
		assertArrayEquals(new double[] {5, 9.5, 14.5}, at(TechniqueRules::bladeDamage), 1e-9);
		assertEquals(120, Technique.FLIGHT.cooldownSeconds());
		assertEquals(0, Technique.BLADE.cooldownSeconds());
		assertEquals(16, Technique.FIRE.cooldownSeconds());
		assertEquals(24, Technique.ICE.cooldownSeconds());
		assertEquals(60, Technique.HEALING.cooldownSeconds());
	}

	@Test
	void statLines() {
		assertEquals("16 blocks", TechniqueRules.stats(Technique.FIRE, 1).get(0).value());
		assertEquals("10 hearts", TechniqueRules.stats(Technique.HEALING, 1).get(1).value());
		assertEquals("2.9", TechniqueRules.num(2.9000001));
	}

	private static double[] at(java.util.function.IntToDoubleFunction f) {
		return new double[] {f.applyAsDouble(1), f.applyAsDouble(10), f.applyAsDouble(20)};
	}
}
