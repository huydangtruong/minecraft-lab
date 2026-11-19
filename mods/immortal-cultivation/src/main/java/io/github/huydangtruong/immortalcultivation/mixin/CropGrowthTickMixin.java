package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.passive.CropGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** P-058 Crop growth: an owned crop gets extra growth attempts after its vanilla random tick. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class CropGrowthTickMixin {
	@Inject(method = "randomTick", at = @At("TAIL"))
	private void permahearts$cropGrowth(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		CropGrowth.afterRandomTick((BlockState) (Object) this, level, pos, random);
	}
}
