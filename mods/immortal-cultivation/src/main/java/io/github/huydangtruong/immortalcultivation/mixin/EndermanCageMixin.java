package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.Crystal;
import io.github.huydangtruong.immortalcultivation.technique.CrystalDisarm;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Crystal Prison (D143): an Enderman inside a live cage, or disarmed, cannot teleport out. */
@Mixin(Enderman.class)
abstract class EndermanCageMixin {
	@Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true)
	private void permahearts$noTeleport(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		Enderman self = (Enderman) (Object) this;
		if (CrystalDisarm.disarmed(self) || Crystal.inCage(self)) {
			cir.setReturnValue(false);
		}
	}
}
