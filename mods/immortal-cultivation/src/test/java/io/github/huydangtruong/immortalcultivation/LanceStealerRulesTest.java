package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.immortalcultivation.technique.LanceRules;
import io.github.huydangtruong.immortalcultivation.technique.StealerRules;
import org.junit.jupiter.api.Test;

class LanceStealerRulesTest {
	@Test
	void lanceBaseDamageGrowsWithLevel() {
		assertEquals(1.0, LanceRules.baseDamagePerSecond(1), 1e-9);
		assertEquals(1.5, LanceRules.baseDamagePerSecond(2), 1e-9);
	}

	@Test
	void lanceDoublesEveryThreeSeconds() {
		assertEquals(0, LanceRules.stage(0));
		assertEquals(0, LanceRules.stage(59));
		assertEquals(1, LanceRules.stage(60));
		assertEquals(3, LanceRules.stage(180));
		assertEquals(2 * LanceRules.damagePerSecond(1, 0), LanceRules.damagePerSecond(1, 60), 1e-9);
		assertEquals(0.5, LanceRules.damagePerHit(1, 0), 1e-9); // 1 per second, a hit every half second
	}

	@Test
	void lanceBar() {
		assertEquals(1f, LanceRules.barFraction(LanceRules.DURATION_TICKS), 1e-6);
		assertEquals(0f, LanceRules.barFraction(0), 1e-6);
	}

	@Test
	void stealerTakesPercentOfMaxHealthUpToCap() {
		assertEquals(5.4, StealerRules.amount(1, 15), 1e-9); // D181: 36% of 15
		assertEquals(7.0, StealerRules.amount(1, 20), 1e-9); // 7.2 capped at 7
		assertEquals(0.74, StealerRules.pct(20), 1e-9);
		assertEquals(7.0, StealerRules.amount(1, 200), 1e-9); // capped at 7
		assertEquals(0.0, StealerRules.amount(1, 0), 1e-9);
		assertEquals(7 + 3, StealerRules.cap(2), 1e-9);
	}

	@Test
	void stealerDebuffDurations() {
		assertEquals(2, StealerRules.BLEED_SECONDS);
		assertEquals(2, StealerRules.SLOW_SECONDS);
		assertEquals(0.8, StealerRules.SLOW, 1e-9);
	}

	@Test
	void stealerDrainsInTenPulsesOverTwoSeconds() { // D180
		assertEquals(10, StealerRules.PULSES);
		assertEquals(40, StealerRules.drainTicks()); // 2 s
		assertEquals(0.7, StealerRules.pulse(1, 20), 1e-9); // 7 / 10
		assertEquals(StealerRules.amount(5, 100), StealerRules.pulse(5, 100) * StealerRules.PULSES, 1e-9);
		assertEquals(0.0, StealerRules.liftProgress(0), 1e-9);
		assertEquals(1.0, StealerRules.liftProgress(StealerRules.LIFT_TICKS), 1e-9);
		assertEquals(1.0, StealerRules.liftProgress(99), 1e-9);
		assertEquals(2.0, StealerRules.LIFT_HEIGHT, 1e-9);
		assertEquals(StealerRules.BAT_FAR, StealerRules.batDistance(0, false), 1e-9);
		assertEquals(StealerRules.BAT_NEAR, StealerRules.batDistance(StealerRules.FLY_IN_TICKS, false), 1e-9);
		assertEquals(StealerRules.BAT_FAR, StealerRules.batDistance(StealerRules.FLY_OUT_TICKS, true), 1e-9);
	}

	@Test
	void theBeamBreaksABlockAfterSixTicksAndNeverAnUnbreakableOne() { // D161
		org.junit.jupiter.api.Assertions.assertFalse(LanceRules.breaksBlock(5));
		org.junit.jupiter.api.Assertions.assertTrue(LanceRules.breaksBlock(6));
		assertEquals(8, LanceRules.crackStage(5));
		assertEquals(0, LanceRules.crackStage(0));
		org.junit.jupiter.api.Assertions.assertTrue(LanceRules.breakable(0f));
		org.junit.jupiter.api.Assertions.assertTrue(LanceRules.breakable(1.5f));
		org.junit.jupiter.api.Assertions.assertFalse(LanceRules.breakable(-1f));
	}

	@org.junit.jupiter.api.Test
	void beamWidthGrowsSmoothlyToTwoAndAHalf() {
		assertEquals(1f, LanceRules.widthFactor(LanceRules.DURATION_TICKS), 1e-6);
		assertEquals(1.75f, LanceRules.widthFactor(LanceRules.DURATION_TICKS / 2), 1e-6);
		assertEquals(2.5f, LanceRules.widthFactor(0), 1e-6);
		org.junit.jupiter.api.Assertions.assertTrue(LanceRules.widthFactor(100) < LanceRules.widthFactor(99));
		assertEquals(2.5f, LanceRules.widthFactor(-5), 1e-6);
	}
}
