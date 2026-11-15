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
		ModeHotbarHud.register();
		// Replaces the wandering trader renderer; ordinary traders keep the vanilla texture.
		EntityRendererRegistry.register(EntityTypes.WANDERING_TRADER, WandererRenderer::new);
		EntityRendererRegistry.register(io.github.huydangtruong.permahearts.technique.VioletGuardian.TYPE, VioletGuardianRenderer::new);
		EntityRendererRegistry.register(io.github.huydangtruong.permahearts.technique.WarriorAngel.TYPE, WarriorAngelRenderer::new);
		EntityRendererRegistry.register(io.github.huydangtruong.permahearts.technique.Illusion.TYPE, IllusionRenderer::new);
		// Cosmic Warping (P-026, D75)
		EntityRendererRegistry.register(io.github.huydangtruong.permahearts.technique.warp.WarpPortal.TYPE, WarpPortalRenderer::new);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
			io.github.huydangtruong.permahearts.technique.warp.Warp.Menu.TYPE, (payload, ctx) -> {
				var mc = ctx.client();
				if (mc.gui.screen() instanceof WarpScreen open) {
					open.update(payload);
				} else {
					mc.gui.setScreen(new WarpScreen(payload));
				}
			});
	}
}
