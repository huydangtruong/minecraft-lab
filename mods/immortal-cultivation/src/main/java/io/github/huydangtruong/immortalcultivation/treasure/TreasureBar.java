package io.github.huydangtruong.immortalcultivation.treasure;

import io.github.huydangtruong.immortalcultivation.wanderer.StayTime;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * D187: while a Deadly Treasure runs, a purple bar at the top of every online player's screen shows where it is, how many
 * guards are left once the chest is placed, and the time before it vanishes (like the Crimson Master's bar). Vanilla packets only.
 */
public final class TreasureBar {
	private static ServerBossEvent bar;

	private TreasureBar() {}

	/** Once per second: show / update the bar while the event is active, hide it otherwise. */
	public static void tick(MinecraftServer server, TreasureSavedData d) {
		if (d.phase != TreasureRules.Phase.ACTIVE) {
			hide();
			return;
		}
		long left = Math.max(0, d.endAt - Wanderer.clockTime(server));
		Component name = DeadlyTreasure.text(label(d, StayTime.format(left)));
		if (bar == null) {
			bar = new ServerBossEvent(UUID.randomUUID(), name, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
		} else {
			bar.setName(name);
		}
		bar.setProgress(Math.max(0f, Math.min(1f, left / (float) TreasureRules.DURATION_TICKS)));
		List<ServerPlayer> online = server.getPlayerList().getPlayers();
		for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
			if (!online.contains(p)) {
				bar.removePlayer(p);
			}
		}
		for (ServerPlayer p : online) {
			if (!bar.getPlayers().contains(p)) {
				bar.addPlayer(p);
			}
		}
	}

	/** The bar text: where, guards left once placed, time left. */
	static String label(TreasureSavedData d, String timeLeft) {
		if (!d.placed) {
			return "Deadly Treasure: near X " + d.x + ", Z " + d.z + " · vanishes in " + timeLeft;
		}
		net.minecraft.core.BlockPos c = net.minecraft.core.BlockPos.of(d.chest);
		return "Deadly Treasure: X " + c.getX() + ", Y " + c.getY() + ", Z " + c.getZ() + " · " + d.guards.size() + " guard"
			+ (d.guards.size() == 1 ? "" : "s") + " left · vanishes in " + timeLeft;
	}

	/** Removes the bar from everyone (event over, switch off). */
	public static void hide() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
	}

	/** Server stopped: drop the bar without sending packets. */
	public static void forget() {
		bar = null;
	}

	/** The bar's current text, empty when hidden (tests). */
	public static String text() {
		return bar == null ? "" : bar.getName().getString();
	}
}
