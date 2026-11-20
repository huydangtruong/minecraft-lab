package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.SeismRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import org.junit.jupiter.api.Test;

/** P-056 / D142: Seism numbers. */
class SeismRulesTest {
	@Test
	void impactHeartsGrowEveryEvenLevel() {
		assertEquals(5, SeismRules.impactHearts(1));
		assertEquals(6, SeismRules.impactHearts(2));
		assertEquals(6, SeismRules.impactHearts(3));
		assertEquals(7, SeismRules.impactHearts(4));
		assertEquals(15, SeismRules.impactHearts(20));
	}

	@Test
	void waveHeartsAndLift() {
		assertEquals(2, SeismRules.waveHearts(3));
		assertEquals(2, SeismRules.waveHearts(4));
		assertEquals(3, SeismRules.waveHearts(5));
		assertEquals(4, SeismRules.waveHearts(7));
		assertEquals(8, SeismRules.waveHearts(15));
		assertEquals(8, SeismRules.waveHearts(19));
		assertEquals(8, SeismRules.waveHearts(20));
		assertEquals(5, SeismRules.waveLift(3));
		assertEquals(5, SeismRules.waveLift(4));
		assertEquals(6, SeismRules.waveLift(5));
		assertEquals(7, SeismRules.waveLift(7));
		assertEquals(11, SeismRules.waveLift(15));
		assertEquals(13, SeismRules.waveLift(19));
		assertEquals(13, SeismRules.waveLift(20));
	}

	@Test
	void waveNeedsLevelThreeAndAFullHold() {
		assertFalse(SeismRules.waveCast(2, 1f));
		assertFalse(SeismRules.waveCast(3, 0.99f));
		assertTrue(SeismRules.waveCast(3, 1f));
		assertTrue(SeismRules.waveCast(20, 1f));
	}

	@Test
	void aTargetAboveTheCasterLosesHeartsAndLift() {
		assertEquals(0, SeismRules.heightAbove(64.9, 64.0));
		assertEquals(1, SeismRules.heightAbove(65.0, 64.0));
		assertEquals(-1, SeismRules.heightAbove(63.5, 64.0));
		assertEquals(2, SeismRules.heartsAt(3, 0));
		assertEquals(1, SeismRules.heartsAt(3, 1));
		assertEquals(0, SeismRules.heartsAt(3, 2));
		assertEquals(0, SeismRules.heartsAt(3, 9));
		assertEquals(2, SeismRules.heartsAt(3, -2), "below the caster: no change");
		assertEquals(4, SeismRules.liftAt(3, 1));
		assertEquals(0, SeismRules.liftAt(3, 5));
		assertEquals(0, SeismRules.liftAt(3, 20));
		assertEquals(12, SeismRules.liftAt(20, 1));
	}

	@Test
	void liftVelocityReachesAboutTheTargetHeight() {
		for (int blocks : new int[] {1, 5, 6, 7, 11, 13}) {
			double v = SeismRules.liftVelocity(blocks);
			assertEquals(blocks, SeismRules.heightReached(v), 1e-6, "height for " + blocks);
		}
		assertEquals(0, SeismRules.liftVelocity(0));
		assertTrue(SeismRules.liftVelocity(13) > SeismRules.liftVelocity(5));
	}

	@Test
	void constantsAndRegistration() {
		assertEquals(48, SeismRules.AIM_RANGE);
		assertEquals(10, SeismRules.FALL_TICKS);
		assertEquals(3, SeismRules.WAVE_RADII[0]);
		assertEquals(4, SeismRules.WAVE_RADII[1]);
		assertEquals(5, SeismRules.WAVE_RADII[2]);
		assertEquals(5, SeismRules.waveRadius(9) , "clamped to the last pulse");
		assertEquals(28, Technique.SEISM.cooldownSeconds());
		assertEquals(28, TechniqueRules.cooldownSeconds(Technique.SEISM, 1));
		assertEquals(28, TechniqueRules.cooldownSeconds(Technique.SEISM, 20));
		assertEquals("seism", Technique.SEISM.id());
		assertEquals("Seism", Technique.SEISM.displayName());
		assertTrue(Technique.SEISM.charged());
		assertEquals(1f, TechniqueRules.fullChargeSeconds(Technique.SEISM));
		assertTrue(Technique.THUNDER.ordinal() + 1 == Technique.SEISM.ordinal(), "Seism follows Thunder Strike in the attack group");
	}

	@Test
	void boulderGrowsWhileItFalls() {
		assertEquals(0.25f, SeismRules.boulderScale(SeismRules.fallProgress(0)), 1e-6);
		assertEquals(1f, SeismRules.boulderScale(SeismRules.fallProgress(10)), 1e-6);
		assertEquals(1f, SeismRules.boulderScale(SeismRules.fallProgress(15)), 1e-6);
		assertTrue(SeismRules.knockbackSpeed(2) > 0);
	}

	@Test
	void statsListHasBothModes() {
		var s = TechniqueRules.stats(Technique.SEISM, 3);
		assertTrue(s.size() >= 4);
		assertEquals("28 s", s.get(s.size() - 1).value());
	}
}
