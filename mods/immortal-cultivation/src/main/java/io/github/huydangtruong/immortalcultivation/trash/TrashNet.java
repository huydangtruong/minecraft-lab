package io.github.huydangtruong.immortalcultivation.trash;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** D217: the trash slot of the inventory screen. The client only says "trash it"; the server reads what is carried. */
public final class TrashNet {
	private TrashNet() {}

	/** Client to server, no data. */
	public record Trash() implements CustomPacketPayload {
		public static final Type<Trash> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "trash"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Trash> CODEC = StreamCodec.unit(new Trash());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Trash.TYPE, Trash.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Trash.TYPE, (payload, ctx) -> apply(ctx.player()));
	}

	/** Destroys the stack on the player's cursor. Returns true when something was destroyed. Also called by game tests. */
	public static boolean apply(ServerPlayer p) {
		var menu = p.containerMenu;
		if (!TrashRules.shouldDestroy(menu.getCarried().isEmpty(), p.isSpectator())) {
			return false;
		}
		menu.setCarried(ItemStack.EMPTY);
		menu.broadcastChanges();
		// only this player hears the fizz
		p.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(SoundEvents.LAVA_EXTINGUISH), SoundSource.PLAYERS, p.getX(), p.getY(), p.getZ(),
			0.5F, 1.0F, p.level().getRandom().nextLong()));
		return true;
	}
}
