package io.github.huydangtruong.immortalcultivation.technique.warp;

import java.util.Collection;
import java.util.Locale;

/** Pure name rules of the portals (D75): readable names from ids, the " 2" / " 3" suffix and the dimension tag. */
public final class WarpNaming {
	/** Structure id prefixes that collapse to one name ("village_plains" is just a "Village"). */
	private static final String[][] GROUPS = {
		{"village", "Village"}, {"ruined_portal", "Ruined Portal"}, {"shipwreck", "Shipwreck"},
		{"ocean_ruin", "Ocean Ruin"}, {"mineshaft", "Abandoned Mineshaft"}, {"fortress", "Nether Fortress"},
		{"bastion_remnant", "Bastion Remnant"}, {"trail_ruins", "Trail Ruins"}};

	private WarpNaming() {}

	/** "minecraft:cherry_grove" or "cherry_grove" to "Cherry Grove". */
	public static String readable(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		StringBuilder out = new StringBuilder();
		for (String word : path.split("[_/]")) {
			if (word.isEmpty()) {
				continue;
			}
			if (out.length() > 0) {
				out.append(' ');
			}
			out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return out.toString();
	}

	/** Name of a structure id: variants collapse ("village_desert" to "Village"), the rest is {@link #readable}. */
	public static String structureName(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		for (String[] g : GROUPS) {
			if (path.equals(g[0]) || path.startsWith(g[0] + "_")) {
				return g[1];
			}
		}
		return readable(path);
	}

	/** The name with " 2", " 3" ... added when the owner already has a portal of that name. */
	public static String unique(String base, Collection<String> existing) {
		if (!existing.contains(base)) {
			return base;
		}
		for (int n = 2;; n++) {
			String candidate = base + " " + n;
			if (!existing.contains(candidate)) {
				return candidate;
			}
		}
	}

	/** Longest name a player may give a portal (D276). */
	public static final int MAX_NAME = 32;

	/** D276: a typed portal name: control characters dropped, spaces collapsed, trimmed, cut to {@link #MAX_NAME}. Null when nothing is left. */
	public static String clean(String typed) {
		if (typed == null) {
			return null;
		}
		String s = typed.replaceAll("\\p{Cntrl}", "").replaceAll("\\s+", " ").strip();
		if (s.length() > MAX_NAME) {
			s = s.substring(0, MAX_NAME).strip();
		}
		return s.isEmpty() ? null : s;
	}

	/** Short tag shown after the coordinates: empty for the Overworld. */
	public static String dimensionTag(String dimensionId) {
		return switch (dimensionId) {
			case "minecraft:overworld" -> "";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "End";
			default -> readable(dimensionId);
		};
	}

	/** "x, y, z" plus the dimension tag when it is not the Overworld, e.g. "12, 64, -30 (Nether)". */
	public static String coordinates(int x, int y, int z, String dimensionId) {
		String tag = dimensionTag(dimensionId);
		return x + ", " + y + ", " + z + (tag.isEmpty() ? "" : " (" + tag + ")");
	}
}
