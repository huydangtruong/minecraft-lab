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
	void filledSlotsInOrder() {
		assertArrayEquals(new int[] {0, 2}, SkillBarLogic.filledSlots(new boolean[] {true, false, true, false}));
	}

	@Test
	void scrollRingWithoutTechniquesIsTheHotbar() {
		assertEquals(1, SkillBarLogic.scrollRing(0, 1, 0));
		assertEquals(8, SkillBarLogic.scrollRing(0, -1, 0));
		assertEquals(0, SkillBarLogic.scrollRing(8, 1, 0));
	}

	@Test
	void scrollRingRunsThroughFilledTechniques() {
		for (int n = 1; n <= 4; n++) {
			assertEquals(9, SkillBarLogic.scrollRing(8, 1, n), "down from hotbar 9 -> first technique, n=" + n);
			assertEquals(8 + n, SkillBarLogic.scrollRing(0, -1, n), "up from hotbar 1 -> last technique, n=" + n);
			assertEquals(8, SkillBarLogic.scrollRing(9, -1, n), "up from first technique -> hotbar 9, n=" + n);
			assertEquals(0, SkillBarLogic.scrollRing(8 + n, 1, n), "down from last technique -> hotbar 1, n=" + n);
			assertEquals(4, SkillBarLogic.scrollRing(3, 1, n));
		}
		assertEquals(10, SkillBarLogic.scrollRing(9, 1, 2));
		assertEquals(9, SkillBarLogic.scrollRing(10, -1, 2));
	}

	@Test
	void ringPositionAndTechniqueAtRing() {
		int[] filled = {0, 2};
		assertEquals(5, SkillBarLogic.ringPosition(5, -1, filled));
		assertEquals(9, SkillBarLogic.ringPosition(5, 0, filled));
		assertEquals(10, SkillBarLogic.ringPosition(5, 2, filled));
		assertEquals(-1, SkillBarLogic.techniqueAtRing(8, filled));
		assertEquals(0, SkillBarLogic.techniqueAtRing(9, filled));
		assertEquals(2, SkillBarLogic.techniqueAtRing(10, filled));
		assertEquals(-1, SkillBarLogic.techniqueAtRing(11, filled));
	}

	@Test
	void slotKeyIgnoresEmptySlots() {
		boolean[] usable = {true, false, true, false};
		assertEquals(0, SkillBarLogic.slotForKey(0, usable));
		assertEquals(-1, SkillBarLogic.slotForKey(1, usable));
		assertEquals(2, SkillBarLogic.slotForKey(2, usable));
		assertEquals(-1, SkillBarLogic.slotForKey(3, usable));
		assertEquals(-1, SkillBarLogic.slotForKey(4, usable));
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

	@Test
	void slideEasingAndStep() {
		assertEquals(0f, SkillBarLogic.slideOffset(0f));
		assertEquals(1f, SkillBarLogic.slideOffset(1f));
		assertEquals(0.875f, SkillBarLogic.slideOffset(0.5f), 1e-6f);
		assertEquals(1f, SkillBarLogic.slideOffset(2f));
		assertEquals(0f, SkillBarLogic.slideOffset(-1f));
		assertEquals(1f, SkillBarLogic.slideStep(0f, true, 150), 1e-6f);
		assertEquals(0.5f, SkillBarLogic.slideStep(0f, true, 75), 1e-6f);
		assertEquals(0.25f, SkillBarLogic.slideStep(0.5f, false, 37), 0.01f); // reversing mid-slide continues from 0.5
		assertEquals(0f, SkillBarLogic.slideStep(0.1f, false, 1000));
		assertEquals(0.3f, SkillBarLogic.slideStep(0.3f, true, 0));
	}

	@Test
	void barTopLoweredAndRaised() {
		assertEquals(188f, SkillBarLogic.barTop(200, 0f));
		assertEquals(179f, SkillBarLogic.barTop(200, 1f));
	}
}
