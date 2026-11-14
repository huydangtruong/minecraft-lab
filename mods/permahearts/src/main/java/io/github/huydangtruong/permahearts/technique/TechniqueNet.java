package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client to server requests of the Techniques tab (D43). The server validates everything: the client only asks.
 * {@link #apply} is also what the game tests call.
 */
public final class TechniqueNet {
	public static final int UPGRADE = 0;
	public static final int RESET = 1;
	public static final int ASSIGN = 2;
	public static final int CLEAR_SLOT = 3;
	public static final int DEPOSIT = 4;

	/** kind: UPGRADE..DEPOSIT. technique: id (UPGRADE, RESET, ASSIGN). slot: 0..3 (ASSIGN, CLEAR_SLOT). */
	public record TechniqueAction(int kind, String technique, int slot) implements CustomPacketPayload {
		public static final Type<TechniqueAction> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "technique_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TechniqueAction> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TechniqueAction::kind,
			ByteBufCodecs.STRING_UTF8, TechniqueAction::technique,
			ByteBufCodecs.VAR_INT, TechniqueAction::slot,
			TechniqueAction::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private TechniqueNet() {}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TechniqueAction.TYPE, TechniqueAction.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TechniqueAction.TYPE, (payload, ctx) -> {
			String error = apply(ctx.player(), payload.kind(), payload.technique(), payload.slot());
			if (error != null) {
				ctx.player().sendOverlayMessage(Component.literal(error));
			}
		});
	}

	/** Runs a request. Returns null when it worked, else the reason it was refused (English, shown to the player). */
	public static String apply(ServerPlayer p, int kind, String technique, int slot) {
		if (!PermaHearts.enabled(p.level().getServer())) {
			return "Immortal Cultivation is off in this world";
		}
		TechniqueData d = TechniqueAttachment.get(p);
		long now = p.level().getServer().overworld().getGameTime();
		Technique t = Technique.byId(technique).orElse(null);
		switch (kind) {
			case UPGRADE -> {
				if (t == null) {
					return "Unknown technique";
				}
				int level = d.level(t);
				if (level >= TechniqueRules.MAX_LEVEL) {
					return t.displayName() + " is at the maximum level";
				}
				int cost = TechniqueRules.upgradeCost(level + 1);
				if (p.experienceLevel < cost) {
					return "Needs " + cost + " experience levels";
				}
				int before = TechniqueRules.points(p.experienceLevel, p.experienceProgress);
				p.giveExperienceLevels(-cost);
				int after = TechniqueRules.points(p.experienceLevel, p.experienceProgress);
				TechniqueAttachment.set(p, d.withLevel(t, level + 1, d.spent(t) + Math.max(0, before - after)));
				return null;
			}
			case RESET -> {
				if (t == null) {
					return "Unknown technique";
				}
				if (d.level(t) == 0) {
					return t.displayName() + " is not learned";
				}
				if (d.onCooldown(t, now)) {
					return t.displayName() + " is on cooldown";
				}
				int refund = TechniqueRules.refund(d.spent(t));
				TechniqueAttachment.set(p, d.withoutFromSlots(t).withLevel(t, 0, 0));
				if (refund > 0) {
					p.giveExperiencePoints(refund);
				}
				return null;
			}
			case ASSIGN -> {
				if (t == null) {
					return "Unknown technique";
				}
				if (slot < 0 || slot >= d.slotsOpen()) {
					return "That slot is locked";
				}
				if (d.level(t) < 1) {
					return "Learn " + t.displayName() + " first";
				}
				if (d.slotOnCooldown(slot, now)) {
					return d.inSlot(slot).displayName() + " is on cooldown";
				}
				TechniqueAttachment.set(p, d.withSlot(slot, t));
				return null;
			}
			case CLEAR_SLOT -> {
				if (slot < 0 || slot >= d.slotsOpen()) {
					return "That slot is locked";
				}
				if (d.slotOnCooldown(slot, now)) {
					return d.inSlot(slot).displayName() + " is on cooldown";
				}
				TechniqueAttachment.set(p, d.withSlot(slot, null));
				return null;
			}
			case DEPOSIT -> {
				int need = TechniqueRules.MAX_EMERALDS - d.emeralds();
				if (need <= 0) {
					return "All slots are open";
				}
				ItemStack carried = p.containerMenu.getCarried();
				if (!carried.is(Items.EMERALD)) {
					return "Hold emeralds on the cursor";
				}
				int take = Math.min(need, carried.getCount());
				carried.shrink(take);
				TechniqueAttachment.set(p, d.withEmeralds(d.emeralds() + take));
				p.containerMenu.broadcastChanges();
				return null;
			}
			default -> {
				return "Unknown request";
			}
		}
	}
}
