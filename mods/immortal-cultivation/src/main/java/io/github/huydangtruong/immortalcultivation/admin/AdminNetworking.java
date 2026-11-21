package io.github.huydangtruong.immortalcultivation.admin;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.danger.Danger;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** P-060 (D155): /admin networking. The server opens the screen (with the current game rule values) and runs the lines the OP confirms. */
public final class AdminNetworking {
	private AdminNetworking() {}

	/**
	 * Server to client: open the panel. Carries the current values of the mod game rules, the player's no-cooldown switch and
	 * (D186) the player's elixir counts in {@link io.github.huydangtruong.immortalcultivation.ElixirStat} order.
	 */
	public record Open(boolean cultivation, boolean danger, int spawnCeiling, boolean noCooldown, List<Integer> elixirs) implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "admin_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Open::cultivation,
			ByteBufCodecs.BOOL, Open::danger,
			ByteBufCodecs.VAR_INT, Open::spawnCeiling,
			ByteBufCodecs.BOOL, Open::noCooldown,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), Open::elixirs,
			Open::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: the lines to run, in order. The server cleans and limits them again. */
	public record Run(List<String> lines) implements CustomPacketPayload {
		public static final Type<Run> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "admin_run"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Run> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(1024).apply(ByteBufCodecs.list(512)), Run::lines,
			Run::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Run.TYPE, Run.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Run.TYPE, (payload, ctx) -> run(ctx.player(), payload.lines()));
	}

	public static boolean isOp(ServerPlayer p) {
		return Commands.LEVEL_GAMEMASTERS.check(p.permissions());
	}

	/** Sends the open packet. False (and a failure message) for a player below permission level 2. */
	public static boolean open(ServerPlayer p) {
		if (!isOp(p)) {
			p.sendSystemMessage(Component.literal("/admin needs operator permission (level 2)."));
			return false;
		}
		MinecraftServer server = p.level().getServer();
		var rules = server.getGameRules();
		var cultivation = io.github.huydangtruong.immortalcultivation.HeartsAttachment.getCultivation(p);
		List<Integer> elixirs = java.util.Arrays.stream(io.github.huydangtruong.immortalcultivation.ElixirStat.values()).map(cultivation::level).toList();
		ServerPlayNetworking.send(p, new Open(rules.get(ImmortalCultivation.ENABLED), rules.get(Danger.ENABLED), rules.get(Danger.SPAWN_CEILING),
			io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment.get(p).noCooldown(), elixirs));
		return true;
	}

	/** Re-checks permission, cleans the lines, runs each one in order as if the player typed it. Returns how many were run (0 for a non-OP). */
	public static int run(ServerPlayer p, List<String> raw) {
		if (!isOp(p)) {
			p.sendSystemMessage(Component.literal("/admin needs operator permission (level 2). Nothing was run."));
			return 0;
		}
		AdminDraft.Result r = AdminDraft.sanitize(raw);
		if (r.dropped() > 0) {
			p.sendSystemMessage(Component.literal("Dropped " + r.dropped() + " line(s): the limit is " + AdminDraft.MAX_LINES
				+ " lines of " + AdminDraft.MAX_LENGTH + " characters."));
		}
		MinecraftServer server = p.level().getServer();
		for (String line : r.lines()) {
			server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), line);
		}
		return r.lines().size();
	}
}
