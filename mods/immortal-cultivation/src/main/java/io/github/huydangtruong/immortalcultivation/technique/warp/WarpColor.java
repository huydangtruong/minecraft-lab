package io.github.huydangtruong.immortalcultivation.technique.warp;

import java.util.UUID;

/** The inner colour of a player's portals (D75): a hue taken from the owner's UUID, never in the purple band of the outline. */
public final class WarpColor {
	/** Hues (degrees) the outline owns: purple and its violet and magenta neighbours. */
	public static final double PURPLE_FROM = 235, PURPLE_TO = 335;

	private WarpColor() {}

	/** Hue in degrees, 0 to 360, outside the purple band; the same UUID always gives the same hue. */
	public static double hue(UUID owner) {
		long h = owner.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ owner.getLeastSignificantBits() * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		double fraction = (h >>> 11) / (double) (1L << 53); // 0 to 1
		double span = 360 - (PURPLE_TO - PURPLE_FROM);
		return (PURPLE_TO + fraction * span) % 360; // PURPLE_TO .. 360 + PURPLE_FROM, wrapped
	}

	/** The colour as 0xRRGGBB (full-bright, saturated). */
	public static int rgb(UUID owner) {
		return hsv(hue(owner), 0.85, 1.0);
	}

	static int hsv(double hueDegrees, double s, double v) {
		double h = hueDegrees / 60;
		int i = (int) Math.floor(h) % 6;
		double f = h - Math.floor(h);
		double p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
		double r, g, b;
		switch (i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round((float) (r * 255)) << 16) | (Math.round((float) (g * 255)) << 8) | Math.round((float) (b * 255));
	}
}
