package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueNet;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Body of the Techniques tab (D43): a row of 4 slot boxes and the emerald box, the list of techniques and a detail page
 * (numbers now and at the next level, Upgrade, Reset, Back). Drawn and clicked by hand inside the cultivation panel;
 * the server checks every request (TechniqueNet), this class only asks.
 */
public final class TechniquesTab {
	private static final int SLOT = 20;
	private static final int SLOT_PITCH = 24;
	private static final int ROW_H = 22;
	private static final int ROW_PITCH = 24;
	private static final int LIST_TOP = 44;
	/** Pixels the list moves per mouse wheel notch. */
	private static final int SCROLL_STEP = 12;
	private static final int PLUS = 18;
	private static final int LINE = 10;

	private static final int TEXT = 0xFF404040;
	private static final int TOTAL = 0xFF202020;
	private static final int BASE = 0xFF707070;
	private static final int BONUS = 0xFFC85A00;
	private static final int GOLD = 0xFFFFC800;
	private static final int GOOD = 0xFF1E8A2E;

	private static final ItemStack EMERALD = new ItemStack(Items.EMERALD);

	/** Selected slot (gold outline), -1 for none. */
	private static int selectedSlot = -1;
	/** Technique whose detail page is open, null for the list. */
	private static Technique detail;
	/** List scroll in pixels (0 = top). */
	private static int scroll;

	private TechniquesTab() {}

	public static int selectedSlot() {
		return selectedSlot;
	}

	public static Technique detail() {
		return detail;
	}

	public static Identifier icon(Technique t) {
		return Identifier.fromNamespaceAndPath("permahearts", "technique/" + t.id());
	}

	static Identifier sprite(String name) {
		return Identifier.fromNamespaceAndPath("permahearts", "technique/" + name);
	}

	private static String description(Technique t) {
		return switch (t) {
			case FIRE -> "Charge and throw a fire orb.";
			case ICE -> "Charge and throw a frost orb.";
			case HEALING -> "A circle that heals everything.";
			case PHASE -> "Walk through blocks for a while.";
			case REPULSE -> "Blast everything around away.";
			case SACRIFICE -> "Health for invulnerability.";
		};
	}

	private static TechniqueData data() {
		var p = Minecraft.getInstance().player;
		return p == null ? TechniqueData.DEFAULT : p.getAttachedOrElse(TechniqueAttachment.TECHNIQUES, TechniqueData.DEFAULT);
	}

	private static long now() {
		var mc = Minecraft.getInstance();
		return mc.level == null ? 0 : mc.level.getGameTime();
	}

	private static int xpLevels() {
		var p = Minecraft.getInstance().player;
		return p == null ? 0 : p.experienceLevel;
	}

	private static void send(int kind, String technique, int slot) {
		ClientPlayNetworking.send(new TechniqueNet.TechniqueAction(kind, technique, slot));
	}

	// ---- geometry (relative to the panel's top-left, tx = content left) ----

	private static int slotX(int tx, int i) {
		return tx + i * SLOT_PITCH;
	}

	private static int emeraldX(int tx) {
		return tx + 4 * SLOT_PITCH + 4;
	}

	private static int rowY(int y0, int i) {
		return y0 + LIST_TOP + i * ROW_PITCH - scroll;
	}

	/** Height of the scrolling list area: from LIST_TOP down to the panel's bottom padding. */
	private static int listH(int h) {
		return h - 7 - LIST_TOP;
	}

	private static int maxScroll(int h) {
		return Math.max(0, Technique.values().length * ROW_PITCH - 2 - listH(h));
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Detail page button rects: {x, y, w, h}. Back is the first thing of the header line (Back, icon, name). */
	private static int[] upgradeRect(int tx, int right, int y0, int h) {
		return new int[] {tx, y0 + h - 7 - 14, 104, 14};
	}

	private static int[] resetRect(int tx, int right, int y0, int h) {
		return new int[] {tx + 104, y0 + h - 7 - 14, right - tx - 104, 14};
	}

	private static int[] backRect(int tx, int right, int y0, int h) {
		return new int[] {tx, y0 + 19, 36, 16};
	}

	// ---- drawing ----

	public static void draw(GuiGraphicsExtractor g, Font font, int x0, int y0, int w, int h, int mx, int my) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		TechniqueData d = data();
		long now = now();
		if (selectedSlot >= d.slotsOpen()) {
			selectedSlot = -1;
		}
		if (detail == null) {
			drawSlots(g, font, d, now, tx, y0 + 19, mx, my);
			drawList(g, font, d, tx, right, y0, h, mx, my);
		} else {
			drawDetail(g, font, d, now, detail, tx, right, y0, h, mx, my);
		}
	}

	private static void box(GuiGraphicsExtractor g, int x, int y, int face) {
		g.fill(x, y, x + SLOT, y + SLOT, 0xFF373737);
		g.fill(x + 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
		g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, face);
	}

	private static void drawSlots(GuiGraphicsExtractor g, Font font, TechniqueData d, long now, int tx, int y, int mx, int my) {
		int open = d.slotsOpen();
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			int x = slotX(tx, i);
			boolean locked = i >= open;
			box(g, x, y, locked ? 0xFF555555 : 0xFF8B8B8B);
			Technique t = locked ? null : d.inSlot(i);
			if (locked) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("lock"), x + 2, y + 2, 16, 16);
			} else if (t != null) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, icon(t), x + 2, y + 2, 16, 16);
				if (d.onCooldown(t, now)) {
					g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xB0303030);
					String s = String.valueOf((d.cooldownLeft(t, now) + 19) / 20);
					g.text(font, s, x + (SLOT - font.width(s)) / 2, y + 6, 0xFFFFFFFF, true);
				}
			}
			if (i == selectedSlot) {
				g.outline(x - 1, y - 1, SLOT + 2, SLOT + 2, GOLD);
				g.outline(x, y, SLOT, SLOT, GOLD);
			}
			if (in(mx, my, x, y, SLOT, SLOT)) {
				boolean cd = t != null && d.onCooldown(t, now);
				Component tip = locked ? Component.literal("Locked: deposit emeralds")
					: t == null ? Component.literal("Slot " + (i + 1) + ": empty (click to put a technique in it)")
					: cd ? Component.literal(t.displayName() + " is on cooldown: " + (d.cooldownLeft(t, now) + 19) / 20 + " s")
					: Component.literal("Slot " + (i + 1) + ": " + t.displayName() + " (click to change, right click clears)");
				g.setTooltipForNextFrame(font, tip, mx, my);
			}
		}
		int ex = emeraldX(tx);
		boolean editing = selectedSlot >= 0;
		box(g, ex, y, editing && in(mx, my, ex, y, SLOT, SLOT) ? 0xFFA8A8A8 : 0xFF8B8B8B);
		if (editing) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("cross"), ex + 2, y + 2, 16, 16);
			if (in(mx, my, ex, y, SLOT, SLOT)) {
				g.setTooltipForNextFrame(font, Component.literal("Cancel"), mx, my);
			}
			return;
		}
		g.fakeItem(EMERALD, ex + 2, y + 2);
		int missing = TechniqueRules.missingForNextSlot(d.emeralds());
		int step = TechniqueRules.nextSlotStep(d.emeralds());
		String label = step == 0 ? "All open" : (step - missing) + "/" + step;
		g.text(font, label, ex + SLOT + 4, y + 6, step == 0 ? GOOD : TEXT, false);
		if (in(mx, my, ex, y, SLOT, SLOT)) {
			Component tip = step == 0 ? Component.literal("All 4 slots are open")
				: Component.literal("Click with emeralds on the cursor: " + (step - missing) + "/" + step + " for slot " + (d.slotsOpen() + 1));
			g.setTooltipForNextFrame(font, tip, mx, my);
		}
	}

	private static void drawList(GuiGraphicsExtractor g, Font font, TechniqueData d, int tx, int right, int y0, int h, int mx, int my) {
		Technique[] all = Technique.values();
		int open = d.slotsOpen();
		boolean assign = selectedSlot >= 0 && selectedSlot < open;
		int viewTop = y0 + LIST_TOP;
		int viewH = listH(h);
		scroll = Math.max(0, Math.min(maxScroll(h), scroll));
		g.enableScissor(tx, viewTop, right, viewTop + viewH);
		for (int i = 0; i < all.length; i++) {
			Technique t = all[i];
			int y = rowY(y0, i);
			if (y + ROW_H < viewTop || y > viewTop + viewH) {
				continue;
			}
			boolean inView = my >= viewTop && my < viewTop + viewH;
			boolean hover = in(mx, my, tx, y, right - tx, ROW_H) && inView;
			int level = d.level(t);
			g.fill(tx, y, right, y + ROW_H, hover ? 0xFFBDBDBD : 0xFFABABAB);
			g.outline(tx, y, right - tx, ROW_H, 0xFF8B8B8B);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, icon(t), tx + 3, y + 3, 16, 16);
			g.text(font, t.displayName(), tx + 24, y + 7, level > 0 ? TOTAL : BASE, false);
			boolean showPlus = assign && level >= 1 && d.slotOf(t) != selectedSlot;
			String lv = "Lv " + level;
			int lvRight = right - (showPlus ? PLUS + 6 : 4);
			g.text(font, lv, lvRight - font.width(lv), y + 7, level > 0 ? BONUS : BASE, false);
			if (showPlus) {
				int[] c = plusRect(right, y);
				boolean can = canAssign(d, t);
				boolean ch = can && in(mx, my, c[0], c[1], c[2], c[3]) && inView;
				g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(!can ? "widget/button_disabled" : ch ? "widget/button_highlighted" : "widget/button"),
					c[0], c[1], c[2], c[3]);
				g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("plus"), c[0] + 1, c[1] + 1, 16, 16);
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

	private static int[] plusRect(int right, int rowY) {
		return new int[] {right - PLUS - 2, rowY + 2, PLUS, PLUS};
	}

	private static boolean canAssign(TechniqueData d, Technique t) {
		return selectedSlot >= 0 && d.level(t) >= 1 && d.slotOf(t) != selectedSlot && !d.slotOnCooldown(selectedSlot, now());
	}

	private static void drawDetail(GuiGraphicsExtractor g, Font font, TechniqueData d, long now, Technique t, int tx, int right, int y0, int h, int mx, int my) {
		int level = d.level(t);
		int[] bk = backRect(tx, right, y0, h);
		int y = bk[1];
		g.blitSprite(RenderPipelines.GUI_TEXTURED, icon(t), tx + 40, y, 16, 16);
		g.text(font, Component.literal(t.displayName()).withStyle(ChatFormatting.BOLD), tx + 60, y, TOTAL, false);
		String lv = "Lv " + level + " / " + TechniqueRules.MAX_LEVEL;
		g.text(font, lv, tx + 60, y + 9, level > 0 ? BONUS : BASE, false);
		y += 24;
		List<FormattedCharSequence> lines = font.split(Component.literal(description(t)), right - tx);
		if (!lines.isEmpty()) {
			g.text(font, lines.get(0), tx, y, TEXT, false);
			y += LINE;
		}
		y += 2;
		int shown = Math.max(1, level);
		boolean max = level >= TechniqueRules.MAX_LEVEL;
		boolean compare = level >= 1 && !max;
		List<TechniqueRules.Stat> now1 = TechniqueRules.stats(t, shown);
		List<TechniqueRules.Stat> next = compare ? TechniqueRules.stats(t, level + 1) : now1;
		int labelW = 0;
		for (TechniqueRules.Stat s : now1) {
			labelW = Math.max(labelW, font.width(s.label()));
		}
		labelW += 6;
		int vx = tx + labelW;
		for (int i = 0; i < now1.size(); i++) {
			String cur = now1.get(i).value();
			String nxt = next.get(i).value();
			g.text(font, now1.get(i).label(), tx, y, TEXT, false);
			int avail = right - vx;
			if (!compare || cur.equals(nxt)) {
				int color = level > 0 ? TOTAL : BASE;
				List<FormattedCharSequence> parts = font.split(Component.literal(cur), avail);
				for (int k = 0; k < Math.min(2, parts.size()); k++) {
					g.text(font, parts.get(k), vx, y, color, false);
					if (k == 0 && parts.size() > 1) {
						y += LINE;
					}
				}
			} else {
				// "16 blocks" -> "18 blocks" reads better as "16 > 18 blocks": the words they share go once, at the end
				String[] a = cur.split(" ");
				String[] b = nxt.split(" ");
				int common = 0;
				while (common < a.length - 1 && common < b.length - 1 && a[a.length - 1 - common].equals(b[b.length - 1 - common])
					&& !Character.isDigit(a[a.length - 1 - common].charAt(0))) {
					common++;
				}
				String curShort = String.join(" ", java.util.Arrays.copyOfRange(a, 0, a.length - common));
				String nxtShort = String.join(" ", java.util.Arrays.copyOfRange(b, 0, b.length - common));
				String tail = common == 0 ? "" : " " + String.join(" ", java.util.Arrays.copyOfRange(a, a.length - common, a.length));
				int arrow = font.width("\u2192") + 6;
				if (font.width(curShort) + arrow + font.width(nxtShort + tail) <= avail) {
					int x = vx;
					g.text(font, curShort, x, y, BASE, false);
					x += font.width(curShort) + 3;
					g.text(font, "\u2192", x, y, BASE, false);
					x += font.width("\u2192") + 3;
					g.text(font, nxtShort + tail, x, y, BONUS, false);
				} else {
					g.text(font, cur, vx, y, BASE, false);
					y += LINE;
					g.text(font, "\u2192 " + nxt, vx, y, BONUS, false);
				}
			}
			y += LINE;
		}
		// buttons
		int xp = xpLevels();
		int[] up = upgradeRect(tx, right, y0, h);
		int[] re = resetRect(tx, right, y0, h);
		String upLabel;
		boolean canUp;
		if (max) {
			upLabel = "Max level";
			canUp = false;
		} else {
			int cost = TechniqueRules.upgradeCost(level + 1);
			upLabel = (level == 0 ? "Learn (" : "Upgrade (") + cost + " levels)";
			canUp = xp >= cost;
		}
		button(g, font, up, upLabel, canUp, mx, my);
		boolean canReset = level > 0 && !d.onCooldown(t, now);
		button(g, font, re, "Reset +" + TechniqueRules.refund(d.spent(t)) + " XP", canReset, mx, my);
		button(g, font, bk, "Back", true, mx, my);
	}

	private static void button(GuiGraphicsExtractor g, Font font, int[] r, String label, boolean enabled, int mx, int my) {
		boolean hover = enabled && in(mx, my, r[0], r[1], r[2], r[3]);
		String sprite = !enabled ? "widget/button_disabled" : hover ? "widget/button_highlighted" : "widget/button";
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(sprite), r[0], r[1], r[2], r[3]);
		g.text(font, label, r[0] + (r[2] - font.width(label)) / 2, r[1] + (r[3] - 8) / 2, enabled ? 0xFFFFFFFF : 0xFFA0A0A0, true);
	}

	// ---- input ----

	/** A mouse click on the panel body. Returns true when it was handled (the screen must not see it). */
	public static boolean click(int x0, int y0, int w, int h, double mx, double my, int button) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		TechniqueData d = data();
		if (detail != null) {
			return clickDetail(d, detail, tx, right, y0, h, mx, my);
		}
		int slotY = y0 + 19;
		long now = now();
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			if (in(mx, my, slotX(tx, i), slotY, SLOT, SLOT)) {
				if (i >= d.slotsOpen() || d.slotOnCooldown(i, now)) {
					return true; // locked, or its technique is cooling down: nothing to do
				}
				if (button == 1) {
					if (d.inSlot(i) != null) {
						send(TechniqueNet.CLEAR_SLOT, "", i);
					}
				} else {
					selectedSlot = selectedSlot == i ? -1 : i;
				}
				return true;
			}
		}
		if (in(mx, my, emeraldX(tx), slotY, SLOT, SLOT)) {
			if (selectedSlot >= 0) {
				selectedSlot = -1; // the cross cancels edit mode
				return true;
			}
			var p = Minecraft.getInstance().player;
			if (p != null && p.containerMenu.getCarried().is(Items.EMERALD)) {
				send(TechniqueNet.DEPOSIT, "", 0);
			}
			return true;
		}
		return clickList(d, tx, right, y0, h, mx, my);
	}

	private static boolean clickList(TechniqueData d, int tx, int right, int y0, int h, double mx, double my) {
		Technique[] all = Technique.values();
		boolean assign = selectedSlot >= 0 && selectedSlot < d.slotsOpen();
		if (!in(mx, my, tx, y0 + LIST_TOP, right - tx, listH(h))) {
			return false;
		}
		for (int i = 0; i < all.length; i++) {
			int y = rowY(y0, i);
			if (!in(mx, my, tx, y, right - tx, ROW_H)) {
				continue;
			}
			int[] c = plusRect(right, y);
			if (assign && d.level(all[i]) >= 1 && d.slotOf(all[i]) != selectedSlot && in(mx, my, c[0], c[1], c[2], c[3])) {
				if (canAssign(d, all[i])) {
					send(TechniqueNet.ASSIGN, all[i].id(), selectedSlot);
					selectedSlot = -1; // edit mode ends after a successful +
				}
			} else {
				detail = all[i];
				selectedSlot = -1;
			}
			return true;
		}
		return false;
	}

	private static boolean clickDetail(TechniqueData d, Technique t, int tx, int right, int y0, int h, double mx, double my) {
		int[] up = upgradeRect(tx, right, y0, h);
		int[] re = resetRect(tx, right, y0, h);
		int[] bk = backRect(tx, right, y0, h);
		int level = d.level(t);
		if (in(mx, my, up[0], up[1], up[2], up[3])) {
			if (level < TechniqueRules.MAX_LEVEL && xpLevels() >= TechniqueRules.upgradeCost(level + 1)) {
				send(TechniqueNet.UPGRADE, t.id(), 0);
			}
			return true;
		}
		if (in(mx, my, re[0], re[1], re[2], re[3])) {
			if (level > 0 && !d.onCooldown(t, now())) {
				send(TechniqueNet.RESET, t.id(), 0);
			}
			return true;
		}
		if (in(mx, my, bk[0], bk[1], bk[2], bk[3])) {
			detail = null;
			return true;
		}
		return false;
	}

	/** Mouse wheel over the list. Returns true when it was over the list (scrolled or not). */
	public static boolean scroll(int x0, int y0, int w, int h, double mx, double my, double amount) {
		if (detail != null || !in(mx, my, x0 + 7, y0 + LIST_TOP, w - 14, listH(h))) {
			return false;
		}
		scroll = Math.max(0, Math.min(maxScroll(h), scroll - (int) Math.signum(amount) * SCROLL_STEP));
		return true;
	}

	// ---- tests ----

	/** Centres of the clickable things in GUI coordinates, for the client test: slots 0..3, emerald box, list rows. */
	public static List<int[]> hotspots(int x0, int y0, int w, int h) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		List<int[]> out = new ArrayList<>();
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			out.add(new int[] {slotX(tx, i) + SLOT / 2, y0 + 19 + SLOT / 2});
		}
		out.add(new int[] {emeraldX(tx) + SLOT / 2, y0 + 19 + SLOT / 2});
		for (int i = 0; i < Technique.values().length; i++) {
			out.add(new int[] {tx + 40, rowY(y0, i) + ROW_H / 2});
		}
		for (int i = 0; i < Technique.values().length; i++) {
			int[] c = plusRect(right, rowY(y0, i));
			out.add(new int[] {c[0] + PLUS / 2, c[1] + PLUS / 2});
		}
		int[] up = upgradeRect(tx, right, y0, h);
		int[] re = resetRect(tx, right, y0, h);
		int[] bk = backRect(tx, right, y0, h);
		out.add(new int[] {up[0] + up[2] / 2, up[1] + 7});
		out.add(new int[] {re[0] + re[2] / 2, re[1] + 7});
		out.add(new int[] {bk[0] + bk[2] / 2, bk[1] + bk[3] / 2});
		return out;
	}
}
