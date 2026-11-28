package io.github.huydangtruong.immortalcultivation.godslayer;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
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
		PayloadTypeRegistry.clientboundPlay().register(Blind.TYPE, Blind.CODEC);
	}

	public record Warning(int ticksLeft) implements CustomPacketPayload {
		public static final Type<Warning> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "god_slayer_warning"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Warning> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, Warning::ticksLeft, Warning::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server to client: the God Slayer blast blinds the player for this many ticks (red screen, D288). */
	public record Blind(int ticks) implements CustomPacketPayload {
		public static final Type<Blind> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "god_slayer_blind"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Blind> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, Blind::ticks, Blind::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
