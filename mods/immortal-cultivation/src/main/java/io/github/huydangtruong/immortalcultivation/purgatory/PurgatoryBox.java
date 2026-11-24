package io.github.huydangtruong.immortalcultivation.purgatory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bedrock box of a Purgatory floor (D235): 64 x 64 x 64 with 2 bedrock layers, a floor and a layout that changes every
 * 5 floors. Built when a floor starts and removed when it ends; its chunks stay loaded meanwhile.
 */
public final class PurgatoryBox {
	/** No neighbour updates, no drops: the box appears and disappears as a whole. */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	/** Nothing is built this close to the centre: the fighters arrive there. */
	private static final int CLEAR_CENTRE = 5;

	private PurgatoryBox() {}

	public static void build(ServerLevel level, int slot, int floor) {
		force(level, slot, true);
		BlockPos c = Purgatory.corner(slot);
		int n = PurgatoryRules.BOX;
		int w = PurgatoryRules.WALL;
		BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					boolean wall = x < w || y < w || z < w || x >= n - w || y >= n - w || z >= n - w;
					set(level, p.set(c.getX() + x, c.getY() + y, c.getZ() + z), wall ? bedrock : air);
				}
			}
		}
		PurgatoryRules.Theme theme = PurgatoryRules.Theme.of(floor);
		RandomSource rng = RandomSource.create(floor * 7919L + slot);
		int fy = Purgatory.floorY();
		int lo = w;
		int hi = n - w - 1;
		int cx = PurgatoryRules.slotCenterX(slot);
		BlockState ground = switch (theme) {
			case LAVA -> Blocks.BLACKSTONE.defaultBlockState();
			case WATER -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
			case WALLS -> Blocks.STONE_BRICKS.defaultBlockState();
			default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
		};
		for (int x = lo; x <= hi; x++) {
			for (int z = lo; z <= hi; z++) {
				int ax = c.getX() + x;
				int az = c.getZ() + z;
				boolean light = (ax - cx) % 8 == 0 && az % 8 == 0;
				set(level, p.set(ax, fy, az), light ? Blocks.SHROOMLIGHT.defaultBlockState() : ground);
				if ((ax - cx) % 10 == 0 && az % 10 == 0) {
					set(level, p.set(ax, c.getY() + n - w - 1, az), Blocks.SHROOMLIGHT.defaultBlockState()); // ceiling lights
				}
			}
		}
		switch (theme) {
			case OPEN -> { }
			case PILLARS -> {
				for (int i = 0; i < 18; i++) {
					int[] at = spot(rng, c, lo + 2, hi - 2, cx);
					column(level, p, at[0], at[1], fy + 1, 3, 4 + rng.nextInt(8), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
				}
			}
			case LAVA -> {
				for (int i = 0; i < 14; i++) {
					int[] at = spot(rng, c, lo + 2, hi - 2, cx);
					int size = 2 + rng.nextInt(3);
					fill(level, p, at[0], fy, at[1], size, 1, size, Blocks.LAVA.defaultBlockState());
				}
			}
			case PLATFORMS -> {
				for (int i = 0; i < 10; i++) {
					int[] at = spot(rng, c, lo + 5, hi - 5, cx);
					for (int step = 0; step < 3; step++) {
						int size = 9 - 2 * step;
						fill(level, p, at[0] - 4 + step, fy + 1 + step, at[1] - 4 + step, size, 1, size, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
					}
				}
			}
			case WATER -> {
				for (int x = lo; x <= hi; x += 7) {
					if (Math.abs(c.getX() + x - cx) > CLEAR_CENTRE) {
						fill(level, p, c.getX() + x, fy, c.getZ() + lo, 2, 1, hi - lo + 1, Blocks.WATER.defaultBlockState());
					}
				}
			}
			case WALLS -> {
				for (int i = 0; i < 16; i++) {
					int[] at = spot(rng, c, lo + 6, hi - 6, cx);
					boolean alongX = rng.nextBoolean();
					int len = 5 + rng.nextInt(8);
					fill(level, p, at[0], fy + 1, at[1], alongX ? len : 1, 3, alongX ? 1 : len, Blocks.STONE_BRICKS.defaultBlockState());
				}
			}
		}
	}

	/** Removes the whole box and lets its chunks unload. */
	public static void release(ServerLevel level, int slot) {
		BlockPos c = Purgatory.corner(slot);
		int n = PurgatoryRules.BOX;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					set(level, p.set(c.getX() + x, c.getY() + y, c.getZ() + z), air);
				}
			}
		}
		force(level, slot, false);
	}

	/** A free spot on the ground of the box, far enough from every fighter, or null. */
	public static BlockPos spawnSpot(ServerLevel level, int slot, List<ServerPlayer> fighters, RandomSource rng) {
		BlockPos c = Purgatory.corner(slot);
		int lo = PurgatoryRules.WALL + 1;
		int hi = PurgatoryRules.BOX - PurgatoryRules.WALL - 2;
		int fy = Purgatory.floorY();
		double minSq = PurgatoryRules.SPAWN_MIN_DISTANCE * (double) PurgatoryRules.SPAWN_MIN_DISTANCE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int tries = 0; tries < 40; tries++) {
			int x = c.getX() + lo + rng.nextInt(hi - lo + 1);
			int z = c.getZ() + lo + rng.nextInt(hi - lo + 1);
			for (int y = fy + 14; y > fy; y--) {
				p.set(x, y, z);
				BlockState below = level.getBlockState(p.below());
				if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && below.isSolid() && below.getFluidState().isEmpty()) {
					boolean far = true;
					for (ServerPlayer f : fighters) {
						far &= f.distanceToSqr(x + 0.5, y, z + 0.5) >= minSq;
					}
					if (far) {
						return p.immutable();
					}
					break;
				}
			}
		}
		return null;
	}

	private static int[] spot(RandomSource rng, BlockPos c, int lo, int hi, int cx) {
		for (int i = 0; i < 20; i++) {
			int x = c.getX() + lo + rng.nextInt(hi - lo + 1);
			int z = c.getZ() + lo + rng.nextInt(hi - lo + 1);
			if (Math.abs(x - cx) > CLEAR_CENTRE + 4 || Math.abs(z) > CLEAR_CENTRE + 4) {
				return new int[] {x, z};
			}
		}
		return new int[] {c.getX() + lo, c.getZ() + lo};
	}

	private static void column(ServerLevel level, BlockPos.MutableBlockPos p, int x, int z, int y0, int size, int height, BlockState s) {
		fill(level, p, x, y0, z, size, height, size, s);
	}

	private static void fill(ServerLevel level, BlockPos.MutableBlockPos p, int x0, int y0, int z0, int sx, int sy, int sz, BlockState s) {
		for (int x = x0; x < x0 + sx; x++) {
			for (int y = y0; y < y0 + sy; y++) {
				for (int z = z0; z < z0 + sz; z++) {
					if (inner(x, y, z, p)) {
						set(level, p.set(x, y, z), s);
					}
				}
			}
		}
	}

	/** Never builds into the bedrock (layouts near the edge are cut). */
	private static boolean inner(int x, int y, int z, BlockPos.MutableBlockPos unused) {
		int slot = Math.round(x / (float) PurgatoryRules.SLOT_SPACING);
		BlockPos c = Purgatory.corner(slot);
		int w = PurgatoryRules.WALL;
		int n = PurgatoryRules.BOX;
		return x >= c.getX() + w && x < c.getX() + n - w && y >= c.getY() + w && y < c.getY() + n - w
			&& z >= c.getZ() + w && z < c.getZ() + n - w;
	}

	private static void set(ServerLevel level, BlockPos.MutableBlockPos p, BlockState s) {
		if (level.getBlockState(p) != s) {
			level.setBlock(p, s, FLAGS);
		}
	}

	private static void force(ServerLevel level, int slot, boolean on) {
		BlockPos c = Purgatory.corner(slot);
		for (int cx = c.getX() >> 4; cx <= (c.getX() + PurgatoryRules.BOX - 1) >> 4; cx++) {
			for (int cz = c.getZ() >> 4; cz <= (c.getZ() + PurgatoryRules.BOX - 1) >> 4; cz++) {
				level.setChunkForced(cx, cz, on);
			}
		}
	}
}
