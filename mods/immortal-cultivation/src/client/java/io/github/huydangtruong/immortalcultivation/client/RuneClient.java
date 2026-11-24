package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.rune.RuneForgeMenu;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screens.MenuScreens;

/** D241-D242 on the client: the Rune Forge screen and the orange rune lines in item tooltips. */
public final class RuneClient {
	private RuneClient() {}

	public static void register() {
		MenuScreens.register(RuneForgeMenu.TYPE, RuneForgeScreen::new);
		ItemTooltipCallback.EVENT.register((stack, ctx, flag, lines) -> {
			var extra = Runes.tooltip(stack);
			if (!extra.isEmpty()) {
				lines.addAll(Math.min(1, lines.size()), extra);
			}
		});
	}
}
