package io.github.huydangtruong.immortalcultivation.purgatory;

import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Dir;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Dungeon;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.DungeonRules;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Grid;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Mat;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Room;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Style;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Puts a generated floor (P-082) into the world and takes it away again. Each {@link Style} maps the dungeon materials to
 * vanilla blocks. Placement skips neighbour updates and drops; fences, walls, bars and rails are shaped afterwards.
 */
public final class DungeonBuilder {
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
	public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE,
		Identifier.withDefaultNamespace("chests/trial_chambers/reward"));
	/** D263: the wooden gift chests in about half the rooms. */
	public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> GIFT = ResourceKey.create(Registries.LOOT_TABLE,
		Identifier.withDefaultNamespace("chests/simple_dungeon"));

	private static final Map<Style, Map<Mat, BlockState>> PALETTE = new EnumMap<>(Style.class);

	private DungeonBuilder() {}

	/** Bottom north-west corner of the grid of a slot. */
	public static BlockPos origin(int slot) {
		return new BlockPos(PurgatoryRules.slotCenterX(slot) - DungeonRules.SIZE_X / 2, PurgatoryRules.BOX_MIN_Y, -DungeonRules.SIZE_Z / 2);
	}

	public static BlockPos world(int slot, int[] g) {
		return origin(slot).offset(g[0], g[1], g[2]);
	}

	public static void build(ServerLevel level, Dungeon d, int slot) {
		force(level, slot, true);
		BlockPos o = origin(slot);
		Grid g = d.grid();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (Room r : d.rooms()) {
			for (int x = r.x0(); x < r.x1(); x++) {
				for (int y = r.y0(); y < r.y1(); y++) {
					for (int z = r.z0(); z < r.z1(); z++) {
						set(level, p.set(o.getX() + x, o.getY() + y, o.getZ() + z), state(g, x, y, z));
					}
				}
			}
		}
		BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
		for (int[] k : d.skin()) { // D259: nothing a skill breaks opens onto the void
			set(level, p.set(o.getX() + k[0], o.getY() + k[1], o.getZ() + k[2]), bedrock);
		}
		for (Room r : d.rooms()) { // shapes that depend on neighbours
			for (int x = r.x0(); x < r.x1(); x++) {
				for (int y = r.y0(); y < r.y1(); y++) {
					for (int z = r.z0(); z < r.z1(); z++) {
						Mat m = g.mat(x, y, z);
						if (m == Mat.FENCE || m == Mat.BARS || m == Mat.LEAVES) {
							p.set(o.getX() + x, o.getY() + y, o.getZ() + z);
							BlockState s = level.getBlockState(p);
							BlockState shaped = Block.updateFromNeighbourShapes(s, level, p);
							if (shaped != s) {
								level.setBlock(p, shaped, FLAGS);
							}
						}
					}
				}
			}
			long seed = d.seed() * 31 + r.index;
			for (int[] c : r.chests) {
				if (level.getBlockEntity(world(slot, c)) instanceof RandomizableContainerBlockEntity chest) {
					chest.setLootTable(TREASURE);
					chest.setLootTableSeed(seed++);
				}
			}
			for (int[] c : r.gifts) {
				if (level.getBlockEntity(world(slot, c)) instanceof RandomizableContainerBlockEntity chest) {
					chest.setLootTable(GIFT);
					chest.setLootTableSeed(seed++);
				}
			}
		}
	}

	/** Takes every block of the floor away (air) and lets the chunks unload. */
	public static void clear(ServerLevel level, Dungeon d, int slot) {
		BlockPos o = origin(slot);
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (Room r : d.rooms()) {
			for (int x = r.x0(); x < r.x1(); x++) {
				for (int y = r.y0(); y < r.y1(); y++) {
					for (int z = r.z0(); z < r.z1(); z++) {
						set(level, p.set(o.getX() + x, o.getY() + y, o.getZ() + z), air);
					}
				}
			}
		}
		for (int[] k : d.skin()) {
			set(level, p.set(o.getX() + k[0], o.getY() + k[1], o.getZ() + k[2]), air);
		}
		force(level, slot, false);
	}

	/** A run saved before P-082 stood in a 64-block bedrock box: take it away. */
	public static void clearOldBox(ServerLevel level, int slot) {
		int n = PurgatoryRules.OLD_BOX;
		BlockPos c = new BlockPos(PurgatoryRules.slotCenterX(slot) - n / 2, PurgatoryRules.BOX_MIN_Y, -n / 2);
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					set(level, p.set(c.getX() + x, c.getY() + y, c.getZ() + z), air);
				}
			}
		}
		for (int cx = c.getX() >> 4; cx <= (c.getX() + n - 1) >> 4; cx++) {
			for (int cz = c.getZ() >> 4; cz <= (c.getZ() + n - 1) >> 4; cz++) {
				level.setChunkForced(cx, cz, false);
			}
		}
	}

	private static void set(ServerLevel level, BlockPos p, BlockState s) {
		if (level.getBlockState(p) != s) {
			level.setBlock(p, s, FLAGS);
		}
	}

	static void force(ServerLevel level, int slot, boolean on) {
		BlockPos o = origin(slot);
		for (int cx = (o.getX() - 1) >> 4; cx <= (o.getX() + DungeonRules.SIZE_X) >> 4; cx++) { // the skin is one block past the grid
			for (int cz = (o.getZ() - 1) >> 4; cz <= (o.getZ() + DungeonRules.SIZE_Z) >> 4; cz++) {
				level.setChunkForced(cx, cz, on);
			}
		}
	}

	private static Direction dir(Dir d) {
		return switch (d) {
			case N -> Direction.NORTH;
			case E -> Direction.EAST;
			case S -> Direction.SOUTH;
			case W -> Direction.WEST;
		};
	}

	/** The real block of one voxel. */
	public static BlockState state(Grid g, int x, int y, int z) {
		Mat m = g.mat(x, y, z);
		BlockState s = base(g.style(x, y, z), m);
		Direction f = dir(g.facing(x, y, z));
		if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, f);
		}
		if (s.hasProperty(BlockStateProperties.FACING)) {
			boolean hang = g.mat(x, y + 1, z).supports() && !g.mat(x, y - 1, z).supports();
			s = s.setValue(BlockStateProperties.FACING, m == Mat.DISPENSER ? f : hang ? Direction.DOWN : Direction.UP);
		}
		if (s.hasProperty(BlockStateProperties.HANGING)) {
			s = s.setValue(BlockStateProperties.HANGING, !g.mat(x, y - 1, z).supports());
		}
		if (s.hasProperty(BlockStateProperties.LIT)) {
			s = s.setValue(BlockStateProperties.LIT, true);
		}
		if (s.hasProperty(BlockStateProperties.PERSISTENT)) {
			s = s.setValue(BlockStateProperties.PERSISTENT, true);
		}
		if (m == Mat.RAIL && s.hasProperty(BlockStateProperties.RAIL_SHAPE)) {
			boolean ew = g.mat(x - 1, y, z) == Mat.RAIL || g.mat(x + 1, y, z) == Mat.RAIL;
			s = s.setValue(BlockStateProperties.RAIL_SHAPE, ew ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
		}
		return s;
	}

	private static BlockState base(Style style, Mat m) {
		Map<Mat, BlockState> p = PALETTE.computeIfAbsent(style, DungeonBuilder::palette);
		BlockState s = p.get(m);
		return s != null ? s : PALETTE.get(Style.BASE) != null && PALETTE.get(Style.BASE).get(m) != null
			? PALETTE.get(Style.BASE).get(m) : Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static BlockState block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id)).defaultBlockState();
	}

	/** Materials every style shares, then the look of each style (vanilla structure it borrows from). */
	private static Map<Mat, BlockState> palette(Style style) {
		Map<Mat, String> m = new EnumMap<>(Mat.class);
		m.put(Mat.SHELL, "bedrock");
		m.put(Mat.AIR, "air");
		m.put(Mat.GATE, "air");
		m.put(Mat.WATER, "water");
		m.put(Mat.LAVA, "lava");
		m.put(Mat.COBWEB, "cobweb");
		m.put(Mat.LADDER, "ladder");
		m.put(Mat.RAIL, "rail");
		m.put(Mat.CHEST, "chest");
		m.put(Mat.DISPENSER, "dispenser");
		m.put(Mat.PLATE, "stone_pressure_plate");
		m.put(Mat.CAMPFIRE, "campfire");
		m.put(Mat.BARS, "iron_bars");
		m.put(Mat.MAGMA, "magma_block");
		m.put(Mat.SCULK, "sculk");
		m.put(Mat.SHELF, "bookshelf");
		m.put(Mat.LEAVES, "azalea_leaves");
		m.put(Mat.LANTERN, "lantern");
		m.put(Mat.LIGHT, "glowstone");
		String[] look = switch (style) {
			// FLOOR, FLOOR2, WALL, WALL2, PILLAR, CEILING, STAIR, FENCE, CARPET, DECOR, ACCENT, LIGHT, LANTERN
			case BASE -> new String[] {"stone_bricks", "chiseled_stone_bricks", "stone_bricks", "mossy_stone_bricks", "polished_andesite",
				"stone_bricks", "stone_brick_stairs", "stone_brick_wall", "gray_carpet", "chiseled_stone_bricks", "cracked_stone_bricks",
				"glowstone", "lantern"};
			case MINE -> new String[] {"cobblestone", "oak_planks", "stone", "oak_planks", "oak_log", "stone", "oak_stairs", "oak_fence",
				"brown_carpet", "mossy_cobblestone", "andesite", "ochre_froglight", "lantern"};
			case TRIAL -> new String[] {"polished_tuff", "chiseled_tuff", "tuff_bricks", "chiseled_tuff_bricks", "waxed_cut_copper",
				"tuff_bricks", "tuff_brick_stairs", "tuff_brick_wall", "cyan_carpet", "waxed_chiseled_copper", "waxed_copper_grate",
				"waxed_copper_bulb", "lantern"};
			case END -> new String[] {"end_stone_bricks", "purpur_block", "purpur_block", "end_stone_bricks", "purpur_pillar", "purpur_block",
				"purpur_stairs", "end_stone_brick_wall", "purple_carpet", "crying_obsidian", "purpur_pillar", "pearlescent_froglight",
				"end_rod"};
			case DEEP -> new String[] {"deepslate_tiles", "polished_deepslate", "deepslate_bricks", "deepslate_tiles", "polished_basalt",
				"deepslate_bricks", "deepslate_tile_stairs", "deepslate_brick_wall", "black_carpet", "reinforced_deepslate",
				"chiseled_deepslate", "sculk_catalyst", "soul_lantern"};
			case NETHER -> new String[] {"nether_bricks", "red_nether_bricks", "nether_bricks", "polished_blackstone_bricks", "blackstone",
				"nether_bricks", "nether_brick_stairs", "nether_brick_fence", "red_carpet", "gilded_blackstone", "chiseled_nether_bricks",
				"shroomlight", "soul_lantern"};
			case OCEAN -> new String[] {"prismarine_bricks", "dark_prismarine", "prismarine_bricks", "prismarine", "dark_prismarine",
				"prismarine_bricks", "prismarine_brick_stairs", "prismarine_wall", "cyan_carpet", "sea_lantern", "prismarine", "sea_lantern",
				"sea_lantern"};
			case LIBRARY -> new String[] {"dark_oak_planks", "spruce_planks", "dark_oak_planks", "bookshelf", "dark_oak_log", "dark_oak_planks",
				"dark_oak_stairs", "dark_oak_fence", "red_carpet", "chiseled_bookshelf", "spruce_planks", "glowstone", "lantern"};
			case GARDEN -> new String[] {"moss_block", "mossy_stone_bricks", "mossy_stone_bricks", "mossy_cobblestone", "oak_log",
				"mossy_stone_bricks", "mossy_stone_brick_stairs", "mossy_stone_brick_wall", "moss_carpet", "moss_block", "azalea_leaves",
				"shroomlight", "lantern"};
		};
		Mat[] order = {Mat.FLOOR, Mat.FLOOR2, Mat.WALL, Mat.WALL2, Mat.PILLAR, Mat.CEILING, Mat.STAIR, Mat.FENCE, Mat.CARPET, Mat.DECOR,
			Mat.ACCENT, Mat.LIGHT, Mat.LANTERN};
		for (int i = 0; i < order.length; i++) {
			m.put(order[i], look[i]);
		}
		if (style == Style.GARDEN) {
			m.put(Mat.LEAVES, "flowering_azalea_leaves");
		}
		Map<Mat, BlockState> out = new EnumMap<>(Mat.class);
		m.forEach((k, v) -> out.put(k, block(v)));
		return out;
	}
}
