package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.permahearts.godslayer.GodSlayerDirector;
import io.github.huydangtruong.permahearts.godslayer.GodSlayerDirector.Kind;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GodSlayerDirectorTest {
	@Test
	void levelOneHasNoSpecials() {
		assertTrue(GodSlayerDirector.unlocked(1).isEmpty());
		assertEquals(List.of(Kind.FIREBALL), GodSlayerDirector.unlocked(2));
	}

	@Test
	void neverNonFittingAndNothingWhenNoneFits() {
		GodSlayerDirector d = new GodSlayerDirector();
		assertNull(d.pick(new Random(1), 2, k -> false));
		assertEquals(Kind.FIREBALL, d.pick(new Random(1), 2, k -> true));
	}

	@Test
	void pressureIsFourToEightSeconds() {
		Random r = new Random(7);
		for (int i = 0; i < 100; i++) {
			int t = GodSlayerDirector.pressureTicks(r);
			assertTrue(t >= 80 && t <= 160);
		}
	}
}
