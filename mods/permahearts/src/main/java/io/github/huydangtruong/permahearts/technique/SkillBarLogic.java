package io.github.huydangtruong.permahearts.technique;

/** Pure maths of the skill bar: scroll ring, slot keys, charge, cooldown overlay. No Minecraft imports. */
public final class SkillBarLogic {

	private SkillBarLogic() {}

	/** Charge 0..1 after holding right click this many ticks (like a bow: FULL_CHARGE_SECONDS to full). */
	public static float charge(int heldTicks) {
		float c = heldTicks / (20f * TechniqueRules.FULL_CHARGE_SECONDS);
		return Math.max(0f, Math.min(1f, c));
	}

	/** Charge 0..1 for a technique that needs fullSeconds of holding to reach full. */
	public static float charge(int heldTicks, float fullSeconds) {
		return Math.max(0f, Math.min(1f, heldTicks / (20f * fullSeconds)));
	}

	/** Slot numbers (0..3) of the filled slots, in slot order. They form the skill bar and the technique part of the scroll ring. */
	public static int[] filledSlots(boolean[] filled) {
		int n = 0;
		for (boolean f : filled) {
			if (f) {
				n++;
			}
		}
		int[] out = new int[n];
		int k = 0;
		for (int i = 0; i < filled.length; i++) {
			if (filled[i]) {
				out[k++] = i;
			}
		}
		return out;
	}

	/** Number of hotbar slots; the scroll ring continues with the filled technique slots after them (D125). */
	public static final int HOTBAR = 9;

	/**
	 * Scroll ring (D125): positions 0..8 are the hotbar slots, 9.. are the filled technique slots in slot order, then it wraps
	 * to 0. delta +1 = next (wheel down), -1 = previous. Without filled techniques it is the plain 9-slot hotbar ring.
	 */
	public static int scrollRing(int position, int delta, int filledCount) {
		int size = HOTBAR + Math.max(0, filledCount);
		return Math.floorMod(position + Integer.signum(delta), size);
	}

	/** Ring position of a selection: the hotbar slot, or 9 + index of the technique slot among the filled slots. */
	public static int ringPosition(int hotbarSlot, int techniqueSlot, int[] filledSlots) {
		if (techniqueSlot >= 0) {
			for (int i = 0; i < filledSlots.length; i++) {
				if (filledSlots[i] == techniqueSlot) {
					return HOTBAR + i;
				}
			}
		}
		return hotbarSlot;
	}

	/** Technique slot at a ring position, or -1 when the position is a hotbar slot. */
	public static int techniqueAtRing(int position, int[] filledSlots) {
		int i = position - HOTBAR;
		return i >= 0 && i < filledSlots.length ? filledSlots[i] : -1;
	}

	/** Technique slot a slot key selects (key index 0..3 = Z X C V): the slot itself when it holds a usable technique, else -1. */
	public static int slotForKey(int keyIndex, boolean[] usable) {
		return keyIndex >= 0 && keyIndex < usable.length && usable[keyIndex] ? keyIndex : -1;
	}

	/** Share of the technique's cooldown still to run (1 right after the cast, 0 when ready). */
	public static float cooldownFraction(long ticksLeft, int totalTicks) {
		if (ticksLeft <= 0 || totalTicks <= 0) {
			return 0f;
		}
		return Math.min(1f, (float) ticksLeft / totalTicks);
	}

	/** Whole seconds to show for a cooldown, rounded up (12 for 11.2 s). */
	public static int secondsLeft(long ticksLeft) {
		return (int) ((Math.max(0, ticksLeft) + 19) / 20);
	}
}
