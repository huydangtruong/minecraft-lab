package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Charging a technique zooms the view like drawing a bow (vanilla: up to 15 percent, easing in with the square). */
@Mixin(AbstractClientPlayer.class)
abstract class ChargeFovMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void permahearts$chargeZoom(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if ((Object) this == Minecraft.getInstance().player && ClientSkills.charging()) {
			float c = ClientSkills.chargeFraction();
			cir.setReturnValue(cir.getReturnValueF() * (1f - c * c * 0.15f));
		}
	}
}
