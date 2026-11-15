package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.warp.Warp;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The Cosmic Warping menu (D75): "Portals" (oldest first, each with an X that removes it) then "Players". Two-line rows
 * (name, then coordinates and dimension); a click on a row warps there. Scrolls with the wheel. Drawn with fills in
 * the same dark-and-gold look as the skill bar. The server checks every request again.
 */
public class WarpScreen extends Screen {
	private static final int PANEL_W = 230;
	private static final int ROW_H = 26;
	private static final int HEADER_H = 16;
	private static final int TOP = 28; // title and header text
	private static final int BOTTOM = 8;
	private static final int X_SIZE = 16;
	private static final int GOLD = 0xFF9A7A20;
	private static final int PANEL = 0xF0101018;

	private Warp.Menu menu;
	private long receivedAt;
	private double scroll;

	/** One drawn line: a section header or a row. */
	private record Line(String header, Warp.Row row, boolean portal) {}

	public WarpScreen(Warp.Menu menu) {
		super(Component.literal("Cosmic Warping"));
		update(menu);
	}

	/** Shows fresh data (after a removal, the server sends the list again). */
	public void update(Warp.Menu m) {
		this.menu = m;
		Minecraft mc = Minecraft.getInstance();
		this.receivedAt = mc.level == null ? 0 : mc.level.getGameTime();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private long cooldownLeft() {
		Minecraft mc = Minecraft.getInstance();
		long now = mc.level == null ? receivedAt : mc.level.getGameTime();
		return Math.max(0, menu.cooldownLeft() - (now - receivedAt));
	}

	private List<Line> lines() {
		List<Line> out = new ArrayList<>();
		out.add(new Line("Portals", null, false));
		for (Warp.Row r : menu.portals()) {
			out.add(new Line(null, r, true));
		}
		out.add(new Line("Players", null, false));
		for (Warp.Row r : menu.players()) {
			out.add(new Line(null, r, false));
		}
		return out;
	}

	private static int heightOf(Line l) {
		return l.row() == null ? HEADER_H : ROW_H;
	}

	private int panelLeft() {
		return (width - PANEL_W) / 2;
	}

	private int panelTop() {
		return Math.max(8, (height - panelHeight()) / 2);
	}

	private int panelHeight() {
		return Math.min(height - 16, 230);
	}

	private int listTop() {
		return panelTop() + TOP;
	}

	private int listHeight() {
		return panelHeight() - TOP - BOTTOM;
	}

	private int contentHeight() {
		int h = 0;
		for (Line l : lines()) {
			h += heightOf(l);
		}
		return h;
	}

	private double maxScroll() {
		return Math.max(0, contentHeight() - listHeight());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int left = panelLeft(), top = panelTop(), w = PANEL_W, h = panelHeight();
		g.fill(left, top, left + w, top + h, PANEL);
		g.outline(left, top, w, h, GOLD);
		long cd = cooldownLeft();
		String portals = "Portals " + menu.portals().size() + " / " + menu.max();
		g.text(font, Component.literal("Cosmic Warping"), left + 8, top + 6, 0xFFFFFFFF, true);
		g.text(font, portals, left + 8, top + 17, 0xFFC8B8FF);
		Component cool = Component.literal(cd > 0 ? "Cooldown " + (cd + 19) / 20 + " s" : "Ready");
		g.text(font, cool, left + w - 8 - font.width(cool), top + 17, cd > 0 ? 0xFFFF8080 : 0xFF80FF80);
		g.enableScissor(left + 1, listTop(), left + w - 1, listTop() + listHeight());
		int y = listTop() - (int) scroll;
		for (Line l : lines()) {
			int lh = heightOf(l);
			if (y + lh > listTop() && y < listTop() + listHeight()) {
				drawLine(g, l, left, y, mouseX, mouseY);
			}
			y += lh;
		}
		g.disableScissor();
		if (maxScroll() > 0) { // slim scroll bar
			int barH = Math.max(12, (int) (listHeight() * (listHeight() / (double) contentHeight())));
			int barY = listTop() + (int) ((listHeight() - barH) * (scroll / maxScroll()));
			g.fill(left + w - 4, barY, left + w - 2, barY + barH, 0xFF8A7A50);
		}
	}

	private void drawLine(GuiGraphicsExtractor g, Line l, int left, int y, int mouseX, int mouseY) {
		if (l.row() == null) {
			g.text(font, l.header(), left + 8, y + 5, 0xFFE8D090, true);
			g.fill(left + 8, y + HEADER_H - 2, left + PANEL_W - 8, y + HEADER_H - 1, 0xFF5A4A20);
			return;
		}
		Warp.Row r = l.row();
		int x0 = left + 6, x1 = left + PANEL_W - 8;
		boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_H - 2 && inList(mouseY);
		g.fill(x0, y, x1, y + ROW_H - 2, hover ? 0xFF2A2A40 : 0xFF1A1A28);
		g.fill(x0, y, x0 + 3, y + ROW_H - 2, 0xFF000000 | r.color());
		g.text(font, r.name(), x0 + 8, y + 4, 0xFFFFFFFF, true);
		g.text(font, r.info(), x0 + 8, y + 15, 0xFFA0A0B0, false);
		if (l.portal()) {
			int bx = x1 - X_SIZE - 3, by = y + (ROW_H - 2 - X_SIZE) / 2;
			boolean overX = mouseX >= bx && mouseX < bx + X_SIZE && mouseY >= by && mouseY < by + X_SIZE && inList(mouseY);
			g.fill(bx, by, bx + X_SIZE, by + X_SIZE, overX ? 0xFF8A2020 : 0xFF4A1818);
			g.outline(bx, by, X_SIZE, X_SIZE, 0xFFB04040);
			g.centeredText(font, "X", bx + X_SIZE / 2, by + 4, 0xFFFFFFFF);
		}
	}

	private boolean inList(int mouseY) {
		return mouseY >= listTop() && mouseY < listTop() + listHeight();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 14));
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (!inList((int) my)) {
			return super.mouseClicked(event, doubleClick);
		}
		int left = panelLeft();
		int y = listTop() - (int) scroll;
		for (Line l : lines()) {
			int lh = heightOf(l);
			if (l.row() != null && my >= y && my < y + ROW_H - 2) {
				int x0 = left + 6, x1 = left + PANEL_W - 8;
				if (mx >= x0 && mx < x1) {
					click(l, mx, x1, y);
				}
				return true;
			}
			y += lh;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void click(Line l, double mx, int x1, int y) {
		Warp.Row r = l.row();
		int bx = x1 - X_SIZE - 3;
		if (l.portal() && mx >= bx && mx < bx + X_SIZE) {
			ClientPlayNetworking.send(new Warp.Request(Warp.DELETE, r.id()));
			return;
		}
		ClientPlayNetworking.send(new Warp.Request(l.portal() ? Warp.GO_PORTAL : Warp.GO_PLAYER, r.id()));
		if (cooldownLeft() == 0) {
			onClose(); // the menu closes when the warp happens; on cooldown it stays and shows the refusal
		}
	}
}
