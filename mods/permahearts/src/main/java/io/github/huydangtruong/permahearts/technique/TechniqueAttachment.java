package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** The `permahearts:techniques` attachment: saved, kept on death, synced to its own player only. */
public final class TechniqueAttachment {
	private static final StreamCodec<ByteBuf, Map<String, Integer>> INT_MAP =
		ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
	private static final StreamCodec<ByteBuf, Map<String, Long>> LONG_MAP =
		ByteBufCodecs.<ByteBuf, String, Long, Map<String, Long>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG);
	private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(TechniqueRules.MAX_SLOTS));

	public static final StreamCodec<ByteBuf, TechniqueData> STREAM_CODEC = StreamCodec.composite(
		INT_MAP, TechniqueData::levels,
		INT_MAP, TechniqueData::spentPoints,
		ByteBufCodecs.VAR_INT, TechniqueData::emeralds,
		STRINGS, TechniqueData::slots,
		LONG_MAP, TechniqueData::cooldownUntil,
		TechniqueData::new);

	public static final AttachmentType<TechniqueData> TECHNIQUES = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "techniques"),
		b -> b.persistent(TechniqueData.CODEC).copyOnDeath().initializer(() -> TechniqueData.DEFAULT)
			.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private TechniqueAttachment() {}

	public static TechniqueData get(ServerPlayer p) {
		return p.getAttachedOrCreate(TECHNIQUES);
	}

	public static void set(ServerPlayer p, TechniqueData d) {
		p.setAttached(TECHNIQUES, d);
	}
}
