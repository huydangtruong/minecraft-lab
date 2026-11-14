package io.github.huydangtruong.permahearts.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrbBallisticsTest {
	@Test
	void speedReachesTheRange() {
		for (double range : new double[] {16, 22, 34, 50}) {
			double v = OrbBallistics.speedForRange(range, 1.62);
			assertEquals(range, OrbBallistics.flatRange(v, 1.62), 0.01, "range " + range);
		}
	}

	@Test
	void fasterGoesFarther() {
		assertTrue(OrbBallistics.flatRange(2.0, 1.62) > OrbBallistics.flatRange(1.5, 1.62));
	}

	@Test
	void chargeScalesTheRangeWithAFloor() {
		assertEquals(16, OrbBallistics.chargedRange(16, 1f), 1e-9);
		assertEquals(8, OrbBallistics.chargedRange(16, 0.5f), 1e-9);
		assertEquals(3.2, OrbBallistics.chargedRange(16, 0f), 1e-6);
		assertEquals(16, OrbBallistics.chargedRange(16, 3f), 1e-9);
	}
}
