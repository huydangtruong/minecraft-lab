package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soul Grasp: mobs that think with a Brain (villagers, piglins ...) stop their own plans; goals drive them instead. */
@Mixin(Brain.class)
public abstract class BrainGraspMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void permahearts$grasp(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
		if (Grasp.isGrasped(entity)) {
			ci.cancel();
		}
	}
}
