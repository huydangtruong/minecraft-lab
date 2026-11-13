package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.permahearts.wanderer.BloodMoon;
import io.github.huydangtruong.permahearts.wanderer.BloodMoonArena;
import org.junit.jupiter.api.Test;

class BloodMoonArenaTest {
	@Test
	void square96AroundTheMaster() {
		// Master at block (100, -20): centre 100.5, -19.5; edges at 52.5..148.5 and -67.5..28.5
		assertTrue(BloodMoonArena.inside(100.5, -19.5, 100, -20));
		assertTrue(BloodMoonArena.inside(148.4, 28.4, 100, -20));
		assertFalse(BloodMoonArena.inside(148.6, -19.5, 100, -20));
		assertFalse(BloodMoonArena.inside(100.5, -67.6, 100, -20));
		assertEquals(0.5, BloodMoonArena.edgeDistance(148.0, -19.5, 100, -20), 1e-9);
		assertEquals(48, BloodMoonArena.edgeDistance(100.5, -19.5, 100, -20), 1e-9);
	}

	@Test
	void threePerWaveCapThirty() {
		assertEquals(3, BloodMoon.toSpawn(0));
		assertEquals(2, BloodMoon.toSpawn(28));
		assertEquals(0, BloodMoon.toSpawn(30));
	}
}
