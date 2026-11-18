package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.CrystalBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Crystal Prison (D143): one block id has 20 states with different blast resistance, but vanilla reads a single number per
 * block. The subclasses of the calculator all call super, so this one hook covers every explosion.
 */
@Mixin(ExplosionDamageCalculator.class)
abstract class CrystalExplosionMixin {
	@Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
	private void permahearts$crystalResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state,
											   FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
		if (state.getBlock() instanceof CrystalBlock) {
			cir.setReturnValue(Optional.of(Math.max(CrystalBlock.blastResistance(state), fluid.getExplosionResistance())));
		}
	}
}
