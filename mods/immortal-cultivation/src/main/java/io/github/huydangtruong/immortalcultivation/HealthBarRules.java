package io.github.huydangtruong.immortalcultivation;

/**
 * D203: the health row is one heart plus a progress bar, whatever the max health. The heart stands for the health level
 * (layer, see HeartLayers: layer 0 = vanilla red, then one layer per 20 hearts); the bar is current health / max health.
 * Heart + gap + bar are exactly as wide as the armour bar (10 icons, 8 apart, 9 wide = 81 px). Pure Java.
 */
public final class HealthBarRules {
	public static final int ICON = 9;
	public static final int GAP = 2;
	/** Same as the armour bar: 9 steps of 8 plus one 9 px icon. */
	public static final int WIDTH = 9 * 8 + 9;
	public static final int BAR_WIDTH = WIDTH - ICON - GAP;
	public static final int BAR_HEIGHT = 7;
	/** Pixels of the absorption strip, drawn along the top inside of the bar. */
	public static final int ABSORPTION_ROWS = 2;
	/** Bar colours (ARGB) per layer: the main colour of each heart sprite (tools/make_heart_sprites.py). */
	private static final int[] LAYER_COLORS = {
		0xFFE02424, // 0 vanilla red
		0xFFFF8A1C, 0xFFFF4FC4, 0xFFA64DFF, 0xFF3B6BFF, // 1-4
		0xFFFF7A6B, 0xFFFFB06B, 0xFFFFA3DD, 0xFFC9A3FF, 0xFF5EE8B5, // 5-9
		0xFFFFB7D5, 0xFFB04AD6, 0xFF3FE03F, 0xFF3FF2FF, 0xFFE8264A, // 10-14
	};
	public static final int POISON_COLOR = 0xFF8A9A1E;
	public static final int WITHER_COLOR = 0xFF3A3A3A;
	public static final int FROZEN_COLOR = 0xFF8FD3F4;
	public static final int ABSORPTION_COLOR = 0xFFFFD040;

	private HealthBarRules() {}

	/** The layer the current health reaches (0 up to HeartLayers.LAYER_HP points, then one more per 40 points), at most the last colour layer. */
	public static int layer(int health) {
		if (health <= 0) {
			return 0;
		}
		return Math.min(LAYER_COLORS.length - 1, (health - 1) / HeartLayers.LAYER_HP);
	}

	/** D216: heart and bar always show the layer of the FULL health (max health), whatever health is left. */
	public static int rowLayer(float maxHealth) {
		return layer((int) Math.ceil(maxHealth));
	}

	public static int layerColor(int layer) {
		return LAYER_COLORS[Math.max(0, Math.min(LAYER_COLORS.length - 1, layer))];
	}

	/** Fraction of the bar filled: health / max health, 0..1. */
	public static float fill(float health, float maxHealth) {
		if (maxHealth <= 0 || health <= 0) {
			return 0f;
		}
		return Math.min(1f, health / maxHealth);
	}

	/** Pixels of the inner width that are filled; any health above 0 shows at least one pixel. */
	public static int fillPixels(float health, float maxHealth, int innerWidth) {
		float f = fill(health, maxHealth);
		if (f <= 0f) {
			return 0;
		}
		return Math.max(1, Math.min(innerWidth, Math.round(f * innerWidth)));
	}

	/** Pixels of the golden absorption strip: the absorption as a share of max health, up to the full bar. */
	public static int absorptionPixels(float absorption, float maxHealth, int innerWidth) {
		return fillPixels(absorption, maxHealth, innerWidth);
	}

	/**
	 * Pixels to move the armour bar DOWN: vanilla puts it above all the heart rows it would draw for this much health plus
	 * absorption (10 per row, tighter above 2 rows), the new health row is one row, so the armour sits right above it.
	 */
	public static int armorShift(float maxHealth, int health, int absorption) {
		float vanillaMax = Math.max(maxHealth, health);
		int vanillaRows = (int) Math.ceil((vanillaMax + absorption) / 2f / HeartLayers.PER_ROW);
		return (vanillaRows - 1) * HeartLayers.rowHeight(vanillaRows);
	}
}
