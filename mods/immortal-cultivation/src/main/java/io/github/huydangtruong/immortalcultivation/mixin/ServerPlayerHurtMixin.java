package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.IllusionTargets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Illusion (D71): ServerPlayer#hurtServer refuses damage from a player under PvP-off before LivingEntity#hurtServer (where
 * the Fabric ALLOW_DAMAGE event sits) runs. This head hook sees every attack on a player, so a blocked hit still makes the
 * attacker a target of the victim's clones.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerHurtMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void permahearts$noteAttack(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		IllusionTargets.noteDamage((ServerPlayer) (Object) this, source);
	}
}
