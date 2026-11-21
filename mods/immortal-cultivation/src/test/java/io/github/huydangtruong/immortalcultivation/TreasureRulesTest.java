package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.reward.RewardOption;
import io.github.huydangtruong.immortalcultivation.treasure.TreasureRules;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TreasureRulesTest {
	@Test
	void lootHasTheBottleTheApplesTheCarrotsAndEightOthers() {
		Random rng = new Random(3);
		for (int i = 0; i < 500; i++) {
			List<RewardOption> loot = TreasureRules.loot(rng);
			assertEquals("permahearts:enlightenment_bottle", loot.get(0).itemId());
			assertEquals("minecraft:enchanted_golden_apple", loot.get(1).itemId());
			assertEquals(2, loot.get(1).count()); // D166
			assertEquals("minecraft:golden_carrot", loot.get(2).itemId()); // D172
			assertEquals(32, loot.get(2).count());
			assertEquals(11, loot.size(), "3 fixed + 8 random (D172)");
			assertEquals(loot.size(), new HashSet<>(loot).size(), "no repeats");
			assertFalse(loot.stream().anyMatch(o -> o.itemId().equals("minecraft:netherite_ingot")));
		}
	}

	@Test
	void groupsMatchTheSpec() {
		assertEquals(6, TreasureRules.group("brutes").size());
		assertEquals(3.0, TreasureRules.group("brutes").guards().get(0).mult());
		assertEquals(4.0, TreasureRules.group("blazes").guards().get(0).mult());
		assertEquals(4.0, TreasureRules.group("wither_skeletons").guards().get(0).mult());
		assertEquals(4.0, TreasureRules.group("breezes").guards().get(0).mult());
		assertEquals(3, TreasureRules.GROUPS.stream().filter(TreasureRules.Group::water).count());
		Random rng = new Random(1);
		for (int i = 0; i < 100; i++) {
			assertTrue(TreasureRules.pickGroup(true, rng).water());
			assertFalse(TreasureRules.pickGroup(false, rng).water());
		}
	}

	@Test
	void treasureTakesTheNextCrimsonMasterDay() { // D172
		assertEquals(12 * 24000L, TreasureRules.slotStart(12));
		assertFalse(TreasureRules.crimsonSkips(TreasureRules.Phase.IDLE));
		assertTrue(TreasureRules.crimsonSkips(TreasureRules.Phase.PENDING));
		assertTrue(TreasureRules.crimsonSkips(TreasureRules.Phase.ACTIVE));
	}
}
