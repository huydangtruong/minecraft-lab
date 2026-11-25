package io.github.huydangtruong.immortalcultivation.purgatory;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Run;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Solo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Purgatory tab traffic (D234-D238): the state once a second to every player, and the tab's buttons back. */
public final class PurgatoryNet {
	public static final int START_SOLO = 0;
	public static final int TEAM = 1;
	public static final int GIVE_UP = 2;
	public static final int WATCH = 3;
	public static final int STOP_WATCH = 4;
	/** D260: a winner goes back before the 30 seconds are up. */
	public static final int COME_BACK = 5;

	/** What the player is doing now. */
	public static final int MODE_NONE = 0;
	public static final int MODE_FIGHTING = 1;
	public static final int MODE_FALLEN = 2;
	public static final int MODE_WATCHING = 3;
	/** D260: the boss is dead, going back soon. */
	public static final int MODE_CLEARED = 4;

	public record Action(int kind, int arg) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "purgatory_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Action::kind,
			ByteBufCodecs.VAR_INT, Action::arg,
			Action::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** One running floor as the tab lists it. */
	public record RunInfo(int id, boolean team, int floor, int ticksLeft, String names) {
		public static final StreamCodec<RegistryFriendlyByteBuf, RunInfo> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RunInfo::id,
			ByteBufCodecs.BOOL, RunInfo::team,
			ByteBufCodecs.VAR_INT, RunInfo::floor,
			ByteBufCodecs.VAR_INT, RunInfo::ticksLeft,
			ByteBufCodecs.STRING_UTF8, RunInfo::names,
			RunInfo::new);
	}

	public record State(int soloFloor, int soloCooldown, int teamFloor, int teamCooldown, boolean queued, int queueSize,
			int countdown, int mode, int victoryTicks, List<RunInfo> runs) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "purgatory_state"));
		public static final State EMPTY = new State(1, 0, 1, 0, false, 0, -1, MODE_NONE, 0, List.of());
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::soloFloor,
			ByteBufCodecs.VAR_INT, State::soloCooldown,
			ByteBufCodecs.VAR_INT, State::teamFloor,
			ByteBufCodecs.VAR_INT, State::teamCooldown,
			ByteBufCodecs.BOOL, State::queued,
			ByteBufCodecs.VAR_INT, State::queueSize,
			ByteBufCodecs.INT, State::countdown,
			ByteBufCodecs.VAR_INT, State::mode,
			ByteBufCodecs.VAR_INT, State::victoryTicks,
			RunInfo.CODEC.apply(ByteBufCodecs.list()), State::runs,
			State::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private PurgatoryNet() {}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, ctx) -> {
			String error = apply(ctx.player(), payload.kind(), payload.arg());
			if (error != null) {
				ctx.player().sendOverlayMessage(Component.literal(error));
			}
			sync(ctx.player().level().getServer(), Purgatory.data(ctx.player().level().getServer()));
		});
	}

	/** Null when done, else the reason (shown on the overlay). Tests call this. */
	public static String apply(ServerPlayer p, int kind, int arg) {
		return switch (kind) {
			case START_SOLO -> Purgatory.startSolo(p);
			case TEAM -> Purgatory.toggleTeam(p);
			case GIVE_UP -> Purgatory.giveUp(p);
			case WATCH -> Purgatory.watch(p, arg);
			case STOP_WATCH -> Purgatory.stopWatching(p);
			case COME_BACK -> Purgatory.comeBack(p);
			default -> "Unknown action";
		};
	}

	public static State stateFor(MinecraftServer server, PurgatorySavedData d, ServerPlayer p) {
		String id = p.getUUID().toString();
		Solo s = d.solo.get(id);
		List<RunInfo> runs = new ArrayList<>();
		for (Run r : d.runs) {
			List<String> names = new ArrayList<>();
			for (String pid : r.players) {
				ServerPlayer o = server.getPlayerList().getPlayer(UUID.fromString(pid));
				names.add(o != null ? o.getName().getString() : "?");
			}
			runs.add(new RunInfo(r.id, r.team, r.floor, r.ticksLeft, String.join(", ", names)));
		}
		Run mine = d.runOf(id);
		int mode = mine != null ? (mine.won() ? MODE_CLEARED : mine.alive(id) ? MODE_FIGHTING : MODE_FALLEN)
			: d.watching.containsKey(id) ? MODE_WATCHING : MODE_NONE;
		return new State(s == null ? 1 : s.floor, s == null ? 0 : s.cooldown, d.teamFloor, d.teamCooldown, Purgatory.queued(p),
			Purgatory.queueSize(), Purgatory.countdown(), mode, mine != null ? mine.victoryTicks : 0, runs);
	}

	static void sync(MinecraftServer server, PurgatorySavedData d) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(p, State.TYPE)) {
				ServerPlayNetworking.send(p, stateFor(server, d, p));
			}
		}
	}
}
