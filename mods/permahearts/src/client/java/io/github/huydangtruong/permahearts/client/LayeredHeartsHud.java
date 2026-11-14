package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.HeartLayers;
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
 * Heart bar for more than 20 hearts (D36): at most 2 rows, extra hearts drawn over the first ones in other colours
 * (HeartLayers). Up to 20 hearts the vanilla bar is untouched. Poison, wither and freezing colour every heart like
 * vanilla; absorption hearts go in rows above. The armour bar is moved down to sit right above them.
 */
public final class LayeredHeartsHud {
	/** Layer 0 = vanilla red; 1-4 bright, 5-9 pastel, 10-14 textured (tools/make_heart_sprites.py, D37). */
	private static final int LAYERS = 15;
	private static final Identifier[] LAYER_FULL = new Identifier[LAYERS];
	private static final Identifier[] LAYER_HALF = new Identifier[LAYERS];

	static {
		for (int i = 1; i < LAYERS; i++) {
			LAYER_FULL[i] = Identifier.fromNamespaceAndPath("permahearts", "hud/heart/layer_" + i + "_full");
			LAYER_HALF[i] = Identifier.fromNamespaceAndPath("permahearts", "hud/heart/layer_" + i + "_half");
		}
	}

	private LayeredHeartsHud() {}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, vanilla -> (g, delta) -> {
			Player p = player();
			if (p != null && HeartLayers.layered(p.getMaxHealth())) {
				draw(g, p);
			} else {
				vanilla.extractRenderState(g, delta);
			}
		});
		// The armour bar is placed by vanilla above all the rows vanilla would draw: move it down to the 2 rows. (Fabric's
		// status bar height registry only applies offsets once some mod adds a bar, so a height provider is not enough.)
		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, vanilla -> (g, delta) -> {
			Player p = player();
			if (p == null || !HeartLayers.layered(p.getMaxHealth())) {
				vanilla.extractRenderState(g, delta);
				return;
			}
			g.pose().pushMatrix();
			g.pose().translate(0, HeartLayers.armorShift(p.getMaxHealth(), Mth.ceil(p.getHealth()), Mth.ceil(p.getAbsorptionAmount())));
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
		HeartLayers.Slot[] slots = HeartLayers.layout(max, health);
		int rows = HeartLayers.rows(slots.length);
		int absSlots = HeartLayers.absorptionSlots(absorption);
		int rowH = HeartLayers.rowHeight(rows + HeartLayers.rows(absSlots));
		int left = g.guiWidth() / 2 - 91;
		int base = g.guiHeight() - 39;
		boolean hardcore = p.level().getLevelData().isHardcore();
		String status = p.hasEffect(MobEffects.POISON) ? "poisoned_"
			: p.hasEffect(MobEffects.WITHER) ? "withered_"
			: p.isFullyFrozen() ? "frozen_"
			: "";
		// Vanilla touches: the heart under the regeneration wave hops up, low health shakes the bar.
		int regen = p.hasEffect(MobEffects.REGENERATION) ? p.tickCount % (slots.length + 5) : -1;
		boolean shake = health + absorption <= 4;
		Random rnd = new Random(p.tickCount * 312871L);
		Identifier container = vanilla(hardcore ? "container_hardcore" : "container");

		for (int i = slots.length - 1; i >= 0; i--) {
			int x = left + (i % HeartLayers.PER_ROW) * 8;
			int y = base - (i / HeartLayers.PER_ROW) * rowH;
			if (shake) {
				y += rnd.nextInt(2);
			}
			if (i == regen) {
				y -= 2;
			}
			sprite(g, container, x, y);
			HeartLayers.Slot s = slots[i];
			if (s.half() && s.under() >= 0) {
				sprite(g, heart(s.under(), false, status, hardcore), x, y);
			}
			if (s.layer() >= 0) {
				sprite(g, heart(s.layer(), s.half(), status, hardcore), x, y);
			}
		}
		for (int j = absSlots - 1; j >= 0; j--) {
			int x = left + (j % HeartLayers.PER_ROW) * 8;
			int y = base - (rows + j / HeartLayers.PER_ROW) * rowH;
			if (shake) {
				y += rnd.nextInt(2);
			}
			sprite(g, container, x, y);
			boolean half = j == absSlots - 1 && absorption % 2 == 1;
			sprite(g, vanilla("absorbing_" + (hardcore ? "hardcore_" : "") + (half ? "half" : "full")), x, y);
		}
	}

	/** Layer 0 and every status heart use the vanilla sprites; layers 1-4 our coloured ones. */
	private static Identifier heart(int layer, boolean half, String status, boolean hardcore) {
		if (layer == 0 || !status.isEmpty() || layer >= LAYER_FULL.length) {
			return vanilla(status + (hardcore ? "hardcore_" : "") + (half ? "half" : "full"));
		}
		return half ? LAYER_HALF[layer] : LAYER_FULL[layer];
	}

	private static Identifier vanilla(String name) {
		return Identifier.withDefaultNamespace("hud/heart/" + name);
	}

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, 9, 9);
	}
}
