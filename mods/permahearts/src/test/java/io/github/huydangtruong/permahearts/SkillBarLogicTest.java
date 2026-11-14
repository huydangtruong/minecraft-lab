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
	void wheelEntriesAreCrossThenFilledSlots() {
		assertArrayEquals(new int[] {-1}, SkillBarLogic.wheelEntries(new boolean[] {false, false, false, false}));
		assertArrayEquals(new int[] {-1, 0, 2}, SkillBarLogic.wheelEntries(new boolean[] {true, false, true, false}));
	}

	@Test
	void wheelIndexPicksByDirection() {
		// 4 entries: top, right, bottom, left
		assertEquals(0, SkillBarLogic.wheelIndex(0, -50, 4));
		assertEquals(1, SkillBarLogic.wheelIndex(50, 0, 4));
		assertEquals(2, SkillBarLogic.wheelIndex(0, 50, 4));
		assertEquals(3, SkillBarLogic.wheelIndex(-50, 0, 4));
		// just left of straight up still belongs to the top entry
		assertEquals(0, SkillBarLogic.wheelIndex(-10, -50, 4));
		// dead zone and empty wheel
		assertEquals(-1, SkillBarLogic.wheelIndex(3, 3, 4));
		assertEquals(-1, SkillBarLogic.wheelIndex(50, 0, 0));
		// a wheel with one entry always gives it
		assertEquals(0, SkillBarLogic.wheelIndex(40, 40, 1));
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
