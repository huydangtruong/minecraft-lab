package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import org.junit.jupiter.api.Test;

class TechniqueDataTest {
	@Test
	void defaultIsEmpty() {
		TechniqueData d = TechniqueData.DEFAULT;
		assertEquals(0, d.level(Technique.FIRE));
		assertEquals(0, d.slotsOpen());
		assertEquals(4, d.slots().size());
		assertNull(d.inSlot(0));
		assertEquals(-1, d.slotOf(Technique.FIRE));
		assertFalse(d.onCooldown(Technique.FIRE, 0));
	}

	@Test
	void levelsAndReset() {
		TechniqueData d = TechniqueData.DEFAULT.withLevel(Technique.ICE, 3, 40);
		assertEquals(3, d.level(Technique.ICE));
		assertEquals(40, d.spent(Technique.ICE));
		TechniqueData r = d.withLevel(Technique.ICE, 0, 0);
		assertEquals(0, r.level(Technique.ICE));
		assertEquals(0, r.spent(Technique.ICE));
		assertEquals(20, d.withLevel(Technique.ICE, 99, 0).level(Technique.ICE), "capped at the max level");
	}

	@Test
	void slotsOpenWithEmeralds() {
		assertEquals(0, TechniqueData.DEFAULT.withEmeralds(63).slotsOpen());
		assertEquals(1, TechniqueData.DEFAULT.withEmeralds(64).slotsOpen());
		assertEquals(2, TechniqueData.DEFAULT.withEmeralds(320).slotsOpen());
		assertEquals(4, TechniqueData.DEFAULT.withEmeralds(5000).slotsOpen(), "emeralds are capped at 1856");
		assertEquals(1856, TechniqueData.DEFAULT.withEmeralds(5000).emeralds());
	}

	@Test
	void aTechniqueSitsInOneSlotOnly() {
		TechniqueData d = TechniqueData.DEFAULT.withSlot(0, Technique.FIRE).withSlot(2, Technique.FIRE);
		assertNull(d.inSlot(0));
		assertEquals(Technique.FIRE, d.inSlot(2));
		assertEquals(2, d.slotOf(Technique.FIRE));
		assertNull(d.withSlot(2, null).inSlot(2));
		assertNull(d.withoutFromSlots(Technique.FIRE).inSlot(2));
	}

	@Test
	void assigningATechniqueFromAnotherSlotMovesItAndEmptiesTheOldSlot() {
		TechniqueData d = TechniqueData.DEFAULT.withSlot(0, Technique.FIRE).withSlot(1, Technique.ICE);
		TechniqueData moved = d.withSlot(0, Technique.ICE);
		assertEquals(Technique.ICE, moved.inSlot(0));
		assertNull(moved.inSlot(1));
	}

	@Test
	void cooldowns() {
		TechniqueData d = TechniqueData.DEFAULT.withSlot(1, Technique.HEALING).withCooldown(Technique.HEALING, 1000);
		assertTrue(d.onCooldown(Technique.HEALING, 999));
		assertFalse(d.onCooldown(Technique.HEALING, 1000));
		assertEquals(40, d.cooldownLeft(Technique.HEALING, 960));
		assertEquals(0, d.cooldownLeft(Technique.HEALING, 2000));
		assertTrue(d.slotOnCooldown(1, 500));
		assertFalse(d.slotOnCooldown(0, 500), "an empty slot is never on cooldown");
		assertFalse(d.withoutCooldowns().onCooldown(Technique.HEALING, 0));
	}

	@Test
	void filledSlotsOnlyCountOpenOnes() {
		TechniqueData d = TechniqueData.DEFAULT.withEmeralds(64).withSlot(0, Technique.FIRE).withSlot(3, Technique.ICE);
		assertArrayEquals(new boolean[] {true, false, false, false}, d.filledSlots());
	}

	@Test
	void codecRoundTrip() {
		TechniqueData d = TechniqueData.DEFAULT.withLevel(Technique.FIRE, 7, 123).withEmeralds(400)
			.withSlot(1, Technique.FIRE).withCooldown(Technique.FIRE, 12345678901L);
		var json = TechniqueData.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
		assertEquals(d, TechniqueData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		assertEquals(TechniqueData.DEFAULT, TechniqueData.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow());
	}
}
