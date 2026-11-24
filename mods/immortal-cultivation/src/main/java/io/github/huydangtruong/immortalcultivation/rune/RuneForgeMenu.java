package io.github.huydangtruong.immortalcultivation.rune;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Rune Forge menu (D242). Tab Inscribe: a piece of gear and its rune slots (putting a rune in costs levels, taking it out is
 * free and loses nothing). Tab Merge: two runes of the same kind and level make one rune a level higher.
 *
 * Slots: 0 gear, 1..4 rune slots of the gear, 5..6 merge inputs, 7 merge result, 8..43 player inventory.
 */
public class RuneForgeMenu extends AbstractContainerMenu {
	public static final int TAB_INSCRIBE = 0;
	public static final int TAB_MERGE = 1;
	public static final int MAX_SLOTS = 4;

	public static final int GEAR = 0;
	public static final int RUNE0 = 1;
	public static final int MERGE_A = RUNE0 + MAX_SLOTS;
	public static final int MERGE_B = MERGE_A + 1;
	public static final int RESULT = MERGE_B + 1;
	private static final int INV_START = RESULT + 1;
	private static final int INV_END = INV_START + 36;

	/* Layout (vanilla 176 x 166 panel). */
	public static final int W = 176;
	public static final int H = 166;
	public static final int GEAR_X = 26;
	public static final int ROW_Y = 40;
	public static final int RUNES_X = 70;
	public static final int MERGE_A_X = 36;
	public static final int MERGE_B_X = 72;
	public static final int RESULT_X = 126;

	public static final ExtendedMenuType<RuneForgeMenu, BlockPos> TYPE = Registry.register(BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "rune_forge"),
		new ExtendedMenuType<>((id, inv, pos) -> new RuneForgeMenu(id, inv, ContainerLevelAccess.NULL), BlockPos.STREAM_CODEC));

	private final ContainerLevelAccess access;
	private final Player player;
	private final SimpleContainer gear = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			slotsChanged(this);
		}
	};
	private final RuneSlots runes = new RuneSlots();
	private final SimpleContainer merge = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			slotsChanged(this);
		}
	};
	private final SimpleContainer result = new SimpleContainer(1);
	private final DataSlot tab = DataSlot.standalone();

	public RuneForgeMenu(int id, Inventory inv, ContainerLevelAccess access) {
		super(TYPE, id);
		this.access = access;
		this.player = inv.player;
		addSlot(new Slot(gear, 0, GEAR_X, ROW_Y) {
			@Override
			public boolean mayPlace(ItemStack s) {
				return Runes.slots(s) > 0;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}

			@Override
			public boolean isActive() {
				return tab() == TAB_INSCRIBE;
			}
		});
		for (int i = 0; i < MAX_SLOTS; i++) {
			addSlot(new RuneSlot(i, RUNES_X + i * 22, ROW_Y));
		}
		addSlot(new MergeSlot(0, MERGE_A_X));
		addSlot(new MergeSlot(1, MERGE_B_X));
		addSlot(new Slot(result, 0, RESULT_X, ROW_Y) {
			@Override
			public boolean mayPlace(ItemStack s) {
				return false;
			}

			@Override
			public boolean isActive() {
				return tab() == TAB_MERGE;
			}

			@Override
			public void onTake(Player p, ItemStack taken) {
				merge.removeItem(0, 1);
				merge.removeItem(1, 1);
				access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1.2f));
				super.onTake(p, taken);
			}
		});
		addStandardInventorySlots(inv, 8, 84);
		addDataSlot(tab);
	}

	public static void init() {
		TYPE.getStreamCodec(); // touch the class so the menu type is registered
	}

	public int tab() {
		return tab.get();
	}

	public ItemStack gearStack() {
		return gear.getItem(0);
	}

	/** Rune slots the gear in the forge has (0 when empty). */
	public int gearSlots() {
		return Math.min(MAX_SLOTS, Runes.slots(gearStack()));
	}

	@Override
	public boolean clickMenuButton(Player p, int id) {
		if (id == TAB_INSCRIBE || id == TAB_MERGE) {
			tab.set(id);
			return true;
		}
		return false;
	}

	@Override
	public void slotsChanged(Container c) {
		super.slotsChanged(c);
		if (c == merge) {
			RuneEntry a = Runes.entryOf(merge.getItem(0));
			RuneEntry b = Runes.entryOf(merge.getItem(1));
			result.setItem(0, a != null && a.sameKind(b) ? Runes.rune(a.enchantment(), a.level() + 1) : ItemStack.EMPTY);
		}
	}

	@Override
	public boolean stillValid(Player p) {
		return stillValid(access, p, RuneForge.BLOCK);
	}

	@Override
	public void removed(Player p) {
		super.removed(p);
		access.execute((level, pos) -> {
			clearContainer(p, gear);
			clearContainer(p, merge);
		});
	}

	/** Levels a rune costs to put in, or 0 for a creative player. */
	public int costFor(RuneEntry r) {
		return player.hasInfiniteMaterials() ? 0 : RuneRules.inscribeCost(r.level());
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index >= RUNE0 && index < RUNE0 + MAX_SLOTS) {
			ItemStack out = stack.copy();
			if (!moveItemStackTo(out, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
			runes.removeItemNoUpdate(index - RUNE0);
			return copy;
		}
		if (index == RESULT) {
			if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onTake(p, copy);
			return copy;
		}
		if (index < INV_START) {
			if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			int target = -1;
			if (tab() == TAB_INSCRIBE) {
				if (!slots.get(GEAR).hasItem() && slots.get(GEAR).mayPlace(stack)) {
					target = GEAR;
				}
				for (int i = RUNE0; i < RUNE0 + MAX_SLOTS && target < 0; i++) {
					Slot s = slots.get(i);
					if (s.isActive() && !s.hasItem() && s.mayPlace(stack)) {
						target = i;
					}
				}
			} else {
				for (int i = MERGE_A; i <= MERGE_B && target < 0; i++) {
					if (!slots.get(i).hasItem() && slots.get(i).mayPlace(stack)) {
						target = i;
					}
				}
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

	/** A rune slot of the gear in the forge. */
	private final class RuneSlot extends Slot {
		RuneSlot(int i, int x, int y) {
			super(runes, i, x, y);
		}

		@Override
		public boolean isActive() {
			return tab() == TAB_INSCRIBE && getContainerSlot() < gearSlots();
		}

		@Override
		public boolean mayPlace(ItemStack s) {
			RuneEntry r = Runes.entryOf(s);
			return isActive() && r != null && Runes.fits(gearStack(), r) && player.experienceLevel >= costFor(r);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public void setByPlayer(ItemStack now, ItemStack was) {
			RuneEntry r = Runes.entryOf(now);
			if (r != null && !player.level().isClientSide()) {
				player.giveExperienceLevels(-costFor(r)); // D242: putting a rune in costs levels
				access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 1f));
			}
			super.setByPlayer(now, was);
		}
	}

	private final class MergeSlot extends Slot {
		MergeSlot(int i, int x) {
			super(merge, i, x, ROW_Y);
		}

		@Override
		public boolean isActive() {
			return tab() == TAB_MERGE;
		}

		@Override
		public boolean mayPlace(ItemStack s) {
			return Runes.entryOf(s) != null;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	/** The rune slots as a container over the gear's inscribed list; slot i past the list end is empty. */
	private final class RuneSlots implements Container {
		private List<RuneEntry> list() {
			return new ArrayList<>(Runes.inscribed(gearStack()));
		}

		@Override
		public int getContainerSize() {
			return MAX_SLOTS;
		}

		@Override
		public boolean isEmpty() {
			return list().isEmpty();
		}

		@Override
		public ItemStack getItem(int i) {
			List<RuneEntry> l = list();
			return i < l.size() ? Runes.rune(l.get(i)) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int i, int count) {
			return count <= 0 ? ItemStack.EMPTY : removeItemNoUpdate(i);
		}

		@Override
		public ItemStack removeItemNoUpdate(int i) {
			List<RuneEntry> l = list();
			if (i >= l.size()) {
				return ItemStack.EMPTY;
			}
			RuneEntry r = l.remove(i);
			write(l);
			return Runes.rune(r);
		}

		@Override
		public void setItem(int i, ItemStack s) {
			List<RuneEntry> l = list();
			RuneEntry r = Runes.entryOf(s);
			if (r == null) {
				if (i < l.size()) {
					l.remove(i);
				}
			} else if (i < l.size()) {
				l.set(i, r);
			} else {
				l.add(r);
			}
			write(l);
		}

		private void write(List<RuneEntry> l) {
			ItemStack g = gearStack();
			if (g.isEmpty()) {
				return;
			}
			ItemStack copy = g.copy();
			Runes.setInscribed(copy, l);
			gear.setItem(0, copy);
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
