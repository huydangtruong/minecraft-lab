package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.ElixirStat;
import io.github.huydangtruong.permahearts.client.mixin.ContainerScreenAccess;
import io.github.huydangtruong.permahearts.client.mixin.RecipeBookScreenAccess;
import io.github.huydangtruong.permahearts.stats.PlayerStats;
import io.github.huydangtruong.permahearts.stats.StatMath;
import io.github.huydangtruong.permahearts.stats.StatsSync;
import java.util.Map;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * Cultivation panel (D31, D39): a rounded taiji button next to the recipe book button in the survival inventory opens a
 * panel on the left with tabs like the recipe book's. Stats tab: each stat as "total (base +gear +cultivation)", the
 * Vitality bonus and the respawn point. Techniques tab: empty for now. Works like the recipe book: the inventory moves
 * right, and opening one closes the other. Data comes from the server (StatsSync), once a second.
 */
public final class StatsPanel {
	public static final int W = 192;
	public static final int H = 166;
	private static final int GAP = 2;
	/** Tabs stick out this far left of the panel (the selected one 2 more), like the recipe book's. */
	private static final int TAB_OUT = 32;
	private static final int TAB_W = 35;
	private static final int TAB_H = 27;
	private static final int LINE = 11;

	private static final int TEXT = 0xFF404040;
	private static final int TOTAL = 0xFF202020;
	private static final int BASE = 0xFF707070;
	private static final int GEAR = 0xFF1F4FB8;
	private static final int BONUS = 0xFFC85A00; // orange (D31)

	private static final Map<String, String> LABELS = Map.of(
		"hearts", "Hearts", "damage", "Damage", "attack_speed", "Atk speed", "speed", "Speed", "armor", "Armor",
		"reach", "Reach", "hit_range", "Hit range", "mining", "Mining");

	private static final Identifier BUTTON = Identifier.fromNamespaceAndPath("permahearts", "stats/button");
	private static final Identifier BUTTON_HOVER = Identifier.fromNamespaceAndPath("permahearts", "stats/button_highlighted");
	private static final Identifier TAIJI = Identifier.fromNamespaceAndPath("permahearts", "stats/icon");

	/** Panel tabs, top to bottom. */
	public enum Tab {
		STATS("Stats"),
		TECHNIQUES("Techniques");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	/** Remembered while the game runs, like the recipe book. */
	private static boolean open;
	private static Tab tab = Tab.STATS;
	private static StatsButton button;

	private StatsPanel() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (!(screen instanceof InventoryScreen inv)) {
				return;
			}
			if (open && recipeBook(inv).isVisible()) {
				open = false; // the recipe book was left open: it wins
			}
			StatsButton b = new StatsButton(inv);
			button = b;
			Screens.getWidgets(inv).add(b);
			TabButton[] tabs = new TabButton[Tab.values().length];
			for (Tab t : Tab.values()) {
				tabs[t.ordinal()] = new TabButton(t);
				Screens.getWidgets(inv).add(tabs[t.ordinal()]);
			}
			layout(inv);
			ScreenEvents.afterBackground(inv).register((s, g, mx, my, delta) -> {
				if (open && recipeBook(inv).isVisible()) {
					open = false; // the recipe book button was pressed: it moved the inventory already
				}
				b.setPosition(left(inv) + 126, inv.height / 2 - 22);
				int top = ((ContainerScreenAccess) inv).permahearts$topPos();
				for (TabButton t : tabs) {
					t.visible = open;
					t.setPosition(panelX(inv) - TAB_OUT + 2, top + 3 + t.tab.ordinal() * TAB_H);
				}
				if (open) {
					draw(inv, g);
				}
			});
		});
	}

	public static boolean isOpen() {
		return open;
	}

	/** Selected tab (tests). */
	public static Tab tab() {
		return tab;
	}

	/** GUI position of the button (tests). */
	public static int[] buttonCenter() {
		return button == null ? new int[] {0, 0} : new int[] {button.getX() + 10, button.getY() + 9};
	}

	private static RecipeBookComponent<?> recipeBook(InventoryScreen inv) {
		return ((RecipeBookScreenAccess) inv).permahearts$recipeBook();
	}

	private static int left(InventoryScreen inv) {
		return ((ContainerScreenAccess) inv).permahearts$leftPos();
	}

	private static boolean fits(InventoryScreen inv) {
		return inv.width >= ((ContainerScreenAccess) inv).permahearts$imageWidth() + W + GAP + TAB_OUT + 8;
	}

	private static void toggle(InventoryScreen inv) {
		RecipeBookComponent<?> rb = recipeBook(inv);
		if (!open && rb.isVisible()) {
			rb.toggleVisibility();
		}
		open = !open;
		layout(inv);
	}

	/** Places the inventory (and the recipe book button) for the current state. */
	private static void layout(InventoryScreen inv) {
		ContainerScreenAccess a = (ContainerScreenAccess) inv;
		if (recipeBook(inv).isVisible()) {
			return; // the recipe book placed it
		}
		int imageW = a.permahearts$imageWidth();
		int left = open && fits(inv) ? (inv.width - (imageW + W + GAP + TAB_OUT)) / 2 + TAB_OUT + W + GAP : (inv.width - imageW) / 2;
		a.permahearts$setLeftPos(left);
		for (AbstractWidget w : Screens.getWidgets(inv)) {
			if (w instanceof ImageButton) {
				w.setPosition(left + 104, inv.height / 2 - 22); // InventoryScreen#getRecipeBookButtonPosition
			}
		}
	}

	/** True when (x, y) is on the open panel of this screen. */
	public static boolean contains(Object screen, double x, double y) {
		if (!open || !(screen instanceof InventoryScreen inv)) {
			return false;
		}
		int x0 = panelX(inv);
		int y0 = ((ContainerScreenAccess) inv).permahearts$topPos();
		boolean onTabs = x >= x0 - TAB_OUT && x < x0 && y >= y0 + 3 && y < y0 + 3 + Tab.values().length * TAB_H;
		return onTabs || x >= x0 && x < x0 + W && y >= y0 && y < y0 + H;
	}

	private static int panelX(InventoryScreen inv) {
		return fits(inv) ? left(inv) - W - GAP : Math.max(2, left(inv) - W - GAP);
	}

	// ---- drawing ----

	private static void draw(InventoryScreen inv, GuiGraphicsExtractor g) {
		Font font = Minecraft.getInstance().font;
		int x0 = panelX(inv);
		int y0 = ((ContainerScreenAccess) inv).permahearts$topPos();
		int x1 = x0 + W;
		int y1 = y0 + H;
		// vanilla-like frame: dark outline, white top-left, grey bottom-right, light grey face
		g.fill(x0 + 1, y0, x1 - 1, y1, 0xFF000000);
		g.fill(x0, y0 + 1, x1, y1 - 1, 0xFF000000);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFFFFFFF);
		g.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
		g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0xFFC6C6C6);

		int tx = x0 + 7;
		int right = x1 - 7;
		g.text(font, Component.literal(tab.label).withStyle(ChatFormatting.BOLD), tx, y0 + 7, TEXT, false);
		if (tab == Tab.TECHNIQUES) {
			g.text(font, "No techniques yet", tx, y0 + 22, BASE, false); // D39: Huy describes them later
			return;
		}
		var player = Minecraft.getInstance().player;
		PlayerStats stats = player == null ? null : player.getAttached(StatsSync.STATS);
		if (stats == null) {
			g.text(font, "Needs the mod on the server", tx, y0 + 22, TEXT, false);
			return;
		}
		// second column: left-aligned, right after the widest label
		int vx = tx + labelWidth(font) + 6;
		int y = y0 + 21;
		for (PlayerStats.Row r : stats.rows()) {
			g.text(font, LABELS.getOrDefault(r.id(), r.id()), tx, y, TEXT, false);
			String[] p = StatMath.parts(r);
			drawLeft(g, font, vx, y, new String[] {p[0], " (", p[1], " ", p[2], " ", p[3], ")"},
				new int[] {TOTAL, BASE, BASE, BASE, GEAR, BASE, BONUS, BASE});
			y += LINE;
		}
		g.text(font, "Food heal", tx, y, TEXT, false);
		String heal = "+" + StatMath.num(ElixirStat.VITALITY.bonus(stats.vitality()) * 100) + "%";
		drawLeft(g, font, vx, y, new String[] {heal}, new int[] {stats.vitality() > 0 ? BONUS : BASE});
		y += LINE + 1;
		drawLeft(g, font, vx, y, new String[] {"base ", "+gear ", "+cultivation"}, new int[] {BASE, GEAR, BONUS});
		y += LINE + 2;
		g.fill(tx, y, right, y + 1, 0xFF8B8B8B);
		y += 4;
		PlayerStats.Spawn s = stats.spawn();
		g.text(font, "Spawn", tx, y, TEXT, false);
		drawLeft(g, font, vx, y, new String[] {(s.personal() ? "bed/anchor" : "world spawn") + " · " + dimension(s.dimension())},
			new int[] {BASE});
		y += LINE;
		drawLeft(g, font, vx, y, new String[] {"X " + s.x() + "   Y " + s.y() + "   Z " + s.z()}, new int[] {TOTAL});
	}

	private static int labelWidth(Font font) {
		int w = font.width("Food heal");
		for (String l : LABELS.values()) {
			w = Math.max(w, font.width(l));
		}
		return Math.max(w, font.width("Spawn"));
	}

	private static void drawLeft(GuiGraphicsExtractor g, Font font, int x, int y, String[] parts, int[] colors) {
		for (int i = 0; i < parts.length; i++) {
			g.text(font, parts[i], x, y, colors[i], false);
			x += font.width(parts[i]);
		}
	}

	private static String dimension(String id) {
		return switch (id) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "End";
			default -> id.substring(id.indexOf(':') + 1);
		};
	}

	/** Same size and rounded style as the recipe book button, with a taiji (tools/make_stats_sprites.py). */
	private static final class StatsButton extends Button {
		StatsButton(InventoryScreen inv) {
			super(0, 0, 20, 18, Component.literal("Cultivation"), b -> toggle(inv), DEFAULT_NARRATION);
			setTooltip(Tooltip.create(Component.literal("Cultivation")));
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? BUTTON_HOVER : BUTTON, getX(), getY(), 20, 18);
		}
	}

	/** A tab on the panel's left edge, drawn with the vanilla recipe book tab sprites. */
	private static final class TabButton extends Button {
		private static final ItemStack BOOK = new ItemStack(Items.ENCHANTED_BOOK);
		private final Tab tab;

		TabButton(Tab t) {
			super(0, 0, TAB_W, TAB_H, Component.literal(t.label), b -> StatsPanel.tab = t, DEFAULT_NARRATION);
			this.tab = t;
			setTooltip(Tooltip.create(Component.literal(t.label)));
			visible = false;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			boolean selected = StatsPanel.tab == tab;
			int x = getX() - (selected ? 2 : 0);
			g.blitSprite(RenderPipelines.GUI_TEXTURED,
				Identifier.withDefaultNamespace(selected ? "recipe_book/tab_selected" : "recipe_book/tab"), x, getY(), TAB_W, TAB_H);
			if (tab == Tab.STATS) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, TAIJI, x + 9, getY() + 5, 16, 16);
			} else {
				g.fakeItem(BOOK, x + 9, getY() + 5);
			}
		}
	}
}
