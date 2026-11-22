package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D195: grasped mobs near a bed do not stop their owner from sleeping (ZombifiedPiglin overrides the check, so both). */
@Mixin({Monster.class, ZombifiedPiglin.class})
public abstract class GraspSleepMixin {
	@Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
	private void permahearts$graspSleep(ServerLevel level, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (Grasp.sparesSleep((Monster) (Object) this, player)) {
			cir.setReturnValue(false);
		}
	}
}
