package io.github.huydangtruong.immortalcultivation.godslayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.huydangtruong.immortalcultivation.GodSlayerMessages;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** /boss summon [player], /boss giveup, /boss freeze, /boss level [1..10]: operators only (P-032). */
public final class BossCommand {
	private BossCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("boss")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("summon")
				.executes(ctx -> summon(ctx.getSource(), null))
				.then(Commands.argument("target", EntityArgument.player())
					.executes(ctx -> summon(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
			.then(Commands.literal("giveup")
				.executes(ctx -> {
					if (GodSlayerHunt.giveUp(ctx.getSource().getServer())) {
						return 1;
					}
					ctx.getSource().sendFailure(GodSlayerMessages.godSlayerNoHunt());
					return 0;
				}))
			.then(Commands.literal("freeze")
				.then(Commands.literal("stand").executes(ctx -> freeze(ctx.getSource(), GodSlayer.FREEZE_STAND)))
				.then(Commands.literal("fly").executes(ctx -> freeze(ctx.getSource(), GodSlayer.FREEZE_FLY)))
				.then(Commands.literal("fly_forward").executes(ctx -> freeze(ctx.getSource(), GodSlayer.FREEZE_FLY_FORWARD)))
				.then(Commands.literal("off").executes(ctx -> freeze(ctx.getSource(), GodSlayer.FREEZE_NONE))))
			.then(Commands.literal("level")
				.executes(ctx -> {
					int level = GodSlayerHunt.level(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> GodSlayerMessages.godSlayerLevelIs(level), false);
					return level;
				})
				.then(Commands.argument("n", IntegerArgumentType.integer(1, GodSlayerRules.MAX_LEVEL))
					.executes(ctx -> {
						int n = IntegerArgumentType.getInteger(ctx, "n");
						GodSlayerHunt.setLevel(ctx.getSource().getServer(), n);
						ctx.getSource().sendSuccess(() -> GodSlayerMessages.godSlayerLevelSet(n), true);
						return n;
					}))));
	}

	/** /boss freeze stand|fly|fly_forward|off: look at the boss without it moving (D104). */
	private static int freeze(CommandSourceStack source, int pose) {
		if (GodSlayerHunt.freeze(source, pose) == null) {
			source.sendFailure(GodSlayerMessages.godSlayerNoFrozen());
			return 0;
		}
		source.sendSuccess(() -> GodSlayerMessages.godSlayerFrozen(pose), true);
		return 1;
	}

	/** Starts the warning for the given player, or a random one (no hearts requirement for a summon). */
	private static int summon(CommandSourceStack source, ServerPlayer target) {
		MinecraftServer server = source.getServer();
		if (GodSlayerHunt.phase(server) != GodSlayerSavedData.Phase.IDLE) {
			source.sendFailure(GodSlayerMessages.godSlayerBusy());
			return 0;
		}
		ServerPlayer pick = target;
		if (pick == null) {
			List<ServerPlayer> all = new ArrayList<>();
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (GodSlayerHunt.huntable(p)) {
					all.add(p);
				}
			}
			if (all.isEmpty()) {
				all.addAll(server.getPlayerList().getPlayers());
			}
			if (all.isEmpty()) {
				source.sendFailure(GodSlayerMessages.godSlayerNoPlayer());
				return 0;
			}
			pick = all.get(new java.util.Random().nextInt(all.size()));
		}
		GodSlayerHunt.start(server, pick);
		String name = GodSlayerHunt.name(pick);
		source.sendSuccess(() -> GodSlayerMessages.godSlayerSummoned(name), true);
		return 1;
	}
}
