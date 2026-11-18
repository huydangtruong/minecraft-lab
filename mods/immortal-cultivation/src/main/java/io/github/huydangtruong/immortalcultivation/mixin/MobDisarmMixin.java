package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.CrystalDisarm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Crystal Prison disarm (D143): a disarmed mob has no target and lands no hit, but its AI still runs, so it can walk about.
 */
@Mixin(Mob.class)
abstract class MobDisarmMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void permahearts$noTarget(LivingEntity target, CallbackInfo ci) {
		if (target != null && CrystalDisarm.disarmed((Mob) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "serverAiStep", at = @At("HEAD"))
	private void permahearts$dropTarget(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (CrystalDisarm.disarmed(self) && self.getTarget() != null) {
			self.setTarget(null);
		}
	}

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void permahearts$noHit(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (CrystalDisarm.disarmed((Mob) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
