package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.technique.PhaseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Phase Walk x-ray view: no in-wall block overlay while the local player phases (like spectator mode). */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectPhaseMixin {
	@Inject(method = "submit", at = @At("HEAD"))
	private void permahearts$noWallOverlay(float partialTick, SubmitNodeCollector collector, PlayerRenderState state,
		CameraRenderState camera, boolean flag, CallbackInfo ci) {
		if (state.blockOverlay != null && PhaseState.phasing(Minecraft.getInstance().player)) {
			state.blockOverlay = null;
		}
	}
}
