package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.Mayhem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Crystal Mayhem on the client (D174): the shards left and the seconds left of the 20 s window, for the HUD line. */
public final class ClientMayhem {
	private static int shards = -1;
	private static int windowTicks;

	private ClientMayhem() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(Mayhem.State.TYPE, (payload, ctx) -> {
			shards = payload.shards();
			windowTicks = payload.windowTicks();
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null) {
				shards = -1;
				windowTicks = 0;
			} else if (windowTicks > 0) {
				windowTicks--;
			}
		});
	}

	/** Shards left, or -1 when nothing runs. */
	public static int shards() {
		return shards;
	}

	/** Ticks left of the window, 0 when none. */
	public static int windowTicks() {
		return windowTicks;
	}

	static boolean visible(Minecraft mc) {
		return mc.player != null && shards >= 0;
	}
}
