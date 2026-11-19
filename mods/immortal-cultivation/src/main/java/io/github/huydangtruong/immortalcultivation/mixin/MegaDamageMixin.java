package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.treasure.MegaMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** P-059: everything a Mega mob hits with deals its multiplier (x2..x4) in damage. */
@Mixin(LivingEntity.class)
abstract class MegaDamageMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float permahearts$megaDamage(float amount, ServerLevel level, DamageSource source) {
		return MegaMobs.scaleDamage(source, amount);
	}
}
