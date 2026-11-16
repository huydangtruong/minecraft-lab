package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.notes.NoteRules;
import io.github.huydangtruong.permahearts.notes.NoteRules.Note;
import io.github.huydangtruong.permahearts.notes.NotesNet;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Notes page of the profile panel (P-042, D117): tabs Shared / Private, a Delete toggle (an X on your own rows), the list
 * (newest on top, mouse wheel scrolls) and, at the bottom, a text box (24 characters) with a Send button. Everything is
 * validated by the server; the lists arrive through {@link NotesNet.Sync}.
 */
public final class NotesTab {
	private static final int TEXT = 0xFF404040;
	private static final int BASE = 0xFF707070;
	private static final int RED = 0xFFC02020;
	private static final int ROW = 11;
	private static final int SCROLL_STEP = 11;

	// Layout, relative to the panel's top-left.
	private static final int TABS_Y = 20;
	private static final int TAB_H = 13;
	private static final int TAB_W = 56;
	private static final int LIST_TOP = 36;
	private static final int BOTTOM_H = 28;
	private static final int BOX_H = 14;
	private static final int SEND_W = 40;
	private static final int DEL_W = 40;

	private static List<Note> shared = List.of();
	private static List<Note> mine = List.of();
	private static boolean sharedTab = true;
	private static boolean deleteMode;
	private static int scroll;
	private static EditBox box;

	private NotesTab() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(NotesNet.Sync.TYPE, (payload, ctx) -> {
			if (payload.shared()) {
				shared = payload.notes();
			} else {
				mine = payload.notes();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			shared = List.of();
			mine = List.of();
			deleteMode = false;
			scroll = 0;
		});
	}

	/** Called by the panel when the Notes page opens: asks the server for both lists. */
	public static void opened() {
		scroll = 0;
		deleteMode = false;
		if (ClientPlayNetworking.canSend(NotesNet.Action.TYPE)) {
			ClientPlayNetworking.send(new NotesNet.Action(NotesNet.REQUEST, true, "", 0));
		}
	}

	/** The text box (one per inventory screen). */
	public static EditBox newBox() {
		box = new EditBox(Minecraft.getInstance().font, 0, 0, 100, BOX_H, Component.literal("Note"));
		box.setMaxLength(NoteRules.MAX_CHARS);
		box.setHint(Component.literal("Write a note..."));
		box.visible = false;
		return box;
	}

	public static boolean boxFocused() {
		return box != null && box.visible && box.isFocused();
	}

	public static void placeBox(int x0, int y0, int w, int h, boolean visible) {
		if (box == null) {
			return;
		}
		box.visible = visible;
		box.active = visible;
		if (!visible) {
			box.setFocused(false);
		}
		box.setX(x0 + 7);
		box.setY(y0 + h - 24);
		box.setWidth(w - 14 - SEND_W - 3);
	}

	public static boolean sharedTab() {
		return sharedTab;
	}

	public static List<Note> current() {
		return sharedTab ? shared : mine;
	}

	/** Test hook: the box text. */
	public static void setText(String text) {
		if (box != null) {
			box.setValue(text);
		}
	}

	/** Sends the box text to the server and clears the box. */
	public static void send() {
		if (box == null || box.getValue().isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new NotesNet.Action(NotesNet.ADD, sharedTab, box.getValue(), 0));
		box.setValue("");
		scroll = 0;
	}

	private static UUID me() {
		var p = Minecraft.getInstance().player;
		return p == null ? new UUID(0, 0) : p.getUUID();
	}

	private static int listH(int h) {
		return h - LIST_TOP - BOTTOM_H;
	}

	private static int maxScroll(int h) {
		return Math.max(0, current().size() * ROW - listH(h));
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---- drawing ----

	public static void draw(GuiGraphicsExtractor g, Font font, int x0, int y0, int w, int h, int mx, int my) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		// Delete toggle, top right
		drawButton(g, font, right - DEL_W, y0 + 4, DEL_W, TAB_H, deleteMode ? "Done" : "Delete", deleteMode, mx, my);
		// Shared / Private tabs
		drawButton(g, font, tx, y0 + TABS_Y, TAB_W, TAB_H, "Shared", sharedTab, mx, my);
		drawButton(g, font, tx + TAB_W + 2, y0 + TABS_Y, TAB_W, TAB_H, "Private", !sharedTab, mx, my);
		List<Note> list = current();
		int count = NoteRules.countBy(list, me());
		String c = count + "/" + NoteRules.limit(sharedTab);
		g.text(font, c, right - font.width(c), y0 + TABS_Y + 3, count >= NoteRules.limit(sharedTab) ? RED : BASE, false);

		// list
		int top = y0 + LIST_TOP;
		int viewH = listH(h);
		scroll = Math.max(0, Math.min(maxScroll(h), scroll));
		g.fill(tx - 1, top - 1, right + 1, top + viewH + 1, 0xFF8B8B8B);
		g.fill(tx, top, right, top + viewH, 0xFFDBDBDB);
		g.enableScissor(tx, top, right, top + viewH);
		UUID me = me();
		for (int i = 0; i < list.size(); i++) {
			Note n = list.get(i);
			int y = top + i * ROW - scroll + 2;
			if (y + ROW < top || y > top + viewH) {
				continue;
			}
			boolean del = deleteMode && NoteRules.canDelete(n, me);
			int textW = right - tx - 4 - (deleteMode ? 10 : 0);
			String line = sharedTab ? n.authorName() + ": " + n.text() : n.text();
			g.text(font, font.plainSubstrByWidth(line, textW), tx + 2, y, TEXT, false);
			if (del) {
				boolean hot = in(mx, my, right - 11, y - 1, 10, ROW);
				g.text(font, "X", right - 9, y, hot ? 0xFFFF5050 : RED, false);
			}
		}
		if (list.isEmpty()) {
			g.text(font, "No notes yet", tx + 2, top + 3, BASE, false);
		}
		g.disableScissor();
		int max = maxScroll(h);
		if (max > 0) {
			int barH = Math.max(8, viewH * viewH / (viewH + max));
			int barY = top + (viewH - barH) * scroll / max;
			g.fill(right - 2, barY, right, barY + barH, 0xFF707070);
		}
		// Send, bottom right (the text box is a real widget)
		drawButton(g, font, right - SEND_W, y0 + h - 24, SEND_W, BOX_H, "Send", false, mx, my);
	}

	private static void drawButton(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, boolean on, int mx, int my) {
		boolean hover = in(mx, my, x, y, w, h);
		g.fill(x, y, x + w, y + h, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, on ? 0xFFE8E8A0 : hover ? 0xFFE0E0E0 : 0xFFA8A8A8);
		g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, on ? 0xFF202020 : TEXT, false);
	}

	// ---- input ----

	/** Left click on the page. Returns true when handled (the screen should not see it). Clicks on the text box are left alone. */
	public static boolean click(int x0, int y0, int w, int h, double mx, double my, int button) {
		int tx = x0 + 7;
		int right = x0 + w - 7;
		if (in(mx, my, tx, y0 + h - 24, w - 14 - SEND_W - 3, BOX_H)) {
			return false; // the text box takes it
		}
		if (box != null) {
			box.setFocused(false);
		}
		if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) { // 26.3 numbers buttons like SDL
			return true;
		}
		if (in(mx, my, right - DEL_W, y0 + 4, DEL_W, TAB_H)) {
			deleteMode = !deleteMode;
			return true;
		}
		if (in(mx, my, tx, y0 + TABS_Y, TAB_W, TAB_H) || in(mx, my, tx + TAB_W + 2, y0 + TABS_Y, TAB_W, TAB_H)) {
			boolean toShared = mx < tx + TAB_W + 1;
			if (toShared != sharedTab) {
				sharedTab = toShared;
				scroll = 0;
			}
			return true;
		}
		if (in(mx, my, right - SEND_W, y0 + h - 24, SEND_W, BOX_H)) {
			send();
			return true;
		}
		int top = y0 + LIST_TOP;
		if (deleteMode && in(mx, my, tx, top, right - tx, listH(h))) {
			List<Note> list = current();
			int i = (int) ((my - top - 2 + scroll) / ROW);
			if (i >= 0 && i < list.size() && NoteRules.canDelete(list.get(i), me()) && mx >= right - 12) {
				ClientPlayNetworking.send(new NotesNet.Action(NotesNet.DELETE, sharedTab, "", list.get(i).id()));
			}
		}
		return true;
	}

	/** Mouse wheel over the list. Returns true when it was over the list. */
	public static boolean scroll(int x0, int y0, int w, int h, double mx, double my, double amount) {
		if (!in(mx, my, x0 + 7, y0 + LIST_TOP, w - 14, listH(h))) {
			return false;
		}
		scroll = Math.max(0, Math.min(maxScroll(h), scroll - (int) Math.signum(amount) * SCROLL_STEP));
		return true;
	}
}
