package io.github.huydangtruong.permahearts.wanderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Finds a place to stand near a point at the same height (caves included), never the surface heightmap. */
public final class SpawnSpots {
	private static final int Y_RANGE = 4;

	private SpawnSpots() {}

	/**
	 * Feet position for a spawn at origin + (dx, dz): solid block below, two free blocks above, searched at the
	 * origin's Y first and then up to 4 blocks above and below. Falls back to the origin itself.
	 */
	public static BlockPos standSpot(ServerLevel level, BlockPos origin, int dx, int dz) {
		int x = origin.getX() + dx;
		int z = origin.getZ() + dz;
		for (int k = 0; k <= Y_RANGE; k++) {
			for (int sign = 1; sign >= -1; sign -= 2) {
				if (k == 0 && sign == -1) {
					continue;
				}
				BlockPos feet = new BlockPos(x, origin.getY() + k * sign, z);
				if (fits(level, feet)) {
					return feet;
				}
			}
		}
		return origin;
	}

	private static boolean fits(ServerLevel level, BlockPos feet) {
		BlockPos below = feet.below();
		BlockState state = level.getBlockState(below);
		boolean solid = state.isFaceSturdy(level, below, Direction.UP) || !state.getCollisionShape(level, below).isEmpty();
		return solid && free(level, feet) && free(level, feet.above());
	}

	private static boolean free(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
	}
}
