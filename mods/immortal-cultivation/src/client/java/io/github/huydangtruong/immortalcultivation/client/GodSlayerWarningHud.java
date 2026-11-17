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

/** Red screen edge while the God Slayer warning runs: fixed thickness, pulses once per second with the drum, gone when it ends (P-035, D102). */
public final class GodSlayerWarningHud implements HudElement {
	private static long endTick = -1; // client game time when the warning ends; -1 = no warning

	private GodSlayerWarningHud() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(GodSlayerNet.Warning.TYPE, (payload, ctx) -> {
			var level = ctx.client().level;
			setTicksLeft(level == null || payload.ticksLeft() <= 0 ? -1 : level.getGameTime() + payload.ticksLeft());
		});
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("permahearts", "god_slayer_warning"), new GodSlayerWarningHud());
	}

	/** Sets the client game time when the warning ends (-1 clears it). Also used by the client test. */
	public static void setTicksLeft(long end) {
		endTick = end;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker t) {
		Minecraft mc = Minecraft.getInstance();
		if (endTick < 0 || mc.level == null) {
			return;
		}
		long left = endTick - mc.level.getGameTime();
		float s = GodSlayerRules.edgeStrength(left);
		if (s <= 0f) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		// pulses once per second, in time with the drum
		float pulse = GodSlayerRules.edgePulse(left);
		int maxThick = Math.max(1, (int) (Math.min(w, h) * GodSlayerRules.EDGE_THICKNESS));
		// 1 px rings, each a little more transparent than the one outside it: a smooth gradient, no bands
		for (int i = 0; i < maxThick; i++) {
			float a = GodSlayerRules.edgeLayerAlpha(s, pulse, i, maxThick);
			int color = ((int) (a * 255) << 24) | 0xC00000;
			if ((color >>> 24) == 0) {
				continue;
			}
			g.fill(i, i, w - i, i + 1, color);
			g.fill(i, h - i - 1, w - i, h - i, color);
			g.fill(i, i + 1, i + 1, h - i - 1, color);
			g.fill(w - i - 1, i + 1, w - i, h - i - 1, color);
		}
	}
}
