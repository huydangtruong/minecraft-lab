package io.github.huydangtruong.immortalcultivation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.huydangtruong.immortalcultivation.passive.Passives;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** D148 XP gain bonus: XP from orbs (what is left after Mending) is raised; commands and refunds are not. */
@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbMixin {
	@WrapOperation(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
	private void permahearts$xpBonus(Player player, int amount, Operation<Void> original) {
		original.call(player, player instanceof ServerPlayer sp ? Passives.bonusXp(sp, amount) : amount);
	}
}
