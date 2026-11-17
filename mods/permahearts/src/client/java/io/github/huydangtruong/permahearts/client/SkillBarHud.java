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
 * Skill bar just above the hunger row (D127, P-046): right-aligned with the food row, one small cell per usable slot with
 * a tiny black key label in its top-left corner, cooldown and the gold selection frame. Also draws the charge power bar.
 */
public class SkillBarHud implements HudElement {
	private static final int BOX = 20; // about the height of the heart row + armour row
	private static final float LABEL_SCALE = 0.6f;
	private static final int GAP = 2;
	private static final net.minecraft.resources.Identifier SELECTION = net.minecraft.resources.Identifier.withDefaultNamespace("hud/hotbar_selection");
	private static final int GOLD = 0xFFFFD040;

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
		int right = g.guiWidth() / 2 + 91; // right edge of the vanilla food row
		int left = right - total;
		int top = rowsTop(mc, g.guiHeight()) - 2 - BOX;
		for (int i = 0; i < slots.length; i++) {
			Technique tech = d.inSlot(slots[i]);
			int x = left + i * (BOX + GAP);
			g.fill(x, top, x + BOX, top + BOX, 0x90000000);
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
			g.text(mc.font, fl, right - mc.font.width(fl), top - 24, flight < 100 ? 0xFFFF8080 : 0xFFDFF4FF, true);
		}
		long aqua = io.github.huydangtruong.permahearts.technique.AquaMarine.remaining(
			mc.player.getAttached(io.github.huydangtruong.permahearts.technique.AquaMarine.END), now);
		if (aqua > 0) { // remaining Aqua Marine time, above the flight line
			Component aq = Component.literal("Aqua " + SkillBarLogic.secondsLeft(aqua) + " s");
			g.text(mc.font, aq, right - mc.font.width(aq), top - (flight > 0 ? 36 : 24), aqua < 100 ? 0xFFFF8080 : 0xFF9FF0E8, true);
		}
		Technique sel = ClientSkills.selectedTechnique(mc);
		if (sel != null) {
			Component name = Component.literal(sel.displayName() + "  Lv " + d.level(sel));
			g.text(mc.font, name, right - mc.font.width(name), top - 11, 0xFFFFFFFF, true);
		}
		if (ClientSkills.charging()) { // power bar at the centre, just above the health / armour rows
			float c = ClientSkills.chargeFraction();
			int w = 102;
			int bx = (g.guiWidth() - w) / 2;
			int lines = 1 + (flight > 0 ? 1 : 0) + (aqua > 0 ? 1 : 0);
			int by = Math.min(g.guiHeight() - 62, top - 12 * lines - 8); // above the skill bar and its text lines
			g.fill(bx, by, bx + w, by + 5, 0xA0000000);
			g.fill(bx + 1, by + 1, bx + 1 + Math.round((w - 2) * c), by + 4, c >= 1f ? 0xFFFFD040 : 0xFF40C0FF);
			g.outline(bx, by, w, 5, 0xFF202020);
		}
	}

	/** Top y of the vanilla rows above the hotbar on the right: food row, or mount health rows, plus the air bubble row. */
	private static int rowsTop(Minecraft mc, int guiHeight) {
		int y = guiHeight - 39; // Hud#extractPlayerHealth: food row y, 9 px icons
		if (mc.player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity v && v.showVehicleHealth()) {
			int hearts = Math.min(30, (int) (v.getMaxHealth() + 0.5f) / 2);
			y -= (Math.max(1, (hearts + 9) / 10) - 1) * 10; // Hud#getVisibleVehicleHeartRows, 10 px per row
		}
		if (mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) || mc.player.getAirSupply() < mc.player.getMaxAirSupply()) {
			y -= 10; // air bubbles one row above (Hud#getAirBubbleYLine)
		}
		return y;
	}
}
