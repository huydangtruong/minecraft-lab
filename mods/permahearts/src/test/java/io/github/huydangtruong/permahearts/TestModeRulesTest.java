package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** D92: the /testmode full table. */
class TestModeRulesTest {
	@Test
	void fourArmorPiecesAndFiveTools() {
		assertEquals(9, TestModeRules.GEAR.size());
		assertEquals(4, TestModeRules.GEAR.stream().filter(TestModeRules.Gear::armor).count());
	}

	@Test
	void everyPieceHasTwoEnchantsAtTheirMax() {
		for (TestModeRules.Gear g : TestModeRules.GEAR) {
			assertEquals(2, g.enchants().size(), g.item());
		}
		assertEquals(new TestModeRules.Ench("sharpness", 5), TestModeRules.GEAR.get(4).enchants().get(0));
		assertEquals(new TestModeRules.Ench("fire_aspect", 2), TestModeRules.GEAR.get(4).enchants().get(1));
	}

	@Test
	void targets() {
		assertEquals(50, TestModeRules.HEARTS);
		assertEquals(10, TestModeRules.ELIXIRS_EACH);
	}
}
