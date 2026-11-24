package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryNet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Client side of the Purgatory (D234-D240): the last state the server sent (once a second), the key that ends watching
 * and the pause menu button "Thoát theo dõi luyện ngục" (D238). The server checks every request; this only asks.
 */
public final class ClientPurgatory {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("permahearts", "purgatory"));
	/** D238: stop watching a floor. */
	public static final KeyMapping LEAVE = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.permahearts.purgatory_leave", com.mojang.blaze3d.platform.InputConstants.KEY_O, CATEGORY));
	static final String LEAVE_LABEL = "Thoát theo dõi luyện ngục";

	private static PurgatoryNet.State state = PurgatoryNet.State.EMPTY;
	/** Client tick when state arrived: timers count down between updates. */
	private static long receivedAt;
	private static long ticks;

	private ClientPurgatory() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PurgatoryNet.State.TYPE, (payload, ctx) -> {
			state = payload;
			receivedAt = ticks;
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ticks++;
			while (LEAVE.consumeClick()) {
				if (state.mode() == PurgatoryNet.MODE_WATCHING) {
					send(PurgatoryNet.STOP_WATCH, 0);
				}
			}
		});
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (screen instanceof PauseScreen && state.mode() == PurgatoryNet.MODE_WATCHING) {
				Screens.getWidgets(screen).add(Button.builder(Component.literal(LEAVE_LABEL), b -> {
					send(PurgatoryNet.STOP_WATCH, 0);
					mc.gui.setScreen(null);
				}).bounds(w / 2 - 102, h - 30, 204, 20).build());
			}
		});
	}

	public static PurgatoryNet.State state() {
		return state;
	}

	/** Ticks since the last update (timers shown in the tab subtract this so they move smoothly). */
	public static int age() {
		return (int) Math.min(20, ticks - receivedAt);
	}

	public static void send(int kind, int arg) {
		if (ClientPlayNetworking.canSend(PurgatoryNet.Action.TYPE)) {
			ClientPlayNetworking.send(new PurgatoryNet.Action(kind, arg));
		}
	}

	public static void reset() {
		state = PurgatoryNet.State.EMPTY;
	}

	/** Tests: pretend the server sent this. */
	public static void testState(PurgatoryNet.State s) {
		state = s;
		receivedAt = ticks;
	}
}
