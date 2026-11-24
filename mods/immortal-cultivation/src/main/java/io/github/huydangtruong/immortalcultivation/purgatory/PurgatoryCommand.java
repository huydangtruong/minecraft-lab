package io.github.huydangtruong.immortalcultivation.purgatory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /purgatory: the tab's buttons as commands (solo, team, giveup, watch, leave) and, for operators, floor control
 * (win, lose, floor solo|team n, ready: no cooldown).
 */
public final class PurgatoryCommand {
	private PurgatoryCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("purgatory")
			.then(Commands.literal("solo").executes(ctx -> run(ctx.getSource(), Purgatory::startSolo)))
			.then(Commands.literal("team").executes(ctx -> run(ctx.getSource(), Purgatory::toggleTeam)))
			.then(Commands.literal("giveup").executes(ctx -> run(ctx.getSource(), Purgatory::giveUp)))
			.then(Commands.literal("leave").executes(ctx -> run(ctx.getSource(), Purgatory::stopWatching)))
			.then(Commands.literal("watch").then(Commands.argument("id", IntegerArgumentType.integer(1))
				.executes(ctx -> run(ctx.getSource(), p -> Purgatory.watch(p, IntegerArgumentType.getInteger(ctx, "id"))))))
			.then(Commands.literal("win").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> run(ctx.getSource(), p -> Purgatory.finish(p, true) ? null : "Not in the Purgatory")))
			.then(Commands.literal("lose").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> run(ctx.getSource(), p -> Purgatory.finish(p, false) ? null : "Not in the Purgatory")))
			.then(Commands.literal("ready").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> run(ctx.getSource(), p -> {
					PurgatorySavedData d = Purgatory.data(p.level().getServer());
					d.solo(p.getUUID().toString()).cooldown = 0;
					d.teamCooldown = 0;
					d.changed();
					return null;
				})))
			.then(Commands.literal("floor").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("solo").then(Commands.argument("n", IntegerArgumentType.integer(1))
					.executes(ctx -> run(ctx.getSource(), p -> {
						PurgatorySavedData d = Purgatory.data(p.level().getServer());
						d.solo(p.getUUID().toString()).floor = IntegerArgumentType.getInteger(ctx, "n");
						d.changed();
						return null;
					}))))
				.then(Commands.literal("team").then(Commands.argument("n", IntegerArgumentType.integer(1))
					.executes(ctx -> run(ctx.getSource(), p -> {
						PurgatorySavedData d = Purgatory.data(p.level().getServer());
						d.teamFloor = IntegerArgumentType.getInteger(ctx, "n");
						d.changed();
						return null;
					}))))));
	}

	private static int run(CommandSourceStack src, Function<ServerPlayer, String> action) throws CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		String error = action.apply(p);
		if (error != null) {
			src.sendFailure(Component.literal(error));
			return 0;
		}
		src.sendSuccess(() -> Component.literal("Done"), false);
		return 1;
	}
}
