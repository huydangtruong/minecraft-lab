package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.passive.ItemGuard;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * D176 Item guard. A stack that would break (damage reaches max) under a player with the passive stays, at full damage,
 * with a debt component. While the component is there the stack is dead weight: an empty hand for mining and attacking,
 * no use, no attribute modifiers (armor, weapon), no enchantment effects (see EnchantmentGuardMixin). Elytra cannot glide
 * on its own because vanilla refuses gliding at damage >= max - 1. Repair (setDamageValue going down) pays the debt first.
 */
@Mixin(ItemStack.class)
abstract class ItemGuardMixin {
	@Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
	private void permahearts$guardBreak(int newDamage, ServerPlayer player, Consumer<ItemStack> onBreak, CallbackInfo ci) {
		ItemStack self = (ItemStack) (Object) this;
		if (ItemGuard.isBroken(self)) {
			ci.cancel(); // already broken: nothing more to lose
			return;
		}
		if (player != null && newDamage >= self.getMaxDamage()) {
			int level = ItemGuard.level(player);
			if (level >= 1) {
				ItemGuard.breakGuarded(self, level);
				ci.cancel();
			}
		}
	}

	@Inject(method = "setDamageValue", at = @At("HEAD"), cancellable = true)
	private void permahearts$payDebt(int damage, CallbackInfo ci) {
		ItemStack self = (ItemStack) (Object) this;
		if (ItemGuard.isBroken(self) && ItemGuard.repair(self, damage)) {
			ci.cancel();
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void permahearts$noSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(1.0F);
		}
	}

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void permahearts$noDrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(!state.requiresCorrectToolForDrops()); // like an empty hand
		}
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void permahearts$noUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void permahearts$noUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
	private void permahearts$noInteract(Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("HEAD"), cancellable = true)
	private void permahearts$noModifiersGroup(EquipmentSlotGroup group, TriConsumer<?, ?, ?> consumer, CallbackInfo ci) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
	private void permahearts$noModifiersSlot(EquipmentSlot slot, java.util.function.BiConsumer<?, ?> consumer, CallbackInfo ci) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
	private void permahearts$barVisible(CallbackInfoReturnable<Boolean> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
	private void permahearts$barWidth(CallbackInfoReturnable<Integer> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(13); // a full red bar
		}
	}

	@Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
	private void permahearts$barColor(CallbackInfoReturnable<Integer> cir) {
		if (ItemGuard.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(0xFFE02020);
		}
	}

	@Inject(method = "getTooltipLines", at = @At("RETURN"))
	private void permahearts$tooltip(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
		ItemStack self = (ItemStack) (Object) this;
		if (ItemGuard.isBroken(self)) {
			List<Component> lines = cir.getReturnValue();
			lines.add(Math.min(1, lines.size()), ItemGuard.tooltip(self));
		}
	}
}
