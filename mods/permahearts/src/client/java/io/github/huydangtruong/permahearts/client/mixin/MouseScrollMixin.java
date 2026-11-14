package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Technique mode: the mouse wheel moves over the 9 hotbar cells (techniques first), not over the real item slots. */
@Mixin(MouseHandler.class)
abstract class MouseScrollMixin {
	@Redirect(method = "onScroll", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
	private int permahearts$currentCell(Inventory inventory) {
		return ClientSkills.mode() ? ClientSkills.cell() : inventory.getSelectedSlot();
	}

	@Redirect(method = "onScroll", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private void permahearts$newCell(Inventory inventory, int slot) {
		if (ClientSkills.mode()) {
			ClientSkills.setCell(slot);
		} else {
			inventory.setSelectedSlot(slot);
		}
	}
}
