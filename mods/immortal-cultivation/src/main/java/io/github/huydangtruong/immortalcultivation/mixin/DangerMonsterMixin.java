package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.DangerSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** P-054: daylight and rain monster spawns. Only a "too bright" rejection is relaxed; block light still blocks. */
@Mixin(Monster.class)
public abstract class DangerMonsterMixin {
	@Inject(method = "isDarkEnoughToSpawn", at = @At("RETURN"), cancellable = true)
	private static void permahearts$daySpawn(ServerLevelAccessor level, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && DangerSpawn.allowDaySpawn(level, pos, random)) {
			cir.setReturnValue(true);
		}
	}
}
