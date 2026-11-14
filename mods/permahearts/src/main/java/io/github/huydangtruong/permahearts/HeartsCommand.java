package io.github.huydangtruong.permahearts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class HeartsCommand {
	private HeartsCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("hearts")
			.executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				HeartsData d = HeartsAttachment.get(p);
				ctx.getSource().sendSuccess(() -> Messages.status(HeartRules.maxHearts(d), d.heartXp(), d.totalHostileKills(), d.totalDeaths()), false);
				CultivationData cd = HeartsAttachment.getCultivation(p);
				ctx.getSource().sendSuccess(() -> Messages.cultivationStatus(cd), false);
				if (!PermaHearts.enabled(ctx.getSource().getServer())) {
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
