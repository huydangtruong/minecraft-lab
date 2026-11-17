package io.github.huydangtruong.immortalcultivation.reward;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A reward box (D76). Right-click rolls its cards (once) and opens the choose screen. */
public class RewardBoxItem extends Item {
	private final RewardTier tier;

	public RewardBoxItem(Properties properties, RewardTier tier) {
		super(properties);
		this.tier = tier;
	}

	public RewardTier tier() {
		return tier;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp) {
			RewardBoxes.open(sp, hand);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
		Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(RewardMessages.boxTooltip(tier.options(), tier.picks()));
	}
}
