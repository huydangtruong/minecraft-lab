package io.github.huydangtruong.immortalcultivation.technique.chrono;

import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;

/**
 * D268: where a time stop is in its recording. Counted in ticks back from the moment of the cast: 0 = the cast, up to
 * {@code usable}. The wheel moves the target by notches; the shown tick follows it at {@link TechniqueRules#CHRONO_PLAY_SPEED}
 * ticks per tick so the rewind plays smoothly. Pure Java.
 */
public final class ChronoCursor {
	private final int usable;
	private int target;
	private int shown;

	public ChronoCursor(int usable) {
		this.usable = Math.max(0, usable);
	}

	/** Usable rewind: the level's maximum, but never more than was recorded (D268: from when the technique went into a slot). */
	public static int usable(int recordedTicks, int level) {
		return Math.max(0, Math.min(recordedTicks, TechniqueRules.chronoRewindTicks(level)));
	}

	/** Notches of the wheel: positive = back in time (wheel down), negative = forward (never past the cast). */
	public void scroll(int notches) {
		target = Math.max(0, Math.min(usable, target + notches * TechniqueRules.CHRONO_NOTCH_TICKS));
	}

	/** Moves the shown tick toward the target; true when it moved. */
	public boolean step() {
		if (shown == target) {
			return false;
		}
		int d = Math.min(TechniqueRules.CHRONO_PLAY_SPEED, Math.abs(target - shown));
		shown += target > shown ? d : -d;
		return true;
	}

	public int usable() {
		return usable;
	}

	public int target() {
		return target;
	}

	public int shown() {
		return shown;
	}
}
