package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.PhaseFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Phase Walk sight limit: dark fog that ends at phaseSightRange(level) while the camera is inside a solid block. */
@Mixin(FogRenderer.class)
abstract class PhaseFogMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void permahearts$phaseFog(Camera camera, int renderDistance, DeltaTracker tracker, float darkness,
		ClientLevel level, CallbackInfoReturnable<FogData> cir) {
		PhaseFog.apply(cir.getReturnValue(), camera, level);
	}
}
