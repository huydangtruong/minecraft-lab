package io.github.huydangtruong.permahearts.technique;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Op command for tests and playtests: /technique set|emeralds|cooldowns. */
public final class TechniqueCommand {
	private TechniqueCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("technique")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("set")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.argument("technique", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Technique.values()).map(Technique::id), b))
						.then(Commands.argument("level", IntegerArgumentType.integer(0, TechniqueRules.MAX_LEVEL))
							.executes(ctx -> {
								ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
								Technique t = Technique.byId(StringArgumentType.getString(ctx, "technique")).orElse(null);
								if (t == null) {
									ctx.getSource().sendFailure(Component.literal("Unknown technique"));
									return 0;
								}
								int level = IntegerArgumentType.getInteger(ctx, "level");
								TechniqueData d = TechniqueAttachment.get(p);
								TechniqueData n = d.withLevel(t, level, d.spent(t));
								TechniqueAttachment.set(p, level == 0 ? n.withoutFromSlots(t) : n);
								ctx.getSource().sendSuccess(() -> Component.literal(t.displayName() + " set to level " + level), true);
								return 1;
							})))))
			.then(Commands.literal("emeralds")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.argument("n", IntegerArgumentType.integer(0, TechniqueRules.MAX_EMERALDS))
						.executes(ctx -> {
							ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
							int n = IntegerArgumentType.getInteger(ctx, "n");
							TechniqueAttachment.set(p, TechniqueAttachment.get(p).withEmeralds(n));
							ctx.getSource().sendSuccess(() -> Component.literal("Emeralds deposited: " + n), true);
							return 1;
						}))))
			.then(Commands.literal("cooldowns")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.literal("clear")
						.executes(ctx -> {
							ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
							TechniqueAttachment.set(p, TechniqueAttachment.get(p).withoutCooldowns());
							ctx.getSource().sendSuccess(() -> Component.literal("Cooldowns cleared"), true);
							return 1;
						})))));
	}
}
