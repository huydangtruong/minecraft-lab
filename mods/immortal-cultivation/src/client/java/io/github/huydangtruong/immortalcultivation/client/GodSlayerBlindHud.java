package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerNet;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** God Slayer blast blindness (D288): the whole screen is red for 3 s, fading out at the end. */
public final class GodSlayerBlindHud implements HudElement {
	private static long endTick = -1;

	private GodSlayerBlindHud() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(GodSlayerNet.Blind.TYPE, (payload, ctx) -> {
			var level = ctx.client().level;
			endTick = level == null || payload.ticks() <= 0 ? -1 : level.getGameTime() + payload.ticks();
		});
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("permahearts", "god_slayer_blind"), new GodSlayerBlindHud());
	}

	static void reset() {
		endTick = -1;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker t) {
		Minecraft mc = Minecraft.getInstance();
		if (endTick < 0 || mc.level == null) {
			return;
		}
		float a = GodSlayerRules.blindAlpha(endTick - mc.level.getGameTime());
		if (a <= 0f) {
			endTick = -1;
			return;
		}
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (a * 255) << 24) | 0xA00000);
	}
}
