package io.github.huydangtruong.immortalcultivation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.world.entity.EntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

public class ImmortalCultivationClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("permahearts", "heart_xp"), new HeartXpHud());
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("permahearts", "skill_bar"), new SkillBarHud());
		ClientSkills.register();
		MegaGlowLayer.register(); // P-059
		MenuScreens.register(WandererMenus.TYPE, WandererScreen::new);
		MenuScreens.register(io.github.huydangtruong.immortalcultivation.reward.RewardBoxes.MENU, RewardBoxScreen::new); // P-027
		StatsPanel.register();
		LayeredHeartsHud.register();
		GodSlayerWarningHud.register();
		// Replaces the wandering trader renderer; ordinary traders keep the vanilla texture.
		EntityRendererRegistry.register(EntityTypes.WANDERING_TRADER, WandererRenderer::new);
		EntityRendererRegistry.register(io.github.huydangtruong.immortalcultivation.technique.VioletGuardian.TYPE, VioletGuardianRenderer::new);
		EntityRendererRegistry.register(io.github.huydangtruong.immortalcultivation.technique.WarriorAngel.TYPE, WarriorAngelRenderer::new);
		EntityRendererRegistry.register(io.github.huydangtruong.immortalcultivation.technique.Illusion.TYPE, IllusionRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(GodSlayerRenderer.LAYER, GodSlayerModel::createLayer);
		EntityRendererRegistry.register(io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer.TYPE, GodSlayerRenderer::new);
		// Cosmic Warping (P-026, D75)
		EntityRendererRegistry.register(io.github.huydangtruong.immortalcultivation.technique.warp.WarpPortal.TYPE, WarpPortalRenderer::new);
		ClientGrasp.register(); // Soul Grasp (P-053)
		ClientMayhem.register(); // Crystal Mayhem HUD (P-064)
		ClientLance.register(); // Solar Lance (P-061)
		LanceBeamHud.register(); // Solar Lance beam over the HUD (D163)
		ClientSeism.register(); // Seism camera shake (P-056)
		QuitClient.register(); // D218: /quit closes the host's game
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
			io.github.huydangtruong.immortalcultivation.admin.AdminNetworking.Open.TYPE,
			(payload, ctx) -> ctx.client().gui.setScreen(new AdminScreen(payload))); // P-060: /admin panel
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
			io.github.huydangtruong.immortalcultivation.technique.warp.Warp.Menu.TYPE, (payload, ctx) -> {
				var mc = ctx.client();
				if (mc.gui.screen() instanceof WarpScreen open) {
					open.update(payload);
				} else {
					mc.gui.setScreen(new WarpScreen(payload));
				}
			});
	}
}
