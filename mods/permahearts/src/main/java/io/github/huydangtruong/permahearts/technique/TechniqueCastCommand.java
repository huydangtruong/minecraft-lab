package io.github.huydangtruong.permahearts.technique;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /technique cast <technique> <level> [charge]}: op command that casts as the player running it (part C). */
public final class TechniqueCastCommand {
	private TechniqueCastCommand() {}

	/** Adds the cast subcommand under the shared /technique literal (Brigadier merges with other registrations). */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("technique")
			.then(Commands.literal("cast")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("technique", StringArgumentType.word())
					.suggests((ctx, b) -> {
						Arrays.stream(Technique.values()).forEach(t -> b.suggest(t.id()));
						return b.buildFuture();
					})
					.then(Commands.argument("level", IntegerArgumentType.integer(1, TechniqueRules.MAX_LEVEL))
						.executes(ctx -> run(ctx.getSource(), StringArgumentType.getString(ctx, "technique"),
							IntegerArgumentType.getInteger(ctx, "level"), 1f))
						.then(Commands.argument("charge", FloatArgumentType.floatArg(0f, 1f))
							.executes(ctx -> run(ctx.getSource(), StringArgumentType.getString(ctx, "technique"),
								IntegerArgumentType.getInteger(ctx, "level"), FloatArgumentType.getFloat(ctx, "charge"))))))));
	}

	private static int run(CommandSourceStack src, String id, int level, float charge) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		var t = Technique.byId(id);
		if (t.isEmpty()) {
			src.sendFailure(Component.literal("Unknown technique '" + id + "'. Use: "
				+ String.join(", ", Arrays.stream(Technique.values()).map(Technique::id).toList())));
			return 0;
		}
		if (!PermaHearts.enabled(src.getServer())) {
			src.sendFailure(Component.literal("ImmortalCultivation is off in this world, techniques do nothing"));
			return 0;
		}
		TechniqueCasts.cast(p, t.get(), level, charge);
		src.sendSuccess(() -> Component.literal("Cast " + t.get().displayName() + " level " + level), false);
		return 1;
	}
}
