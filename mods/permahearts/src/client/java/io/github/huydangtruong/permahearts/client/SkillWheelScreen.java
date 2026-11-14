package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.SkillBarLogic;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Radial wheel opened by holding the skill key (P-011b): the cross plus every filled slot around a circle; the entry in
 * the mouse direction is highlighted, releasing the key picks it, Escape cancels. Does not pause the game.
 */
public class SkillWheelScreen extends Screen {
	private static final int RADIUS = 62;
	private static final int BOX = 26;
	private int[] entries = {-1};
	private int hovered = -1; // index into entries, -1 = none

	public SkillWheelScreen() {
		super(Component.literal("Techniques"));
	}

	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			entries = SkillBarLogic.wheelEntries(ClientSkills.data(mc.player).filledSlots());
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** The entry under a mouse position (GUI pixels), or -1. */
	int entryAt(double mouseX, double mouseY) {
		return SkillBarLogic.wheelIndex(mouseX - width / 2.0, mouseY - height / 2.0, entries.length);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (ClientSkills.KEY.matches(event)) {
			Minecraft mc = Minecraft.getInstance();
			double scale = mc.getWindow().getGuiScale();
			int pick = entryAt(mc.mouseHandler.xpos() / scale, mc.mouseHandler.ypos() / scale);
			if (pick >= 0) {
				ClientSkills.select(entries[pick]);
			}
			onClose();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		g.fill(0, 0, width, height, 0x60000000);
		TechniqueData d = ClientSkills.data(mc.player);
		long now = mc.level.getGameTime();
		int cx = width / 2;
		int cy = height / 2;
		hovered = entryAt(mouseX, mouseY);
		g.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0xFFB0B0B0);
		for (int i = 0; i < entries.length; i++) {
			double a = SkillBarLogic.wheelAngle(i, entries.length);
			int x = cx + (int) Math.round(Math.sin(a) * RADIUS) - BOX / 2;
			int y = cy - (int) Math.round(Math.cos(a) * RADIUS) - BOX / 2;
			boolean hover = i == hovered;
			boolean current = entries[i] == ClientSkills.selected();
			g.fill(x, y, x + BOX, y + BOX, hover ? 0xE0505050 : 0xB0101010);
			g.outline(x, y, BOX, BOX, hover ? 0xFFFFFFFF : current ? 0xFFE8C040 : 0xFF606060);
			Technique t = entries[i] < 0 ? null : d.inSlot(entries[i]);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, t == null ? TechniquesTab.sprite("cross") : TechniquesTab.icon(t), x + 5, y + 5, 16, 16);
			if (t != null) {
				long left = d.cooldownLeft(t, now);
				if (left > 0) {
					int h = Math.round(BOX * SkillBarLogic.cooldownFraction(left, t.cooldownTicks()));
					g.fill(x + 1, y + 1, x + BOX - 1, y + 1 + Math.min(h, BOX - 2), 0xA0808080);
					g.centeredText(font, Component.literal(SkillBarLogic.secondsLeft(left) + ""), x + BOX / 2, y + BOX / 2 - 4, 0xFFFFFFFF);
				}
			}
		}
		int shown = hovered >= 0 ? hovered : indexOfSelected();
		Technique t = shown < 0 || entries[shown] < 0 ? null : d.inSlot(entries[shown]);
		Component name = t == null ? Component.literal("No technique") : Component.literal(t.displayName() + "  Lv " + d.level(t));
		g.centeredText(font, name, cx, cy + RADIUS + BOX / 2 + 8, 0xFFFFFFFF);
		if (t != null) {
			long left = d.cooldownLeft(t, now);
			Component cd = Component.literal(left > 0 ? "Cooldown " + SkillBarLogic.secondsLeft(left) + " s" : "Ready  (" + t.cooldownSeconds() + " s cooldown)");
			g.centeredText(font, cd, cx, cy + RADIUS + BOX / 2 + 20, left > 0 ? 0xFFFF8080 : 0xFFA0E0A0);
		}
	}

	private int indexOfSelected() {
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] == ClientSkills.selected()) {
				return i;
			}
		}
		return -1;
	}
}
