package io.github.huydangtruong.permahearts.technique;

/** Pure maths of the skill bar: technique-mode hotbar mapping, charge, cooldown overlay. No Minecraft imports. */
public final class SkillBarLogic {

	private SkillBarLogic() {}

	/** Charge 0..1 after holding right click this many ticks (like a bow: FULL_CHARGE_SECONDS to full). */
	public static float charge(int heldTicks) {
		float c = heldTicks / (20f * TechniqueRules.FULL_CHARGE_SECONDS);
		return Math.max(0f, Math.min(1f, c));
	}

	/** Slot numbers (0..3) of the filled slots, in slot order. These become the first N hotbar cells in technique mode. */
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

	/** Technique mode: slot of the technique shown in a hotbar cell, or -1 when the cell shows an item. */
	public static int techniqueAtCell(int cell, int[] slots) {
		return cell >= 0 && cell < slots.length ? slots[cell] : -1;
	}

	/** Technique mode: real item slot shown in a hotbar cell (cell - N), or -1 when the cell shows a technique. */
	public static int itemSlotAtCell(int cell, int techniqueCount) {
		return cell >= techniqueCount ? cell - techniqueCount : -1;
	}

	/** Cell after scrolling (delta +1 = down/right) in a hotbar of 9 cells, wrapping. */
	public static int scrollCell(int cell, int delta) {
		return Math.floorMod(cell + (int) Math.signum(delta), 9);
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
