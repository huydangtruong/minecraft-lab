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
		assertEquals(2.5, PurgatoryRules.healthMultiplier(10), 1e-9);
		assertEquals(2.0, PurgatoryRules.damageMultiplier(10), 1e-9);
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
		assertEquals(68, PurgatoryRules.chestRunes(5, 4)); // D271
		assertEquals(4, PurgatoryRules.chestRunes(1, 1));
		assertEquals(0, PurgatoryRules.xpForLevels(0));
		assertEquals(7, PurgatoryRules.xpForLevels(1));
		assertEquals(352, PurgatoryRules.xpForLevels(16)); // vanilla table
		assertEquals(1395, PurgatoryRules.xpForLevels(30));
		assertEquals(1507, PurgatoryRules.xpForLevels(31));
		assertEquals(2 * 1507, PurgatoryRules.rewardXp(1, 2));
	}

	/** D264: reward rune levels grow with the floor: up to 3 below 20, 4, 5 from 30, 6 from 50, 7 from 70, 8 from 100. */
	@Test
	void rewardRuneLevelsGrowWithTheFloor() {
		int[][] cases = {{1, 3}, {19, 3}, {20, 4}, {29, 4}, {30, 5}, {49, 5}, {50, 6}, {69, 6}, {70, 7}, {99, 7}, {100, 8}, {500, 8}};
		for (int[] c : cases) {
			assertEquals(c[1], PurgatoryRules.maxRuneLevel(c[0]), "floor " + c[0]);
			Random rng = new Random(7);
			boolean[] seen = new boolean[10];
			for (int i = 0; i < 2000; i++) {
				int l = PurgatoryRules.rollRuneLevel(rng, c[0]);
				assertTrue(l >= 1 && l <= c[1]);
				seen[l] = true;
			}
			for (int l = 1; l <= c[1]; l++) {
				assertTrue(seen[l], "floor " + c[0] + " level " + l);
			}
		}
	}

	@Test
	void runeStonesHaveAColourByKind() {
		assertEquals("attack", io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind("sharpness"));
		assertEquals("armor", io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind("protection"));
		assertEquals("ranged", io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind("power"));
		assertEquals("ranged", io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind("throw_power")); // D277
		assertEquals("tool", io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind("efficiency"));
		assertEquals("other", io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind("something_new"));
	}

	@Test
	void noCreepersEver() {
		assertTrue(!PurgatoryRules.mobPool(100).contains("creeper"));
	}

	@Test
	void teamHealthGrowsWithPlayers() {
		assertEquals(1.0, PurgatoryRules.teamHealthFactor(1));
		assertEquals(2.0, PurgatoryRules.teamHealthFactor(3));
	}

	@Test
	void clockShowsMinutesAndSeconds() {
		assertEquals("10:00", PurgatoryRules.clock(PurgatoryRules.RUN_TICKS));
		assertEquals("3:00", PurgatoryRules.clock(PurgatoryRules.COOLDOWN_TICKS));
		assertEquals("0:01", PurgatoryRules.clock(1));
		assertEquals("0:00", PurgatoryRules.clock(0));
	}
}
