package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.SkillBarLogic;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hotbar in technique mode (P-012b): cells 0..N-1 show the filled techniques (dark cells with a gold edge), then the
 * items shifted right (item slot cell - N). Only a view: the inventory is untouched. Outside the mode vanilla draws.
 */
public final class ModeHotbarHud {
	private static final Identifier BAR = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
	private static final int GOLD = 0xFFFFD040;

	private ModeHotbarHud() {}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, vanilla -> (g, delta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (!ClientSkills.mode() || mc.player == null || mc.gui.hud.isHidden()) {
				vanilla.extractRenderState(g, delta);
				return;
			}
			draw(g, mc);
		});
	}

	private static void draw(GuiGraphicsExtractor g, Minecraft mc) {
		Player p = mc.player;
		TechniqueData d = ClientSkills.data(mc.player);
		long now = mc.level.getGameTime();
		int[] slots = ClientSkills.modeSlots(mc);
		int x0 = g.guiWidth() / 2 - 91;
		int y0 = g.guiHeight() - 22;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, BAR, x0, y0, 182, 22);
		int selected = ClientSkills.cell();
		for (int cell = 0; cell < 9; cell++) {
			int x = x0 + 1 + cell * 20;
			int tech = SkillBarLogic.techniqueAtCell(cell, slots);
			if (tech >= 0) {
				Technique t = d.inSlot(tech);
				g.fill(x, y0 + 1, x + 20, y0 + 21, 0xF0101018); // darker and opaque: stands out from the item cells
				g.outline(x, y0 + 1, 20, 20, 0xFF9A7A20);
				g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(t), x + 2, y0 + 3, 16, 16);
				long cd = d.cooldownLeft(t, now);
				if (cd > 0) {
					int h = Math.round(18 * SkillBarLogic.cooldownFraction(cd, t.cooldownTicks()));
					g.fill(x + 1, y0 + 2, x + 19, y0 + 2 + h, 0xB0808080);
					g.centeredText(mc.font, Component.literal(Integer.toString(SkillBarLogic.secondsLeft(cd))),
						x + 10, y0 + 7, 0xFFFFFFFF);
				}
			} else {
				ItemStack stack = p.getInventory().getItem(SkillBarLogic.itemSlotAtCell(cell, slots.length));
				g.item(p, stack, x + 2, y0 + 3, cell);
				g.itemDecorations(mc.font, stack, x + 2, y0 + 3);
			}
		}
		int sx = x0 - 1 + selected * 20;
		if (SkillBarLogic.techniqueAtCell(selected, slots) >= 0) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, sx, y0 - 1, 24, 23, GOLD);
		} else {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, sx, y0 - 1, 24, 23);
		}
	}
}
