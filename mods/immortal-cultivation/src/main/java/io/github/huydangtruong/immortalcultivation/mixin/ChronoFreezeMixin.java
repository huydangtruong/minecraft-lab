package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.chrono.ChronoFreeze;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D268: entities in a Time Rewind stop skip their tick (the level asks this for every entity, on both sides). */
@Mixin(TickRateManager.class)
public class ChronoFreezeMixin {
	@Inject(method = "isEntityFrozen", at = @At("HEAD"), cancellable = true)
	private void permahearts$chrono(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ChronoFreeze.frozen(entity)) {
			cir.setReturnValue(true);
		}
	}
}
