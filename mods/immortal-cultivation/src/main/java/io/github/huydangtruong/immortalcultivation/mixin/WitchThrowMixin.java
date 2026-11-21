package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.WitchPotions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** D167: a grasped witch throws the potions of WitchPotions; a vanilla witch is untouched. */
@Mixin(Witch.class)
public abstract class WitchThrowMixin {
	@Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
	private void permahearts$grasped(LivingEntity target, float power, CallbackInfo ci) {
		Witch self = (Witch) (Object) this;
		if (WitchPotions.isGrasped(self)) {
			WitchPotions.throwAtEnemy(self, target);
			ci.cancel();
		}
	}
}
