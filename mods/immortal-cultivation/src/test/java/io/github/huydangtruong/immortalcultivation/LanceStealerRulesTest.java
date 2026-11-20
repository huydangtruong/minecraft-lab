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
		assertEquals(4.0, StealerRules.amount(1, 20), 1e-9); // 20% of 20
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
}
