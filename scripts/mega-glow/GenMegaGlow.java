import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Builds the Mega glow textures (P-059): the vanilla mob texture with only the eyes, mouth and pattern pixels kept, in
 * the species colour (drawn with RenderTypes.eyes, like spider eyes). Run once by hand; the PNGs are committed.
 *   java scripts/mega-glow/GenMegaGlow.java <26.3.jar> <out dir> [preview dir]
 */
public class GenMegaGlow {
	interface Filter { boolean keep(int argb, int x, int y, BufferedImage other); }
	record Rule(int x0, int y0, int x1, int y1, Filter f, String other) {}
	record Species(String name, String texture, int color, List<Rule> rules) {}

	static int r(int c) { return c >> 16 & 255; }
	static int g(int c) { return c >> 8 & 255; }
	static int b(int c) { return c & 255; }
	static int a(int c) { return c >>> 24; }
	static double luma(int c) { return 0.299 * r(c) + 0.587 * g(c) + 0.114 * b(c); }

	static final Filter ALL = (c, x, y, o) -> true;
	static Filter dark(int t) { return (c, x, y, o) -> luma(c) < t; }
	static Filter bright(int t) { return (c, x, y, o) -> luma(c) > t; }
	static final Filter GOLD = (c, x, y, o) -> r(c) > 200 && g(c) > 130 && b(c) < 70 && r(c) >= g(c);
	static final Filter ORANGE = (c, x, y, o) -> r(c) > 170 && g(c) > 60 && g(c) < 150 && b(c) < 90;
	static final Filter TRIM = (c, x, y, o) -> r(c) > 170 && g(c) > 140 && b(c) < 140 && r(c) - b(c) > 60;
	static final Filter CYAN = (c, x, y, o) -> b(c) > 140 && g(c) > 140 && r(c) < 150;
	static final Filter GREEN = (c, x, y, o) -> g(c) > r(c) + 40 && g(c) > b(c) + 20;
	static final Filter MASK = (c, x, y, o) -> a(o.getRGB(x, y)) > 0;

	static Rule all(int x0, int y0, int x1, int y1) { return new Rule(x0, y0, x1, y1, ALL, null); }
	static Rule rule(int x0, int y0, int x1, int y1, Filter f) { return new Rule(x0, y0, x1, y1, f, null); }
	static Rule whole(Filter f) { return new Rule(0, 0, 4096, 4096, f, null); }
	static Rule mask(String other) { return new Rule(0, 0, 4096, 4096, MASK, other); }

	static final List<Rule> ILLAGER_EYES = List.of(all(9, 13, 11, 14), all(13, 13, 15, 14));
	static final List<Rule> SKELETON_EYES = List.of(all(9, 12, 11, 13), all(13, 12, 15, 13));

	static final List<Species> ALL_SPECIES = List.of(
		new Species("piglin_brute", "piglin/piglin_brute", 0xFFB020, List.of(all(10, 11, 11, 12), all(15, 11, 16, 12), whole(GOLD))),
		new Species("blaze", "blaze/blaze", 0xFF4A10, List.of(all(9, 11, 11, 12), all(13, 11, 15, 12), rule(0, 16, 8, 26, bright(215)))),
		new Species("wither_skeleton", "skeleton/wither_skeleton", 0xFF2A1A, List.of(rule(8, 8, 16, 16, dark(28)))),
		new Species("breeze", "breeze/breeze", 0x80F0FF, List.of(mask("breeze/breeze_eyes"), rule(0, 28, 32, 32, bright(200)))),
		new Species("vindicator", "illager/vindicator", 0xFF3030, concat(ILLAGER_EYES, List.of(rule(0, 44, 30, 58, bright(110))))),
		new Species("evoker", "illager/evoker", 0xFFE060, concat(ILLAGER_EYES, List.of(whole(TRIM)))),
		new Species("ravager", "illager/ravager", 0xFF6A20, List.of(all(18, 29, 22, 30), all(26, 29, 30, 30), whole(ORANGE))),
		new Species("hoglin", "hoglin/hoglin", 0xFF8A2A, List.of(all(80, 8, 82, 10), all(92, 8, 94, 10), rule(0, 12, 20, 28, bright(215)))),
		new Species("zoglin", "hoglin/zoglin", 0xA0FF40, List.of(all(80, 8, 81, 10), all(92, 8, 93, 9), rule(47, 27, 64, 36, bright(220)))),
		new Species("enderman", "enderman/enderman", 0xE070FF, List.of(mask("enderman/enderman_eyes"))),
		new Species("witch", "witch/witch", 0x60FF40, List.of(all(9, 14, 11, 15), all(13, 14, 15, 15), rule(8, 83, 13, 88, GREEN))),
		new Species("stray", "skeleton/stray", 0x90E8FF, SKELETON_EYES),
		new Species("bogged", "skeleton/bogged", 0xC0FF40, SKELETON_EYES),
		new Species("drowned", "zombie/drowned", 0x30FFE0, List.of(whole(CYAN))),
		new Species("guardian", "guardian/guardian", 0xFF9A20, List.of(all(0, 0, 14, 11), rule(24, 28, 36, 38, ORANGE))),
		new Species("elder_guardian", "guardian/guardian_elder", 0xFFE8A0, List.of(all(0, 0, 14, 11), rule(24, 28, 36, 38, ORANGE))));

	static <T> List<T> concat(List<T> a, List<T> b) {
		java.util.ArrayList<T> out = new java.util.ArrayList<>(a);
		out.addAll(b);
		return out;
	}

	static BufferedImage read(ZipFile jar, String path) throws Exception {
		return ImageIO.read(jar.getInputStream(jar.getEntry("assets/minecraft/textures/entity/" + path + ".png")));
	}

	public static void main(String[] args) throws Exception {
		try (ZipFile jar = new ZipFile(args[0])) {
			File out = new File(args[1]);
			out.mkdirs();
			for (Species s : ALL_SPECIES) {
				BufferedImage src = read(jar, s.texture());
				int w = src.getWidth(), h = src.getHeight();
				BufferedImage glow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				int count = 0;
				for (Rule rule : s.rules()) {
					BufferedImage other = rule.other() == null ? null : read(jar, rule.other());
					for (int y = Math.max(0, rule.y0()); y < Math.min(h, rule.y1()); y++) {
						for (int x = Math.max(0, rule.x0()); x < Math.min(w, rule.x1()); x++) {
							int c = src.getRGB(x, y);
							if (a(c) > 0 && rule.f().keep(c, x, y, other) && glow.getRGB(x, y) == 0) {
								glow.setRGB(x, y, 0xFF000000 | s.color());
								count++;
							}
						}
					}
				}
				ImageIO.write(glow, "png", new File(out, s.name() + ".png"));
				System.out.println(s.name() + ": " + count + " px");
				if (args.length > 2) {
					preview(src, glow, new File(args[2], s.name() + ".png"));
				}
			}
		}
	}

	static void preview(BufferedImage src, BufferedImage glow, File f) throws Exception {
		int s = Math.max(4, 640 / src.getWidth());
		BufferedImage out = new BufferedImage(src.getWidth() * s, src.getHeight() * s, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(40, 0, 40));
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		for (int y = 0; y < src.getHeight(); y++) {
			for (int x = 0; x < src.getWidth(); x++) {
				int c = glow.getRGB(x, y);
				if (a(c) == 0) {
					c = src.getRGB(x, y);
					if (a(c) == 0) {
						continue;
					}
					c = (r(c) / 4) << 16 | (g(c) / 4) << 8 | b(c) / 4;
				}
				g.setColor(new Color(c & 0xFFFFFF));
				g.fillRect(x * s, y * s, s, s);
			}
		}
		f.getParentFile().mkdirs();
		ImageIO.write(out, "png", f);
	}
}
