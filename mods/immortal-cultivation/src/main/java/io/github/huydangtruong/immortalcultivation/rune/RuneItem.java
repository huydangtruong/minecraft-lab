package io.github.huydangtruong.immortalcultivation.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A rune (D241): its name says the enchantment and the level. */
public class RuneItem extends Item {
	public RuneItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		RuneEntry r = stack.get(Runes.RUNE);
		return r == null ? super.getName(stack) : Runes.name(r);
	}
}
