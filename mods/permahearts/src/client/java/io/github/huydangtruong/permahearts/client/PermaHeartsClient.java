package io.github.huydangtruong.permahearts.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.world.entity.EntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import io.github.huydangtruong.permahearts.wanderer.WandererMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

public class PermaHeartsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("permahearts", "heart_xp"), new HeartXpHud());
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("permahearts", "skill_bar"), new SkillBarHud());
		ClientSkills.register();
		MenuScreens.register(WandererMenus.TYPE, WandererScreen::new);
		StatsPanel.register();
		LayeredHeartsHud.register();
		// Replaces the wandering trader renderer; ordinary traders keep the vanilla texture.
		EntityRendererRegistry.register(EntityTypes.WANDERING_TRADER, WandererRenderer::new);
	}
}
