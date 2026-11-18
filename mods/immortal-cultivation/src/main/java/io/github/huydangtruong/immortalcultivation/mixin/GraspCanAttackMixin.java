package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Soul Grasp (D134): grasped mobs never attack their owner or each other, golems and wolves leave them alone. */
@Mixin(Mob.class)
public abstract class GraspCanAttackMixin {
	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void permahearts$grasp(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		if (Grasp.blocksAttack((Mob) (Object) this, target)) {
			cir.setReturnValue(false);
		}
	}
}
