package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.PassiveVision;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D148 Increase vision above 100%: clearer under water (fog reads this value). */
@Mixin(LocalPlayer.class)
abstract class PassiveWaterVisionMixin {
	@Inject(method = "getWaterVision", at = @At("RETURN"), cancellable = true)
	private void permahearts$passiveWater(CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(PassiveVision.waterVision((LocalPlayer) (Object) this, cir.getReturnValueF()));
	}
}
