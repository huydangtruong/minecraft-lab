package io.github.huydangtruong.permahearts.client.mixin;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import io.github.huydangtruong.permahearts.technique.FocusItems;
import io.github.huydangtruong.permahearts.technique.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: while a technique is selected the main hand shows its focus item (render state only, D46). */
@Mixin(FirstPersonHandsAndItems.class)
abstract class FirstPersonFocusMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void permahearts$focusInHand(LocalPlayer player, float partialTick, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		Technique t = ClientSkills.focusOf(player);
		if (t == null) {
			return;
		}
		ItemStack stack = FocusItems.stack(t);
		ItemDisplayContext context = player.getMainArm() == HumanoidArm.RIGHT
			? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		state.mainHandItem = stack;
		state.mainHandRenderState.clear();
		Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.mainHandRenderState, stack, context,
			player.level(), player, player.getId() + context.ordinal());
		state.mainHandUseDuration = 0;
		state.mainHandChargeDuration = 0;
		state.mainHandSwapScale = 1f;
	}
}
