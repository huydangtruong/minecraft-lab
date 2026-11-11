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

	private HeartsAttachment() {}

	public static HeartsData get(ServerPlayer p) {
		return p.getAttachedOrCreate(HEARTS);
	}

	public static void set(ServerPlayer p, HeartsData d) {
		p.setAttached(HEARTS, d);
	}
}
