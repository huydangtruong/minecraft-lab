package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.permahearts.wanderer.GhastLeash;
import org.junit.jupiter.api.Test;

class GhastLeashTest {
	@Test
	void insideStaysFree() {
		assertFalse(GhastLeash.outOfBounds(10, 80, 10, 0, 0, 64)); // 14 blocks out, 16 above
	}

	@Test
	void tooFarFromTheMaster() {
		assertTrue(GhastLeash.outOfBounds(25, 70, 0, 0, 0, 64));
	}

	@Test
	void sixteenAboveIsTheLimit() {
		assertFalse(GhastLeash.outOfBounds(0, 80, 0, 0, 0, 64));
		assertTrue(GhastLeash.outOfBounds(0, 80.5, 0, 0, 0, 64));
	}

	@Test
	void returnsOverThePlayer() {
		double[] p = GhastLeash.returnPoint(5, 5, 64, 0, 0, 10, 1, -1);
		assertEquals(6, p[0], 1e-9);
		assertEquals(74, p[1], 1e-9);
		assertEquals(4, p[2], 1e-9);
	}

	@Test
	void playerOutsideTheRadiusPullsToTheEdgeInside() {
		double[] p = GhastLeash.returnPoint(100, 0, 64, 0, 0, 20, 0, 0);
		assertEquals(GhastLeash.RADIUS - 4, Math.hypot(p[0], p[2]), 1e-9);
		assertEquals(64 + GhastLeash.HOVER_MAX, p[1], 1e-9); // hover clamped to 8..12
		assertFalse(GhastLeash.outOfBounds(p[0], p[1], p[2], 0, 0, 64));
	}

	@Test
	void shootsTwentyPercentFaster() {
		int charge = 20; // ticks from the warning to the shot, vanilla
		double vanilla = charge + GhastLeash.VANILLA_COOLDOWN;
		double boss = charge + GhastLeash.BOSS_COOLDOWN;
		assertEquals(1.2, vanilla / boss, 1e-9);
	}
}
