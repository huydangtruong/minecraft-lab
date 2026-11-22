package io.github.huydangtruong.immortalcultivation.technique;

/** Pure rules of the technique wheel slots (D193). No Minecraft imports. */
public final class WheelLogic {
	private WheelLogic() {}

	/**
	 * Slot to open the wheel on when none is selected: the lowest open slot that is ready (holds a usable technique that is
	 * not cooling down); when none is ready, slot 0; -1 when no slot is open at all.
	 *
	 * @param open    number of open slots
	 * @param usable  per slot: holds a technique of level 1 or more
	 * @param cooling per slot: that technique is on cooldown
	 */
	public static int defaultSlot(int open, boolean[] usable, boolean[] cooling) {
		if (open <= 0) {
			return -1;
		}
		for (int i = 0; i < open && i < usable.length; i++) {
			if (usable[i] && !(i < cooling.length && cooling[i])) {
				return i;
			}
		}
		return 0;
	}

	/** Next (delta > 0) or previous (delta < 0) open slot, wrapping around. A current slot outside the open ones starts at 0. */
	public static int step(int current, int delta, int open) {
		if (open <= 0) {
			return -1;
		}
		if (current < 0 || current >= open) {
			return 0;
		}
		return Math.floorMod(current + Integer.signum(delta), open);
	}
}
