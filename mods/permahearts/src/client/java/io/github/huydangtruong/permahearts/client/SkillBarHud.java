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
				int h = Math.round((BOX - 2) * SkillBarLogic.cooldownFraction(cd, io.github.huydangtruong.permahearts.technique.TechniqueRules.cooldownTicks(tech, Math.max(1, d.level(tech)))));
				g.fill(x + 1, top + 1, x + BOX - 1, top + 1 + h, 0xB0808080);
				g.centeredText(mc.font, Component.literal(Integer.toString(SkillBarLogic.secondsLeft(cd))),
					x + BOX / 2, top + BOX / 2 - 4, 0xFFFFFFFF);
			}
		}
		// time-left lines, right side, stacked upwards above the technique name: Flight, Aqua, Spirit Walk (D129)
		int lineY = top - 24;
		long flight = io.github.huydangtruong.permahearts.technique.Flight.remaining(
			mc.player.getAttached(io.github.huydangtruong.permahearts.technique.Flight.END), now);
		if (flight > 0) {
			lineY = timeLine(g, mc, "Flight", flight, 0xFFDFF4FF, lineY);
		}
		long aqua = io.github.huydangtruong.permahearts.technique.AquaMarine.remaining(
			mc.player.getAttached(io.github.huydangtruong.permahearts.technique.AquaMarine.END), now);
		if (aqua > 0) {
			lineY = timeLine(g, mc, "Aqua", aqua, 0xFF9FF0E8, lineY);
		}
		net.minecraft.world.effect.MobEffectInstance walk = mc.player.getEffect(io.github.huydangtruong.permahearts.technique.TechniqueTimers.SHADOW);
		if (walk != null && walk.getDuration() > 0) { // the server mirrors the Spirit Walk end tick into this effect (D115)
			lineY = timeLine(g, mc, "Spirit Walk", walk.getDuration(), 0xFFC8A8FF, lineY);
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

	/** One right-aligned "Name N s" line (red under 5 s); returns the y of the next line up. */
	private static int timeLine(GuiGraphicsExtractor g, Minecraft mc, String name, long ticksLeft, int color, int y) {
		Component c = Component.literal(name + " " + SkillBarLogic.secondsLeft(ticksLeft) + " s");
		g.text(mc.font, c, g.guiWidth() - MARGIN - mc.font.width(c), y, ticksLeft < 100 ? 0xFFFF8080 : color, true);
		return y - 12;
	}
}
