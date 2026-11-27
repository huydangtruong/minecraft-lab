package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.ChronoGrey;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** D268: the grey of a Time Rewind stop is a post effect asked for while a stop is near, fed with the camera every frame. */
@Mixin(GameRenderer.class)
abstract class ChronoPostMixin {
	@Shadow
	@Final
	private List<Identifier> requestedPostEffects;

	@Shadow
	@Final
	private List<PostChain> appliedPostEffects;

	@Inject(method = "update", at = @At("TAIL"))
	private void permahearts$request(DeltaTracker delta, CallbackInfo ci) {
		if (ChronoGrey.wanted()) {
			requestedPostEffects.add(ChronoGrey.ID);
		}
	}

	@Inject(method = "applyPostEffects", at = @At("HEAD"))
	private void permahearts$feed(CallbackInfo ci) {
		for (PostChain chain : appliedPostEffects) {
			if (ChronoGrey.ID.equals(chain.id())) {
				ChronoGrey.feed(chain, ((GameRenderer) (Object) this).mainCamera());
			}
		}
	}
}
