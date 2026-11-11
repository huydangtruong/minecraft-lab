package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.HeartRules;
import io.github.huydangtruong.permahearts.HeartsAttachment;
import io.github.huydangtruong.permahearts.HeartsData;
import io.github.huydangtruong.permahearts.Messages;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** HeartXP bar at the bottom-left of the screen. */
public class HeartXpHud implements HudElement {
	private static final int X = 20; // 16px further from the left edge (Huy, 2026-11-12)
	private static final int W = 102;
	private static final int H = 5;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker t) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || mc.gui.hud.isHidden() || p.isCreative() || p.isSpectator()
			|| !p.getAttachedOrElse(HeartsAttachment.ACTIVE, false)) {
			return;
		}
		HeartsData d = p.getAttachedOrElse(HeartsAttachment.HEARTS, HeartsData.DEFAULT);
		int barY = g.guiHeight() - 20 - H; // 16px further from the bottom edge
		int labelY = barY - 10;
		Component label = HeartRules.maxHearts(d) >= HeartRules.MAX ? Messages.hudMax() : Messages.hudLabel(d.heartXp());
		g.text(mc.font, label, X, labelY, 0xFFFF5555, true);
		g.fill(X, barY, X + W, barY + H, 0xA0000000);
		g.fill(X + 1, barY + 1, X + 1 + (int) ((W - 2) * HeartRules.xpFraction(d)), barY + H - 1, 0xFFE03C3C);
		g.outline(X, barY, W, H, 0xFF3A0A0A);
	}
}
