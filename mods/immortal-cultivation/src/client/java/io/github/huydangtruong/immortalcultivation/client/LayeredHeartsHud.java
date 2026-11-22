package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.HealthBarRules;
import java.util.Random;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * D203: the health row is one heart plus a progress bar (HealthBarRules), at any max health. The heart is the health
 * level: vanilla red for layer 0, our coloured layer sprites above (HeartLayers, D36-D38); poison, wither and freezing use
 * the vanilla status heart and tint the bar; hardcore uses the hardcore heart. The bar fills with health / max health in the
 * layer colour; absorption is a thin golden strip along the top of the bar. Heart + bar are as wide as the armour bar,
 * and the armour bar sits right above the row.
 */
public final class LayeredHeartsHud {
	/** Layer 0 = vanilla red; 1-4 bright, 5-9 pastel, 10-14 textured (tools/make_heart_sprites.py, D37, D38). */
	private static final int LAYERS = 15;
	private static final Identifier[] LAYER_FULL = new Identifier[LAYERS];

	static {
		for (int i = 1; i < LAYERS; i++) {
			LAYER_FULL[i] = Identifier.fromNamespaceAndPath("permahearts", "hud/heart/layer_" + i + "_full");
		}
	}

	private LayeredHeartsHud() {}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, vanilla -> (g, delta) -> {
			Player p = player();
			if (p != null) {
				draw(g, p);
			} else {
				vanilla.extractRenderState(g, delta);
			}
		});
		// The armour bar is placed by vanilla above all the rows vanilla would draw: move it down to the one row. (Fabric's
		// status bar height registry only applies offsets once some mod adds a bar, so a height provider is not enough.)
		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, vanilla -> (g, delta) -> {
			Player p = player();
			if (p == null) {
				vanilla.extractRenderState(g, delta);
				return;
			}
			g.pose().pushMatrix();
			g.pose().translate(0, HealthBarRules.armorShift(p.getMaxHealth(), Mth.ceil(p.getHealth()), Mth.ceil(p.getAbsorptionAmount())));
			vanilla.extractRenderState(g, delta);
			g.pose().popMatrix();
		});
	}

	private static Player player() {
		return Minecraft.getInstance().getCameraEntity() instanceof Player p ? p : null;
	}

	private static void draw(GuiGraphicsExtractor g, Player p) {
		float max = p.getMaxHealth();
		int health = Mth.ceil(p.getHealth());
		int absorption = Mth.ceil(p.getAbsorptionAmount());
		int x = g.guiWidth() / 2 - 91;
		int y = g.guiHeight() - 39;
		boolean hardcore = p.level().getLevelData().isHardcore();
		String status = p.hasEffect(MobEffects.POISON) ? "poisoned_"
			: p.hasEffect(MobEffects.WITHER) ? "withered_"
			: p.isFullyFrozen() ? "frozen_"
			: "";
		int layer = HealthBarRules.layer(health);
		int color = status.equals("poisoned_") ? HealthBarRules.POISON_COLOR
			: status.equals("withered_") ? HealthBarRules.WITHER_COLOR
			: status.equals("frozen_") ? HealthBarRules.FROZEN_COLOR
			: HealthBarRules.layerColor(layer);

		// The heart: low health still shakes it like vanilla.
		int hy = y;
		if (health + absorption <= 4) {
			hy += new Random(p.tickCount * 312871L).nextInt(2);
		}
		sprite(g, vanilla(hardcore ? "container_hardcore" : "container"), x, hy);
		if (health > 0) {
			sprite(g, heart(layer, status, hardcore), x, hy);
		}

		// The bar, to the right of the heart: dark cell, outline, fill, thin golden absorption strip on top.
		int bx = x + HealthBarRules.ICON + HealthBarRules.GAP;
		int by = y + 1;
		int w = HealthBarRules.BAR_WIDTH;
		int h = HealthBarRules.BAR_HEIGHT;
		int innerW = w - 2;
		Round r = new Round(bx, by, w, h);
		r.fill(g, bx, bx + w, by, by + h, 0xA0000000);
		int filled = HealthBarRules.fillPixels(p.getHealth(), max, innerW);
		if (filled > 0) {
			r.fill(g, bx + 1, bx + 1 + filled, by + 1, by + h - 1, color);
			r.fill(g, bx + 1, bx + 1 + filled, by + 1, by + 2, lighten(color)); // a light top edge, like a glossy bar
		}
		int gold = HealthBarRules.absorptionPixels(p.getAbsorptionAmount(), max, innerW);
		if (gold > 0) {
			r.fill(g, bx + 1, bx + 1 + gold, by + 1, by + 1 + HealthBarRules.ABSORPTION_ROWS, HealthBarRules.ABSORPTION_COLOR);
		}
		r.outline(g, 0xFF202020);
	}

	/** D204: the bar's rounded shape (corner radius 2 px): fills are clipped to it and the outline follows it. */
	private record Round(int x, int y, int w, int h) {
		/** How many pixels row yy is cut in on each side (outline included). */
		private int inset(int yy) {
			int d = Math.min(yy - y, y + h - 1 - yy);
			return d <= 0 ? 2 : d == 1 ? 1 : 0;
		}

		void fill(GuiGraphicsExtractor g, int x0, int x1, int y0, int y1, int color) {
			for (int yy = y0; yy < y1; yy++) {
				int in = inset(yy);
				int a = Math.max(x0, x + in), b = Math.min(x1, x + w - in);
				if (a < b) {
					g.fill(a, yy, b, yy + 1, color);
				}
			}
		}

		void outline(GuiGraphicsExtractor g, int color) {
			g.fill(x + 2, y, x + w - 2, y + 1, color);
			g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
			g.fill(x, y + 2, x + 1, y + h - 2, color);
			g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
			for (int yy : new int[] {y + 1, y + h - 2}) {
				g.fill(x + 1, yy, x + 2, yy + 1, color);
				g.fill(x + w - 2, yy, x + w - 1, yy + 1, color);
			}
		}
	}

	/** Layer 0 and every status heart use the vanilla sprites; the other layers our coloured ones. */
	private static Identifier heart(int layer, String status, boolean hardcore) {
		if (layer == 0 || !status.isEmpty() || layer >= LAYER_FULL.length) {
			return vanilla(status + (hardcore ? "hardcore_" : "") + "full");
		}
		return LAYER_FULL[layer];
	}

	private static int lighten(int argb) {
		int r = ((argb >> 16) & 0xFF) + (255 - ((argb >> 16) & 0xFF)) / 3;
		int gr = ((argb >> 8) & 0xFF) + (255 - ((argb >> 8) & 0xFF)) / 3;
		int b = (argb & 0xFF) + (255 - (argb & 0xFF)) / 3;
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	private static Identifier vanilla(String name) {
		return Identifier.withDefaultNamespace("hud/heart/" + name);
	}

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, 9, 9);
	}
}
