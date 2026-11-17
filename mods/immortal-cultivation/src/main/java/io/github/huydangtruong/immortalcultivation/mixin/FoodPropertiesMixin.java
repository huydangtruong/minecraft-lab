package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla feeds the player in FoodProperties#onConsume; right after it, the Vitality elixir heals extra. */
@Mixin(FoodProperties.class)
abstract class FoodPropertiesMixin {
	@Inject(method = "onConsume", at = @At("TAIL"))
	private void permahearts$vitality(Level level, LivingEntity entity, ItemStack stack, Consumable consumable, CallbackInfo ci) {
		if (entity instanceof ServerPlayer p) {
			ImmortalCultivation.onAte(p, ((FoodProperties) (Object) this).nutrition());
		}
	}
}
