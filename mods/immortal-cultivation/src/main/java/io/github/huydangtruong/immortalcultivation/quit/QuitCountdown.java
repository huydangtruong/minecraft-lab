package io.github.huydangtruong.immortalcultivation.quit;

/**
 * D218: the 10-second countdown behind /quit. Pure Java (no Minecraft imports) so JUnit can test it.
 * The server calls {@link #tick()} once per server tick; running /quit again while it counts cancels it.
 */
public final class QuitCountdown {
	public static final int SECONDS = 10;
	public static final int TICKS_PER_SECOND = 20;
	public static final int TOTAL_TICKS = SECONDS * TICKS_PER_SECOND;

	/** What one tick did. */
	public enum Tick {
		/** Nothing is counting. */
		IDLE,
		/** Counting, no whole second passed. */
		RUNNING,
		/** A whole second passed; {@link #secondsLeft()} is the new number. */
		SECOND,
		/** The countdown reached zero and is over: do the final step now (once). */
		FIRE
	}

	private int ticksLeft = -1;

	public boolean active() {
		return ticksLeft >= 0;
	}

	public int ticksLeft() {
		return Math.max(ticksLeft, 0);
	}

	/** Whole seconds left, rounded up (10 right after the start, 1 in the last second). */
	public int secondsLeft() {
		return (ticksLeft() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
	}

	/** Starts the countdown from 10 s. Returns false (and changes nothing) when it already runs. */
	public boolean start() {
		if (active()) {
			return false;
		}
		ticksLeft = TOTAL_TICKS;
		return true;
	}

	/** Stops the countdown. Returns false when none was running. */
	public boolean cancel() {
		boolean was = active();
		ticksLeft = -1;
		return was;
	}

	/** Second /quit cancels, first one starts. Returns true when it started, false when it cancelled. */
	public boolean toggle() {
		if (active()) {
			cancel();
			return false;
		}
		return start();
	}

	public Tick tick() {
		if (!active()) {
			return Tick.IDLE;
		}
		ticksLeft--;
		if (ticksLeft <= 0) {
			ticksLeft = -1;
			return Tick.FIRE;
		}
		return ticksLeft % TICKS_PER_SECOND == 0 ? Tick.SECOND : Tick.RUNNING;
	}
}
