package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.huydangtruong.permahearts.Messages;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class QuestCommand {
	private QuestCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("quest")
			.executes(ctx -> status(ctx.getSource()))
			.then(Commands.literal("summon")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					Wanderer.summon(p);
					ctx.getSource().sendSuccess(() -> Messages.questAdmin("Summoned"), true);
					return 1;
				}))
			.then(Commands.literal("giveup")
				.executes(ctx -> giveUp(ctx.getSource())))
			.then(Commands.literal("skip")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> {
					Wanderer.skip(ctx.getSource().getServer());
					// Plan right now instead of waiting for the next scheduler pass.
					WandererScheduler.work(ctx.getSource().getServer(), false);
					ctx.getSource().sendSuccess(() -> Messages.questAdmin("Skipped: planning the next Wanderer"), true);
					return 1;
				}))
			.then(Commands.literal("complete")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
					.executes(ctx -> {
						int line = IntegerArgumentType.getInteger(ctx, "line");
						if (!Wanderer.complete(ctx.getSource().getServer(), line - 1)) {
							ctx.getSource().sendFailure(Messages.questAdmin("No such quest line"));
							return 0;
						}
						return 1;
					})))
			.then(Commands.literal("reset")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> {
					Wanderer.reset(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Messages.questAdmin("Wanderer reset"), true);
					return 1;
				})));
	}

	/** Anyone, like the Give up button (D28): the Crimson Master leaves, the next one comes in 4 days. */
	private static int giveUp(CommandSourceStack src) {
		MinecraftServer server = src.getServer();
		if (!io.github.huydangtruong.permahearts.PermaHearts.enabled(server)) {
			src.sendFailure(Messages.disabledHint());
			return 0;
		}
		Component who = src.getDisplayName();
		Phase phase = Wanderer.data(server).phase();
		if (phase != Phase.PLANNED && phase != Phase.PRESENT) {
			src.sendFailure(Messages.nothingToGiveUp());
			return 0;
		}
		Wanderer.leave(server, Messages.gaveUp(who));
		return 1;
	}

	private static int status(CommandSourceStack src) {
		MinecraftServer server = src.getServer();
		WandererData d = Wanderer.data(server);
		switch (d.phase()) {
			case PLANNED, PRESENT -> {
				int dist = 0;
				ServerPlayer p = src.getPlayer();
				if (p != null) {
					dist = (int) Math.round(Math.hypot(p.getX() - d.x(), p.getZ() - d.z()));
				}
				int fd = dist;
				src.sendSuccess(() -> Messages.questWhere(d.x(), d.z(), fd), false);
			}
			case WAITING -> {
				long days = Math.max(0, d.nextDay() - Wanderer.currentDay(server));
				src.sendSuccess(() -> Messages.questNext(days), false);
			}
		}
		if (d.phase() == Phase.PLANNED || d.phase() == Phase.PRESENT) {
			String left = StayTime.format(Wanderer.ticksLeft(server));
			src.sendSuccess(() -> Messages.leavesIn(left), false);
		}
		if (d.phase() == Phase.PRESENT && d.quest().isPresent()) {
			Quest q = d.quest().get();
			if (!q.accepted()) {
				src.sendSuccess(Messages::questNoLines, false);
			} else {
				List<QuestLine> lines = q.lines();
				for (int i = 0; i < lines.size(); i++) {
					QuestLine l = lines.get(i);
					Component text = Wanderer.lineText(l.type(), l.target(), l.required(), l.progress());
					int n = i + 1;
					src.sendSuccess(() -> Messages.questLineStatus(n, text), false);
				}
			}
		}
		return 1;
	}
}
