package io.github.huydangtruong.permahearts.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/** D128: the hotbar and everything stacked on it move up 4 GUI pixels to make room for the skill bar under the hotbar. */
public final class RaisedHud {
	public static final int RAISE = 4;

	private RaisedHud() {}

	public static void register() {
		Identifier[] ids = {
			VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
			VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
			VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.INFO_BAR_FOREGROUND,
			VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.OVERLAY_MESSAGE
		};
		for (Identifier id : ids) {
			HudElementRegistry.replaceElement(id, vanilla -> (g, delta) -> {
				g.pose().pushMatrix();
				g.pose().translate(0, -RAISE);
				vanilla.extractRenderState(g, delta);
				if (id.equals(VanillaHudElements.HOTBAR)) {
					// 26.3 items cannot be drawn with alpha: a translucent black layer over the hotbar and offhand slots fades it
					// to about 50% while the skill bar slides up (D128)
					float fade = SkillBarHud.hotbarFade();
					if (fade > 0f) {
						int cx = g.guiWidth() / 2;
						var mc = net.minecraft.client.Minecraft.getInstance();
						int side = mc.player != null && !mc.player.getOffhandItem().isEmpty() ? 29 : 0; // offhand slot sits beside the bar
						g.fill(cx - 91 - side, g.guiHeight() - 22, cx + 91 + side, g.guiHeight(), Math.round(fade * 255) << 24);
					}
				}
				g.pose().popMatrix();
			});
		}
	}
}
