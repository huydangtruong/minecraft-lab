package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.DangerMobs;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryFloor;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryRules;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D65: a Crimson Master chieftain (quest boss) drops 10 times its vanilla XP. D207: a danger-boosted mob drops XP in step with its extra health. D267: a Purgatory floor boss drops 10 times its vanilla XP. */
@Mixin(LivingEntity.class)
abstract class LivingEntityXpMixin {
	@Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
	private void permahearts$chieftainXp(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
		if (((Entity) (Object) this).entityTags().contains(QuestBoss.TAG)) {
			cir.setReturnValue(cir.getReturnValueI() * QuestBoss.XP_MULTIPLIER);
			return;
		}
		if (((Entity) (Object) this).entityTags().contains(PurgatoryFloor.BOSS_TAG)) {
			cir.setReturnValue(cir.getReturnValueI() * PurgatoryRules.BOSS_XP_MULTIPLIER); // D267
			return;
		}
		cir.setReturnValue(DangerRules.scaledXp(cir.getReturnValueI(), DangerMobs.xpFactor((LivingEntity) (Object) this)));
	}
}
