package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.HeartsAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Death Match (D25): a player that fell to another player during the match keeps items and XP, like keepInventory.
 * Vanilla decides both in Player (it reads GameRules.KEEP_INVENTORY there); we skip the same two methods when the
 * victim carries the dm_protected flag (set in ALLOW_DEATH, before die()).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	/** The inventory is dropped here (the super method in Avatar is empty, so cancelling equals keepInventory). */
	@Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
	private void permahearts$keepItems(ServerLevel level, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer sp && HeartsAttachment.isDeathMatchProtected(sp)) {
			ci.cancel();
		}
	}

	/** The XP orbs of a dead player come from this value (0 under keepInventory). */
	@Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
	private void permahearts$keepXp(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof ServerPlayer sp && HeartsAttachment.isDeathMatchProtected(sp)) {
			cir.setReturnValue(0);
		}
	}
}
