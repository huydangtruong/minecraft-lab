package io.github.huydangtruong.immortalcultivation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * D241 runes: wherever the enchantment code reads an item's enchantments to apply them (levels and effects), it sees the
 * item's own enchantments plus its rune bonuses. Tooltips, anvils and the enchanting table still see the real ones.
 */
@Mixin(EnchantmentHelper.class)
abstract class RuneEnchantmentMixin {
	@WrapOperation(method = "getItemEnchantmentLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemInstance;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object permahearts$runeLevel(ItemInstance stack, DataComponentType<?> type, Object fallback, Operation<Object> original) {
		Object base = original.call(stack, type, fallback);
		return type == DataComponents.ENCHANTMENTS ? Runes.withRunes(stack, (ItemEnchantments) base) : base;
	}

	@WrapOperation(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
		at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object permahearts$runeEffects(ItemStack stack, DataComponentType<?> type, Object fallback, Operation<Object> original) {
		Object base = original.call(stack, type, fallback);
		return type == DataComponents.ENCHANTMENTS ? Runes.withRunes(stack, (ItemEnchantments) base) : base;
	}

	@WrapOperation(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
	private static Object permahearts$runeSlotEffects(ItemStack stack, DataComponentType<?> type, Operation<Object> original) {
		Object base = original.call(stack, type);
		if (type != DataComponents.ENCHANTMENTS) {
			return base;
		}
		ItemEnchantments merged = Runes.withRunes(stack, (ItemEnchantments) base);
		return merged == null || base == null && merged.isEmpty() ? base : merged;
	}
}
