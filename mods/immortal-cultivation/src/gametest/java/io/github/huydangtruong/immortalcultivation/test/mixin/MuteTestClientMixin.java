package io.github.huydangtruong.immortalcultivation.test.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client game tests run silent (Huy may be playing or asleep while tests run): no sound is ever started.
 * Lives in the test mod only, so the real mod and runClient are not affected.
 */
@Mixin(SoundEngine.class)
abstract class MuteTestClientMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void permahearts_test$mute(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
	}
}
