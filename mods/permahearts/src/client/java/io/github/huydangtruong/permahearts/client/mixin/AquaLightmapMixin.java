package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.AquaVision;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Aqua Marine: Night-Vision-like brightness, only under water (a real effect would also show on land). */
@Mixin(LightmapRenderStateExtractor.class)
abstract class AquaLightmapMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extract", at = @At("RETURN"))
	private void permahearts$aquaBright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (minecraft.player != null && AquaVision.active(minecraft.player)) {
			state.nightVisionEffectIntensity = 1.0f;
		}
	}
}
