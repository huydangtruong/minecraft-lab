package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * D260: opens once when the floor's boss dies. Shows the seconds left before going back, a Come back now button and Stay
 * (or Esc), which closes it; the same Come back now button then waits in the inventory.
 */
public class PurgatoryClearedScreen extends Screen {
	static final String COME_BACK_LABEL = "Come back now";

	public PurgatoryClearedScreen() {
		super(Component.literal("Floor cleared!"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal(COME_BACK_LABEL), b -> {
			ClientPurgatory.send(PurgatoryNet.COME_BACK, 0);
			onClose();
		}).bounds(width / 2 - 104, height / 2 + 4, 100, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Stay"), b -> onClose())
			.bounds(width / 2 + 4, height / 2 + 4, 100, 20).build());
	}

	@Override
	public void tick() {
		if (ClientPurgatory.state().mode() != PurgatoryNet.MODE_CLEARED) {
			onClose(); // went back (time up or another way)
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		g.centeredText(font, title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), width / 2, height / 2 - 30, 0xFFFFAA00);
		g.centeredText(font, Component.literal("Going back in " + ClientPurgatory.victorySeconds() + " s"), width / 2, height / 2 - 14,
			0xFFFFFFFF);
	}
}
