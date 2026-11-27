package io.github.huydangtruong.immortalcultivation.technique.chrono;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** D268: the packets of Time Rewind. */
public final class ChronoNet {
	private ChronoNet() {}

	/**
	 * Server to players near a time stop, every tick while it lasts. {@code size} 0 = the stop is over. Ticks are counted back
	 * from the moment of the cast: {@code shown} is where the stopped box is now, {@code target} where the wheel asked to go.
	 */
	public record Zone(int owner, int minX, int minY, int minZ, int size, int ticksLeft, int shown, int target, int usable,
		boolean casterHeld)
		implements CustomPacketPayload {
		public static final Type<Zone> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "chrono_zone"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Zone> CODEC = StreamCodec.of((buf, z) -> {
			buf.writeVarInt(z.owner);
			buf.writeInt(z.minX);
			buf.writeInt(z.minY);
			buf.writeInt(z.minZ);
			buf.writeVarInt(z.size);
			buf.writeVarInt(z.ticksLeft);
			buf.writeVarInt(z.shown);
			buf.writeVarInt(z.target);
			buf.writeVarInt(z.usable);
			buf.writeBoolean(z.casterHeld);
		}, buf -> new Zone(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readVarInt(), buf.readVarInt(),
			buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: wheel notches while the player's own time stop runs (positive = back in time). */
	public record Scroll(int notches) implements CustomPacketPayload {
		public static final Type<Scroll> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "chrono_scroll"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Scroll> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Scroll::notches, Scroll::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server (D272): the held caster moved or clicked, they leave the stop. */
	public record Release() implements CustomPacketPayload {
		public static final Type<Release> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "chrono_release"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Release> CODEC = StreamCodec.unit(new Release());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
