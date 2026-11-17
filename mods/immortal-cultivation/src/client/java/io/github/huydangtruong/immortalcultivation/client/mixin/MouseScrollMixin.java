package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** D125: the mouse wheel is one ring: hotbar 1..9, then the filled technique slots, then hotbar 1 again. */
@Mixin(MouseHandler.class)
abstract class MouseScrollMixin {
	@Redirect(method = "onScroll", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
	private int permahearts$ring(double wheel, int hotbarSlot, int size) {
		return ClientSkills.scroll(net.minecraft.client.Minecraft.getInstance(), wheel, hotbarSlot);
	}
}
