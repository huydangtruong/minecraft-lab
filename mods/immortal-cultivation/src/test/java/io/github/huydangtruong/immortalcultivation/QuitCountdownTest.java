package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.quit.QuitCountdown;
import io.github.huydangtruong.immortalcultivation.quit.QuitCountdown.Tick;
import org.junit.jupiter.api.Test;

/** D218: the /quit countdown. */
class QuitCountdownTest {
	@Test
	void startsAtTenSeconds() {
		QuitCountdown c = new QuitCountdown();
		assertFalse(c.active());
		assertEquals(Tick.IDLE, c.tick());
		assertTrue(c.start());
		assertTrue(c.active());
		assertEquals(200, c.ticksLeft());
		assertEquals(10, c.secondsLeft());
		assertFalse(c.start(), "a running countdown is not restarted");
		assertEquals(200, c.ticksLeft());
	}

	@Test
	void firesAtExactlyTenSeconds() {
		QuitCountdown c = new QuitCountdown();
		c.start();
		int fires = 0;
		int seconds = 0;
		for (int i = 1; i <= 199; i++) {
			Tick t = c.tick();
			assertTrue(t != Tick.FIRE, "fired early at tick " + i);
			if (t == Tick.SECOND) {
				seconds++;
			}
		}
		assertEquals(9, seconds, "a SECOND for 9..1");
		assertEquals(1, c.ticksLeft());
		assertEquals(1, c.secondsLeft());
		assertEquals(Tick.FIRE, c.tick(), "tick 200 fires");
		fires++;
		assertFalse(c.active());
		for (int i = 0; i < 50; i++) {
			assertEquals(Tick.IDLE, c.tick());
		}
		assertEquals(1, fires);
	}

	@Test
	void secondEventsCarryTheNumber() {
		QuitCountdown c = new QuitCountdown();
		c.start();
		for (int i = 0; i < 20; i++) {
			c.tick();
		}
		assertEquals(9, c.secondsLeft());
		assertEquals(Tick.RUNNING, c.tick());
	}

	@Test
	void secondRunCancels() {
		QuitCountdown c = new QuitCountdown();
		assertTrue(c.toggle(), "first run starts");
		for (int i = 0; i < 37; i++) {
			c.tick();
		}
		assertFalse(c.toggle(), "second run cancels");
		assertFalse(c.active());
		assertEquals(0, c.ticksLeft());
	}

	@Test
	void nothingFiresAfterCancel() {
		QuitCountdown c = new QuitCountdown();
		c.start();
		for (int i = 0; i < 100; i++) {
			c.tick();
		}
		assertTrue(c.cancel());
		assertFalse(c.cancel(), "nothing left to cancel");
		for (int i = 0; i < 500; i++) {
			assertEquals(Tick.IDLE, c.tick());
		}
	}

	@Test
	void canStartAgainAfterCancelOrFire() {
		QuitCountdown c = new QuitCountdown();
		c.start();
		c.cancel();
		assertTrue(c.start());
		assertEquals(200, c.ticksLeft());
		Tick last = Tick.IDLE;
		for (int i = 0; i < 200; i++) {
			last = c.tick();
		}
		assertEquals(Tick.FIRE, last);
		assertTrue(c.start());
	}
}
