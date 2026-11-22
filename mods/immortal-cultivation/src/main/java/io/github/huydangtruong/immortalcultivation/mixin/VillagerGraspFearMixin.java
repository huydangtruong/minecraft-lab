package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D200: villagers do not see a grasped mob (or a grasped evoker's vex) as a hostile, so they never panic or flee from it. */
@Mixin(VillagerHostilesSensor.class)
public abstract class VillagerGraspFearMixin {
	@Inject(method = "isMatchingEntity", at = @At("HEAD"), cancellable = true)
	private void permahearts$graspFear(ServerLevel level, LivingEntity villager, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		if (Grasp.isOnGraspSide(target)) {
			cir.setReturnValue(false);
		}
	}
}
