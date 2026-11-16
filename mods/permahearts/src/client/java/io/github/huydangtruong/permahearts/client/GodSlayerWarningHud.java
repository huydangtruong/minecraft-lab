package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.godslayer.GodSlayerNet;
import io.github.huydangtruong.permahearts.godslayer.GodSlayerRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Red screen edge while the God Slayer warning runs: grows toward 0, gone when it ends (P-035, D102). */
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
		// a pulse that quickens with the heartbeat
		double beat = GodSlayerRules.heartbeatInterval(GodSlayerRules.secondsLeft(left));
		float pulse = 0.85f + 0.15f * (float) Math.sin((mc.level.getGameTime() + t.getGameTimeDeltaPartialTick(false)) * (2 * Math.PI / beat));
		int maxThick = (int) (Math.min(w, h) * (0.06 + 0.22 * s));
		int layers = Math.min(60, Math.max(1, maxThick));
		for (int i = 0; i < layers; i++) {
			int inset = (int) (maxThick * (i / (double) layers));
			float a = s * pulse * 0.85f * (1f - i / (float) layers);
			int color = ((int) (Math.min(1f, a) * 255) << 24) | 0xC00000;
			int thick = Math.max(1, maxThick / layers);
			g.fill(inset, inset, w - inset, inset + thick, color);
			g.fill(inset, h - inset - thick, w - inset, h - inset, color);
			g.fill(inset, inset + thick, inset + thick, h - inset - thick, color);
			g.fill(w - inset - thick, inset + thick, w - inset, h - inset - thick, color);
		}
	}
}
