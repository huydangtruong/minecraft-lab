package io.github.huydangtruong.immortalcultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Heart Elixir (BL-017): drinking it gives +1 max heart; at the cap it is not consumed. */
public class HeartElixirItem extends Item {
	public HeartElixirItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer sp) {
			if (!ImmortalCultivation.enabled(sp.level().getServer())) {
				sp.sendSystemMessage(Messages.elixirDisabled());
				return stack;
			}
			if (HeartRules.maxHearts(HeartsAttachment.get(sp)) >= HeartRules.MAX) {
				sp.sendSystemMessage(Component.translatableWithFallback("permahearts.msg.heart_elixir_cap",
					"You already have the maximum hearts"));
				return stack;
			}
			drink(sp);
		}
		return super.finishUsingItem(stack, level, entity);
	}

	void drink(ServerPlayer sp) {
		HeartsData next = HeartRules.addHeart(HeartsAttachment.get(sp));
		HeartsAttachment.set(sp, next);
		HealthApplier.apply(sp, false);
		sp.setHealth(Math.min(sp.getMaxHealth(), sp.getHealth() + 2f));
		sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
		sp.sendSystemMessage(Messages.gained(1, HeartRules.maxHearts(next)));
	}
}
