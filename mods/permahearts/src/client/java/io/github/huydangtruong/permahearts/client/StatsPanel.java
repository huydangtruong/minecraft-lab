package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.ElixirItems;
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
import net.minecraft.world.item.ItemStack;

/**
 * Stats panel (D31): a button next to the recipe book button in the survival inventory opens a panel on the left with
 * each stat as "total (base +gear +cultivation)", the Vitality bonus and the respawn point. Works like the recipe book:
 * the inventory moves right, and opening one closes the other. Data comes from the server (StatsSync), once a second.
 */
public final class StatsPanel {
	public static final int W = 192;
	public static final int H = 166;
	private static final int GAP = 2;
	private static final int LINE = 11;

	private static final int TEXT = 0xFF404040;
	private static final int TOTAL = 0xFF202020;
	private static final int BASE = 0xFF707070;
	private static final int GEAR = 0xFF1F4FB8;
	private static final int BONUS = 0xFFC85A00; // orange (D31)

	private static final Map<String, String> LABELS = Map.of(
		"hearts", "Hearts", "damage", "Damage", "attack_speed", "Atk speed", "speed", "Speed", "armor", "Armor",
		"reach", "Reach", "hit_range", "Hit range", "mining", "Mining");

	/** Remembered while the game runs, like the recipe book. */
	private static boolean open;
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
			layout(inv);
			ScreenEvents.afterBackground(inv).register((s, g, mx, my, delta) -> {
				if (open && recipeBook(inv).isVisible()) {
					open = false; // the recipe book button was pressed: it moved the inventory already
				}
				b.setPosition(left(inv) + 126, inv.height / 2 - 22);
				if (open) {
					draw(inv, g);
				}
			});
		});
	}

	public static boolean isOpen() {
		return open;
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
		return inv.width >= ((ContainerScreenAccess) inv).permahearts$imageWidth() + W + GAP + 8;
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
		int left = open && fits(inv) ? (inv.width - (imageW + W + GAP)) / 2 + W + GAP : (inv.width - imageW) / 2;
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
		return x >= x0 && x < x0 + W && y >= y0 && y < y0 + H;
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
		g.text(font, Component.literal("Stats").withStyle(ChatFormatting.BOLD), tx, y0 + 7, TEXT, false);
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

	/** Same size as the recipe book button; shows an elixir bottle. */
	private static final class StatsButton extends Button {
		private static final ItemStack ICON = new ItemStack(ElixirItems.get(ElixirStat.STRENGTH));

		StatsButton(InventoryScreen inv) {
			super(0, 0, 20, 18, Component.literal("Stats"), b -> toggle(inv), DEFAULT_NARRATION);
			setTooltip(Tooltip.create(Component.literal("Stats")));
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			extractDefaultSprite(g);
			g.item(ICON, getX() + 2, getY() + 1);
		}
	}
}
