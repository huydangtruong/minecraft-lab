package io.github.huydangtruong.permahearts;

/**
 * Heart bar layout for more than 20 hearts (D36), pure Java. The bar never grows past 2 rows (20 hearts): hearts
 * 21-40 are drawn over hearts 1-20 in a second colour starting from the first heart of the first row, 41-60 in a third
 * colour, and so on (300 hearts = 15 layers, D37). Damage takes the top layer's last heart first. Health is counted in half
 * hearts (vanilla health points, rounded up).
 */
public final class HeartLayers {
	public static final int PER_ROW = 10;
	public static final int MAX_ROWS = 2;
	public static final int SLOTS = PER_ROW * MAX_ROWS;
	/** Health points one full layer holds (20 hearts). */
	public static final int LAYER_HP = SLOTS * 2;

	/**
	 * What one heart position shows: the colour layer of the heart (0 = red, 1 = second colour...; -1 = empty), whether it
	 * is only half, and for a half heart the layer shown under it (-1 = empty container).
	 */
	public record Slot(int layer, boolean half, int under) {}

	private HeartLayers() {}

	/** True when the bar needs layers: more than 20 hearts of max health. Otherwise vanilla draws it. */
	public static boolean layered(float maxHealth) {
		return maxHealth > LAYER_HP;
	}

	/** Heart positions shown for this max health (at most 20). */
	public static int slots(float maxHealth) {
		return Math.max(1, Math.min(SLOTS, (int) Math.ceil(maxHealth / 2f)));
	}

	/** Rows needed for this many positions (1 or 2). */
	public static int rows(int slots) {
		return (slots + PER_ROW - 1) / PER_ROW;
	}

	/** Absorption (golden) hearts shown in rows above: at most 2 rows. */
	public static int absorptionSlots(int absorption) {
		return Math.min(SLOTS, (absorption + 1) / 2);
	}

	/** Vanilla spacing between rows: 10 pixels, tighter when there are more than 2 rows. */
	public static int rowHeight(int totalRows) {
		return Math.max(10 - (totalRows - 2), 3);
	}

	/** Pixel height of the whole bar (health rows + absorption rows), as the status bar layout expects it. */
	public static int height(float maxHealth, int absorption) {
		int rows = rows(slots(maxHealth)) + rows(absorptionSlots(absorption));
		return 10 + rowHeight(rows) * (rows - 1);
	}

	/**
	 * Pixels to move the armour bar down: vanilla places it above all the rows it would draw for this much health
	 * (health + absorption, up to 30 rows at 300 hearts), the layered bar has at most 2 + 2.
	 */
	public static int armorShift(float maxHealth, int health, int absorption) {
		float vanillaMax = Math.max(maxHealth, health);
		int vanillaRows = (int) Math.ceil((vanillaMax + absorption) / 2f / PER_ROW);
		int vanilla = (vanillaRows - 1) * rowHeight(vanillaRows);
		int rows = rows(slots(maxHealth)) + rows(absorptionSlots(absorption));
		return vanilla - (rows - 1) * rowHeight(rows);
	}

	/** Number of colour layers used at this max health (300 hearts = 15). */
	public static int layers(float maxHealth) {
		return Math.max(1, (int) Math.ceil(maxHealth / LAYER_HP));
	}

	/** The heart positions, first heart of the first row first. health = current health points, rounded up. */
	public static Slot[] layout(float maxHealth, int health) {
		int n = slots(maxHealth);
		Slot[] out = new Slot[n];
		if (health <= 0) {
			for (int i = 0; i < n; i++) {
				out[i] = new Slot(-1, false, -1);
			}
			return out;
		}
		int top = (health - 1) / LAYER_HP; // layer of the last health point
		int inTop = health - top * LAYER_HP; // 1..40 points in that layer
		int full = inTop / 2;
		boolean half = inTop % 2 == 1;
		for (int i = 0; i < n; i++) {
			if (i < full) {
				out[i] = new Slot(top, false, -1);
			} else if (i == full && half) {
				out[i] = new Slot(top, true, top - 1);
			} else {
				out[i] = new Slot(top - 1, false, -1);
			}
		}
		return out;
	}
}
