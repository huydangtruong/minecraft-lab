package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The Soul Grasp list (D134, D136): one block per held mob with its yellow name, a stats line (health with the bonus,
 * armor, attack; hover for the gear), distance or "far away", the mode buttons, Call and Release (a second click
 * confirms). Same dark-and-gold look as the warp menu. Refreshes itself every second; the server checks every request.
 */
public class GraspScreen extends Screen {
	private static final int PANEL_W = 300;
	private static final int ROW_H = 46;
	private static final int TOP = 30;
	private static final int BOTTOM = 8;
	private static final int ALL_H = 20;
	/** D168 "All" row buttons: x offset from the panel left, width, label. Follow, Stay, Guard, Call, Release all. */
	private static final int[] ALL_X = {30, 74, 118, 162, 206};
	private static final int[] ALL_W = {42, 42, 42, 40, 66};
	private static final String[] ALL_LABEL = {"Follow", "Stay", "Guard", "Call", "Release all"};
	private static final int GOLD = 0xFF9A7A20;
	private static final int PANEL = 0xF0101018;
	private static final String[] MODES = {"Follow", "Stay", "Guard"};

	private Grasp.Menu menu;
	private double scroll;
	private String confirm;
	private boolean confirmAll;
	private int sinceRefresh;

	public GraspScreen(Grasp.Menu menu) {
		super(Component.literal("Soul Grasp"));
		this.menu = menu;
	}

	public void update(Grasp.Menu m) {
		this.menu = m;
		this.sinceRefresh = 0;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (++sinceRefresh >= 20) {
			sinceRefresh = 0;
			ClientGrasp.openList();
		}
	}

	private int panelLeft() {
		return (width - PANEL_W) / 2;
	}

	private int panelHeight() {
		return Math.min(height - 16, 230);
	}

	private int panelTop() {
		return Math.max(8, (height - panelHeight()) / 2);
	}

	/** D168: the "All" row sits between the header and the list; no row, no mobs to command. */
	private boolean hasAllRow() {
		return !menu.rows().isEmpty();
	}

	private int allRowY() {
		return panelTop() + TOP;
	}

	private int listTop() {
		return panelTop() + TOP + (hasAllRow() ? ALL_H : 0);
	}

	private int listHeight() {
		return panelHeight() - (listTop() - panelTop()) - BOTTOM;
	}

	private double maxScroll() {
		return Math.max(0, menu.rows().size() * ROW_H - listHeight());
	}

	private boolean inList(double my) {
		return my >= listTop() && my < listTop() + listHeight();
	}

	private static String typeName(Grasp.Row r) {
		var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(r.type()));
		return Grasp.nameOf(type, r.number()).getString();
	}

	// button geometry inside a row: modes at the left, Call and Release at the right
	private static int modeX(int x0, int i) {
		return x0 + 8 + i * 46;
	}

	private static int callX(int x1) {
		return x1 - 8 - 44 - 4 - 56;
	}

	private static int releaseX(int x1) {
		return x1 - 8 - 56;
	}

	private static final int BTN_Y = 28, BTN_H = 14;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int left = panelLeft(), top = panelTop(), w = PANEL_W, h = panelHeight();
		g.fill(left, top, left + w, top + h, PANEL);
		g.outline(left, top, w, h, GOLD);
		g.text(font, Component.literal("Soul Grasp"), left + 8, top + 6, 0xFFFFFFFF, true);
		String count = menu.rows().size() + " / " + menu.max() + " mobs";
		g.text(font, count, left + 8, top + 17, 0xFFE8D090);
		if (menu.rows().isEmpty()) {
			g.text(font, "Hold right click on a mob to grasp it.", left + 8, listTop() + 6, 0xFFA0A0B0);
		}
		if (hasAllRow()) {
			drawAllRow(g, left, allRowY(), mouseX, mouseY);
		}
		g.enableScissor(left + 1, listTop(), left + w - 1, listTop() + listHeight());
		int y = listTop() - (int) scroll;
		List<Runnable> tips = new ArrayList<>();
		for (Grasp.Row r : menu.rows()) {
			if (y + ROW_H > listTop() && y < listTop() + listHeight()) {
				drawRow(g, r, left, y, mouseX, mouseY, tips);
			}
			y += ROW_H;
		}
		g.disableScissor();
		for (Runnable t : tips) {
			t.run();
		}
		if (maxScroll() > 0) {
			int barH = Math.max(12, (int) (listHeight() * (listHeight() / (double) (menu.rows().size() * ROW_H))));
			int barY = listTop() + (int) ((listHeight() - barH) * (scroll / maxScroll()));
			g.fill(left + w - 4, barY, left + w - 2, barY + barH, 0xFF8A7A50);
		}
	}

	private void drawAllRow(GuiGraphicsExtractor g, int left, int y, int mouseX, int mouseY) {
		g.text(font, Component.literal("All"), left + 8, y + 3, 0xFFFFD24A, true);
		for (int i = 0; i < ALL_X.length; i++) {
			int bx = left + ALL_X[i];
			boolean release = i == ALL_X.length - 1;
			String text = release && confirmAll ? "Sure?" : ALL_LABEL[i];
			button(g, bx, y, ALL_W[i], text, release && confirmAll, over(mouseX, mouseY, bx, y, ALL_W[i], BTN_H), 0xFF8A2020);
		}
	}

	private void clickAll(int mx, int my) {
		int left = panelLeft(), y = allRowY();
		for (int i = 0; i < ALL_X.length; i++) {
			if (!over(mx, my, left + ALL_X[i], y, ALL_W[i], BTN_H)) {
				continue;
			}
			boolean release = i == ALL_X.length - 1;
			confirm = null;
			if (i < 3) {
				confirmAll = false;
				ClientPlayNetworking.send(new Grasp.Request(Grasp.ALL_MODE, "", i));
			} else if (i == 3) {
				confirmAll = false;
				ClientPlayNetworking.send(new Grasp.Request(Grasp.ALL_CALL, "", 0));
			} else if (release && confirmAll) {
				confirmAll = false;
				ClientPlayNetworking.send(new Grasp.Request(Grasp.ALL_RELEASE, "", 0));
			} else {
				confirmAll = true;
			}
			return;
		}
		confirmAll = false;
	}

	private void button(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean on, boolean hover, int onColor) {
		g.fill(x, y, x + w, y + BTN_H, on ? onColor : hover ? 0xFF3A3A58 : 0xFF2A2A40);
		g.outline(x, y, w, BTN_H, on ? 0xFFE8D090 : 0xFF5A5A70);
		g.centeredText(font, text, x + w / 2, y + 3, 0xFFFFFFFF);
	}

	private static boolean over(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void drawRow(GuiGraphicsExtractor g, Grasp.Row r, int left, int y, int mouseX, int mouseY, List<Runnable> tips) {
		int x0 = left + 6, x1 = left + PANEL_W - 8;
		g.fill(x0, y, x1, y + ROW_H - 3, 0xFF1A1A28);
		g.fill(x0, y, x0 + 3, y + ROW_H - 3, 0xFFFFD24A);
		g.text(font, Component.literal(typeName(r)).withStyle(net.minecraft.ChatFormatting.YELLOW), x0 + 8, y + 4, 0xFFFFFF55, true);
		String where = !r.loaded() ? "far away" : r.dist() < 0 ? r.dim() : Math.round(r.dist()) + " blocks";
		g.text(font, where, x1 - 6 - font.width(where), y + 4, r.loaded() ? 0xFFA0A0B0 : 0xFF707080, false);
		// D136: [heart] N   [armor] N   [sword] N, icon before each number, details on hover
		int sx = x0 + 8, sy = y + 15;
		String hearts = Integer.toString((int) Math.ceil(r.hp() / 2f));
		String armor = Integer.toString(Math.round(r.armor()));
		String dmg = r.damage() == Math.rint(r.damage()) ? Integer.toString((int) r.damage()) : String.format(java.util.Locale.ROOT, "%.1f", r.damage());
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/heart/full"), sx, sy, 9, 9);
		g.text(font, hearts, sx + 11, sy + 1, 0xFFD0D0E0, false);
		int heartEnd = sx + 11 + font.width(hearts);
		int ax = heartEnd + 10;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/armor_full"), ax, sy, 9, 9);
		g.text(font, armor, ax + 11, sy + 1, 0xFFD0D0E0, false);
		int armorEnd = ax + 11 + font.width(armor);
		int wx = armorEnd + 10;
		// The item renders at 16 px: scale it to 9 px so it matches the heart and armor sprites
		g.pose().pushMatrix();
		g.pose().translate(wx, sy);
		g.pose().scale(9f / 16f, 9f / 16f);
		g.item(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD), 0, 0);
		g.pose().popMatrix();
		g.text(font, dmg, wx + 11, sy + 1, 0xFFD0D0E0, false);
		int dmgEnd = wx + 11 + font.width(dmg);
		if (inList(mouseY) && mouseY >= sy - 2 && mouseY < sy + 11) {
			List<Component> lines = null;
			if (mouseX >= sx && mouseX < heartEnd) {
				lines = List.of(Component.literal("Health " + Math.round(r.hp()) + " / " + Math.round(r.maxHp()) + " HP (bonus +" + Math.round(r.bonus()) + ")"));
			} else if (mouseX >= ax && mouseX < armorEnd) {
				lines = new ArrayList<>();
				if (r.armorGear().isEmpty()) {
					lines.add(Component.literal("No armor"));
				} else {
					for (String s : r.armorGear().split("\n")) {
						lines.add(Component.literal(s));
					}
				}
			} else if (mouseX >= wx && mouseX < dmgEnd) {
				lines = List.of(Component.literal(r.weaponGear().isEmpty() ? "Bare hands" : r.weaponGear().split("\n")[0]));
			}
			if (lines != null) {
				List<Component> shown = lines;
				tips.add(() -> g.setComponentTooltipForNextFrame(font, shown, mouseX, mouseY));
			}
		}
		int by = y + BTN_Y;
		for (int i = 0; i < MODES.length; i++) {
			int bx = modeX(x0, i);
			button(g, bx, by, 42, MODES[i], r.mode() == i, over(mouseX, mouseY, bx, by, 42, BTN_H) && inList(mouseY), 0xFF6A5A18);
		}
		int cx = callX(x1), rx = releaseX(x1);
		boolean callWait = r.callLeft() > 0;
		button(g, cx, by, 44, callWait ? "Call " + (r.callLeft() + 19) / 20 : "Call", false, over(mouseX, mouseY, cx, by, 44, BTN_H) && inList(mouseY), 0xFF2A2A40);
		boolean sure = r.id().equals(confirm);
		button(g, rx, by, 56, sure ? "Sure?" : "Release", sure, over(mouseX, mouseY, rx, by, 56, BTN_H) && inList(mouseY), 0xFF8A2020);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 14));
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (hasAllRow() && my >= allRowY() && my < allRowY() + ALL_H) {
			clickAll((int) mx, (int) my);
			return true;
		}
		if (!inList(my)) {
			return super.mouseClicked(event, doubleClick);
		}
		int left = panelLeft();
		int y = listTop() - (int) scroll;
		for (Grasp.Row r : menu.rows()) {
			if (my >= y && my < y + ROW_H - 3) {
				click(r, (int) mx, (int) my, left + 6, left + PANEL_W - 8, y);
				return true;
			}
			y += ROW_H;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void click(Grasp.Row r, int mx, int my, int x0, int x1, int y) {
		confirmAll = false;
		int by = y + BTN_Y;
		for (int i = 0; i < MODES.length; i++) {
			if (over(mx, my, modeX(x0, i), by, 42, BTN_H)) {
				confirm = null;
				ClientPlayNetworking.send(new Grasp.Request(Grasp.MODE, r.id(), i));
				return;
			}
		}
		if (over(mx, my, callX(x1), by, 44, BTN_H)) {
			confirm = null;
			ClientPlayNetworking.send(new Grasp.Request(Grasp.CALL, r.id(), 0));
		} else if (over(mx, my, releaseX(x1), by, 56, BTN_H)) {
			if (r.id().equals(confirm)) {
				confirm = null;
				ClientPlayNetworking.send(new Grasp.Request(Grasp.RELEASE, r.id(), 0));
			} else {
				confirm = r.id();
			}
		} else {
			confirm = null;
		}
	}

	/** Mode names for tests and other screens. */
	static String modeName(int i) {
		return MODES[GraspRules.Mode.of(i).ordinal()];
	}
}
