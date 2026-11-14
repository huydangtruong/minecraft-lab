package io.github.huydangtruong.permahearts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.huydangtruong.permahearts.PermaHearts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vitality elixirs make natural regeneration from food stronger (D41): both heals in FoodData#tick (fast regen from
 * saturation with a full food bar, slow regen at 18+ food) are multiplied, +20% per bottle. Food cost is unchanged.
 */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
	private void permahearts$vitalityRegen(ServerPlayer player, float amount, Operation<Void> original) {
		original.call(player, amount * PermaHearts.regenMultiplier(player));
	}
}
