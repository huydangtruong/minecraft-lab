package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The two blocks of Crystal Mayhem (D174): a cluster that looks like a vanilla amethyst cluster and a budding amethyst that
 * never grows anything. Neither drops anything. They are placed and removed only by {@link Mayhem}.
 */
public final class MayhemBlocks {
	public static final Identifier CLUSTER_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "mayhem_cluster");
	public static final Identifier BUDDING_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "mayhem_budding");

	/** A cluster; decor = true for the ones that grow on a budding block (never chain targets). */
	public static final class Cluster extends AmethystClusterBlock {
		public static final BooleanProperty DECOR = BooleanProperty.create("decor");

		Cluster(Properties properties) {
			super(7, 3, properties);
			registerDefaultState(defaultBlockState().setValue(DECOR, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(DECOR);
		}
	}

	/** Budding amethyst that gives light 15 while it is active (lit). No random growth. */
	public static final class Budding extends AmethystBlock {
		public static final BooleanProperty LIT = BooleanProperty.create("lit");

		Budding(Properties properties) {
			super(properties);
			registerDefaultState(stateDefinition.any().setValue(LIT, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(LIT);
		}
	}

	public static final Block CLUSTER = Registry.register(BuiltInRegistries.BLOCK, CLUSTER_ID, new Cluster(
		Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, CLUSTER_ID))
			.noOcclusion()
			.sound(SoundType.AMETHYST_CLUSTER)
			.strength(0.5f)
			.noLootTable()));

	public static final Block BUDDING = Registry.register(BuiltInRegistries.BLOCK, BUDDING_ID, new Budding(
		Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, BUDDING_ID))
			.sound(SoundType.AMETHYST)
			.strength(1.5f)
			.lightLevel(s -> s.getValue(Budding.LIT) ? 15 : 0)
			.noLootTable()));

	private MayhemBlocks() {}

	public static void init() {
		CLUSTER.defaultBlockState(); // touch the class so both blocks are registered
	}

	public static BlockState cluster(net.minecraft.core.Direction facing, boolean decor) {
		return CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing).setValue(Cluster.DECOR, decor);
	}

	/** True for a cluster that can still become a budding (placed by a shard, not decoration). */
	public static boolean isChainCluster(BlockState s) {
		return s.is(CLUSTER) && !s.getValue(Cluster.DECOR);
	}
}
