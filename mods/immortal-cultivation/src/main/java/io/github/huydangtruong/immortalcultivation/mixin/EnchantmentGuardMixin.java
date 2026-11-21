package io.github.huydangtruong.immortalcultivation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.huydangtruong.immortalcultivation.passive.ItemGuard;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** D176 Item guard: a broken stack gives no enchantment effects (except for Mending, so Mending can repair it). */
@Mixin(EnchantmentHelper.class)
abstract class EnchantmentGuardMixin {
	@WrapOperation(method = "runIterationOnItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object permahearts$noEnchantments(ItemStack stack, DataComponentType<?> type, Object fallback, Operation<Object> original) {
		if (type == DataComponents.ENCHANTMENTS && !ItemGuard.mending && ItemGuard.isBroken(stack)) {
			return ItemEnchantments.EMPTY;
		}
		return original.call(stack, type, fallback);
	}
}
