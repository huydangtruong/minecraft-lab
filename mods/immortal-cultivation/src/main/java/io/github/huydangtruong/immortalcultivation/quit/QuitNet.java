package io.github.huydangtruong.immortalcultivation.quit;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** D218: server to the host's client, no data: "leave the world the normal way, then close the game". */
public final class QuitNet {
	private QuitNet() {}

	public record CloseGame() implements CustomPacketPayload {
		public static final Type<CloseGame> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "close_game"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CloseGame> CODEC = StreamCodec.unit(new CloseGame());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(CloseGame.TYPE, CloseGame.CODEC);
	}
}
