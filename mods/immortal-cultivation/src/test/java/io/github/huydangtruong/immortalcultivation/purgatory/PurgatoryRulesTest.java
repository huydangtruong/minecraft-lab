package io.github.huydangtruong.immortalcultivation.purgatory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PurgatoryRulesTest {
	@Test
	void everyFloorIsHarderThanTheOneBefore() {
		for (int f = 2; f <= 1000; f++) {
			assertTrue(PurgatoryRules.healthMultiplier(f) > PurgatoryRules.healthMultiplier(f - 1), "health " + f);
			assertTrue(PurgatoryRules.damageMultiplier(f) > PurgatoryRules.damageMultiplier(f - 1), "damage " + f);
			assertTrue(PurgatoryRules.armorBonus(f) >= PurgatoryRules.armorBonus(f - 1));
			assertTrue(PurgatoryRules.speedBonus(f) >= PurgatoryRules.speedBonus(f - 1));
			assertTrue(PurgatoryRules.eliteChance(f) >= PurgatoryRules.eliteChance(f - 1));
			assertTrue(PurgatoryRules.mobPool(f).containsAll(PurgatoryRules.mobPool(f - 1)), "pool " + f);
		}
	}

	@Test
	void floorOneIsPlain() {
		assertEquals(1.0, PurgatoryRules.healthMultiplier(1));
		assertEquals(1.0, PurgatoryRules.damageMultiplier(1));
		assertEquals(0, PurgatoryRules.eliteChance(9));
		assertTrue(PurgatoryRules.eliteChance(10) > 0);
	}

	@Test
	void rewardIsThirtyPlusFloorLevelsAndFloorRunes() {
		assertEquals(31, PurgatoryRules.rewardLevels(1));
		assertEquals(57, PurgatoryRules.rewardLevels(27));
		assertEquals(30, PurgatoryRules.rewardRunes(27, 1));
		assertEquals(4, PurgatoryRules.rewardRunes(1, 1));
		assertEquals(17, PurgatoryRules.rewardRunes(5, 4));
	}

	@Test
	void rewardRunesRollOneToFive() {
		Random rng = new Random(7);
		boolean[] seen = new boolean[6];
		for (int i = 0; i < 2000; i++) {
			int l = PurgatoryRules.rollRuneLevel(rng);
			assertTrue(l >= 1 && l <= 5);
			seen[l] = true;
		}
		for (int l = 1; l <= 5; l++) {
			assertTrue(seen[l], "level " + l);
		}
	}

	@Test
	void themeChangesEveryFiveFloors() {
		assertEquals(PurgatoryRules.Theme.OPEN, PurgatoryRules.Theme.of(1));
		assertEquals(PurgatoryRules.Theme.OPEN, PurgatoryRules.Theme.of(5));
		assertEquals(PurgatoryRules.Theme.PILLARS, PurgatoryRules.Theme.of(6));
		assertEquals(PurgatoryRules.Theme.OPEN, PurgatoryRules.Theme.of(31));
	}

	@Test
	void teamHealthGrowsWithPlayers() {
		assertEquals(1.0, PurgatoryRules.teamHealthFactor(1));
		assertEquals(2.0, PurgatoryRules.teamHealthFactor(3));
	}

	@Test
	void clockShowsMinutesAndSeconds() {
		assertEquals("6:00", PurgatoryRules.clock(PurgatoryRules.RUN_TICKS));
		assertEquals("5:00", PurgatoryRules.clock(PurgatoryRules.COOLDOWN_TICKS));
		assertEquals("0:01", PurgatoryRules.clock(1));
		assertEquals("0:00", PurgatoryRules.clock(0));
	}
}
