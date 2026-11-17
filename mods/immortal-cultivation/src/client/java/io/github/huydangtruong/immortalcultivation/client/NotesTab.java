package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.notes.NoteRules;
import io.github.huydangtruong.immortalcultivation.notes.NoteRules.Note;
import io.github.huydangtruong.immortalcultivation.notes.NotesNet;
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
 * Notes page of the profile panel (P-042, D117): side tabs Shared / Private (like the Cultivating tabs), a Delete toggle (an X button on your own rows), notes wrapped in their author's colour, the list
 * (newest on top, mouse wheel scrolls) and, at the bottom, a text box (32 characters) with a Send button. Everything is
 * validated by the server; the lists arrive through {@link NotesNet.Sync}.
 */
public final class NotesTab {
	private static final int TEXT = 0xFF404040;
	private static final int BASE = 0xFF707070;
	private static final int RED = 0xFFC02020;
	private static final int ROW = 11;
	private static final int SCROLL_STEP = 11;

	// Layout, relative to the panel's top-left.
	private static final int TAB_H = 13; // Delete button height
	/** Side tabs: the same size and sprites as the Cultivating panel's edge tabs. */
	private static final int SIDE_OUT = 32;
	private static final int SIDE_W = 35;
	private static final int SIDE_H = 27;
	private static final int LIST_TOP = 22;
	private static final int X_W = 11;
	private static net.minecraft.world.item.ItemStack sharedIcon;
	private static net.minecraft.world.item.ItemStack privateIcon;
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

	/** Test hook: delete mode on or off. */
	public static void setDeleteMode(boolean on) {
		deleteMode = on;
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

	private static int textWidth(int w) {
		return w - 14 - 4 - (deleteMode ? X_W + 1 : 0);
	}

	/** The wrapped lines of every note of the current list (a note is as many rows as it needs). */
	private static List<List<net.minecraft.util.FormattedCharSequence>> wrapped(Font font, int w) {
		List<List<net.minecraft.util.FormattedCharSequence>> out = new java.util.ArrayList<>();
		for (Note n : current()) {
			out.add(font.split(Component.literal(n.text()), textWidth(w)));
		}
		return out;
	}

	private static int totalHeight(List<List<net.minecraft.util.FormattedCharSequence>> rows) {
		int t = 0;
		for (var r : rows) {
			t += Math.max(1, r.size()) * ROW + 2;
		}
		return t;
	}

	private static int maxScroll(Font font, int w, int h) {
		return Math.max(0, totalHeight(wrapped(font, w)) + 2 - listH(h));
	}

	/** The author's colour: the one their Cosmic Warping portals use (opaque). */
	static int colorOf(UUID author) {
		return 0xFF000000 | io.github.huydangtruong.immortalcultivation.technique.warp.WarpColor.rgb(author);
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
		// Shared / Private: side tabs on the panel edge
		drawSideTab(g, x0, y0, 0, sharedTab, sharedIcon());
		drawSideTab(g, x0, y0, 1, !sharedTab, privateIcon());
		List<Note> list = current();
		int count = NoteRules.countBy(list, me());
		String c = count + "/" + NoteRules.limit(sharedTab);
		g.text(font, c, right - DEL_W - 5 - font.width(c), y0 + 8, count >= NoteRules.limit(sharedTab) ? RED : BASE, false);

		// list
		int top = y0 + LIST_TOP;
		int viewH = listH(h);
		var rows = wrapped(font, w);
		int max = Math.max(0, totalHeight(rows) + 2 - viewH);
		scroll = Math.max(0, Math.min(max, scroll));
		g.fill(tx - 1, top - 1, right + 1, top + viewH + 1, 0xFF8B8B8B);
		g.fill(tx, top, right, top + viewH, 0xFF303030);
		g.enableScissor(tx, top, right, top + viewH);
		UUID me = me();
		int y = top - scroll + 2;
		for (int i = 0; i < list.size(); i++) {
			Note n = list.get(i);
			var lines = rows.get(i);
			int rowH = Math.max(1, lines.size()) * ROW;
			if (y + rowH >= top && y <= top + viewH) {
				int color = colorOf(n.author());
				for (int l = 0; l < lines.size(); l++) {
					g.text(font, lines.get(l), tx + 2, y + l * ROW + 1, color, false);
				}
				if (deleteMode && NoteRules.canDelete(n, me)) {
					drawX(g, font, right - X_W - 1, y, mx, my);
				}
			}
			y += rowH + 2;
		}
		if (list.isEmpty()) {
			g.text(font, "No notes yet", tx + 2, top + 3, 0xFFB0B0B0, false);
		}
		g.disableScissor();
		if (max > 0) {
			int barH = Math.max(8, viewH * viewH / (viewH + max));
			int barY = top + (viewH - barH) * scroll / max;
			g.fill(right - 2, barY, right, barY + barH, 0xFFB0B0B0);
		}
		// Send, bottom right (the text box is a real widget)
		drawButton(g, font, right - SEND_W, y0 + h - 24, SEND_W, BOX_H, "Send", false, mx, my);
	}

	private static net.minecraft.world.item.ItemStack sharedIcon() {
		if (sharedIcon == null) { // items are not bound when this class loads
			sharedIcon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
		}
		return sharedIcon;
	}

	private static net.minecraft.world.item.ItemStack privateIcon() {
		if (privateIcon == null) {
			privateIcon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_CHEST);
		}
		return privateIcon;
	}

	private static void drawSideTab(GuiGraphicsExtractor g, int x0, int y0, int index, boolean selected, net.minecraft.world.item.ItemStack icon) {
		int x = x0 - SIDE_OUT + 2 - (selected ? 2 : 0);
		int y = y0 + 3 + index * SIDE_H;
		g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
			net.minecraft.resources.Identifier.withDefaultNamespace(selected ? "recipe_book/tab_selected" : "recipe_book/tab"), x, y, SIDE_W, SIDE_H);
		g.fakeItem(icon, x + 9, y + 5);
	}

	/** The delete button of a row: bordered, lighter on hover. */
	private static void drawX(GuiGraphicsExtractor g, Font font, int x, int y, int mx, int my) {
		boolean hover = in(mx, my, x, y, X_W, ROW);
		g.fill(x, y, x + X_W, y + ROW, 0xFF000000);
		g.fill(x + 1, y + 1, x + X_W - 1, y + ROW - 1, hover ? 0xFFE05050 : 0xFF8B2020);
		g.text(font, "X", x + (X_W - font.width("X")) / 2 + 1, y + 2, 0xFFFFFFFF, false);
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
		for (int i = 0; i < 2; i++) {
			if (!in(mx, my, x0 - SIDE_OUT, y0 + 3 + i * SIDE_H, SIDE_OUT, SIDE_H)) {
				continue;
			}
			boolean toShared = i == 0;
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
			Font font = Minecraft.getInstance().font;
			List<Note> list = current();
			var rows = wrapped(font, w);
			int y = top - scroll + 2;
			for (int i = 0; i < list.size(); i++) {
				int rowH = Math.max(1, rows.get(i).size()) * ROW;
				if (NoteRules.canDelete(list.get(i), me()) && in(mx, my, right - X_W - 1, y, X_W, ROW)) {
					ClientPlayNetworking.send(new NotesNet.Action(NotesNet.DELETE, sharedTab, "", list.get(i).id()));
					break;
				}
				y += rowH + 2;
			}
		}
		return true;
	}

	/** Mouse wheel over the list. Returns true when it was over the list. */
	public static boolean scroll(int x0, int y0, int w, int h, double mx, double my, double amount) {
		if (!in(mx, my, x0 + 7, y0 + LIST_TOP, w - 14, listH(h))) {
			return false;
		}
		scroll = Math.max(0, Math.min(maxScroll(Minecraft.getInstance().font, w, h), scroll - (int) Math.signum(amount) * SCROLL_STEP));
		return true;
	}
}
