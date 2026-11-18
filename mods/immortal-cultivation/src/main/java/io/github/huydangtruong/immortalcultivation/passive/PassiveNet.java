package io.github.huydangtruong.immortalcultivation.passive;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Client to server requests of the Passives tab (D148). The server checks everything; {@link #apply} is what tests call. */
public final class PassiveNet {
	public static final int UPGRADE = 0;
	public static final int TOGGLE_VISION = 1;

	public record PassiveAction(int kind, String passive) implements CustomPacketPayload {
		public static final Type<PassiveAction> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "passive_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PassiveAction> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PassiveAction::kind,
			ByteBufCodecs.STRING_UTF8, PassiveAction::passive,
			PassiveAction::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private PassiveNet() {}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(PassiveAction.TYPE, PassiveAction.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PassiveAction.TYPE, (payload, ctx) -> {
			String error = apply(ctx.player(), payload.kind(), payload.passive());
			if (error != null) {
				ctx.player().sendOverlayMessage(Component.literal(error));
			}
		});
	}

	/** Returns null when done, else the reason (English, shown on the overlay). */
	public static String apply(ServerPlayer p, int kind, String id) {
		if (!ImmortalCultivation.enabled(p.level().getServer())) {
			return "Immortal Cultivation is off in this world";
		}
		PassiveData d = PassiveAttachment.get(p);
		switch (kind) {
			case UPGRADE -> {
				Passive passive = Passive.byId(id).orElse(null);
				if (passive == null) {
					return "Unknown passive";
				}
				int level = d.level(passive);
				if (level >= PassiveRules.MAX_LEVEL) {
					return passive.displayName() + " is at the maximum level";
				}
				if (p.experienceLevel < PassiveRules.COST_LEVELS) {
					return "Needs " + PassiveRules.COST_LEVELS + " experience levels";
				}
				p.giveExperienceLevels(-PassiveRules.COST_LEVELS);
				PassiveAttachment.set(p, d.withLevel(passive, level + 1));
				return null;
			}
			case TOGGLE_VISION -> {
				if (d.level(Passive.VISION) < 1) {
					return "Learn Increase vision first";
				}
				PassiveAttachment.set(p, d.withVisionOn(!d.visionOn()));
				return null;
			}
			default -> {
				return "Unknown action";
			}
		}
	}
}
