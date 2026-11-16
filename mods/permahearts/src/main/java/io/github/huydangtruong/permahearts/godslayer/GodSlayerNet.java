package io.github.huydangtruong.permahearts.godslayer;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: ticks left of the God Slayer warning (0 = no warning), for the red screen edge (P-035, D102). */
public final class GodSlayerNet {
	private GodSlayerNet() {}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Warning.TYPE, Warning.CODEC);
	}

	public record Warning(int ticksLeft) implements CustomPacketPayload {
		public static final Type<Warning> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "god_slayer_warning"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Warning> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, Warning::ticksLeft, Warning::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
