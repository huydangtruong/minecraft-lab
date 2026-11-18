package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.Seism;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client side of Seism: the camera shake the server asks for (decays over its ticks). */
public final class ClientSeism {
	private static float strength;
	private static int ticks;
	private static int total;
	private static final java.util.Random RANDOM = new java.util.Random();

	private ClientSeism() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(Seism.Shake.TYPE, (payload, ctx) -> {
			strength = Math.max(strength * (ticks > 0 ? 1f : 0f), payload.strength());
			total = Math.max(1, payload.ticks());
			ticks = total;
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (ticks > 0) {
				ticks--;
			}
		});
	}

	/** Degrees of shake for this frame (0 when none). */
	public static float shakeNow() {
		return ticks <= 0 ? 0f : strength * 3f * ticks / total;
	}

	/** A random number in [-1, 1]. */
	public static float noise() {
		return RANDOM.nextFloat() * 2f - 1f;
	}
}
