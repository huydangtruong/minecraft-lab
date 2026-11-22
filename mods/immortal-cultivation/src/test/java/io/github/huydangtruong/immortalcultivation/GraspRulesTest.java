package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules.Facts;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules.Reason;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import org.junit.jupiter.api.Test;

class GraspRulesTest {
	private static final Facts OK = new Facts(false, false, false, false, false, false, false, false, false);

	@Test
	void healthCapAndBonus() {
		assertEquals(20, GraspRules.maxTameHealth(1));
		assertEquals(115, GraspRules.maxTameHealth(20));
		assertEquals(5, GraspRules.bonusHealth(1));
		assertEquals(43, GraspRules.bonusHealth(20));
	}

	@Test
	void slots() {
		assertEquals(1, GraspRules.maxMobs(1));
		assertEquals(2, GraspRules.maxMobs(2));
		assertEquals(2, GraspRules.maxMobs(3));
		assertEquals(10, GraspRules.maxMobs(19));
		assertEquals(11, GraspRules.maxMobs(20));
	}

	@Test
	void chargeSeconds() {
		assertEquals(1, GraspRules.chargeSeconds(10));
		assertEquals(1, GraspRules.chargeSeconds(20));
		assertEquals(2, GraspRules.chargeSeconds(40));
		assertEquals(5, GraspRules.chargeSeconds(100));
		assertEquals(5, GraspRules.chargeSeconds(300));
		assertEquals(20, GraspRules.chargeTicks(20));
		assertEquals(100, GraspRules.chargeTicks(300));
	}

	@Test
	void heartsRoundUp() {
		assertEquals(10, GraspRules.hearts(20));
		assertEquals(13, GraspRules.hearts(25));
		assertEquals(1, GraspRules.hearts(0.5));
	}

	@Test
	void eligibility() {
		assertEquals(Reason.OK, GraspRules.check(OK, 20, 1, 0).reason());
		assertEquals(Reason.FULL, GraspRules.check(OK, 20, 1, 1).reason());
		var strong = GraspRules.check(OK, 40, 1, 0);
		assertEquals(Reason.TOO_STRONG, strong.reason());
		assertEquals(5, strong.levelNeeded()); // 20 + 5 * 4 = 40
		assertEquals(Reason.TOO_STRONG, GraspRules.check(OK, 116, 20, 0).reason());
		assertEquals(21, GraspRules.check(OK, 116, 20, 0).levelNeeded());
		assertEquals(Reason.OK, GraspRules.check(OK, 115, 20, 10).reason());
		assertEquals(Reason.FULL, GraspRules.check(OK, 115, 20, 11).reason());
		// not tameable wins over the others
		assertEquals(Reason.NOT_TAMEABLE, GraspRules.check(new Facts(true, false, false, false, false, false, false, false, false), 400, 1, 5).reason());
	}

	@Test
	void everyNotTameableFlag() {
		assertTrue(OK.tameable());
		assertFalse(new Facts(false, true, false, false, false, false, false, false, false).tameable());
		assertFalse(new Facts(false, false, true, false, false, false, false, false, false).tameable());
		assertFalse(new Facts(false, false, false, true, false, false, false, false, false).tameable());
		assertFalse(new Facts(false, false, false, false, true, false, false, false, false).tameable());
		assertFalse(new Facts(false, false, false, false, false, true, false, false, false).tameable());
		assertFalse(new Facts(false, false, false, false, false, false, true, false, false).tameable());
		assertFalse(new Facts(false, false, false, false, false, false, false, true, false).tameable());
		assertFalse(new Facts(false, false, false, false, false, false, false, false, true).tameable());
	}

	@Test
	void messages() {
		assertEquals("Too strong (needs Lv 5)", GraspRules.message(GraspRules.check(OK, 40, 1, 0), 1, 0));
		assertEquals("You already hold 3 mobs", GraspRules.message(GraspRules.check(OK, 20, 5, 3), 5, 3));
		assertEquals("This mob cannot be tamed", GraspRules.message(new GraspRules.Check(Reason.NOT_TAMEABLE, 0), 1, 0));
	}

	@Test
	void damageAndModes() {
		assertEquals(3, GraspRules.fallbackDamage(true));
		assertEquals(2, GraspRules.fallbackDamage(false));
		assertEquals(GraspRules.Mode.GUARD, GraspRules.Mode.of(2));
		assertEquals(GraspRules.Mode.FOLLOW, GraspRules.Mode.of(9));
		assertEquals("Skeleton 7", GraspRules.label("Skeleton", 7));
	}

	@Test
	void technique() {
		assertEquals("grasp", Technique.GRASP.id());
		assertEquals("Soul Grasp", Technique.GRASP.displayName());
		assertEquals(120, TechniqueRules.cooldownSeconds(Technique.GRASP, 1));
		assertEquals(120, TechniqueRules.cooldownSeconds(Technique.GRASP, 20));
		assertEquals("11", TechniqueRules.stats(Technique.GRASP, 20).get(2).value());
	}

	@Test
	void followSpotsAreTwoAndAHalfToThreeAndAHalfBlocksOut() { // D165
		for (int seed : new int[] {0, 1, 7, -3, 123456789, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
			double[] d = GraspRules.followSpot(seed);
			double r = Math.hypot(d[0], d[1]);
			assertTrue(r >= GraspRules.FOLLOW_RING_MIN - 1e-9 && r <= GraspRules.FOLLOW_RING_MAX + 1e-9, "seed " + seed + " r " + r);
		}
		double[] a = GraspRules.followSpot(10), b = GraspRules.followSpot(12);
		assertTrue(Math.hypot(a[0] - b[0], a[1] - b[1]) > 1, "neighbouring seeds get different spots");
	}
}
