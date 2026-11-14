package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.technique.PhaseState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Phase Walk: no suffocation damage while phasing. */
@Mixin(Entity.class)
abstract class EntityInWallMixin {
	@Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
	private void permahearts$phase(CallbackInfoReturnable<Boolean> cir) {
		if (PhaseState.phasing((Entity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
