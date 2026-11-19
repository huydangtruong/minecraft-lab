package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.passive.CropGrowth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** P-058 Crop growth: a seed a player plants remembers the planter. */
@Mixin(BlockItem.class)
abstract class CropPlantMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void permahearts$planted(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && ctx.getPlayer() instanceof ServerPlayer sp && ctx.getLevel() instanceof ServerLevel sl) {
			CropGrowth.planted(sl, ctx.getClickedPos(), sp.getUUID());
		}
	}
}
