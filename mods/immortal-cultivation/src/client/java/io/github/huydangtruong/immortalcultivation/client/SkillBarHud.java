package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.SkillBarLogic;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Skill bar in the bottom-right corner of the screen (D130, back to the D125 spot): one dark cell per usable slot with a
 * tiny black key label in its top-left corner (D127), cooldown and the gold selection frame. Also draws the charge power bar.
 */
public class SkillBarHud implements HudElement {
	private static final int MARGIN = 4; // D133: 4 px from the right and bottom edges
	private static final int BOX = 22;
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
		int right = g.guiWidth() - MARGIN;
		int left = right - total;
		int top = g.guiHeight() - MARGIN - BOX;
		for (int i = 0; i < slots.length; i++) {
			Technique tech = d.inSlot(slots[i]);
			int x = left + i * (BOX + GAP);
			g.fill(x, top, x + BOX, top + BOX, 0xD0000000); // dark cell like the P-047 trial
			g.outline(x, top, BOX, BOX, 0xFF505050);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(tech), x + 3, top + 3, 16, 16);
			if (slots[i] == ClientSkills.selected()) { // D49: gold selection frame
				g.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, x - 1, top - 1, 24, 23, GOLD);
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
				int h = Math.round((BOX - 2) * SkillBarLogic.cooldownFraction(cd, io.github.huydangtruong.immortalcultivation.technique.TechniqueRules.cooldownTicks(tech, Math.max(1, d.level(tech)))));
				g.fill(x + 1, top + 1, x + BOX - 1, top + 1 + h, 0xB0808080);
				g.centeredText(mc.font, Component.literal(Integer.toString(SkillBarLogic.secondsLeft(cd))),
					x + BOX / 2, top + BOX / 2 - 4, 0xFFFFFFFF);
			}
		}
		// time-left lines, right side, stacked upwards above the technique name: Flight, Aqua, Spirit Walk (D129)
		int lineY = top - 24;
		long flight = io.github.huydangtruong.immortalcultivation.technique.TechniqueTimers.remaining(
			mc.player.getAttached(io.github.huydangtruong.immortalcultivation.technique.Flight.END), now);
		if (flight > 0) {
			lineY = timeLine(g, mc, right, "Flight", flight, 0xFFDFF4FF, lineY);
		}
		long aqua = io.github.huydangtruong.immortalcultivation.technique.TechniqueTimers.remaining(
			mc.player.getAttached(io.github.huydangtruong.immortalcultivation.technique.AquaMarine.END), now);
		if (aqua > 0) {
			lineY = timeLine(g, mc, right, "Aqua", aqua, 0xFF9FF0E8, lineY);
		}
		net.minecraft.world.effect.MobEffectInstance walk = mc.player.getEffect(io.github.huydangtruong.immortalcultivation.technique.TechniqueTimers.SHADOW);
		if (walk != null && walk.getDuration() > 0) { // the server mirrors the Spirit Walk end tick into this effect (D115)
			lineY = timeLine(g, mc, right, "Spirit Walk", walk.getDuration(), 0xFFC8A8FF, lineY);
		}
		if (ClientMayhem.visible(mc)) { // D174: shards left and the seconds left of Light Strike Reflection
			if (ClientMayhem.windowTicks() > 0) {
				lineY = timeLine(g, mc, right, "Light Strike", ClientMayhem.windowTicks(), 0xFFD9A8FF, lineY);
			}
			Component shards = Component.literal("Shards " + ClientMayhem.shards() + "/" + io.github.huydangtruong.immortalcultivation.technique.MayhemRules.SHARDS);
			g.text(mc.font, shards, right - mc.font.width(shards), lineY, 0xFFD9A8FF, true);
			lineY -= 12;
		}
		Technique sel = ClientSkills.selectedTechnique(mc);
		if (sel != null) {
			Component name = Component.literal(sel.displayName() + "  Lv " + d.level(sel));
			g.text(mc.font, name, right - mc.font.width(name), top - 11, 0xFFFFFFFF, true);
		}
		float lance = ClientLance.localFraction();
		if (lance >= 0f) { // D157: Solar Lance bar, full to empty over 12 s, same spot as the charge bar
			int w = 102;
			int bx = (g.guiWidth() - w) / 2;
			int by = g.guiHeight() - 62;
			g.fill(bx, by, bx + w, by + 5, 0xA0000000);
			g.fill(bx + 1, by + 1, bx + 1 + Math.round((w - 2) * lance), by + 4, 0xFFFF9A20);
			g.outline(bx, by, w, 5, 0xFF202020);
		}
		if (ClientSkills.charging()) { // power bar at the centre, just above the health / armour rows
			float c = ClientSkills.chargeFraction();
			int w = 102;
			int bx = (g.guiWidth() - w) / 2;
			int by = g.guiHeight() - 62;
			g.fill(bx, by, bx + w, by + 5, 0xA0000000);
			g.fill(bx + 1, by + 1, bx + 1 + Math.round((w - 2) * c), by + 4, c >= 1f ? 0xFFFFD040 : 0xFF40C0FF);
			g.outline(bx, by, w, 5, 0xFF202020);
			if (sel == Technique.TELEPORT && !ClientGrasp.tethering()) { // D199: how far the leap goes at this charge
				Component far = Component.literal(io.github.huydangtruong.immortalcultivation.technique.TechniqueRules.leapBlocks(d.level(sel), c) + " blocks");
				g.text(mc.font, far, (g.guiWidth() - mc.font.width(far)) / 2, by - 10, 0xFFFFFFFF, true);
			}
		}
	}

	/** One right-aligned "Name N s" line (red under 5 s); returns the y of the next line up. */
	private static int timeLine(GuiGraphicsExtractor g, Minecraft mc, int right, String name, long ticksLeft, int color, int y) {
		Component c = Component.literal(name + " " + SkillBarLogic.secondsLeft(ticksLeft) + " s");
		g.text(mc.font, c, right - mc.font.width(c), y, ticksLeft < 100 ? 0xFFFF8080 : color, true);
		return y - 12;
	}
}
