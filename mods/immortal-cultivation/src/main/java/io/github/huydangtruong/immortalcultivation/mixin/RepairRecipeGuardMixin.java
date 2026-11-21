package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.passive.ItemGuard;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D176 Item guard: the crafting grid builds a fresh stack, which would skip the debt; broken items repair at the anvil only. */
@Mixin(RepairItemRecipe.class)
abstract class RepairRecipeGuardMixin {
	@Inject(method = "canCombine", at = @At("HEAD"), cancellable = true)
	private static void permahearts$noBrokenCombine(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
		if (ItemGuard.isBroken(a) || ItemGuard.isBroken(b)) {
			cir.setReturnValue(false);
		}
	}
}
