package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The wall of Crystal Prison (D143). One block, 20 states: the property is the technique level the cage was cast at, and
 * it sets how long the block takes to break and how well it resists explosions (CrystalRules). No item, no loot.
 */
public final class CrystalBlock extends TransparentBlock {
	public static final IntegerProperty TIER = IntegerProperty.create("tier", CrystalRules.MIN_TIER, CrystalRules.MAX_TIER);
	public static final Identifier ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "crystal_prison");

	public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, ID, new CrystalBlock(
		Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ID))
			.noOcclusion()
			.lightLevel(s -> CrystalRules.LIGHT)
			.noLootTable()
			.sound(SoundType.GLASS)
			.destroyTime((float) CrystalRules.crystalHardness(1))
			.explosionResistance((float) CrystalRules.crystalBlast(1))
			.isValidSpawn((state, level, pos, type) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos, box) -> false)));

	private CrystalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TIER, CrystalRules.MIN_TIER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TIER);
	}

	public static BlockState state(int tier) {
		return BLOCK.defaultBlockState().setValue(TIER, Math.max(CrystalRules.MIN_TIER, Math.min(CrystalRules.MAX_TIER, tier)));
	}

	/** Blast resistance of one state (used by the explosion mixin). */
	public static float blastResistance(BlockState state) {
		return (float) CrystalRules.crystalBlast(state.getValue(TIER));
	}

	/** Like vanilla, but the hardness comes from the tier instead of one number for the whole block. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		float hardness = (float) CrystalRules.crystalHardness(state.getValue(TIER));
		return player.getDestroySpeed(state) / hardness / (player.hasCorrectToolForDrops(state) ? 30 : 100);
	}

	public static void init() {
		BLOCK.defaultBlockState(); // touch the class so the block is registered
	}
}
