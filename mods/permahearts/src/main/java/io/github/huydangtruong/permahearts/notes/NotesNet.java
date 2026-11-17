package io.github.huydangtruong.permahearts.notes;

import io.github.huydangtruong.permahearts.PermaHearts;
import io.github.huydangtruong.permahearts.notes.NoteRules.Note;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** P-042 (D117): Notes panel networking. The client only asks; the server validates everything and sends the lists back. */
public final class NotesNet {
	public static final int ADD = 0;
	public static final int DELETE = 1;
	public static final int REQUEST = 2;

	private NotesNet() {}

	/** Client to server. kind ADD: text. kind DELETE: id. REQUEST: nothing (sends both lists). */
	public record Action(int kind, boolean shared, String text, long id) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "notes_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Action::kind,
			ByteBufCodecs.BOOL, Action::shared,
			ByteBufCodecs.stringUtf8(256), Action::text,
			ByteBufCodecs.VAR_LONG, Action::id,
			Action::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, Note> NOTE = StreamCodec.composite(
		ByteBufCodecs.VAR_LONG, Note::id,
		UUIDUtil.STREAM_CODEC, Note::author,
		ByteBufCodecs.stringUtf8(64), Note::authorName,
		ByteBufCodecs.stringUtf8(256), Note::text,
		ByteBufCodecs.VAR_LONG, Note::time,
		Note::new);

	/** Server to client: a whole list (shared for everyone, private for its owner). */
	public record Sync(boolean shared, List<Note> notes) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "notes_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Sync::shared,
			NOTE.apply(ByteBufCodecs.list(64)), Sync::notes,
			Sync::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Sync.TYPE, Sync.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, ctx) -> {
			String error = apply(ctx.player(), payload.kind(), payload.shared(), payload.text(), payload.id());
			if (error != null) {
				ctx.player().sendOverlayMessage(Component.literal(error));
			}
		});
	}

	/** Applies a request and sends the changed lists. Returns null when fine, else the reason (English). Also used by game tests. */
	public static String apply(ServerPlayer p, int kind, boolean shared, String text, long id) {
		MinecraftServer server = p.level().getServer();
		NotesData data = NotesData.of(server);
		if (kind == ADD) {
			String error = data.add(shared, p.getUUID(), p.getPlainTextName(), text, server.overworld().getGameTime());
			if (error != null) {
				return error;
			}
			sendAfterChange(server, p, data, shared);
			if (shared) {
				server.getPlayerList().broadcastSystemMessage(io.github.huydangtruong.permahearts.Messages.sharedNoteAdded(p.getDisplayName()), false);
			}
		} else if (kind == DELETE) {
			if (data.delete(shared, p.getUUID(), id)) {
				sendAfterChange(server, p, data, shared);
			}
		} else if (kind == REQUEST) {
			send(p, data, true);
			send(p, data, false);
		}
		return null;
	}

	private static void sendAfterChange(MinecraftServer server, ServerPlayer p, NotesData data, boolean shared) {
		if (shared) {
			for (ServerPlayer q : server.getPlayerList().getPlayers()) {
				send(q, data, true);
			}
		} else {
			send(p, data, false);
		}
	}

	private static void send(ServerPlayer p, NotesData data, boolean shared) {
		ServerPlayNetworking.send(p, new Sync(shared, shared ? data.shared() : data.privateOf(p.getUUID())));
	}
}
