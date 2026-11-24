package io.github.huydangtruong.immortalcultivation.rune;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** The Rune Forge block (D242): right-click to inscribe runes on gear or merge two runes into a stronger one. */
public final class RuneForge extends Block {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "rune_forge");
	public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, ID, new RuneForge(Properties.of()
		.setId(ResourceKey.create(Registries.BLOCK, ID))
		.mapColor(MapColor.COLOR_GRAY)
		.strength(3.5f, 6f)
		.requiresCorrectToolForDrops()
		.lightLevel(s -> 7)
		.sound(SoundType.METAL)));
	public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()));

	private RuneForge(Properties p) {
		super(p);
	}

	static void init() {
		RuneForgeMenu.init();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer sp) {
			sp.openMenu(new ExtendedMenuProvider<BlockPos>() {
				@Override
				public BlockPos getScreenOpeningData(ServerPlayer p) {
					return pos;
				}

				@Override
				public Component getDisplayName() {
					return Component.translatable("block.permahearts.rune_forge");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
					return new RuneForgeMenu(id, inv, ContainerLevelAccess.create(level, pos));
				}
			});
		}
		return InteractionResult.SUCCESS;
	}
}
