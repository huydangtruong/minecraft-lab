package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.PassiveVision;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** D148 Increase vision: night vision light, brighter and less Darkness above 100%. */
@Mixin(LightmapRenderStateExtractor.class)
abstract class PassiveVisionMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extract", at = @At("RETURN"))
	private void permahearts$passiveVision(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (minecraft.player != null) {
			PassiveVision.apply(minecraft.player, state);
		}
	}
}
