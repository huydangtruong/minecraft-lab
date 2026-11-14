package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.technique.PhaseState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Phase Walk: the locked floor follows falls, on both sides (client prediction and server). */
@Mixin(Player.class)
abstract class PlayerPhaseMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void permahearts$phaseFloor(CallbackInfo ci) {
		PhaseState.followFall((Player) (Object) this);
	}
}
