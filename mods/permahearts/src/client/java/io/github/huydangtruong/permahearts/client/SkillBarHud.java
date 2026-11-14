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

/**
 * Display-only skill bar at the bottom-right: a flat label with the bound key, then one cell per filled slot with its
 * cooldown (P-012b). Also draws the charge power bar at the screen centre, above the health / armour rows.
 */
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
		int[] slots = ClientSkills.modeSlots(mc);
		Component key = ClientSkills.KEY.getTranslatedKeyMessage();
		int labelW = Math.max(14, mc.font.width(key) + 6);
		int total = labelW + GAP + slots.length * BOX + Math.max(0, slots.length - 1) * GAP;
		int left = g.guiWidth() - MARGIN - total;
		int top = g.guiHeight() - MARGIN - BOX;
		// flat label with the bound key, no box
		g.text(mc.font, key, left + (labelW - mc.font.width(key)) / 2, top + BOX / 2 - 4, 0xFFE0E0E0, true);
		for (int i = 0; i < slots.length; i++) {
			Technique tech = d.inSlot(slots[i]);
			int x = left + labelW + GAP + i * (BOX + GAP);
			g.fill(x, top, x + BOX, top + BOX, 0x90000000);
			g.outline(x, top, BOX, BOX, 0xFF505050);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(tech), x + 3, top + 3, 16, 16);
			long cd = d.cooldownLeft(tech, now);
			if (cd > 0) {
				int h = Math.round((BOX - 2) * SkillBarLogic.cooldownFraction(cd, tech.cooldownTicks()));
				g.fill(x + 1, top + 1, x + BOX - 1, top + 1 + h, 0xB0808080);
				g.centeredText(mc.font, Component.literal(Integer.toString(SkillBarLogic.secondsLeft(cd))),
					x + BOX / 2, top + BOX / 2 - 4, 0xFFFFFFFF);
			}
		}
		long flight = io.github.huydangtruong.permahearts.technique.Flight.remaining(
			mc.player.getAttached(io.github.huydangtruong.permahearts.technique.Flight.END), now);
		if (flight > 0) { // remaining flight time, right side, above the technique name
			Component fl = Component.literal("Flight " + SkillBarLogic.secondsLeft(flight) + " s");
			g.text(mc.font, fl, g.guiWidth() - MARGIN - mc.font.width(fl), top - 24, flight < 100 ? 0xFFFF8080 : 0xFFDFF4FF, true);
		}
		long aqua = io.github.huydangtruong.permahearts.technique.AquaMarine.remaining(
			mc.player.getAttached(io.github.huydangtruong.permahearts.technique.AquaMarine.END), now);
		if (aqua > 0) { // remaining Aqua Marine time, above the flight line
			Component aq = Component.literal("Aqua " + SkillBarLogic.secondsLeft(aqua) + " s");
			g.text(mc.font, aq, g.guiWidth() - MARGIN - mc.font.width(aq), top - (flight > 0 ? 36 : 24), aqua < 100 ? 0xFFFF8080 : 0xFF9FF0E8, true);
		}
		Technique sel = ClientSkills.selectedTechnique(mc);
		if (sel != null) {
			Component name = Component.literal(sel.displayName() + "  Lv " + d.level(sel));
			g.text(mc.font, name, g.guiWidth() - MARGIN - mc.font.width(name), top - 12, 0xFFFFFFFF, true);
		}
		if (ClientSkills.charging()) { // power bar at the centre, just above the health / armour rows
			float c = ClientSkills.chargeFraction();
			int w = 102;
			int bx = (g.guiWidth() - w) / 2;
			int by = g.guiHeight() - 62;
			g.fill(bx, by, bx + w, by + 5, 0xA0000000);
			g.fill(bx + 1, by + 1, bx + 1 + Math.round((w - 2) * c), by + 4, c >= 1f ? 0xFFFFD040 : 0xFF40C0FF);
			g.outline(bx, by, w, 5, 0xFF202020);
		}
	}
}
