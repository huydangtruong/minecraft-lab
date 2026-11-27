package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.WheelGroups;
import io.github.huydangtruong.immortalcultivation.technique.chrono.ChronoCursor;
import io.github.huydangtruong.immortalcultivation.technique.chrono.FrameRing;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** D268: Time Rewind numbers, the recording ring and the rewind cursor. */
class ChronoRulesTest {
	@Test
	void numbersScaleWithTheLevel() {
		assertEquals(14, TechniqueRules.chronoDurationSeconds(1), 1e-9);
		assertEquals(24, TechniqueRules.chronoDurationSeconds(20), 1e-9);
		assertEquals(8, TechniqueRules.chronoRewindSeconds(1), 1e-9);
		assertEquals(24, TechniqueRules.chronoRewindSeconds(20), 1e-9);
		assertEquals(480, TechniqueRules.chronoRewindTicks(20));
		assertEquals(280, TechniqueRules.chronoDurationTicks(1));
		for (int l = 1; l < 20; l++) {
			assertTrue(TechniqueRules.chronoDurationSeconds(l + 1) > TechniqueRules.chronoDurationSeconds(l));
			assertTrue(TechniqueRules.chronoRewindSeconds(l + 1) > TechniqueRules.chronoRewindSeconds(l));
		}
		assertEquals(140, TechniqueRules.cooldownSeconds(Technique.CHRONO, 1));
		assertEquals(140, TechniqueRules.cooldownSeconds(Technique.CHRONO, 20), "the cooldown never scales");
	}

	@Test
	void isADefenseTechnique() {
		assertTrue(Arrays.asList(WheelGroups.GROUPS[1].members()).contains(Technique.CHRONO));
		assertEquals("Defense", WheelGroups.GROUPS[1].label());
		assertEquals(5, TechniqueRules.stats(Technique.CHRONO, 1).size() + 1);
	}

	@Test
	void theBoxIsCentredWithItsFloorFourBelowTheFeet() {
		assertArrayEquals(new int[] {-8, 60, -8}, TechniqueRules.chronoBoxMin(0.3, 64, 0.2));
		assertArrayEquals(new int[] {3, -4, -18}, TechniqueRules.chronoBoxMin(10.6, 0.5, -9.7));
	}

	@Test
	void theUsableRewindIsCappedByWhatWasRecorded() {
		assertEquals(60, ChronoCursor.usable(60, 20), "only 3 s in a slot = only 3 s back");
		assertEquals(160, ChronoCursor.usable(10_000, 1));
		assertEquals(0, ChronoCursor.usable(0, 5));
	}

	@Test
	void theCursorMovesByNotchesAndPlaysSmoothly() {
		ChronoCursor c = new ChronoCursor(25);
		c.scroll(-1);
		assertEquals(0, c.target(), "never forward past the cast");
		c.scroll(1);
		assertEquals(10, c.target());
		c.scroll(5);
		assertEquals(25, c.target(), "never back past the recording");
		int steps = 0;
		while (c.step()) {
			steps++;
		}
		assertEquals(25, c.shown());
		assertEquals(13, steps, "2 recorded ticks per tick");
		c.scroll(-2);
		assertEquals(5, c.target());
		c.step();
		assertEquals(23, c.shown());
	}

	@Test
	void theRingKeepsTheLastFramesOnly() {
		FrameRing r = new FrameRing(4, 2);
		float[] out = new float[2];
		for (int f = 0; f < 6; f++) {
			r.put(f, new float[] {f, -f});
		}
		assertFalse(r.get(1, out), "too old");
		assertTrue(r.get(2, out));
		assertEquals(2, out[0]);
		assertEquals(2, r.first());
		assertEquals(5, r.last());
		r.truncate(3);
		assertEquals(3, r.last());
		assertFalse(r.get(4, out));
		r.put(4, new float[] {40, 0});
		assertTrue(r.get(4, out));
		assertEquals(40, out[0]);
		assertFalse(r.get(5, out), "a forgotten frame never comes back");
		FrameRing gap = new FrameRing(10, 1);
		gap.put(3, new float[] {1});
		gap.put(7, new float[] {1});
		assertFalse(gap.get(5, new float[1]), "not recorded while it was away");
		assertEquals(3, gap.first());
		gap.truncate(2);
		assertEquals(-1, gap.last());
		assertEquals(-1, gap.first());
	}
}
