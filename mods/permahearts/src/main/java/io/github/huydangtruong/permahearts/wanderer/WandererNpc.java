package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.HeartsAttachment;
import io.github.huydangtruong.permahearts.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

/** The Wanderer is a vanilla Wandering Trader marked with the WANDERER attachment: no AI, never despawns, no trading. */
public final class WandererNpc {
	private WandererNpc() {}

	/** Builds the NPC but does not add it to the level (the caller stores its UUID first). Null if creation failed. */
	public static WanderingTrader create(ServerLevel level, BlockPos pos, float yaw) {
		WanderingTrader npc = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.EVENT);
		if (npc == null) {
			return null;
		}
		npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
		npc.setNoAi(true);
		npc.setPersistenceRequired();
		npc.setDespawnDelay(0);
		npc.setCustomName(Messages.wandererName());
		npc.setCustomNameVisible(true);
		npc.setAttached(HeartsAttachment.WANDERER, true);
		return npc;
	}

	public static boolean isWanderer(Entity e) {
		return e.getAttachedOrElse(HeartsAttachment.WANDERER, false);
	}
}
