package io.github.huydangtruong.immortalcultivation.technique.grasp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Gear of a grasped mob (D135): it picks up armor, weapons and shields thrown by its owner, wears them, swaps for a better
 * item (the old one drops) and keeps a list of the slots that hold given items so release drops only those.
 */
final class GraspGear {
	private static final int DEFAULT_DROP_SLOTS = 0;

	private GraspGear() {}

	/** Sum of the attribute add-values an item gives in a slot. */
	private static double sum(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attr) {
		if (stack.isEmpty()) {
			return 0;
		}
		double[] total = {0};
		stack.forEachModifier(slot, (a, m) -> {
			if (a.equals(attr) && m.operation() == AttributeModifier.Operation.ADD_VALUE) {
				total[0] += m.amount();
			}
		});
		return total[0];
	}

	/** Attack damage bonus a held item adds (main hand). */
	static double weaponBonus(ItemStack stack) {
		return sum(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
	}

	/** Worth of an item in a slot: armor and toughness for armor, attack damage for the main hand, 1 for a shield. */
	private static double score(ItemStack stack, EquipmentSlot slot) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (slot == EquipmentSlot.MAINHAND) {
			return sum(stack, slot, Attributes.ATTACK_DAMAGE);
		}
		if (slot == EquipmentSlot.OFFHAND) {
			return stack.is(Items.SHIELD) ? 1 : 0;
		}
		return sum(stack, slot, Attributes.ARMOR) + 0.5 * sum(stack, slot, Attributes.ARMOR_TOUGHNESS) + 0.001;
	}

	/** The slot a thrown item could go in, or null when the mob takes no such item. */
	static EquipmentSlot slotFor(Mob mob, ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (stack.is(Items.SHIELD)) {
			return EquipmentSlot.OFFHAND;
		}
		EquipmentSlot slot = mob.getEquipmentSlotForItem(stack);
		if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
			return score(stack, slot) > 0 ? slot : null;
		}
		return weaponBonus(stack) > 0 ? EquipmentSlot.MAINHAND : null;
	}

	/** Tries to take an item entity the owner threw. Returns true when the mob took it (the entity is shrunk or removed). */
	static boolean pickUp(ServerLevel sl, Mob mob, GraspedMob ga, ItemEntity ie) {
		ItemStack stack = ie.getItem();
		EquipmentSlot slot = slotFor(mob, stack);
		if (slot == null) {
			return false;
		}
		ItemStack current = mob.getItemBySlot(slot);
		if (score(stack, slot) <= score(current, slot)) {
			return false;
		}
		ItemStack take = stack.split(1);
		if (!current.isEmpty()) {
			mob.spawnAtLocation(sl, current.copy()); // the old one drops (no thrower, so it is not picked up again)
		}
		mob.setItemSlot(slot, take);
		mob.setDropChance(slot, 1f);
		List<String> given = new ArrayList<>(ga.given());
		if (!given.contains(slot.getName())) {
			given.add(slot.getName());
		}
		mob.setAttached(GraspedMob.ATTACHMENT, ga.withGiven(given));
		if (stack.isEmpty()) {
			ie.discard();
		}
		sl.playSound(null, mob.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.NEUTRAL, 1f, 1f);
		return true;
	}

	/** Release: drops every item the owner gave, keeps the mob's own gear. */
	static void dropGiven(ServerLevel sl, Mob mob, GraspedMob ga) {
		for (String name : ga.given()) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (slot.getName().equals(name)) {
					ItemStack s = mob.getItemBySlot(slot);
					if (!s.isEmpty()) {
						mob.spawnAtLocation(sl, s.copy());
						mob.setItemSlot(slot, ItemStack.EMPTY);
					}
					mob.setDropChance(slot, 0.085f);
				}
			}
		}
	}

	/** Names of the worn armor pieces, one per line, for the list tooltip. */
	static String armorLines(Mob mob) {
		StringBuilder b = new StringBuilder();
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack s = mob.getItemBySlot(slot);
			if (!s.isEmpty()) {
				b.append(b.isEmpty() ? "" : "\n").append(s.getHoverName().getString());
			}
		}
		return b.toString();
	}

	/** Names of the held items (main hand, off hand), one per line. */
	static String heldLines(Mob mob) {
		StringBuilder b = new StringBuilder();
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
			ItemStack s = mob.getItemBySlot(slot);
			if (!s.isEmpty()) {
				b.append(b.isEmpty() ? "" : "\n").append(s.getHoverName().getString());
			}
		}
		return b.toString();
	}
}
