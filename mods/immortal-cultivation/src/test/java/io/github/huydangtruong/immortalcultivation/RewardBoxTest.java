package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.reward.RewardOption;
import io.github.huydangtruong.immortalcultivation.reward.RewardRoll;
import io.github.huydangtruong.immortalcultivation.reward.RewardSelection;
import io.github.huydangtruong.immortalcultivation.reward.RewardTier;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestGenerator;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestType;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RewardBoxTest {
	private static boolean isElixir(RewardOption o) {
		return o.itemId().startsWith("permahearts:elixir_");
	}

	private static final int ROLLS = 200_000;

	private static int[] countTiers(QuestType type, long seed) {
		Random rng = new Random(seed);
		int[] n = new int[RewardTier.values().length];
		for (int i = 0; i < ROLLS; i++) {
			n[RewardTier.roll(type, rng).ordinal()]++;
		}
		return n;
	}

	private static void assertOdds(int[] counts, int silver, int gold, int diamond, int emerald, String what) {
		int[] want = {silver, gold, diamond, emerald};
		for (RewardTier t : RewardTier.values()) {
			double got = 100.0 * counts[t.ordinal()] / ROLLS;
			assertTrue(Math.abs(got - want[t.ordinal()]) < 1.0, what + " " + t + ": " + got + "% vs " + want[t.ordinal()] + "%");
		}
	}

	@Test
	void tierOddsD90() {
		for (QuestType t : QuestType.values()) {
			boolean hard = t == QuestType.BOSS || t == QuestType.BLOOD_MOON;
			assertOdds(countTiers(t, 11), hard ? 20 : 40, 30, hard ? 30 : 20, hard ? 20 : 10, t.toString());
			int total = 0;
			for (RewardTier tier : RewardTier.values()) {
				total += RewardTier.oddsPercent(t, tier);
			}
			assertEquals(100, total, t + " odds add up to 100");
		}
	}

	@Test
	void tierRollIsDeterministicPerSeed() {
		assertEquals(RewardTier.roll(QuestType.BOSS, new Random(3)), RewardTier.roll(QuestType.BOSS, new Random(3)));
	}

	@Test
	void emeraldBox() {
		assertEquals(8, RewardTier.EMERALD.options()); // D210
		assertEquals(4, RewardTier.EMERALD.picks());
		assertEquals("emerald_reward_box", RewardTier.EMERALD.itemName());
	}

	@Test
	void tierSizes() {
		assertEquals(3, RewardTier.SILVER.options());
		assertEquals(4, RewardTier.GOLD.options());
		assertEquals(6, RewardTier.DIAMOND.options()); // D210
		assertEquals(8, RewardTier.EMERALD.options());
		assertEquals(1, RewardTier.SILVER.picks());
		assertEquals(2, RewardTier.GOLD.picks());
		assertEquals(3, RewardTier.DIAMOND.picks());
		assertEquals(4, RewardTier.EMERALD.picks());
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
			for (int i = 0; i < 40000; i++) { // D210: an all-item emerald box (8 cards) is 0.4^8, rare
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
		valid.add(RewardRoll.HEART_ELIXIR);
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
	void netherite() {
		Random rng = new Random(5);
		boolean diamondNether = false;
		for (int i = 0; i < 8000; i++) {
			for (RewardTier t : RewardTier.values()) {
				for (RewardOption o : RewardRoll.roll(t, rng)) {
					if (o.itemId().equals("minecraft:netherite_ingot")) {
						assertFalse(t != RewardTier.DIAMOND, "netherite only in diamond boxes");
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
						assertTrue(items.add(o.key()), "duplicate " + o + " in " + box);
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

	@Test
	void emeraldSpecials() {
		assertEquals(List.of("minecraft:totem_of_undying", "permahearts:enlightenment_bottle", "minecraft:enchanted_book",
			"minecraft:enchanted_book", "minecraft:enchanted_book", "minecraft:enchanted_book",
			"minecraft:diamond", "minecraft:emerald", "minecraft:ender_pearl"),
			RewardRoll.EMERALD_SPECIALS.stream().map(RewardOption::itemId).toList());
		assertEquals(Set.of("minecraft:mending@1", "minecraft:sharpness@5", "minecraft:protection@4", "minecraft:unbreaking@3"), // D211, D212
			RewardRoll.EMERALD_SPECIALS.stream().filter(RewardOption::hasEnchant)
				.map(o -> o.enchant() + "@" + o.enchantLevel()).collect(java.util.stream.Collectors.toSet()));
	}

	/** D211: the item cards of each box and their amounts. */
	@Test
	void itemPoolsPerTier() {
		assertEquals(Set.of("minecraft:gold_ingot x64", "minecraft:diamond x16", "minecraft:emerald x16", "minecraft:ender_pearl x32"),
			pool(RewardTier.SILVER));
		assertEquals(Set.of("minecraft:gold_ingot x64", "minecraft:diamond x32", "minecraft:emerald x32", "minecraft:ender_pearl x32",
			"minecraft:enchanted_golden_apple x4"), pool(RewardTier.GOLD));
		assertEquals(Set.of("minecraft:gold_ingot x64", "minecraft:diamond x32", "minecraft:emerald x32", "minecraft:ender_pearl x48",
			"minecraft:enchanted_golden_apple x4", "minecraft:netherite_ingot x1"), pool(RewardTier.DIAMOND));
		assertEquals(Set.of("minecraft:gold_ingot x64", "minecraft:diamond x48", "minecraft:emerald x64", "minecraft:ender_pearl x48",
			"minecraft:totem_of_undying x1", "permahearts:enlightenment_bottle x1", "minecraft:enchanted_book:minecraft:mending x1",
			"minecraft:enchanted_book:minecraft:sharpness x1", "minecraft:enchanted_book:minecraft:protection x1",
			"minecraft:enchanted_book:minecraft:unbreaking x1"), pool(RewardTier.EMERALD));
	}

	private static Set<String> pool(RewardTier t) {
		return RewardRoll.itemPool(t).stream()
			.map(o -> o.itemId() + (o.hasEnchant() ? ":" + o.enchant() : "") + " x" + o.count())
			.collect(java.util.stream.Collectors.toSet());
	}

	@Test
	void emeraldRollEightCardsBooksMayShareAnItemIdButNotAnEnchantment() {
		Random rng = new Random(12);
		boolean twoBooks = false;
		int specialCards = 0;
		int itemCards = 0;
		for (int i = 0; i < 20000; i++) {
			List<RewardOption> box = RewardRoll.roll(RewardTier.EMERALD, rng);
			assertEquals(8, box.size());
			Set<String> keys = new HashSet<>();
			int books = 0;
			for (RewardOption o : box) {
				if (isElixir(o)) {
					continue;
				}
				assertTrue(keys.add(o.key()), "duplicate " + o + " in " + box);
				itemCards++;
				if (RewardRoll.EMERALD_SPECIALS.contains(o)) {
					specialCards++;
				}
				if (o.itemId().equals("minecraft:enchanted_book")) {
					assertTrue(o.hasEnchant(), "a book card carries its enchantment");
					books++;
				}
			}
			twoBooks |= books >= 2;
		}
		assertTrue(twoBooks, "two different books can be in one box");
	}

	@Test
	void optionCodecRoundTripWithAndWithoutEnchant() {
		for (RewardOption o : List.of(new RewardOption("minecraft:diamond", 16), RewardOption.book("minecraft:sharpness", 5))) {
			var json = RewardOption.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, o).getOrThrow();
			assertEquals(o, RewardOption.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow());
		}
		// A plain card does not even write the enchantment fields.
		var plain = RewardOption.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, new RewardOption("minecraft:diamond", 16))
			.getOrThrow().getAsJsonObject();
		assertFalse(plain.has("enchant") && !plain.get("enchant").getAsString().isEmpty());
	}

	@Test
	void oldSavedCardWithoutEnchantStillLoads() {
		var old = new com.google.gson.JsonObject();
		old.addProperty("item", "minecraft:gold_ingot");
		old.addProperty("count", 64);
		RewardOption o = RewardOption.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow();
		assertEquals(new RewardOption("minecraft:gold_ingot", 64), o);
		assertFalse(o.hasEnchant());
	}
}
