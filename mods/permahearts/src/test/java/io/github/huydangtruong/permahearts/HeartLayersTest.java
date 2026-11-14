package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.permahearts.HeartLayers.Slot;
import org.junit.jupiter.api.Test;

class HeartLayersTest {
	@Test
	void twentyHeartsAreVanilla() {
		assertFalse(HeartLayers.layered(40f));
		assertTrue(HeartLayers.layered(42f));
	}

	@Test
	void neverMoreThanTwoRows() {
		assertEquals(20, HeartLayers.slots(200f));
		assertEquals(2, HeartLayers.rows(HeartLayers.slots(200f)));
		assertEquals(5, HeartLayers.layers(200f));
		assertEquals(20, HeartLayers.height(200f, 0)); // two rows, 10 apart, like vanilla 20 hearts
	}

	@Test
	void twentyOneHeartsColourTheFirstHeart() {
		Slot[] s = HeartLayers.layout(42f, 42);
		assertEquals(new Slot(1, false, -1), s[0]);
		for (int i = 1; i < 20; i++) {
			assertEquals(new Slot(0, false, -1), s[i]);
		}
	}

	@Test
	void damageTakesTheColouredHeartFirst() {
		Slot[] s = HeartLayers.layout(42f, 41); // half of heart 21 left
		assertEquals(new Slot(1, true, 0), s[0]);
		s = HeartLayers.layout(42f, 40); // heart 21 gone: 20 red hearts
		assertEquals(new Slot(0, false, -1), s[0]);
		assertEquals(new Slot(0, false, -1), s[19]);
	}

	@Test
	void fiftyHeartsFull() {
		Slot[] s = HeartLayers.layout(100f, 100); // hearts 41-50 = layer 2 on the first row
		assertEquals(new Slot(2, false, -1), s[0]);
		assertEquals(new Slot(2, false, -1), s[9]);
		assertEquals(new Slot(1, false, -1), s[10]);
		assertEquals(new Slot(1, false, -1), s[19]);
	}

	@Test
	void hundredHeartsFullIsTheFifthColourEverywhere() {
		for (Slot x : HeartLayers.layout(200f, 200)) {
			assertEquals(new Slot(4, false, -1), x);
		}
	}

	@Test
	void threeHundredHeartsEndWithLava() {
		assertEquals(15, HeartLayers.layers(600f));
		for (Slot x : HeartLayers.layout(600f, 600)) {
			assertEquals(new Slot(14, false, -1), x); // 281-300 everywhere
		}
		Slot[] s = HeartLayers.layout(600f, 501); // 250.5 hearts: 10.5 soul fire hearts (241-260) over galaxy
		assertEquals(new Slot(12, false, -1), s[9]);
		assertEquals(new Slot(12, true, 11), s[10]);
		assertEquals(new Slot(11, false, -1), s[11]);
	}

	@Test
	void lowHealthLooksLikeVanilla() {
		Slot[] s = HeartLayers.layout(100f, 5); // 2.5 hearts
		assertEquals(new Slot(0, false, -1), s[1]);
		assertEquals(new Slot(0, true, -1), s[2]);
		assertEquals(new Slot(-1, false, -1), s[3]);
		for (Slot x : HeartLayers.layout(100f, 0)) {
			assertEquals(-1, x.layer());
		}
	}

	@Test
	void armourMovesDownToTheTwoRows() {
		assertEquals(20, HeartLayers.armorShift(114f, 114, 0)); // 57 hearts: vanilla 6 rows 6 px apart = 30, ours 10
		assertEquals(17, HeartLayers.armorShift(200f, 200, 0)); // 100 hearts: vanilla 10 rows 3 px apart = 27, ours 10
	}

	@Test
	void absorptionAddsRows() {
		assertEquals(28, HeartLayers.height(200f, 8)); // 3 rows, 9 apart
	}
}
