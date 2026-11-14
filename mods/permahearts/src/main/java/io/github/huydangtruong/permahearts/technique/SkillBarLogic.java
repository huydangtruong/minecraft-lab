package io.github.huydangtruong.permahearts.technique;

/** Pure maths of the skill bar: tap/hold of the skill key, charge, radial wheel, cooldown overlay. No Minecraft imports. */
public final class SkillBarLogic {
	/** A key press shorter than this is a tap (next technique); longer opens the wheel. 0.25 s. */
	public static final int TAP_TICKS = 5;
	/** Mouse distance (GUI pixels) from the wheel centre under which nothing is hovered. */
	public static final double WHEEL_DEAD_ZONE = 14;

	private SkillBarLogic() {}

	/** Charge 0..1 after holding right click this many ticks (like a bow: FULL_CHARGE_SECONDS to full). */
	public static float charge(int heldTicks) {
		float c = heldTicks / (20f * TechniqueRules.FULL_CHARGE_SECONDS);
		return Math.max(0f, Math.min(1f, c));
	}

	/** Selections the wheel shows, in order: -1 (the cross) then each filled slot. */
	public static int[] wheelEntries(boolean[] filled) {
		int n = 1;
		for (boolean f : filled) {
			if (f) {
				n++;
			}
		}
		int[] entries = new int[n];
		entries[0] = -1;
		int k = 1;
		for (int i = 0; i < filled.length; i++) {
			if (filled[i]) {
				entries[k++] = i;
			}
		}
		return entries;
	}

	/**
	 * Wheel entry under the mouse, offset (dx, dy) from the centre in screen pixels (y grows downwards), or -1 inside the
	 * dead zone. Entry 0 sits at the top, the others follow clockwise.
	 */
	public static int wheelIndex(double dx, double dy, int count) {
		if (count <= 0 || Math.hypot(dx, dy) < WHEEL_DEAD_ZONE) {
			return -1;
		}
		double angle = Math.atan2(dx, -dy); // 0 = up, clockwise positive
		if (angle < 0) {
			angle += 2 * Math.PI;
		}
		double step = 2 * Math.PI / count;
		return (int) ((angle + step / 2) / step) % count;
	}

	/** Angle (radians, 0 = up, clockwise) of entry i of count. */
	public static double wheelAngle(int i, int count) {
		return 2 * Math.PI * i / count;
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
