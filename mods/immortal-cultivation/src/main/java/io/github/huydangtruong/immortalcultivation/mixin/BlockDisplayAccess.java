package io.github.huydangtruong.immortalcultivation.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** BlockDisplay#setBlockState is private in 26.3. */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccess {
	@Invoker("setBlockState")
	void permahearts$setBlockState(BlockState state);
}
