package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.MegaGlowLayer;
import io.github.huydangtruong.immortalcultivation.treasure.MegaMobs;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** P-059: hands the Mega species (or null) to MegaGlowLayer through the render state. */
@Mixin(LivingEntityRenderer.class)
abstract class MegaRenderStateMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
		at = @At("TAIL"))
	private void permahearts$mega(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
		MegaMobs.Mega m = MegaMobs.mega(entity);
		((FabricRenderState) state).setData(MegaGlowLayer.SPECIES, m == null ? null : m.species());
	}
}
