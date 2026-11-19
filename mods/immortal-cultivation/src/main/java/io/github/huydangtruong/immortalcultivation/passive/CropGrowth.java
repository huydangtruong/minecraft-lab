package io.github.huydangtruong.immortalcultivation.passive;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crop growth (P-058): crops a player (or their clone) plants remember the planter; each vanilla random tick of such a
 * crop gets extra growth attempts by the planter's level (+20% per level). Only the vanilla random tick drives it, so
 * a crop grows only while its chunk is loaded, as in vanilla. Cost: one map lookup per random tick of a crop.
 */
public final class CropGrowth {
	private CropGrowth() {}

	/** Wheat, carrots, potatoes, beetroots (CropBlock), nether wart, cocoa, pumpkin and melon stems. */
	public static boolean grows(Block b) {
		return b instanceof CropBlock || b instanceof StemBlock || b instanceof CocoaBlock || b instanceof NetherWartBlock;
	}

	private static CropOwners owners(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(CropOwners.TYPE);
	}

	private static CropOwners levels(MinecraftServer server) {
		return owners(server.overworld());
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (level instanceof ServerLevel sl && grows(state.getBlock())) {
				owners(sl).remove(pos.asLong());
			}
		});
		ServerPlayerEvents.JOIN.register(CropGrowth::remember);
		ServerPlayerEvents.LEAVE.register(CropGrowth::remember);
	}

	/** Keeps the owner's level for when they are offline. Called on join, leave, upgrade and switch. */
	public static void remember(ServerPlayer p) {
		levels(p.level().getServer()).setLevel(p.getUUID(), PassiveAttachment.get(p).activeLevel(Passive.GROWTH));
	}

	/** A crop was planted at pos by owner (a player, or a clone for its owner). */
	public static void planted(ServerLevel level, BlockPos pos, UUID owner) {
		if (owner != null && grows(level.getBlockState(pos).getBlock())) {
			owners(level).put(pos.asLong(), owner);
		}
	}

	/** Level in use for this owner: the live data when online, else the remembered one. 0 when the mod is off. */
	public static int levelOf(MinecraftServer server, UUID owner) {
		if (!ImmortalCultivation.enabled(server)) {
			return 0;
		}
		ServerPlayer p = server.getPlayerList().getPlayer(owner);
		return p != null ? PassiveAttachment.get(p).activeLevel(Passive.GROWTH) : levels(server).level(owner);
	}

	/** Extra growth attempts the crop at pos gets on this random tick (0 when nobody owns it). */
	public static int extraTicks(ServerLevel level, BlockPos pos, RandomSource random) {
		UUID owner = owners(level).owner(pos.asLong());
		if (owner == null) {
			return 0;
		}
		return PassiveRules.extraGrowth(levelOf(level.getServer(), owner), random.nextDouble());
	}

	private static boolean inExtra;

	/** After a vanilla random tick of a growing block: run the extra attempts (never nested). */
	public static void afterRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (inExtra || !grows(state.getBlock())) {
			return;
		}
		int extra = extraTicks(level, pos, random);
		if (extra <= 0) {
			return;
		}
		inExtra = true;
		try {
			for (int i = 0; i < extra; i++) {
				BlockState now = level.getBlockState(pos);
				if (!grows(now.getBlock())) {
					owners(level).remove(pos.asLong()); // the crop is gone
					return;
				}
				now.randomTick(level, pos, random);
			}
		} finally {
			inExtra = false;
		}
	}
}
