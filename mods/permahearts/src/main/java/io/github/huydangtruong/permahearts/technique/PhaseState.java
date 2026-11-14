package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Phase Walk state of a player: blocks whose Y is at or above floorY have no collision for the player until endTick
 * (level game time). Synced to the owner so the client predicts the same movement as the server. Not saved.
 */
public record PhaseState(int floorY, long endTick) {
	public static final PhaseState NONE = new PhaseState(0, -1);

	public static final StreamCodec<io.netty.buffer.ByteBuf, PhaseState> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, PhaseState::floorY,
		ByteBufCodecs.VAR_LONG, PhaseState::endTick,
		PhaseState::new);

	public static final AttachmentType<PhaseState> ATTACHMENT = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "phase"),
		b -> b.initializer(() -> NONE).syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	public boolean active(long now) {
		return now < endTick;
	}

	/** True while the entity is a player in Phase Walk. Called from collision code: keep it cheap. */
	public static boolean phasing(Entity e) {
		if (!(e instanceof Player)) {
			return false;
		}
		PhaseState s = e.getAttached(ATTACHMENT);
		return s != null && s.active(e.level().getGameTime());
	}

	/** True when the block at pos has no collision for this entity right now. */
	public static boolean passesThrough(Entity e, BlockPos pos) {
		if (!(e instanceof Player)) {
			return false;
		}
		PhaseState s = e.getAttached(ATTACHMENT);
		return s != null && s.active(e.level().getGameTime()) && pos.getY() >= s.floorY();
	}
}
