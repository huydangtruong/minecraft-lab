package io.github.huydangtruong.immortalcultivation.quit;

import com.mojang.brigadier.CommandDispatcher;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * D218: /quit. Anyone in a hosted world (integrated server) starts a 10 s countdown; /quit again cancels it. At zero the
 * world is saved, the friends are kicked and the host's client leaves the world and closes the game. Thin glue around
 * {@link QuitCountdown}; the final step sits behind {@link #finalStep} so game tests never really stop the server.
 */
public final class Quit {
	private Quit() {}

	private static final QuitCountdown COUNTDOWN = new QuitCountdown();

	/** Test hook: is this a hosted (integrated) world? Dedicated servers answer false. */
	public static Predicate<MinecraftServer> hosted = server -> !server.isDedicatedServer();
	/** Test hook: the final "save + kick + close" step. */
	public static Consumer<MinecraftServer> finalStep = Quit::saveAndClose;

	public static QuitCountdown countdown() {
		return COUNTDOWN;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Quit::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> COUNTDOWN.cancel());
	}

	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("quit")
			.requires(Commands.hasPermission(Commands.LEVEL_ALL))
			.executes(ctx -> run(ctx.getSource())));
	}

	static int run(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		if (!hosted.test(server)) {
			source.sendFailure(Component.literal("/quit only works in a hosted world, not on a dedicated server."));
			return 0;
		}
		String who = source.getTextName();
		if (COUNTDOWN.toggle()) {
			broadcast(server, Component.literal(who + " started it: the world saves and closes in " + QuitCountdown.SECONDS
				+ " s. Type /quit again to cancel."));
			overlay(server, QuitCountdown.SECONDS);
		} else {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				p.sendSystemMessage(Component.literal(who + " cancelled it: the world stays open."));
				p.sendOverlayMessage(Component.literal("Closing the world cancelled"));
			}
		}
		return 1;
	}

	/** Called every server tick (also by game tests). */
	public static void tick(MinecraftServer server) {
		switch (COUNTDOWN.tick()) {
			case SECOND -> overlay(server, COUNTDOWN.secondsLeft());
			case FIRE -> finalStep.accept(server);
			default -> { }
		}
	}

	private static void broadcast(MinecraftServer server, Component text) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.sendSystemMessage(text);
		}
	}

	private static void overlay(MinecraftServer server, int seconds) {
		Component text = Component.literal("World saves and closes in " + seconds + " s - type /quit again to cancel");
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.sendOverlayMessage(text);
		}
	}

	/**
	 * Save everything, kick everybody but the host, then tell the host's client to leave and close the game. The client's
	 * normal leave (Minecraft#disconnectFromWorld) halts the integrated server and waits for it, which saves players and
	 * chunks again in MinecraftServer#stopServer; only after that does the client stop the game.
	 */
	static void saveAndClose(MinecraftServer server) {
		ImmortalCultivation.LOGGER.info("/quit: saving the world and closing the game");
		server.saveEverything(false, true, true);
		ServerPlayer host = null;
		for (ServerPlayer p : server.getPlayerList().getPlayers().toArray(new ServerPlayer[0])) {
			if (host == null && server.isSingleplayerOwner(p.nameAndId())) {
				host = p;
			} else {
				p.connection.disconnect(Component.literal("The host's world was saved and closed"));
			}
		}
		if (host != null) {
			ServerPlayNetworking.send(host, new QuitNet.CloseGame());
		} else {
			server.halt(false);
		}
	}
}
