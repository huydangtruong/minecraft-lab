package io.github.huydangtruong.permahearts;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ElixirItem extends Item {
	private final ElixirStat stat;

	public ElixirItem(Properties properties, ElixirStat stat) {
		super(properties);
		this.stat = stat;
	}

	public ElixirStat stat() {
		return stat;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer sp) {
			if (!PermaHearts.enabled(sp.level().getServer())) {
				sp.sendSystemMessage(Messages.elixirDisabled());
				return stack;
			}
			drink(sp);
		}
		return super.finishUsingItem(stack, level, entity);
	}

	/** Raises the stat by one level and tells the player. Caller has checked the world switch. */
	void drink(ServerPlayer sp) {
		CultivationData next = HeartsAttachment.getCultivation(sp).plus(stat, 1);
		HeartsAttachment.setCultivation(sp, next);
		StatApplier.apply(sp);
		sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
		sp.sendSystemMessage(Messages.elixirDrunk(stat, next.level(stat)));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
		Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Messages.elixirTooltip(stat));
	}
}
