package io.github.huydangtruong.permahearts;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class HeartsAttachment {
	public static final Codec<HeartsData> CODEC = HeartsData.CODEC;

	public static final StreamCodec<ByteBuf, HeartsData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, HeartsData::bonusHearts,
		ByteBufCodecs.VAR_INT, HeartsData::heartXp,
		ByteBufCodecs.VAR_INT, HeartsData::totalHostileKills,
		ByteBufCodecs.VAR_INT, HeartsData::totalDeaths,
		HeartsData::new);

	public static final AttachmentType<HeartsData> HEARTS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "hearts"),
		b -> b.persistent(CODEC).copyOnDeath().initializer(() -> HeartsData.DEFAULT)
			.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	/** Mirror of the world's game rule for this player, so the client HUD knows whether to draw. Not saved. */
	public static final AttachmentType<Boolean> ACTIVE = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "active"),
		b -> b.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly()));

	/** Elixir levels. Saved, kept on death, server side only. */
	public static final AttachmentType<CultivationData> CULTIVATION = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "cultivation"),
		b -> b.persistent(CultivationData.CODEC).copyOnDeath().initializer(() -> CultivationData.DEFAULT));

	/** Marks the Wandering Trader that is our Wanderer NPC. Saved with the entity and synced to every client (the client use-callback needs it). */
	public static final AttachmentType<Boolean> WANDERER = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "wanderer"),
		b -> b.persistent(Codec.BOOL).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/**
	 * Death Match: this player's current death fell to another player during a match, so hearts, items and XP are kept.
	 * Saved and copied on death (a host that quits on the death screen still respawns correctly); cleared after respawn.
	 */
	public static final AttachmentType<Boolean> DM_PROTECTED = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "dm_protected"),
		b -> b.persistent(Codec.BOOL).copyOnDeath().initializer(() -> false));

	private HeartsAttachment() {}

	public static boolean isDeathMatchProtected(ServerPlayer p) {
		return p.getAttachedOrElse(DM_PROTECTED, false);
	}

	public static void setDeathMatchProtected(ServerPlayer p, boolean on) {
		if (on) {
			p.setAttached(DM_PROTECTED, true);
		} else {
			p.removeAttached(DM_PROTECTED);
		}
	}

	public static CultivationData getCultivation(ServerPlayer p) {
		return p.getAttachedOrCreate(CULTIVATION);
	}

	public static void setCultivation(ServerPlayer p, CultivationData d) {
		p.setAttached(CULTIVATION, d);
	}

	public static void setActive(ServerPlayer p, boolean active) {
		p.setAttached(ACTIVE, active);
	}

	public static HeartsData get(ServerPlayer p) {
		return p.getAttachedOrCreate(HEARTS);
	}

	public static void set(ServerPlayer p, HeartsData d) {
		p.setAttached(HEARTS, d);
	}
}
