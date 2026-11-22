package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.passive.Passive;
import io.github.huydangtruong.immortalcultivation.passive.PassiveAttachment;
import io.github.huydangtruong.immortalcultivation.passive.PassiveData;
import io.github.huydangtruong.immortalcultivation.passive.PassiveNet;
import io.github.huydangtruong.immortalcultivation.passive.PassiveRules;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * Body of the Passives tab (P-057, D148; Crop growth P-058): seven rows (gem, title with the value, 10-segment bar, "+" for 5 XP levels)
 * and a detail page per passive (description, all 10 levels, Back). Switchable passives have an on/off switch left of "+".
 * The server checks every request (PassiveNet); this class only asks.
 */
public final class PassivesTab {
	private static final int LIST_TOP = 20;
	private static final int ROW_H = 22;
	private static final int ROW_PITCH = 23;
	private static final int SCROLL_STEP = 12;
	/** List scroll in pixels (0 = top): the rows keep their size and the list scrolls when they do not fit. */
	private static int scroll;
	private static final int PLUS = 16;
	private static final int SEG_W = 9;
	private static final int SEG_GAP = 1;
	private static final int BAR_H = 4;
	private static final int LINE = 10;

	private static final int TEXT = 0xFF404040;
	private static final int TOTAL = 0xFF202020;
	private static final int BASE = 0xFF707070;
	private static final int GOLD = 0xFFFFC800;
	private static final int XP_GREEN = 0xFF80FF20;
	private static final int BAR_BACK = 0xFF373737;

	/** Gems drawn pixel by pixel, one shape each (D148): O outline, W white glint, L light, M mid, D dark face. */
	private static final String[] DIAMOND = { // brilliant cut
		"  OOOOOOOO  ",
		" OWLLMMMDDO ",
		"OWLLLMMMDDDO",
		"OLLLLMMMMDDO",
		" OLLLMMDDDO ",
		"  OLLMMDDO  ",
		"   OLMMDO   ",
		"    OMDO    ",
		"     OO     ",
	};
	private static final String[] SAPPHIRE = { // cushion / oval
		"   OOOOOO   ",
		"  OWWLLLMO  ",
		" OWLLLLMMMO ",
		"OWLLLMMMMMDO",
		"OLLLMMMMMMDO",
		"OLLMMMMMMDDO",
		"OLMMMMMMMDDO",
		"OLMMMMMMDDDO",
		" OMMMMMDDDO ",
		"  OMMDDDDO  ",
		"   OOOOOO   ",
	};
	private static final String[] AMETHYST = { // tall pointed crystal
		"   OO   ",
		"  OWLO  ",
		" OWLLMO ",
		"OWLLMMDO",
		"OLLLMMDO",
		"OLLMMMDO",
		"OLLMMDDO",
		"OLLMMDDO",
		"OLLMMDDO",
		"OLMMMDDO",
		"OLMMDDDO",
		" OMMDDO ",
		"  OMDO  ",
		"   OO   ",
	};
	private static final String[] AMBER = { // drop
		"    OO    ",
		"   OLLO   ",
		"   OLMO   ",
		"  OWLMMO  ",
		"  OLLMMO  ",
		" OWLLMMDO ",
		" OLLLMMDO ",
		"OWLLMMMDDO",
		"OLLLMMMDDO",
		"OLLMMMDDDO",
		" OLMMMDDO ",
		"  OMMDDO  ",
		"   OOOO   ",
	};
	private static final String[] TOPAZ = { // step-cut rectangle
		"  OOOOOOOO  ",
		" OWLLLLLLMO ",
		"OWLLLLLLLMDO",
		"OLLMMMMMMLDO",
		"OLLMWLLLMLDO",
		"OLLMLLLLMLDO",
		"OLLMMMMMMLDO",
		"OLMMMMMMMMDO",
		" OMDDDDDDDO ",
		"  OOOOOOOO  ",
	};

	private static final String[] EMERALD = { // hexagonal cut
		"   OOOOOO   ",
		"  OWLLLMMO  ",
		" OWLLLMMMDO ",
		"OWLLLMMMMDDO",
		"OLLLMMMMMDDO",
		"OLLMMMMMDDDO",
		" OLMMMMDDDO ",
		"  OMMMDDDO  ",
		"   OOOOOO   ",
	};

	private static final String[] RUBY = { // inverted triangle (trillion cut)
		"OOOOOOOOOOOO",
		"OWLLLMMMMDDO",
		" OWLLMMMDDO ",
		"  OLLMMMDO  ",
		"   OLMMDO   ",
		"    OMDO    ",
		"     OO     ",
	};

	private static String[] shape(Passive p) {
		return switch (p) {
			case COOLDOWN -> SAPPHIRE;
			case VISION -> AMETHYST;
			case HUNGER -> AMBER;
			case XP -> TOPAZ;
			case GROWTH -> EMERALD;
			case INVENTORY -> DIAMOND;
			case GUARD -> RUBY;
		};
	}

	/** {light, mid, dark, outline} per passive (D148 colours). */
	private static int[] colors(Passive p) {
		return switch (p) {
			case COOLDOWN -> new int[] {0xFF6FA8FF, 0xFF2F6BE0, 0xFF1A3C9A, 0xFF0E2260}; // sapphire
			case VISION -> new int[] {0xFFD9A6FF, 0xFFA35BE0, 0xFF6B2FA6, 0xFF3F1766}; // amethyst
			case HUNGER -> new int[] {0xFFFFC27A, 0xFFF08A1E, 0xFFB55A0A, 0xFF6A3305}; // amber
			case XP -> new int[] {0xFFFFF09A, 0xFFF2CF2A, 0xFFB8920E, 0xFF6B5406}; // topaz
			case GROWTH -> new int[] {0xFF7CF29A, 0xFF2DBE5A, 0xFF178A3A, 0xFF0B4F20}; // emerald
			case INVENTORY -> new int[] {0xFFE6FFFF, 0xFF9FE8F0, 0xFF4FB7C8, 0xFF1F6C78}; // diamond
			case GUARD -> new int[] {0xFFFF9AA0, 0xFFE0243A, 0xFF9A1226, 0xFF5A0814}; // ruby
		};
	}

	/** Passive whose detail page is open, null for the list. */
	private static Passive detail;

	private PassivesTab() {}

	public static Passive detail() {
		return detail;
	}

	private static String description(Passive p) {
		return switch (p) {
			case COOLDOWN -> "Every technique cools down faster, on top of its own level.";
			case VISION -> "See in the dark like Night Vision, with no effect icon. Above 100%: brighter, clearer under water, weaker Darkness. Use the switch next to + to turn it on or off.";
			case HUNGER -> "Your food bar never drops below this many drumsticks. From level 3 you can always sprint.";
			case XP -> "More experience from every XP orb you pick up.";
			case GROWTH -> "Crops you or your clones plant grow faster (wheat, carrots, potatoes, beetroots, nether wart, cocoa, pumpkin and melon stems) while their area is loaded. Use the switch next to + to turn it on or off.";
			case INVENTORY -> "Items in these slots stay with you when you die, also in Death Match. Curse of Vanishing still destroys them; XP still drops.";
			case GUARD -> "Gear at 0 durability is never destroyed: it stops working until repaired. More wear builds a debt, at most 100% of its durability at Lv 1, 10% at Lv 10. Repair (anvil or Mending) pays the debt first. Switch next to + turns it on or off.";
		};
	}

	private static PassiveData data() {
		var p = Minecraft.getInstance().player;
		return p == null ? PassiveData.DEFAULT : PassiveAttachment.get(p);
	}

	private static int xpLevels() {
		var p = Minecraft.getInstance().player;
		return p == null ? 0 : p.experienceLevel;
	}

	private static void send(int kind, Passive p) {
		ClientPlayNetworking.send(new PassiveNet.PassiveAction(kind, p.id()));
	}

	// ---- geometry ----

	private static int rowY(int y0, int i) {
		return y0 + LIST_TOP + i * ROW_PITCH - scroll;
	}

	/** Height of the scrolling list area: from LIST_TOP down to the panel's bottom padding. */
	private static int listH(int h) {
		return h - 7 - LIST_TOP;
	}

	private static int maxScroll(int h) {
		return Math.max(0, Passive.values().length * ROW_PITCH - 2 - listH(h));
	}

	private static int[] gemRect(int tx, int rowY) {
		return new int[] {tx + 1, rowY + (ROW_H - 16) / 2, 16, 16};
	}

	/** The on/off switch, left of "+" (only for passives that can be switched off). */
	private static final int SWITCH_W = 18;
	private static final int SWITCH_H = 10;

	private static boolean toggleable(Passive p) {
		return p.toggleable();
	}

	private static boolean isOn(PassiveData d, Passive p) {
		return d.isOn(p);
	}

	private static int[] switchRect(int right, int rowY) {
		int[] plus = plusRect(right, rowY);
		return new int[] {plus[0] - 3 - SWITCH_W, rowY + (ROW_H - SWITCH_H) / 2, SWITCH_W, SWITCH_H};
	}

	/** Track green with the knob right when on; grey with the knob left when off; dim when not learned. */
	private static void drawSwitch(GuiGraphicsExtractor g, int[] r, boolean on, boolean enabled, boolean hover) {
		int x = r[0];
		int y = r[1];
		g.fill(x, y, x + r[2], y + r[3], 0xFF373737);
		int track = !enabled ? 0xFF6B6B6B : on ? (hover ? 0xFF5CC85C : 0xFF3FA83F) : (hover ? 0xFF9A9A9A : 0xFF7A7A7A);
		g.fill(x + 1, y + 1, x + r[2] - 1, y + r[3] - 1, track);
		int kx = on ? x + r[2] - 1 - 8 : x + 1;
		g.fill(kx, y + 1, kx + 8, y + r[3] - 1, enabled ? 0xFFF0F0F0 : 0xFFA0A0A0);
		g.fill(kx, y + r[3] - 2, kx + 8, y + r[3] - 1, enabled ? 0xFFB0B0B0 : 0xFF808080);
	}

	private static int[] plusRect(int right, int rowY) {
		return new int[] {right - 2 - PLUS, rowY + (ROW_H - PLUS) / 2, PLUS, PLUS};
	}

	private static int[] backRect(int tx, int y0) {
		return new int[] {tx, y0 + 19, 36, 16};
	}

	private static boolean in(double mx, double my, int[] r) {
		return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
	}

	private static boolean canUpgrade(PassiveData d, Passive p) {
		return d.level(p) < PassiveRules.MAX_LEVEL && xpLevels() >= PassiveRules.COST_LEVELS;
	}

	// ---- drawing ----

	public static void draw(GuiGraphicsExtractor g, Font font, int x0, int y0, int w, int h, int mx, int my) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		PassiveData d = data();
		if (detail == null) {
			drawList(g, font, d, tx, right, y0, h, mx, my);
		} else {
			drawDetail(g, font, d, detail, tx, right, y0, mx, my);
		}
	}

	/** The passive's own gem, centred in a 16 x 16 box; grey when off. */
	static void gem(GuiGraphicsExtractor g, Passive p, int x, int y, boolean grey) {
		int[] c = colors(p);
		String[] mask = shape(p);
		int ox = x + (16 - mask[0].length()) / 2;
		int oy = y + (16 - mask.length) / 2;
		for (int row = 0; row < mask.length; row++) {
			String line = mask[row];
			for (int col = 0; col < line.length(); col++) {
				int color = switch (line.charAt(col)) {
					case 'O' -> c[3];
					case 'W' -> 0xFFFFFFFF;
					case 'L' -> c[0];
					case 'M' -> c[1];
					case 'D' -> c[2];
					default -> 0;
				};
				if (color != 0) {
					int px = ox + col;
					int py = oy + row;
					g.fill(px, py, px + 1, py + 1, grey ? grey(color) : color);
				}
			}
		}
	}

	private static int grey(int argb) {
		int r = argb >> 16 & 0xFF;
		int gr = argb >> 8 & 0xFF;
		int b = argb & 0xFF;
		int v = (r * 30 + gr * 59 + b * 11) / 100;
		v = 60 + v * 100 / 255; // dull and dark
		return 0xFF000000 | v << 16 | v << 8 | v;
	}

	/** 10 segments, one per level, vanilla XP green when reached. */
	private static void segments(GuiGraphicsExtractor g, int x, int y, int level) {
		for (int i = 0; i < PassiveRules.MAX_LEVEL; i++) {
			int sx = x + i * (SEG_W + SEG_GAP);
			g.fill(sx, y, sx + SEG_W, y + BAR_H, i < level ? XP_GREEN : BAR_BACK);
		}
	}

	private static void drawList(GuiGraphicsExtractor g, Font font, PassiveData d, int tx, int right, int y0, int h, int mx, int my) {
		Passive[] all = Passive.values();
		int xp = xpLevels();
		int viewTop = y0 + LIST_TOP;
		int viewH = listH(h);
		scroll = Math.max(0, Math.min(maxScroll(h), scroll));
		boolean inView = my >= viewTop && my < viewTop + viewH;
		g.enableScissor(tx, viewTop, right, viewTop + viewH);
		for (int i = 0; i < all.length; i++) {
			Passive p = all[i];
			int y = rowY(y0, i);
			if (y + ROW_H < viewTop || y > viewTop + viewH) {
				continue;
			}
			int level = d.level(p);
			int[] row = {tx, y, right - tx, ROW_H};
			int[] gem = gemRect(tx, y);
			int[] plus = plusRect(right, y);
			boolean hoverRow = inView && in(mx, my, row);
			boolean hoverPlus = inView && in(mx, my, plus);
			int[] sw = switchRect(right, y);
			boolean hasSwitch = toggleable(p);
			boolean hoverSwitch = inView && hasSwitch && in(mx, my, sw);
			g.fill(tx, y, right, y + ROW_H, hoverRow && !hoverPlus && !hoverSwitch ? 0xFFBDBDBD : 0xFFABABAB);
			g.outline(tx, y, right - tx, ROW_H, 0xFF8B8B8B);
			gem(g, p, gem[0], gem[1], level == 0);
			if (hasSwitch) {
				drawSwitch(g, sw, isOn(d, p), level >= 1, hoverSwitch && level >= 1);
			}
			String title = PassiveRules.title(p, level);
			int maxW = (hasSwitch ? sw[0] : plus[0]) - 1 - (tx + 20);
			if (font.width(title) > maxW) {
				title = font.plainSubstrByWidth(title, maxW - font.width("..")) + "..";
			}
			g.text(font, title, tx + 20, y + 3, level > 0 ? TOTAL : BASE, false);
			segments(g, tx + 20, y + 14, level);
			boolean up = level < PassiveRules.MAX_LEVEL && xp >= PassiveRules.COST_LEVELS;
			String sprite = !up ? "widget/button_disabled" : hoverPlus ? "widget/button_highlighted" : "widget/button";
			g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(sprite), plus[0], plus[1], PLUS, PLUS);
			g.text(font, "+", plus[0] + (PLUS - font.width("+")) / 2 + 1, plus[1] + 4, up ? 0xFFFFFFFF : 0xFFA0A0A0, true);
			if (hoverPlus) {
				String tip = level >= PassiveRules.MAX_LEVEL ? "Max level"
					: xp < PassiveRules.COST_LEVELS ? "Needs " + PassiveRules.COST_LEVELS + " XP levels"
					: "Upgrade to Lv " + (level + 1) + ": " + PassiveRules.value(p, level + 1) + " (" + PassiveRules.COST_LEVELS + " levels)";
				g.setTooltipForNextFrame(font, Component.literal(tip), mx, my);
			} else if (hoverSwitch) {
				String tip = level < 1 ? "Learn it first" : isOn(d, p) ? "On: click to turn off" : "Off: click to turn on";
				g.setTooltipForNextFrame(font, Component.literal(tip), mx, my);
			} else if (hoverRow) {
				g.setTooltipForNextFrame(font, Component.literal("Lv " + level + " / " + PassiveRules.MAX_LEVEL + ": click for details"), mx, my);
			}
		}
		g.disableScissor();
		int max = maxScroll(h);
		if (max > 0) {
			int content = all.length * ROW_PITCH - 2;
			int barH = Math.max(8, viewH * viewH / content);
			int barY = viewTop + (viewH - barH) * scroll / max;
			g.fill(right + 1, barY, right + 3, barY + barH, 0xFF555555);
		}
	}

	private static void drawDetail(GuiGraphicsExtractor g, Font font, PassiveData d, Passive p, int tx, int right, int y0, int mx, int my) {
		int level = d.level(p);
		int[] bk = backRect(tx, y0);
		boolean hb = in(mx, my, bk);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(hb ? "widget/button_highlighted" : "widget/button"), bk[0], bk[1], bk[2], bk[3]);
		g.text(font, "Back", bk[0] + (bk[2] - font.width("Back")) / 2, bk[1] + 4, 0xFFFFFFFF, true);
		gem(g, p, tx + 40, bk[1], level == 0);
		Component name = Component.literal(p.displayName()).withStyle(ChatFormatting.BOLD);
		g.text(font, name, tx + 60, bk[1] + 4, TOTAL, false);

		int y = bk[1] + 22;
		for (FormattedCharSequence line : font.split(Component.literal(description(p)).withStyle(ChatFormatting.ITALIC), right - tx)) {
			g.text(font, line, tx, y, TEXT, false);
			y += LINE;
		}
		y += 4;
		// the 10 levels in two columns, the current one highlighted
		int col2 = 92; // the left column holds the longest values ("+Main hand")
		for (int l = 1; l <= PassiveRules.MAX_LEVEL; l++) {
			int col = (l - 1) / 5;
			int x = tx + col * col2;
			int colW = col == 0 ? col2 : right - tx - col2;
			int ly = y + (l - 1) % 5 * LINE;
			boolean cur = l == level;
			if (cur) {
				g.fill(x - 1, ly - 1, x + colW - 2, ly + LINE - 1, 0xFFE8D27A);
			}
			int color = cur ? TOTAL : l < level ? TEXT : BASE;
			String lv = "Lv " + l;
			g.text(font, cur ? Component.literal(lv).withStyle(ChatFormatting.BOLD) : Component.literal(lv), x + 1, ly, color, false);
			g.text(font, PassiveRules.tableValue(p, l), x + (col == 0 ? 30 : 36), ly, color, false);
		}
	}

	// ---- input ----

	/** A mouse click on the panel body. Returns true when it was handled (the screen must not see it). */
	public static boolean click(int x0, int y0, int w, int h, double mx, double my, int button) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		PassiveData d = data();
		if (detail != null) {
			if (in(mx, my, backRect(tx, y0))) {
				detail = null;
				return true;
			}
			return false;
		}
		if (!in(mx, my, new int[] {tx, y0 + LIST_TOP, right - tx, listH(h)})) {
			return false; // a row scrolled out of view cannot be clicked
		}
		Passive[] all = Passive.values();
		for (int i = 0; i < all.length; i++) {
			Passive p = all[i];
			int y = rowY(y0, i);
			if (!in(mx, my, new int[] {tx, y, right - tx, ROW_H})) {
				continue;
			}
			if (in(mx, my, plusRect(right, y))) {
				if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && canUpgrade(d, p)) {
					send(PassiveNet.UPGRADE, p); // one click spends 5 levels, no confirm (D148)
				}
			} else if (toggleable(p) && in(mx, my, switchRect(right, y))) {
				if (d.level(p) >= 1) {
					send(PassiveNet.TOGGLE, p);
				}
			} else {
				detail = p;
			}
			return true;
		}
		return false;
	}

	/** Mouse wheel over the list (not on a detail page). Returns true when it was handled. */
	public static boolean scroll(int x0, int y0, int w, int h, double mx, double my, double amount) {
		if (detail != null || !in(mx, my, new int[] {x0 + 7, y0 + LIST_TOP, w - 14, listH(h)})) {
			return false;
		}
		scroll = Math.max(0, Math.min(maxScroll(h), scroll - (int) Math.signum(amount) * SCROLL_STEP));
		return true;
	}

	// ---- tests ----

	/** Scroll the list to the top (false) or the bottom (true) for the client test; clamped on the next draw or hotspots call. */
	public static void scrollTo(boolean end) {
		scroll = end ? Integer.MAX_VALUE : 0;
	}

	/** GUI centres for the client test: row titles, "+" buttons, on/off switches (one per passive, at the current scroll), then Back. */
	public static List<int[]> hotspots(int x0, int y0, int w, int h) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		scroll = Math.max(0, Math.min(maxScroll(h), scroll));
		List<int[]> out = new ArrayList<>();
		int n = Passive.values().length;
		for (int i = 0; i < n; i++) {
			out.add(new int[] {tx + 60, rowY(y0, i) + 8});
		}
		for (int i = 0; i < n; i++) {
			int[] r = plusRect(right, rowY(y0, i));
			out.add(new int[] {r[0] + PLUS / 2, r[1] + PLUS / 2});
		}
		for (int i = 0; i < n; i++) {
			int[] r = switchRect(right, rowY(y0, i));
			out.add(new int[] {r[0] + SWITCH_W / 2, r[1] + SWITCH_H / 2});
		}
		int[] bk = backRect(tx, y0);
		out.add(new int[] {bk[0] + bk[2] / 2, bk[1] + bk[3] / 2});
		return out;
	}
}
