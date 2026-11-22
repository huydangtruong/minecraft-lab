package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.trash.TrashRules;
import org.junit.jupiter.api.Test;

/** D217: the trash slot destroys a non-empty carried stack, never for a spectator. */
class TrashRulesTest {
	@Test
	void destroysCarriedStack() {
		assertTrue(TrashRules.shouldDestroy(false, false));
	}

	@Test
	void emptyCursorDoesNothing() {
		assertFalse(TrashRules.shouldDestroy(true, false));
	}

	@Test
	void spectatorDoesNothing() {
		assertFalse(TrashRules.shouldDestroy(false, true));
		assertFalse(TrashRules.shouldDestroy(true, true));
	}
}
