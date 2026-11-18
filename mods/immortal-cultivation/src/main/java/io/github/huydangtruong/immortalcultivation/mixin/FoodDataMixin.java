package io.github.huydangtruong.immortalcultivation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vitality elixirs make natural regeneration from food stronger (D41): both heals in FoodData#tick (fast regen from
 * saturation with a full food bar, slow regen at 18+ food) are multiplied, +20% per bottle. Food cost is unchanged.
 */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
	private void permahearts$vitalityRegen(ServerPlayer player, float amount, Operation<Void> original) {
		original.call(player, amount * ImmortalCultivation.regenMultiplier(player));
	}

	/** D148 Hunger sustain: the food bar never ends a tick below the passive's floor. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void permahearts$hungerFloor(ServerPlayer player, CallbackInfo ci) {
		io.github.huydangtruong.immortalcultivation.passive.Passives.holdFood(player);
	}
}
