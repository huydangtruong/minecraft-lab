package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.SkillBarLogic;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Skill bar at the bottom-right, mirroring the HeartXP bar: the cross box, then one box per open slot (P-011b). */
public class SkillBarHud implements HudElement {
	private static final int MARGIN = 8; // same distance from the edges as the HeartXP bar
	private static final int BOX = 22;
	private static final int GAP = 2;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker t) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden() || !ClientSkills.available(mc)) {
			return;
		}
		TechniqueData d = ClientSkills.data(mc.player);
		long now = mc.level.getGameTime();
		int boxes = 1 + d.slotsOpen();
		int total = boxes * BOX + (boxes - 1) * GAP;
		int left = g.guiWidth() - MARGIN - total;
		int top = g.guiHeight() - MARGIN - BOX;
		int selected = ClientSkills.selected();
		for (int i = 0; i < boxes; i++) {
			int slot = i - 1; // -1 = the cross
			int x = left + i * (BOX + GAP);
			boolean isSelected = slot == selected;
			Technique tech = slot < 0 ? null : d.inSlot(slot);
			g.fill(x, top, x + BOX, top + BOX, isSelected ? 0xB0303030 : 0x90000000);
			g.outline(x, top, BOX, BOX, isSelected ? 0xFFFFFFFF : 0xFF505050);
			if (isSelected) {
				g.outline(x - 1, top - 1, BOX + 2, BOX + 2, 0xFFE8C040);
			}
			if (slot < 0) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.sprite("cross"), x + 3, top + 3, 16, 16);
			} else if (tech != null) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(tech), x + 3, top + 3, 16, 16);
				long cd = d.cooldownLeft(tech, now);
				if (cd > 0) {
					int h = Math.round((BOX - 2) * SkillBarLogic.cooldownFraction(cd, tech.cooldownTicks()));
					g.fill(x + 1, top + 1, x + BOX - 1, top + 1 + h, 0xB0808080);
					g.centeredText(mc.font, Component.literal(Integer.toString(SkillBarLogic.secondsLeft(cd))),
						x + BOX / 2, top + BOX / 2 - 4, 0xFFFFFFFF);
				}
			}
		}
		Technique sel = ClientSkills.selectedTechnique(mc);
		if (sel != null) {
			Component name = Component.literal(sel.displayName() + "  Lv " + d.level(sel));
			g.text(mc.font, name, g.guiWidth() - MARGIN - mc.font.width(name), top - 16, 0xFFFFFFFF, true);
		}
		if (ClientSkills.charging()) {
			float c = ClientSkills.chargeFraction();
			int by = top - 7;
			g.fill(left, by, left + total, by + 4, 0xA0000000);
			g.fill(left + 1, by + 1, left + 1 + Math.round((total - 2) * c), by + 3, c >= 1f ? 0xFFFFD040 : 0xFF40C0FF);
			g.outline(left, by, total, 4, 0xFF202020);
		}
	}
}
