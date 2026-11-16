package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.DeathMatchSky;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Death Match: purple fog color. */
@Mixin(FogRenderer.class)
abstract class DeathMatchFogMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void permahearts$purpleFog(Camera camera, int renderDistance, DeltaTracker tracker, float darkness,
		ClientLevel level, CallbackInfoReturnable<FogData> cir) {
		DeathMatchSky.applyFog(cir.getReturnValue(), camera, level);
	}
}
