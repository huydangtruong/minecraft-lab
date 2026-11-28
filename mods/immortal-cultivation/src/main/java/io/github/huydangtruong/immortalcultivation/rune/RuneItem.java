package io.github.huydangtruong.immortalcultivation.rune;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** A rune (D241): its name says the enchantment and the level. */
public class RuneItem extends Item {
	public RuneItem(Properties properties) {
		super(properties);
	}

	/** D278: runes made before the glyph and number textures get their new look once they sit in an inventory. */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		RuneEntry r = stack.get(Runes.RUNE);
		if (r != null) {
			CustomModelData look = Runes.look(r);
			if (!look.equals(stack.get(DataComponents.CUSTOM_MODEL_DATA))) {
				stack.set(DataComponents.CUSTOM_MODEL_DATA, look);
			}
		}
	}

	@Override
	public Component getName(ItemStack stack) {
		RuneEntry r = stack.get(Runes.RUNE);
		return r == null ? super.getName(stack) : Runes.name(r);
	}
}
