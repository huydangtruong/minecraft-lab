package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * D244: right-click your own grasped mob to open its gear: helmet, chestplate, leggings, boots, main hand and off hand on a
 * panel to the left of the player inventory (like the villager screen). The slots are the mob's real equipment on the
 * server, so nothing can be copied. Items put in count as given (they drop on release and always drop on death).
 *
 * Slot indices: 0..5 mob gear (GEAR order), 6..41 player inventory.
 */
public class GraspEquipMenu extends AbstractContainerMenu {
	public static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static final Identifier[] EMPTY_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
		InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, null, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD};

	/* Layout: a gear panel on the left, a vanilla 176x166 inventory panel on the right. */
	public static final int GEAR_W = 120;
	public static final int INV_PANEL_X = GEAR_W + 2;
	public static final int INV_PANEL_W = 176;
	public static final int PANEL_W = INV_PANEL_X + INV_PANEL_W;
	public static final int PANEL_H = 166;
	/** Armor column on the left, hands column on the right, the mob between them. */
	public static final int ARMOR_X = 8;
	public static final int HANDS_X = GEAR_W - 8 - 16;
	public static final int SLOT_Y0 = 18;
	public static final int PREVIEW_X = ARMOR_X + 18 + 2;
	public static final int PREVIEW_Y = SLOT_Y0 - 1;
	public static final int PREVIEW_W = HANDS_X - 2 - 1 - PREVIEW_X;
	public static final int PREVIEW_H = 4 * 18;
	public static final int INV_X = INV_PANEL_X + 8;
	public static final int INV_Y = 84;
	private static final int INV_START = GEAR.length;
	private static final int INV_END = INV_START + 36;
	/** Close the menu when the player is farther than this from the mob. */
	private static final double REACH_SQR = 8.0 * 8.0;

	public static final ExtendedMenuType<GraspEquipMenu, Integer> TYPE = Registry.register(BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_gear"),
		new ExtendedMenuType<>(GraspEquipMenu::new, ByteBufCodecs.VAR_INT));

	private final int entityId;
	private final Mob mob; // null on the client
	private final Player player;

	/** Client constructor (the type's factory). */
	public GraspEquipMenu(int id, Inventory inv, Integer entityId) {
		this(id, inv, entityId, null);
	}

	private GraspEquipMenu(int id, Inventory inv, int entityId, Mob mob) {
		super(TYPE, id);
		this.entityId = entityId;
		this.mob = mob;
		this.player = inv.player;
		Container gear = mob == null ? new SimpleContainer(GEAR.length) : new MobGear(mob);
		for (int i = 0; i < GEAR.length; i++) {
			boolean armor = i < 4;
			int x = armor ? ARMOR_X : HANDS_X;
			int y = SLOT_Y0 + (armor ? i : i - 4 + 1) * 18;
			addSlot(new GearSlot(gear, i, x, y));
		}
		addStandardInventorySlots(inv, INV_X, INV_Y);
	}

	public static void init() {
		TYPE.getStreamCodec(); // touch the class so the menu type is registered
	}

	/** Server: opens the gear of the player's own grasped mob. */
	public static void open(ServerPlayer p, Mob mob) {
		p.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(ServerPlayer sp) {
				return mob.getId();
			}

			@Override
			public Component getDisplayName() {
				return mob.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inv, Player pl) {
				return new GraspEquipMenu(id, inv, mob.getId(), mob);
			}
		});
	}

	public int entityId() {
		return entityId;
	}

	/** Which gear slots an item may go in (also used by the tests). */
	public static boolean fits(Mob mob, EquipmentSlot slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}
		if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
			return true;
		}
		return mob.getEquipmentSlotForItem(stack) == slot;
	}

	@Override
	public boolean stillValid(Player p) {
		if (mob == null) {
			return true;
		}
		return mob.isAlive() && !mob.isRemoved() && mob.level() == p.level() && Grasp.isGraspedBy(mob, p.getUUID())
			&& mob.distanceToSqr(p) <= REACH_SQR;
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < INV_START) {
			if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			int target = -1;
			for (int i = 0; i < INV_START && target < 0; i++) {
				Slot s = slots.get(i);
				boolean preferred = i < 4 || i == 4; // armor goes to its own slot, anything else to the main hand first
				if (preferred && !s.hasItem() && s.mayPlace(stack)) {
					target = i;
				}
			}
			if (target < 0 && !slots.get(5).hasItem() && slots.get(5).mayPlace(stack)) {
				target = 5;
			}
			if (target < 0) {
				return ItemStack.EMPTY;
			}
			slots.get(target).setByPlayer(stack.split(1));
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	/** One gear slot: one item, only what fits that slot. */
	private final class GearSlot extends Slot {
		GearSlot(Container c, int index, int x, int y) {
			super(c, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			EquipmentSlot es = GEAR[getContainerSlot()];
			if (mob != null) {
				return fits(mob, es, stack);
			}
			if (es == EquipmentSlot.MAINHAND || es == EquipmentSlot.OFFHAND) {
				return true;
			}
			var eq = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
			return eq != null && eq.slot() == es;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return 1;
		}

		@Override
		public Identifier getNoItemIcon() {
			return EMPTY_ICONS[getContainerSlot()];
		}
	}

	/** The mob's equipment as a container; keeps the "given" list and drop chances in step. */
	private static final class MobGear implements Container {
		private final Mob mob;

		MobGear(Mob mob) {
			this.mob = mob;
		}

		@Override
		public int getContainerSize() {
			return GEAR.length;
		}

		@Override
		public boolean isEmpty() {
			for (EquipmentSlot s : GEAR) {
				if (!mob.getItemBySlot(s).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ItemStack getItem(int i) {
			return mob.getItemBySlot(GEAR[i]);
		}

		@Override
		public ItemStack removeItem(int i, int count) {
			ItemStack cur = mob.getItemBySlot(GEAR[i]);
			if (cur.isEmpty() || count <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack out = cur.split(count);
			setItem(i, cur.isEmpty() ? ItemStack.EMPTY : cur);
			return out;
		}

		@Override
		public ItemStack removeItemNoUpdate(int i) {
			ItemStack cur = mob.getItemBySlot(GEAR[i]);
			setItem(i, ItemStack.EMPTY);
			return cur;
		}

		@Override
		public void setItem(int i, ItemStack stack) {
			EquipmentSlot slot = GEAR[i];
			mob.setItemSlot(slot, stack);
			GraspedMob ga = mob.getAttached(GraspedMob.ATTACHMENT);
			if (ga == null) {
				return;
			}
			List<String> given = new ArrayList<>(ga.given());
			if (stack.isEmpty()) {
				given.remove(slot.getName());
				mob.setDropChance(slot, 0.085f);
			} else {
				if (!given.contains(slot.getName())) {
					given.add(slot.getName());
				}
				mob.setDropChance(slot, 1f); // like a thrown item (D135): the owner's gear is never lost with the mob
			}
			mob.setAttached(GraspedMob.ATTACHMENT, ga.withGiven(given));
		}

		@Override
		public void setChanged() {}

		@Override
		public boolean stillValid(Player p) {
			return true;
		}

		@Override
		public void clearContent() {}
	}
}
