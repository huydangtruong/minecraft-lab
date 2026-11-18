package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.ClientSeism;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Seism: a short camera shake (a few tenths of a degree) after the camera has been placed for the frame. */
@Mixin(Camera.class)
abstract class SeismShakeMixin {
	@Shadow
	private float xRot;
	@Shadow
	private float yRot;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "update", at = @At("TAIL"))
	private void permahearts$shake(DeltaTracker delta, CallbackInfo ci) {
		float s = ClientSeism.shakeNow();
		if (s > 0f) {
			setRotation(yRot + ClientSeism.noise() * s, xRot + ClientSeism.noise() * s);
		}
	}
}
