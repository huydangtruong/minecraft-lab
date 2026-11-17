package io.github.huydangtruong.immortalcultivation.client.mixin;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While a technique is selected, right click, left click and breaking blocks do not act on the held item (P-011b). */
@Mixin(Minecraft.class)
abstract class MinecraftInputMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void permahearts$noItemUse(CallbackInfo ci) {
		if (ClientSkills.intercepting((Minecraft) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void permahearts$leftClickCasts(CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = (Minecraft) (Object) this;
		if (ClientSkills.intercepting(mc)) {
			ClientSkills.onAttackClick(mc);
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void permahearts$noBlockBreaking(boolean down, CallbackInfo ci) {
		if (ClientSkills.intercepting((Minecraft) (Object) this)) {
			ci.cancel();
		}
	}

	/** D125: a number key goes back to that hotbar slot and clears the technique selection. */
	@Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private void permahearts$numberKeyBackToHotbar(Inventory inventory, int slot) {
		ClientSkills.backToHotbar();
		inventory.setSelectedSlot(slot);
	}
}
