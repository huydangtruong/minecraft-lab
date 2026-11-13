package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;

/**
 * Makes the Crimson Master hard to miss (D26): a big title in the middle of the screen when it appears and when it is
 * about to leave, and a boss bar at the top with its coordinates and the time left, for every online player, for the
 * whole visit. Vanilla packets only, so it also works for players without the mod.
 */
public final class WandererBar {
	private static ServerBossEvent bar;

	private WandererBar() {}

	/** Once per second from the scheduler: show / update the bar while a visit runs, hide it otherwise. */
	public static void tick(MinecraftServer server) {
		WandererData d = Wanderer.data(server);
		if (d.phase() != Phase.PLANNED && d.phase() != Phase.PRESENT) {
			hide();
			return;
		}
		long left = Wanderer.ticksLeft(server);
		Component name = Messages.masterBar(d.x(), d.z(), StayTime.format(left));
		if (bar == null) {
			bar = new ServerBossEvent(UUID.randomUUID(), name, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
		} else {
			bar.setName(name);
		}
		bar.setProgress(Math.max(0f, Math.min(1f, left / (float) Wanderer.STAY_TICKS)));
		// Players respawn as new ServerPlayer objects and can leave: keep exactly the current online players.
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

	/** Removes the bar from everyone (visit over, switch off, reset). */
	public static void hide() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
	}

	/** Server stopped (singleplayer can open another world in the same game): drop the bar without sending packets. */
	public static void forget() {
		bar = null;
	}

	/** True while the bar is shown (tests). */
	public static boolean shown() {
		return bar != null;
	}

	/** The bar's current text (tests). */
	public static String text() {
		return bar == null ? "" : bar.getName().getString();
	}

	/** Big title for every online player: the Crimson Master appeared. */
	public static void announceArrival(MinecraftServer server, int x, int z) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			title(p, Messages.masterTitle(), Messages.masterArrivedSubtitle(x, z));
		}
	}

	/** Big title for every online player: the Crimson Master leaves soon. */
	public static void announceLeavingSoon(MinecraftServer server, int x, int z) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			title(p, Messages.masterTitle(), Messages.masterLeavingSubtitle(x, z));
		}
	}

	private static void title(ServerPlayer p, Component title, Component subtitle) {
		// fade in 10, stay 100 (5 s), fade out 20 ticks; the subtitle must be sent before the title.
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
		p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 1f);
	}
}
