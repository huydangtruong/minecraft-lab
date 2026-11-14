package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.permahearts.technique.SkillBarLogic;
import org.junit.jupiter.api.Test;

class SkillBarLogicTest {
	@Test
	void chargeGrowsToFullInOneSecond() {
		assertEquals(0f, SkillBarLogic.charge(0));
		assertEquals(0.5f, SkillBarLogic.charge(10), 1e-6);
		assertEquals(1f, SkillBarLogic.charge(20));
		assertEquals(1f, SkillBarLogic.charge(99));
	}

	@Test
	void techniqueModeMapsCells() {
		int[] slots = SkillBarLogic.filledSlots(new boolean[] {true, false, true, false});
		assertArrayEquals(new int[] {0, 2}, slots);
		assertEquals(0, SkillBarLogic.techniqueAtCell(0, slots));
		assertEquals(2, SkillBarLogic.techniqueAtCell(1, slots));
		assertEquals(-1, SkillBarLogic.techniqueAtCell(2, slots));
		assertEquals(-1, SkillBarLogic.itemSlotAtCell(1, 2));
		assertEquals(0, SkillBarLogic.itemSlotAtCell(2, 2));
		assertEquals(6, SkillBarLogic.itemSlotAtCell(8, 2));
		assertEquals(8, SkillBarLogic.itemSlotAtCell(8, 0));
	}

	@Test
	void scrollWrapsOverNineCells() {
		assertEquals(1, SkillBarLogic.scrollCell(0, 1));
		assertEquals(8, SkillBarLogic.scrollCell(0, -1));
		assertEquals(0, SkillBarLogic.scrollCell(8, 3));
	}

	@Test
	void cooldownFractionAndSeconds() {
		assertEquals(1f, SkillBarLogic.cooldownFraction(320, 320));
		assertEquals(0.5f, SkillBarLogic.cooldownFraction(160, 320));
		assertEquals(0f, SkillBarLogic.cooldownFraction(0, 320));
		assertEquals(12, SkillBarLogic.secondsLeft(224));
		assertEquals(1, SkillBarLogic.secondsLeft(1));
		assertEquals(0, SkillBarLogic.secondsLeft(0));
	}
}
