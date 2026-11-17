package io.github.huydangtruong.immortalcultivation.reward;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The grey "?" chest a done quest line shows (D90): the real box is rolled when the line is claimed. Never given to a player. */
public class MysteryBoxItem extends Item {
	public MysteryBoxItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
		Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(RewardMessages.mysteryTooltip());
	}
}
