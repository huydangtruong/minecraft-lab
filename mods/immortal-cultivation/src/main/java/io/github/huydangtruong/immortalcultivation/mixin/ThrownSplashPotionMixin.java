package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.WitchPotions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** D167: a splash thrown by a grasped witch skips the entities of the wrong side (harm on allies, good effects on enemies). */
@Mixin(ThrownSplashPotion.class)
public abstract class ThrownSplashPotionMixin {
	@Redirect(method = "onHitAsPotion", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;isAffectedByPotions()Z"))
	private boolean permahearts$side(LivingEntity target) {
		return target.isAffectedByPotions() && WitchPotions.allows((AbstractThrownPotion) (Object) this, target);
	}
}
