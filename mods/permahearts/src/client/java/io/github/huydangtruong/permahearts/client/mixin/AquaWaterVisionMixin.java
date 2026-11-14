package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.AquaVision;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aqua Marine: full water vision at once (the vanilla value ramps up over 30 s underwater; fog reads this). */
@Mixin(LocalPlayer.class)
abstract class AquaWaterVisionMixin {
	@Inject(method = "getWaterVision", at = @At("RETURN"), cancellable = true)
	private void permahearts$aquaClear(CallbackInfoReturnable<Float> cir) {
		if (AquaVision.active((LocalPlayer) (Object) this)) {
			cir.setReturnValue(1.0f);
		}
	}
}
