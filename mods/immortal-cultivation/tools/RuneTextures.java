import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * D278: draws the rune textures and writes their models and the item model definition. Run from the repo root:
 * {@code java mods/immortal-cultivation/tools/RuneTextures.java}. A rune is a flat-topped shield with a dithered
 * top-to-bottom gradient in its kind colour, the enchantment's glyph (7x6) at the top and the level (3x5 digits) below,
 * both centred on column 7. The number is its own transparent layer (composite model) so any level 1-99 shows.
 */
public class RuneTextures {
	static final String ASSETS = "mods/immortal-cultivation/src/main/resources/assets/permahearts/";

	/** Kind palettes: outline, light, base, shadow (D264 colours). */
	static final Map<String, int[]> KIND = new LinkedHashMap<>();
	static {
		KIND.put("attack", new int[]{0x2a1012, 0xa8585e, 0x803c44, 0x56262c});
		KIND.put("armor", new int[]{0x10182c, 0x5a78a8, 0x3e5684, 0x283a5c});
		KIND.put("ranged", new int[]{0x122a14, 0x5e9a5e, 0x427442, 0x2a502c});
		KIND.put("tool", new int[]{0x0e2626, 0x529690, 0x3a6e6a, 0x264a48});
		KIND.put("other", new int[]{0x1e1e22, 0x8a8a90, 0x66666c, 0x46464c});
	}
	static final int GLYPH = 0xff9a2e, GLYPH_HI = 0xffd27a, NUM = 0xfff4d0;

	/** Enchantment path, kind, glyph (7 wide, 6 tall). */
	static final Object[][] GLYPHS = {
		// attack
		{"sharpness", "attack", "...#...", "..###..", "..###..", "..###..", ".#####.", "...#..."},
		{"smite", "attack", "...#...", "...#...", "#######", "...#...", "...#...", "...#..."},
		{"bane_of_arthropods", "attack", "#.#.#.#", ".#####.", "#.###.#", ".#####.", "#.#.#.#", "......."},
		{"knockback", "attack", "...#...", "....#..", "######.", "....#..", "...#...", "......."},
		{"fire_aspect", "attack", "...#...", "..##...", "..###..", ".#####.", ".##.##.", "..###.."},
		{"looting", "attack", "..#.#..", "...#...", ".#####.", "##...##", "##...##", ".#####."},
		{"sweeping_edge", "attack", "....##.", "......#", "......#", ".....#.", "#..##..", ".##...."},
		{"density", "attack", ".#####.", "..###..", "..###..", ".#####.", "#######", "#######"},
		{"breach", "attack", "###.###", "##.####", "###.###", ".##.##.", "..#.#..", "...#..."},
		{"wind_burst", "attack", "####...", "....#..", ".#####.", "......#", "###..#.", "...##.."},
		// armor
		{"protection", "armor", "#######", "#.....#", "#.....#", ".#...#.", "..#.#..", "...#..."},
		{"fire_protection", "armor", "#######", "#..#..#", "#.###.#", ".#####.", "..###..", "...#..."},
		{"blast_protection", "armor", "#######", "#.#.#.#", "#..#..#", ".#.#.#.", "..#.#..", "...#..."},
		{"projectile_protection", "armor", "#######", "#..#..#", "#.###.#", ".#.#.#.", "..#.#..", "...#..."},
		{"thorns", "armor", "...#...", "..#.#..", "...#...", "#..#..#", ".#.#.#.", "...#..."},
		{"feather_falling", "armor", "....##.", "...###.", "..###..", ".###...", ".#.....", "#......"},
		{"respiration", "armor", ".##....", "#..#.#.", "#..#...", ".##..#.", "....#.#", ".....#."},
		{"depth_strider", "armor", "..#...#", "##.###.", ".......", "..#...#", "##.###.", "......."},
		{"frost_walker", "armor", "#..#..#", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#."},
		{"soul_speed", "armor", "..###..", ".#####.", ".#.#.#.", ".#####.", ".#.#.#.", "......."},
		{"swift_sneak", "armor", ".##....", "###....", ".#..##.", "...###.", "....#..", "......."},
		// ranged
		{"power", "ranged", "...#...", "..###..", "#..#..#", ".#.#.#.", "..###..", "...#..."},
		{"punch", "ranged", ".#..#..", "..#..#.", "...#..#", "..#..#.", ".#..#..", "......."},
		{"quick_charge", "ranged", "...##..", "..##...", ".####..", "...##..", "..##...", ".##...."},
		{"piercing", "ranged", "...#...", "..###..", "#######", "...#...", "...#...", "..#.#.."},
		{"loyalty", "ranged", ".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."},
		{"riptide", "ranged", ".####..", "#....#.", "#.##..#", "#.#.#.#", "#..#..#", ".###.#."},
		{"impaling", "ranged", "...#...", "..###.#", ".######", "..###.#", "...#...", "......."},
		{"lunge", "ranged", "....#..", ".....#.", "#.#####", ".....#.", "....#..", "......."},
		{"throw_power", "ranged", ".#.#.#.", ".#.#.#.", ".#####.", "...#...", "...#...", "...#..."},
		// tool
		{"efficiency", "tool", ".#####.", "#..#..#", "...#...", "...#...", "...#...", "...#..."},
		{"fortune", "tool", "..###..", ".#...#.", "#######", ".#...#.", "..#.#..", "...#..."},
		{"unbreaking", "tool", ".......", ".##.##.", "#..#..#", "#..#..#", ".##.##.", "......."},
		{"lure", "tool", "...#...", "...#...", "...#...", "#..#...", "#..#...", ".##...."},
		{"luck_of_the_sea", "tool", "..#.#..", ".##.##.", "..###..", ".##.##.", "..#.#..", "...#..."},
	};

	/** 3x5 digits. */
	static final String[] DIGITS = {"###,#.#,#.#,#.#,###", ".#.,##.,.#.,.#.,###", "###,..#,###,#..,###", "###,..#,###,..#,###",
		"#.#,#.#,###,..#,..#", "###,#..,###,..#,###", "###,#..,###,#.#,###", "###,..#,..#,.#.,.#.", "###,#.#,###,#.#,###", "###,#.#,###,..#,###"};

	static final int CENTER = 7, GLYPH_X = 4, GLYPH_Y = 2, NUM_Y = 9;

	static double sq(double v) {
		return v * v;
	}

	/** Flat top, straight sides, round bottom; 13 wide (x 1-13) so column 7 is the middle. */
	static boolean in(int x, int y) {
		return x >= 1 && x <= 13 && y >= 0 && y <= 15 && (y <= 7 || sq((x - 7) / 6.6) + sq((y - 7) / 8.6) <= 1);
	}

	static int mix(int a, int b, double t) {
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	static BufferedImage base(int[] pal) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			if (!in(x, y)) continue;
			boolean edge = !in(x - 1, y) || !in(x + 1, y) || !in(x, y - 1) || !in(x, y + 1);
			int c;
			if (edge) c = pal[0];
			else if (!in(x - 2, y) || !in(x, y - 2)) c = pal[1];
			else if (!in(x + 2, y) || !in(x, y + 2)) c = pal[3];
			else { // dithered gradient, light at the top
				int lvl = (int) (y / 15.0 * 4);
				boolean dot = (x + y) % 2 == 0;
				int lightish = mix(pal[2], pal[1], 0.35);
				c = lvl == 0 ? lightish : lvl == 1 ? (dot ? lightish : pal[2]) : lvl == 2 ? (dot ? pal[2] : pal[3]) : pal[3];
			}
			img.setRGB(x, y, 0xFF000000 | c);
		}
		return img;
	}

	static void glyph(BufferedImage img, Object[] g) {
		for (int y = 0; y < 6; y++) {
			String row = (String) g[2 + y];
			for (int x = 0; x < 7; x++) {
				if (row.charAt(x) == '#') {
					img.setRGB(GLYPH_X + x, GLYPH_Y + y, 0xFF000000 | ((x + y) % 5 == 1 ? GLYPH_HI : GLYPH));
				}
			}
		}
	}

	static BufferedImage digit(int d, int left) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		String[] rows = DIGITS[d].split(",");
		for (int y = 0; y < 5; y++) for (int x = 0; x < 3; x++) {
			if (rows[y].charAt(x) == '#') img.setRGB(left + x, NUM_Y + y, 0xFF000000 | NUM);
		}
		return img;
	}

	static void png(BufferedImage img, String path) throws IOException {
		File f = new File(ASSETS + "textures/item/" + path + ".png");
		f.getParentFile().mkdirs();
		ImageIO.write(img, "png", f);
	}

	static void model(String path) throws IOException {
		File f = new File(ASSETS + "models/item/" + path + ".json");
		f.getParentFile().mkdirs();
		Files.writeString(f.toPath(), "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"permahearts:item/" + path + "\"\n  }\n}\n");
	}

	static String ref(String path) {
		return "{\"type\": \"minecraft:model\", \"model\": \"permahearts:item/" + path + "\"}";
	}

	static String select(int index, List<String[]> cases, String fallback) {
		StringBuilder b = new StringBuilder("{\"type\": \"minecraft:select\", \"property\": \"minecraft:custom_model_data\", \"index\": " + index + ", \"cases\": [");
		for (int i = 0; i < cases.size(); i++) {
			b.append(i == 0 ? "\n" : ",\n").append("        {\"when\": \"").append(cases.get(i)[0]).append("\", \"model\": ").append(cases.get(i)[1]).append("}");
		}
		return b.append("\n      ], \"fallback\": ").append(fallback).append("}").toString();
	}

	public static void main(String[] args) throws IOException {
		// kind backgrounds (old runes and enchantments without a glyph): rune_<kind>, grey "rune"
		List<String[]> kinds = new ArrayList<>();
		for (Map.Entry<String, int[]> k : KIND.entrySet()) {
			String path = k.getKey().equals("other") ? "rune" : "rune_" + k.getKey();
			png(base(k.getValue()), path);
			model(path);
			if (!k.getKey().equals("other")) kinds.add(new String[]{k.getKey(), ref(path)});
		}
		List<String[]> glyphs = new ArrayList<>();
		for (Object[] g : GLYPHS) {
			BufferedImage img = base(KIND.get((String) g[1]));
			glyph(img, g);
			String path = "rune/" + g[0];
			png(img, path);
			model(path);
			glyphs.add(new String[]{(String) g[0], ref(path)});
		}
		// number layers: c = a lone digit in the middle, t = tens, o = ones
		List<String[]> first = new ArrayList<>(), second = new ArrayList<>();
		for (int d = 0; d <= 9; d++) {
			if (d > 0) {
				png(digit(d, CENTER - 1), "rune/digit_c" + d);
				model("rune/digit_c" + d);
				first.add(new String[]{"c" + d, ref("rune/digit_c" + d)});
				png(digit(d, CENTER - 3), "rune/digit_t" + d);
				model("rune/digit_t" + d);
				first.add(new String[]{"t" + d, ref("rune/digit_t" + d)});
			}
			png(digit(d, CENTER + 1), "rune/digit_o" + d);
			model("rune/digit_o" + d);
			second.add(new String[]{"o" + d, ref("rune/digit_o" + d)});
		}
		String empty = "{\"type\": \"minecraft:empty\"}";
		String json = "{\n  \"model\": {\"type\": \"minecraft:composite\", \"models\": [\n    "
			+ select(1, glyphs, select(0, kinds, ref("rune"))) + ",\n    "
			+ select(2, first, empty) + ",\n    "
			+ select(3, second, empty) + "\n  ]}\n}\n";
		Files.writeString(new File(ASSETS + "items/rune.json").toPath(), json);
		System.out.println("wrote " + glyphs.size() + " rune textures and the digit layers");
	}
}
