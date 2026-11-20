package io.github.huydangtruong.immortalcultivation.admin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** P-060 (D155): /admin opens the test panel. Not hidden for non-OPs, so they get a clear refusal instead of "unknown command". */
public final class AdminCommand {
	private AdminCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("admin").executes(ctx -> AdminNetworking.open(ctx.getSource().getPlayerOrException()) ? 1 : 0));
	}
}
