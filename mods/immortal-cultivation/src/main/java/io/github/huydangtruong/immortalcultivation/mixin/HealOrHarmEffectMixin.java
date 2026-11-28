package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.WitchDanger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D282: Instant Health and Harming of a danger-boosted witch scale with its strength (splash: thrower; drink: the witch itself). */
@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
abstract class HealOrHarmEffectMixin {
	/** Splash: the proximity multiplies both the heal and the damage. */
	@ModifyVariable(method = "applyInstantaneousEffect", at = @At("HEAD"), argsOnly = true)
	private double permahearts$witchSplash(double proximity, ServerLevel level, Entity source, Entity indirect) {
		return proximity * WitchDanger.instantFactor(indirect);
	}

	@Shadow
	@Final
	private boolean isHarm;

	/** A witch drinking Instant Health heals itself more (same rule as vanilla, times the factor). */
	@Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
	private void permahearts$witchDrink(ServerLevel level, LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
		double k = WitchDanger.instantFactor(entity);
		if (k > 1.0 && !isHarm && !entity.isInvertedHealAndHarm()) {
			entity.heal((float) (Math.max(4 << amplifier, 0) * k));
			cir.setReturnValue(true);
		}
	}
}
