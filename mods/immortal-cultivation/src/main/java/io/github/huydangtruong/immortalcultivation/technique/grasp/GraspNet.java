package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Soul Grasp network: the payload records and their registration (split from Grasp, P-076). */
public final class GraspNet {
	private GraspNet() {}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Menu.TYPE, Menu.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, ctx) -> Grasp.handle(ctx.player(), payload.kind(), payload.target(), payload.arg()));
	}

	/** Client to server. kind: START (arg = entity id), CANCEL, LIST, MODE (target = mob uuid, arg = mode), CALL, RELEASE. */
	public record Request(int kind, String target, int arg) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Request::kind,
			ByteBufCodecs.STRING_UTF8, Request::target,
			ByteBufCodecs.VAR_INT, Request::arg,
			Request::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server to client: mobs held now, and the tether in progress (targetId -1 = none) with its length in ticks. */
	public record State(int held, int targetId, int duration) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::held,
			ByteBufCodecs.VAR_INT, State::targetId,
			ByteBufCodecs.VAR_INT, State::duration,
			State::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * One line of the list. dist is -1 for a mob that is not loaded (shown "far away"); armorGear and weaponGear are the
	 * tooltip lines (separated by newlines).
	 */
	public record Row(String id, String type, int number, int mode, boolean loaded, String dim, float dist, float hp, float maxHp,
			float bonus, float armor, float damage, String armorGear, String weaponGear, int callLeft) {
		static void write(RegistryFriendlyByteBuf b, Row r) {
			b.writeUtf(r.id);
			b.writeUtf(r.type);
			b.writeVarInt(r.number);
			b.writeVarInt(r.mode);
			b.writeBoolean(r.loaded);
			b.writeUtf(r.dim);
			b.writeFloat(r.dist);
			b.writeFloat(r.hp);
			b.writeFloat(r.maxHp);
			b.writeFloat(r.bonus);
			b.writeFloat(r.armor);
			b.writeFloat(r.damage);
			b.writeUtf(r.armorGear);
			b.writeUtf(r.weaponGear);
			b.writeVarInt(r.callLeft);
		}

		static Row read(RegistryFriendlyByteBuf b) {
			return new Row(b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readUtf(), b.readFloat(),
				b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readUtf(), b.readUtf(), b.readVarInt());
		}
	}

	/** Server to client: what the list screen shows. */
	public record Menu(List<Row> rows, int max, int level) implements CustomPacketPayload {
		public static final Type<Menu> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Menu> CODEC = StreamCodec.of((b, m) -> {
			b.writeVarInt(m.rows.size());
			for (Row r : m.rows) {
				Row.write(b, r);
			}
			b.writeVarInt(m.max);
			b.writeVarInt(m.level);
		}, b -> {
			int n = b.readVarInt();
			List<Row> rows = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				rows.add(Row.read(b));
			}
			return new Menu(rows, b.readVarInt(), b.readVarInt());
		});

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
