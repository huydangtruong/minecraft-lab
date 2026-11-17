package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerRules;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerRules.Mode;
import io.github.huydangtruong.immortalcultivation.reward.RewardTier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GodSlayerRulesTest {
	@Test
	void scaleAndStatsByLevel() {
		assertEquals(1.0, GodSlayerRules.scale(1), 1e-9);
		assertEquals(1.4, GodSlayerRules.scale(3), 1e-9);
		assertEquals(2.8, GodSlayerRules.scale(10), 1e-9);
		assertEquals(500f, GodSlayerRules.maxHealth(1), 1e-3);
		assertEquals(700f, GodSlayerRules.maxHealth(3), 1e-3);
		assertEquals(30f, GodSlayerRules.slashDamage(1), 1e-3);
		assertEquals(42f, GodSlayerRules.slashDamage(3), 1e-3);
	}

	@Test
	void levelIsClamped() {
		assertEquals(1, GodSlayerRules.clampLevel(-5));
		assertEquals(10, GodSlayerRules.clampLevel(99));
		assertEquals(GodSlayerRules.scale(1), GodSlayerRules.scale(0), 1e-9);
		assertEquals(GodSlayerRules.scale(10), GodSlayerRules.scale(50), 1e-9);
	}

	@Test
	void damagePerHitIsClamped() {
		assertEquals(40f, GodSlayerRules.clampDamage(1000f));
		assertEquals(12f, GodSlayerRules.clampDamage(12f));
	}

	@Test
	void fiveBoxesWithEqualOdds() {
		Random rng = new Random(7);
		Map<RewardTier, Integer> counts = new EnumMap<>(RewardTier.class);
		for (int i = 0; i < 4000; i++) {
			List<RewardTier> boxes = GodSlayerRules.rewardBoxes(rng);
			assertEquals(5, boxes.size());
			for (RewardTier t : boxes) {
				counts.merge(t, 1, Integer::sum);
			}
		}
		for (RewardTier t : RewardTier.values()) {
			double share = counts.get(t) / 20000.0;
			assertTrue(share > 0.22 && share < 0.28, t + " share " + share);
		}
	}

	@Test
	void teleportRules() {
		assertFalse(GodSlayerRules.shouldTeleport(10, true, 0));
		assertFalse(GodSlayerRules.shouldTeleport(64, true, 159));
		assertFalse(GodSlayerRules.shouldTeleport(50, true, 0));
		assertTrue(GodSlayerRules.shouldTeleport(64.5, true, 0));
		assertTrue(GodSlayerRules.shouldTeleport(5, true, 160));
		assertTrue(GodSlayerRules.shouldTeleport(5, false, 0));
	}

	@Test
	void modeChoice() {
		assertEquals(Mode.WATER, GodSlayerRules.chooseMode(true, 10, 10, 0, Mode.GROUND));
		assertEquals(Mode.AIR, GodSlayerRules.chooseMode(false, 5, 5, 0, Mode.GROUND));
		assertEquals(Mode.GROUND, GodSlayerRules.chooseMode(false, 0, 1, 0, Mode.GROUND));
		assertEquals(Mode.AIR, GodSlayerRules.chooseMode(false, 0, 3, 50, Mode.GROUND));
		assertEquals(Mode.AIR, GodSlayerRules.chooseMode(false, 2, 1, 0, Mode.AIR));
		assertEquals(Mode.GROUND, GodSlayerRules.chooseMode(false, 0.2, 0, 0, Mode.AIR));
	}

	@Test
	void regenWhenUnreachableOrAlone() {
		assertFalse(GodSlayerRules.regenActive(10, 0, false));
		assertTrue(GodSlayerRules.regenActive(10, 50, false));
		assertFalse(GodSlayerRules.regenActive(10, 50, true));
		assertTrue(GodSlayerRules.regenActive(600, 0, true));
		assertEquals(10f, GodSlayerRules.regenAmount(500f), 1e-3);
	}

	@Test
	void huntVisitAndHearts() {
		assertFalse(GodSlayerRules.isHuntVisit(99, 4));
		assertTrue(GodSlayerRules.isHuntVisit(100, 4));
		assertFalse(GodSlayerRules.isHuntVisit(100, 5));
		assertTrue(GodSlayerRules.isHuntVisit(300, 8));
		assertFalse(GodSlayerRules.isHuntVisit(300, 0));
		assertTrue(GodSlayerRules.heartsEnough(30));
		assertFalse(GodSlayerRules.heartsEnough(29));
	}

	@Test
	void frostAndCountdown() {
		assertEquals(5, GodSlayerRules.frostPercent(50));
		assertEquals(1, GodSlayerRules.frostPercent(3));
		assertEquals(2, GodSlayerRules.stunTicks(20)); // D99: a tenth of Heaven Blast's stun
		assertEquals(6, GodSlayerRules.stunTicks(58));
		assertEquals(1, GodSlayerRules.stunTicks(3));
		assertEquals(60, GodSlayerRules.secondsLeft(1200));
		assertEquals(1, GodSlayerRules.secondsLeft(1));
		assertEquals(0, GodSlayerRules.secondsLeft(0));
		assertEquals(1.5, GodSlayerRules.WATER_SPEED, 1e-9);
	}

	@Test
	void warningPulseAndEdge() {
		assertEquals(1f, GodSlayerRules.edgePulse(100), 1e-6);
		assertTrue(GodSlayerRules.edgePulse(90) < 1f && GodSlayerRules.edgePulse(90) >= 0.85f);
		assertEquals(GodSlayerRules.edgePulse(100), GodSlayerRules.edgePulse(40), 1e-6);
		assertEquals(0f, GodSlayerRules.edgeStrength(0), 1e-6);
		assertTrue(GodSlayerRules.edgeStrength(1200) < GodSlayerRules.edgeStrength(600));
		assertTrue(GodSlayerRules.edgeStrength(600) < GodSlayerRules.edgeStrength(20));
		assertTrue(GodSlayerRules.edgeStrength(1) <= 1f);
		// the edge fades smoothly from the rim to nothing and is never more opaque than 0.35
		float prev = Float.MAX_VALUE;
		for (int i = 0; i < 16; i++) {
			float a = GodSlayerRules.edgeLayerAlpha(1f, 1f, i, 16);
			assertTrue(a <= GodSlayerRules.EDGE_MAX_ALPHA && a < prev);
			prev = a;
		}
		assertEquals(GodSlayerRules.EDGE_MAX_ALPHA, GodSlayerRules.edgeLayerAlpha(1f, 1f, 0, 16), 1e-6);
		assertEquals(0f, GodSlayerRules.edgeLayerAlpha(1f, 1f, 16, 16), 1e-6);
	}
}
