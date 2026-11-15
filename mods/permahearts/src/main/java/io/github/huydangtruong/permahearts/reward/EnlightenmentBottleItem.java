package io.github.huydangtruong.permahearts.reward;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Bottle of Enlightenment (D89): drinking it (32 ticks, like a potion) gives {@link #LEVELS} experience levels. */
public class EnlightenmentBottleItem extends Item {
	public static final int LEVELS = 16;

	public EnlightenmentBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer sp) {
			sp.giveExperienceLevels(LEVELS); // plays the level-up sound itself
		}
		return super.finishUsingItem(stack, level, entity); // the consumable component shrinks the stack (not in creative)
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
		Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(RewardMessages.bottleTooltip(LEVELS));
	}
}
