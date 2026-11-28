package io.github.huydangtruong.immortalcultivation.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RuneRulesTest {
	@Test
	void levelNGivesTwoToTheNMinusOne() {
		assertEquals(1, RuneRules.bonus(1));
		assertEquals(2, RuneRules.bonus(2)); // D292: ceil(1.5^(n-1))
		assertEquals(3, RuneRules.bonus(3));
		assertEquals(4, RuneRules.bonus(4));
		assertEquals(6, RuneRules.bonus(5));
		assertEquals(26, RuneRules.bonus(9));
		assertEquals(195, RuneRules.bonus(14));
		assertEquals(255, RuneRules.bonus(15)); // 292 capped
		for (int n = 1; n < 15; n++) {
			org.junit.jupiter.api.Assertions.assertTrue(RuneRules.bonus(n + 1) > RuneRules.bonus(n), "level " + (n + 1) + " stronger");
		}
		assertEquals(255, RuneRules.bonus(20));
	}

	@Test
	void totalIsCappedAt255() {
		assertEquals(9, RuneRules.total(5, 4));
		assertEquals(255, RuneRules.total(200, 200));
	}

	@Test
	void inscribingCostsFiveLevelsPerRuneLevel() {
		assertEquals(5, RuneRules.inscribeCost(1));
		assertEquals(25, RuneRules.inscribeCost(5));
	}

	@Test
	void slotsByMaterial() {
		assertEquals(4, RuneRules.slots("netherite_sword"));
		assertEquals(4, RuneRules.slots("mace"));
		assertEquals(4, RuneRules.slots("trident"));
		assertEquals(2, RuneRules.slots("diamond_chestplate"));
		assertEquals(2, RuneRules.slots("bow"));
		assertEquals(2, RuneRules.slots("crossbow"));
		assertEquals(1, RuneRules.slots("iron_pickaxe"));
		assertEquals(1, RuneRules.slots("fishing_rod"));
		assertEquals(0, RuneRules.slots("golden_sword"));
		assertEquals(0, RuneRules.slots("stone_axe"));
	}

	@Test
	void romanNumerals() {
		assertEquals("III", RuneRules.roman(3));
		assertEquals("11", RuneRules.roman(11));
	}
}
