package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D200: the wandering trader (an AbstractVillager with avoid goals) does not run from a grasped mob either. */
@Mixin(AvoidEntityGoal.class)
public abstract class TraderGraspFearMixin<T extends LivingEntity> {
	@Shadow @Final protected PathfinderMob mob;
	@Shadow protected T toAvoid;

	@Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
	private void permahearts$graspFear(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && mob instanceof AbstractVillager && Grasp.isOnGraspSide(toAvoid)) {
			cir.setReturnValue(false);
		}
	}
}
