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
	void lootHasTheBottleTheAppleAndSixToEightOthers() {
		Random rng = new Random(3);
		for (int i = 0; i < 500; i++) {
			List<RewardOption> loot = TreasureRules.loot(rng);
			assertEquals("permahearts:enlightenment_bottle", loot.get(0).itemId());
			assertEquals("minecraft:enchanted_golden_apple", loot.get(1).itemId());
			assertEquals(2, loot.get(1).count()); // D166
			assertTrue(loot.size() >= 8 && loot.size() <= 10, "size " + loot.size());
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
	void triggersOnlyScheduleWhenIdle() {
		assertEquals(100 + TreasureRules.DELAY_TICKS, TreasureRules.scheduleAt(TreasureRules.Phase.IDLE, -1, 100));
		assertEquals(500, TreasureRules.scheduleAt(TreasureRules.Phase.PENDING, 500, 900));
		assertEquals(500, TreasureRules.scheduleAt(TreasureRules.Phase.ACTIVE, 500, 900));
	}
}
