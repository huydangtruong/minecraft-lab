package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.quit.QuitNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** D218: the host's client leaves the world like "Save and Quit to Title" (the integrated server saves and stops), then closes the game. */
public final class QuitClient {
	private QuitClient() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(QuitNet.CloseGame.TYPE, (payload, ctx) -> ctx.client().execute(() -> closeGame(ctx.client())));
	}

	private static void closeGame(Minecraft mc) {
		// Same call as the pause screen button. For a local server it shows "Saving world" and blocks until the server thread has stopped.
		mc.disconnectFromWorld(Component.translatable("menu.savingLevel"));
		mc.stop();
	}
}
