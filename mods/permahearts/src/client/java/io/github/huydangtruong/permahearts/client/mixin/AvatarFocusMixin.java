package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import io.github.huydangtruong.permahearts.technique.FocusItems;
import io.github.huydangtruong.permahearts.technique.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person: a player with a technique selected holds its focus item in the main hand (D46). */
@Mixin(AvatarRenderer.class)
abstract class AvatarFocusMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL"))
	private void permahearts$focusInHand(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		if (!(avatar instanceof Player player)) {
			return;
		}
		Technique t = ClientSkills.focusOf(player);
		if (t == null) {
			return;
		}
		ItemStack stack = FocusItems.stack(t);
		boolean right = state.mainArm == HumanoidArm.RIGHT;
		var itemState = right ? state.rightHandItemState : state.leftHandItemState;
		itemState.clear();
		Minecraft.getInstance().getItemModelResolver().updateForLiving(itemState, stack,
			right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, player);
		if (right) {
			state.rightHandItemStack = stack;
			state.rightArmPose = HumanoidModel.ArmPose.ITEM;
		} else {
			state.leftHandItemStack = stack;
			state.leftArmPose = HumanoidModel.ArmPose.ITEM;
		}
	}
}
