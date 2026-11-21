package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.PhaseState;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D169: tamed wolves, cats and parrots do not teleport to an owner who is phasing through walls (Spirit Walk). */
@Mixin(TamableAnimal.class)
abstract class TamableTeleportMixin {
	@Inject(method = "shouldTryTeleportToOwner", at = @At("HEAD"), cancellable = true)
	private void permahearts$notIntoWalls(CallbackInfoReturnable<Boolean> cir) {
		if (PhaseState.phasing(((TamableAnimal) (Object) this).getOwner())) {
			cir.setReturnValue(false);
		}
	}
}
