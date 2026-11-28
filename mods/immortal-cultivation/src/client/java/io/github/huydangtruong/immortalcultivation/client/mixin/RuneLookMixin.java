package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.rune.RuneEntry;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * D278 fix: a rune made before the glyph and number textures keeps its old model data until it ticks in an inventory,
 * so in a chest, an item frame or on the ground it showed a blank stone. The client draws every rune with the look
 * computed from its rune component, wherever it is.
 */
@Mixin(ItemModelResolver.class)
abstract class RuneLookMixin {
	@ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true)
	private ItemStack permahearts$runeLook(ItemStack stack) {
		RuneEntry r = Runes.entryOf(stack);
		if (r == null) {
			return stack;
		}
		CustomModelData look = Runes.look(r);
		if (look.equals(stack.get(DataComponents.CUSTOM_MODEL_DATA))) {
			return stack;
		}
		ItemStack copy = stack.copy();
		copy.set(DataComponents.CUSTOM_MODEL_DATA, look);
		return copy;
	}
}
