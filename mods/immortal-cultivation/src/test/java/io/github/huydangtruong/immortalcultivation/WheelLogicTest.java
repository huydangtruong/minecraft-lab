package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.immortalcultivation.technique.WheelLogic;
import org.junit.jupiter.api.Test;

class WheelLogicTest {
	@Test
	void defaultIsLowestReadySlot() {
		assertEquals(0, WheelLogic.defaultSlot(3, new boolean[] {true, true, true, false}, new boolean[4]));
		assertEquals(1, WheelLogic.defaultSlot(3, new boolean[] {true, true, true, false}, new boolean[] {true, false, false, false}));
		assertEquals(2, WheelLogic.defaultSlot(3, new boolean[] {false, false, true, false}, new boolean[4]));
	}

	@Test
	void defaultIgnoresLockedSlots() {
		assertEquals(0, WheelLogic.defaultSlot(2, new boolean[] {false, false, true, true}, new boolean[4]));
	}

	@Test
	void defaultFallsBackToFirstSlot() {
		assertEquals(0, WheelLogic.defaultSlot(3, new boolean[] {true, true, true, false}, new boolean[] {true, true, true, false}));
		assertEquals(0, WheelLogic.defaultSlot(2, new boolean[4], new boolean[4]));
	}

	@Test
	void defaultNoOpenSlot() {
		assertEquals(-1, WheelLogic.defaultSlot(0, new boolean[] {true, true, true, true}, new boolean[4]));
	}

	@Test
	void stepWrapsAmongOpenSlots() {
		assertEquals(1, WheelLogic.step(0, 1, 3));
		assertEquals(0, WheelLogic.step(2, 1, 3));
		assertEquals(2, WheelLogic.step(0, -1, 3));
		assertEquals(1, WheelLogic.step(2, -1, 3));
		assertEquals(0, WheelLogic.step(0, 1, 1));
		assertEquals(0, WheelLogic.step(5, 1, 3));
		assertEquals(-1, WheelLogic.step(0, 1, 0));
	}
}
