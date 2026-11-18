package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.DangerMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** P-054 (D138): elite affixes. Ironhide takes 40% less damage; Blazing ignores fire damage. */
@Mixin(LivingEntity.class)
public abstract class EliteHurtMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void permahearts$fireImmune(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (source.is(DamageTypeTags.IS_FIRE) && DangerMobs.ignoresFire(self)) {
			self.clearFire();
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float permahearts$ironhide(float amount) {
		return DangerMobs.adjustDamage((LivingEntity) (Object) this, amount);
	}
}
