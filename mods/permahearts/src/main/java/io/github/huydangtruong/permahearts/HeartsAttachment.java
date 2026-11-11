package io.github.huydangtruong.permahearts;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class HeartsAttachment {
	public static final Codec<HeartsData> CODEC = HeartsData.CODEC;

	public static final AttachmentType<HeartsData> HEARTS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "hearts"),
		b -> b.persistent(CODEC).copyOnDeath().initializer(() -> HeartsData.DEFAULT));

	private HeartsAttachment() {}

	public static HeartsData get(ServerPlayer p) {
		return p.getAttachedOrCreate(HEARTS);
	}

	public static void set(ServerPlayer p, HeartsData d) {
		p.setAttached(HEARTS, d);
	}
}
