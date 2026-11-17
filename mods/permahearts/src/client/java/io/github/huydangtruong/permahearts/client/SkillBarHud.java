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
 * Skill bar centred under the hotbar, sliding up when a technique is selected (D125, D128): one cell per usable slot with its own key label, cooldown and the gold selection
 * frame (P-045). Also draws the charge power bar at the screen centre, above the health / armour rows.
 */
public class SkillBarHud implements HudElement {
	private static final int MARGIN = 8; // same distance from the edges as the HeartXP bar
	private static final int BOX = 20;
	private static final float LABEL_SCALE = 0.6f;
	private static final int GAP = 2;
	private static final net.minecraft.resources.Identifier SELECTION = net.minecraft.resources.Identifier.withDefaultNamespace("hud/hotbar_selection");
	private static final int GOLD = 0xFFFFD040;

	private static float progress; // 0 = lowered under the hotbar, 1 = raised (D128)
	private static long lastMillis;

	/** Share of black laid over the hotbar: 0 lowered, 0.5 raised, following the slide (the hotbar looks 50% opaque). */
	static float hotbarFade() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientSkills.available(mc) || ClientSkills.selected() < 0 && progress <= 0f) {
			return 0f;
		}
		return 0.5f * SkillBarLogic.slideOffset(progress);
	}

	/** Slides towards the raised position while a technique slot is selected; driven by real frame time. */
	private static void advance(boolean raised) {
		long now = net.minecraft.util.Util.getMillis();
		long dt = lastMillis == 0 ? 0 : Math.min(100, now - lastMillis);
		lastMillis = now;
		progress = SkillBarLogic.slideStep(progress, raised, dt);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker t) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden() || !ClientSkills.available(mc)) {
			return;
		}
		TechniqueData d = ClientSkills.data(mc.player);
		long now = mc.level.getGameTime();
		int[] slots = ClientSkills.usableSlots(mc);
		int total = slots.length * BOX + Math.max(0, slots.length - 1) * GAP;
		int left = (g.guiWidth() - total) / 2;
		advance(ClientSkills.selected() >= 0);
		int top = Math.round(SkillBarLogic.barTop(g.guiHeight(), progress));
		int textBase = g.guiHeight() - MARGIN - 22; // the info lines stay at the bottom right (as in P-045)
		for (int i = 0; i < slots.length; i++) {
			Technique tech = d.inSlot(slots[i]);
			int x = left + i * (BOX + GAP);
			g.fill(x, top, x + BOX, top + BOX, 0xD0000000);
			g.outline(x, top, BOX, BOX, 0xFF505050);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(tech), x + 2, top + 2, 16, 16);
			if (slots[i] == ClientSkills.selected()) { // D49: gold selection frame
				g.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, x - 1, top - 1, BOX + 2, BOX + 1, GOLD);
			}
			// small black key square in the top-left corner (follows rebinding), never wider than the cell
			Component key = ClientSkills.KEYS[slots[i]].getTranslatedKeyMessage();
			int boxW = Math.min(BOX, Math.max(7, Math.round(mc.font.width(key) * LABEL_SCALE) + 2));
			g.fill(x, top, x + boxW, top + 7, 0xE0000000);
			g.pose().pushMatrix();
			g.pose().translate(x + 1.5f, top + 1.2f);
			g.pose().scale(LABEL_SCALE, LABEL_SCALE);
			g.text(mc.font, key, 0, 0, 0xFFFFFFFF, false);
			g.pose().popMatrix();
			long cd = d.cooldownLeft(tech, now);
			if (cd > 0) {
				int h = Math.round((BOX - 2) * SkillBarLogic.cooldownFraction(cd, io.github.huydangtruong.permahearts.technique.TechniqueRules.cooldownTicks(tech, Math.max(1, d.level(tech)))));
				g.fill(x + 1, top + 1, x + BOX - 1, top + 1 + h, 0xB0808080);
				g.centeredText(mc.font, Component.literal(Integer.toString(SkillBarLogic.secondsLeft(cd))),
					x + BOX / 2, top + BOX / 2 - 3, 0xFFFFFFFF);
			}
		}
		long flight = io.github.huydangtruong.permahearts.technique.Flight.remaining(
			mc.player.getAttached(io.github.huydangtruong.permahearts.technique.Flight.END), now);
		if (flight > 0) { // remaining flight time, right side, above the technique name
			Component fl = Component.literal("Flight " + SkillBarLogic.secondsLeft(flight) + " s");
			g.text(mc.font, fl, g.guiWidth() - MARGIN - mc.font.width(fl), textBase - 24, flight < 100 ? 0xFFFF8080 : 0xFFDFF4FF, true);
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
			g.text(mc.font, name, g.guiWidth() - MARGIN - mc.font.width(name), textBase - 12, 0xFFFFFFFF, true);
		}
		if (ClientSkills.charging()) { // power bar at the centre, just above the health / armour rows
			float c = ClientSkills.chargeFraction();
			int w = 102;
			int bx = (g.guiWidth() - w) / 2;
			int by = g.guiHeight() - 66;
			g.fill(bx, by, bx + w, by + 5, 0xA0000000);
			g.fill(bx + 1, by + 1, bx + 1 + Math.round((w - 2) * c), by + 4, c >= 1f ? 0xFFFFD040 : 0xFF40C0FF);
			g.outline(bx, by, w, 5, 0xFF202020);
		}
	}
}
