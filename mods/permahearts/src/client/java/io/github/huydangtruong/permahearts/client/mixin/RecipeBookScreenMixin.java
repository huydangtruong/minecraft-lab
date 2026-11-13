package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.StatsPanel;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A click on the stats panel is not a click outside the inventory (which would throw the carried item). */
@Mixin(AbstractRecipeBookScreen.class)
abstract class RecipeBookScreenMixin {
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void permahearts$panelIsInside(double x, double y, int left, int top, CallbackInfoReturnable<Boolean> cir) {
		if (StatsPanel.contains(this, x, y)) {
			cir.setReturnValue(false);
		}
	}
}
