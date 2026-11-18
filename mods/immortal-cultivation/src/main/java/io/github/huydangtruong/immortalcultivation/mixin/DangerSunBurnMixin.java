package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.DangerMobs;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** P-054: mobs that spawned by daylight (danger level) never burn in the sun. D134: nor do Soul Grasp mobs. */
@Mixin(Mob.class)
public abstract class DangerSunBurnMixin {
	@Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
	private void permahearts$noBurn(CallbackInfoReturnable<Boolean> cir) {
		if (DangerMobs.isDaySpawn((Mob) (Object) this) || io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp.isGrasped((Mob) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
