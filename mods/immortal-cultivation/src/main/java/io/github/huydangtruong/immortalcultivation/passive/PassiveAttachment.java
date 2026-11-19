package io.github.huydangtruong.immortalcultivation.passive;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** The `permahearts:passives` attachment: saved, kept on death, synced to its own player only. */
public final class PassiveAttachment {
	private static final StreamCodec<ByteBuf, Map<String, Integer>> INT_MAP =
		ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);

	public static final StreamCodec<ByteBuf, PassiveData> STREAM_CODEC = StreamCodec.composite(
		INT_MAP, PassiveData::levels,
		ByteBufCodecs.BOOL, PassiveData::visionOn,
		ByteBufCodecs.BOOL, PassiveData::growthOn,
		PassiveData::new);

	public static final AttachmentType<PassiveData> PASSIVES = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "passives"),
		b -> b.persistent(PassiveData.CODEC).copyOnDeath().initializer(() -> PassiveData.DEFAULT)
			.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private PassiveAttachment() {}

	/** Works on both sides (the client reads the synced copy). */
	public static PassiveData get(Player p) {
		return p.getAttachedOrElse(PASSIVES, PassiveData.DEFAULT);
	}

	public static void set(Player p, PassiveData d) {
		p.setAttached(PASSIVES, d);
	}
}
