package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.Stun;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Heaven Blast stun (D99): a stunned mob runs no AI at all (goals, navigation, move control), steers nowhere and cannot
 * land a hit. Gravity and knockback still move it, because those are physics, not AI.
 */
@Mixin(Mob.class)
abstract class MobStunMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void permahearts$stunnedAi(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (self.hasEffect(Stun.EFFECT)) {
			self.getNavigation().stop();
			self.xxa = 0f;
			self.zza = 0f;
			self.setJumping(false);
			ci.cancel();
		}
	}

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void permahearts$stunnedHit(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (((Mob) (Object) this).hasEffect(Stun.EFFECT)) {
			cir.setReturnValue(false);
		}
	}
}
