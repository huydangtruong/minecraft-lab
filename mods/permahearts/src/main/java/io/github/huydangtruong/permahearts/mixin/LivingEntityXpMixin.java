package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.wanderer.QuestBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D65: a Crimson Master chieftain (quest boss) drops 10 times its vanilla XP. */
@Mixin(LivingEntity.class)
abstract class LivingEntityXpMixin {
	@Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
	private void permahearts$chieftainXp(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
		if (((Entity) (Object) this).entityTags().contains(QuestBoss.TAG)) {
			cir.setReturnValue(cir.getReturnValueI() * QuestBoss.XP_MULTIPLIER);
		}
	}
}
