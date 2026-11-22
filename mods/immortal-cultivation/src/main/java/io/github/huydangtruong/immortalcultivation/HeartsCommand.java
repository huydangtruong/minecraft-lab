package io.github.huydangtruong.immortalcultivation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class HeartsCommand {
	/** Highest elixir count /hearts elixir accepts. */
	public static final int ELIXIR_MAX = 1000;

	private HeartsCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("hearts")
			.executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				HeartsData d = HeartsAttachment.get(p);
				ctx.getSource().sendSuccess(() -> Messages.status(HeartRules.maxHearts(d), d.heartXp(), d.totalHostileKills(), d.totalDeaths()), false);
				CultivationData cd = HeartsAttachment.getCultivation(p);
				ctx.getSource().sendSuccess(() -> Messages.cultivationStatus(cd), false);
				if (!ImmortalCultivation.enabled(ctx.getSource().getServer())) {
					ctx.getSource().sendSuccess(Messages::disabledHint, false);
				}
				return 1;
			})
			.then(Commands.literal("set")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("n", IntegerArgumentType.integer(1, HeartRules.MAX))
						.executes(ctx -> {
							int n = IntegerArgumentType.getInteger(ctx, "n");
							var targets = EntityArgument.getPlayers(ctx, "targets");
							for (ServerPlayer p : targets) {
								HeartsAttachment.set(p, HeartRules.withHearts(HeartsAttachment.get(p), n));
								HealthApplier.apply(p, false);
								ctx.getSource().sendSuccess(() -> Messages.set(p.getDisplayName(), n), true);
							}
							return targets.size();
						}))))
			.then(Commands.literal("elixir") // D186: /admin Elixirs tab sets each elixir bonus, down as well as up
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("stat", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(ElixirStat.values()).map(ElixirStat::id), b))
						.then(Commands.argument("n", IntegerArgumentType.integer(0, ELIXIR_MAX))
							.executes(ctx -> {
								String id = StringArgumentType.getString(ctx, "stat");
								ElixirStat stat = ElixirStat.byId(id).orElse(null);
								if (stat == null) {
									ctx.getSource().sendFailure(Component.literal("Unknown elixir: " + id));
									return 0;
								}
								int n = IntegerArgumentType.getInteger(ctx, "n");
								var targets = EntityArgument.getPlayers(ctx, "targets");
								for (ServerPlayer p : targets) {
									CultivationData c = HeartsAttachment.getCultivation(p);
									HeartsAttachment.setCultivation(p, c.plus(stat, n - c.level(stat)));
									HealthApplier.apply(p, false);
									ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + ": " + stat.id() + " elixirs " + n
										+ " (" + stat.format(stat.bonus(n)) + ")"), true);
								}
								return targets.size();
							})))))
			.then(Commands.literal("xp")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("n", IntegerArgumentType.integer(0, 499))
						.executes(ctx -> {
							int n = IntegerArgumentType.getInteger(ctx, "n");
							var targets = EntityArgument.getPlayers(ctx, "targets");
							for (ServerPlayer p : targets) {
								HeartsAttachment.set(p, HeartRules.withXp(HeartsAttachment.get(p), n));
								HealthApplier.apply(p, false);
								ctx.getSource().sendSuccess(() -> Messages.xpSet(p.getDisplayName(), n), true);
							}
							return targets.size();
						})))));
	}
}
