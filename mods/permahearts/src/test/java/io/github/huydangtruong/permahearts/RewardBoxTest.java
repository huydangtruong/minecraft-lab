package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.permahearts.reward.RewardOption;
import io.github.huydangtruong.permahearts.reward.RewardRoll;
import io.github.huydangtruong.permahearts.reward.RewardSelection;
import io.github.huydangtruong.permahearts.reward.RewardTier;
import io.github.huydangtruong.permahearts.wanderer.QuestGenerator;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RewardBoxTest {
	private static boolean isElixir(RewardOption o) {
		return o.itemId().startsWith("permahearts:elixir_");
	}

	@Test
	void tierByLineType() {
		assertEquals(RewardTier.SILVER, RewardTier.forLine(QuestType.COLLECT));
		assertEquals(RewardTier.SILVER, RewardTier.forLine(QuestType.KILL));
		assertEquals(RewardTier.GOLD, RewardTier.forLine(QuestType.EXPLORE));
		assertEquals(RewardTier.GOLD, RewardTier.forLine(QuestType.BLOOD_MOON));
		assertEquals(RewardTier.DIAMOND, RewardTier.forLine(QuestType.BOSS));
		assertEquals(RewardTier.DIAMOND, RewardTier.forLine(QuestType.DEATH_MATCH));
		for (QuestType t : QuestType.values()) {
			assertTrue(RewardTier.forLine(t) != null);
		}
	}

	@Test
	void tierSizes() {
		assertEquals(3, RewardTier.SILVER.options());
		assertEquals(4, RewardTier.GOLD.options());
		assertEquals(5, RewardTier.DIAMOND.options());
		for (RewardTier t : RewardTier.values()) {
			assertEquals(1, t.picks());
		}
	}

	@Test
	void rollSizes() {
		Random rng = new Random(1);
		for (RewardTier t : RewardTier.values()) {
			for (int i = 0; i < 200; i++) {
				assertEquals(t.options(), RewardRoll.roll(t, rng).size());
			}
		}
	}

	@Test
	void mixOfElixirsAndItems() {
		Random rng = new Random(2);
		for (RewardTier t : RewardTier.values()) {
			boolean allElixir = false;
			boolean allItem = false;
			boolean elixirSeen = false;
			boolean itemSeen = false;
			for (int i = 0; i < 4000; i++) {
				List<RewardOption> box = RewardRoll.roll(t, rng);
				long elixirs = box.stream().filter(RewardBoxTest::isElixir).count();
				elixirSeen |= elixirs > 0;
				itemSeen |= elixirs < box.size();
				allElixir |= elixirs == box.size();
				allItem |= elixirs == 0;
			}
			assertTrue(elixirSeen && itemSeen, t + ": both kinds appear");
			assertTrue(allElixir, t + ": an all-elixir box occurs");
			assertTrue(allItem, t + ": an all-item box occurs");
		}
	}

	@Test
	void elixirsAreOneBottleOfARealStat() {
		Set<String> valid = new HashSet<>();
		for (ElixirStat s : ElixirStat.values()) {
			valid.add("permahearts:elixir_" + s.id());
		}
		Random rng = new Random(3);
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 2000; i++) {
			for (RewardOption o : RewardRoll.roll(RewardTier.DIAMOND, rng)) {
				if (isElixir(o)) {
					assertEquals(1, o.count());
					assertTrue(valid.contains(o.itemId()), o.itemId());
					seen.add(o.itemId());
				}
			}
		}
		assertEquals(valid, seen, "every stat shows up");
	}

	@Test
	void specialsOnlyInGoldAndDiamond() {
		Random rng = new Random(4);
		for (RewardTier t : RewardTier.values()) {
			Set<RewardOption> specialsSeen = new HashSet<>();
			for (int i = 0; i < 6000; i++) {
				for (RewardOption o : RewardRoll.roll(t, rng)) {
					if (isElixir(o)) {
						continue;
					}
					if (RewardRoll.GOLD_SPECIALS.contains(o) || RewardRoll.DIAMOND_SPECIALS.contains(o)) {
						specialsSeen.add(o);
					}
				}
			}
			if (t == RewardTier.SILVER) {
				assertTrue(specialsSeen.isEmpty(), "silver has no specials: " + specialsSeen);
			} else {
				assertEquals(Set.copyOf(RewardRoll.specials(t)), specialsSeen, t + " specials all appear");
			}
		}
	}

	@Test
	void netherite() {
		Random rng = new Random(5);
		boolean diamondNether = false;
		for (int i = 0; i < 8000; i++) {
			for (RewardTier t : RewardTier.values()) {
				for (RewardOption o : RewardRoll.roll(t, rng)) {
					if (o.itemId().equals("minecraft:netherite_ingot")) {
						assertFalse(t == RewardTier.SILVER || t == RewardTier.GOLD, "netherite only in diamond boxes");
						assertEquals(1, o.count());
						diamondNether = true;
					}
				}
			}
		}
		assertTrue(diamondNether);
	}

	@Test
	void itemAmountsMatchTheCollectQuests() {
		Random rng = new Random(6);
		Set<String> baseSeen = new HashSet<>();
		for (int i = 0; i < 4000; i++) {
			for (RewardOption o : RewardRoll.roll(RewardTier.values()[i % 3], rng)) {
				if (RewardRoll.BASE_ITEMS.contains(o.itemId()) && !RewardRoll.GOLD_SPECIALS.contains(o)
					&& !RewardRoll.DIAMOND_SPECIALS.contains(o)) {
					assertEquals(QuestGenerator.collectAmount(o.itemId()), o.count(), o.itemId());
					baseSeen.add(o.itemId());
				}
			}
		}
		assertEquals(Set.copyOf(RewardRoll.BASE_ITEMS), baseSeen);
		// The numbers from D76 (they are the collect amounts).
		assertEquals(64, QuestGenerator.collectAmount("minecraft:iron_ingot"));
		assertEquals(64, QuestGenerator.collectAmount("minecraft:gold_ingot"));
		assertEquals(16, QuestGenerator.collectAmount("minecraft:diamond"));
		assertEquals(16, QuestGenerator.collectAmount("minecraft:emerald"));
		assertEquals(64, QuestGenerator.collectAmount("minecraft:redstone"));
		assertEquals(64, QuestGenerator.collectAmount("minecraft:lapis_lazuli"));
		assertEquals(32, QuestGenerator.collectAmount("minecraft:ender_pearl"));
		assertEquals(32, QuestGenerator.collectAmount("minecraft:gunpowder"));
		for (String id : RewardRoll.BASE_ITEMS) {
			assertTrue(QuestGenerator.collectAmount(id) > 0, id + " is a collect item");
		}
	}

	@Test
	void noDuplicateItemOptions() {
		Random rng = new Random(7);
		for (int i = 0; i < 6000; i++) {
			for (RewardTier t : RewardTier.values()) {
				List<RewardOption> box = RewardRoll.roll(t, rng);
				Set<String> items = new HashSet<>();
				for (RewardOption o : box) {
					if (!isElixir(o)) {
						assertTrue(items.add(o.itemId()), "duplicate " + o + " in " + box);
					}
				}
			}
		}
	}

	@Test
	void sameSeedSameBox() {
		assertEquals(RewardRoll.roll(RewardTier.GOLD, new Random(9)), RewardRoll.roll(RewardTier.GOLD, new Random(9)));
	}

	@Test
	void selectionOnePick() {
		int mask = 0;
		mask = RewardSelection.toggle(mask, 1, 1, 4);
		assertEquals(0b0010, mask);
		assertTrue(RewardSelection.complete(mask, 1));
		mask = RewardSelection.toggle(mask, 3, 1, 4); // moves
		assertEquals(0b1000, mask);
		mask = RewardSelection.toggle(mask, 3, 1, 4); // same card keeps the selection
		assertEquals(0b1000, mask);
		assertEquals(0b1000, RewardSelection.toggle(mask, 4, 1, 4), "out of range ignored");
		assertEquals(0b1000, RewardSelection.toggle(mask, -1, 1, 4), "negative ignored");
		assertFalse(RewardSelection.complete(0, 1));
	}

	@Test
	void selectionTwoPicks() {
		int mask = RewardSelection.toggle(0, 0, 2, 5);
		assertFalse(RewardSelection.complete(mask, 2));
		mask = RewardSelection.toggle(mask, 4, 2, 5);
		assertEquals(0b10001, mask);
		assertTrue(RewardSelection.complete(mask, 2));
		assertEquals(mask, RewardSelection.toggle(mask, 2, 2, 5), "a third card is ignored while two are selected");
		mask = RewardSelection.toggle(mask, 0, 2, 5); // deselect
		assertEquals(0b10000, mask);
		mask = RewardSelection.toggle(mask, 2, 2, 5);
		assertEquals(0b10100, mask);
	}

	@Test
	void validSelection() {
		assertTrue(RewardSelection.valid(0b001, 1, 3));
		assertTrue(RewardSelection.valid(0b100, 1, 3));
		assertFalse(RewardSelection.valid(0b1000, 1, 3), "card 3 does not exist in a 3-card box");
		assertFalse(RewardSelection.valid(0, 1, 3));
		assertFalse(RewardSelection.valid(0b011, 1, 3), "two cards with picks 1");
		assertTrue(RewardSelection.valid(0b011, 2, 3));
		assertFalse(RewardSelection.valid(0b001, 2, 3), "one card with picks 2");
		assertFalse(RewardSelection.valid(-1, 1, 3));
		assertFalse(RewardSelection.valid(1 << 20, 1, 5));
		assertFalse(RewardSelection.valid(Integer.MIN_VALUE, 1, 5));
	}
}
