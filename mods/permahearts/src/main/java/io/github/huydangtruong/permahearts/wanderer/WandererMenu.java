package io.github.huydangtruong.permahearts.wanderer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * The shared quest screen. Per quest line i there are two slots at the same spot, only one of them active:
 * a deposit slot (COLLECT, accepted, not done) and a reward slot (done, not claimed).
 * Progress and claimed flags are data slots read live from the world on the server, so every open menu agrees.
 *
 * Slot indices: 0..3 deposit, 4..7 reward, 8..43 player inventory.
 */
public class WandererMenu extends AbstractContainerMenu {
	public static final int LINES = 4;
	/*
	 * Layout like the villager trade screen: a quest panel on the left, and on the right a vanilla-sized
	 * 176x166 inventory panel with the player inventory at the usual place (8, 84).
	 */
	public static final int QUEST_W = 170;
	public static final int INV_PANEL_X = QUEST_W + 2;
	public static final int INV_PANEL_W = 176;
	public static final int PANEL_W = INV_PANEL_X + INV_PANEL_W;
	public static final int PANEL_H = 166;
	/** Top of the first quest row; each row is LINE_STEP high. */
	public static final int LINE_TOP = 18;
	public static final int LINE_STEP = 36;
	/** Left edge of the item area of the per-line slot (the drawn square is one pixel larger on each side). */
	public static final int LINE_SLOT_X = QUEST_W - 6 - 17;
	public static final int LINE_SLOT_Y0 = LINE_TOP + (LINE_STEP - 18) / 2 + 1;
	public static final int INV_X = INV_PANEL_X + 8;
	public static final int INV_Y = 84;

	private static final int REWARD_BASE = LINES;
	private static final int INV_START = 2 * LINES;
	private static final int INV_END = INV_START + 36;

	private final QuestSnapshot snapshot;
	private final MinecraftServer server; // null on the client
	private final Entity npc;
	private final Player player;
	private final DataSlot accepted;
	private final DataSlot[] progress = new DataSlot[LINES];
	private final DataSlot[] claimed = new DataSlot[LINES];
	/** Per event line: 1 while the blood moon / boss of that line is running. */
	private final DataSlot[] eventState = new DataSlot[LINES];
	private final Container deposit;
	private final Container rewards;
	private boolean processing;

	/** Client constructor (the type's factory). */
	public WandererMenu(int id, Inventory inv, QuestSnapshot snapshot) {
		this(id, inv, snapshot, null, null);
	}

	public WandererMenu(int id, Inventory inv, QuestSnapshot snapshot, MinecraftServer server, Entity npc) {
		super(WandererMenus.TYPE, id);
		this.snapshot = snapshot;
		this.server = server;
		this.npc = npc;
		this.player = inv.player;
		this.accepted = addDataSlot(server == null ? DataSlot.standalone()
			: live(() -> quest(snapshot.questId(), q -> q.accepted() ? 1 : 0)));
		for (int i = 0; i < LINES; i++) {
			int line = i;
			progress[i] = addDataSlot(server == null ? DataSlot.standalone()
				: live(() -> line(snapshot.questId(), line, QuestLine::progress)));
			claimed[i] = addDataSlot(server == null ? DataSlot.standalone()
				: live(() -> line(snapshot.questId(), line, l -> l.claimed() ? 1 : 0)));
			eventState[i] = addDataSlot(server == null ? DataSlot.standalone()
				: live(() -> quest(snapshot.questId(), q -> Wanderer.eventState(server, line))));
		}
		this.deposit = new SimpleContainer(LINES) {
			@Override
			public void setChanged() {
				super.setChanged();
				slotsChanged(this);
			}
		};
		this.rewards = server == null ? new SimpleContainer(LINES) : Wanderer.rewards(server, snapshot.questId());
		for (int i = 0; i < LINES; i++) {
			addSlot(new DepositSlot(deposit, i, LINE_SLOT_X, LINE_SLOT_Y0 + i * LINE_STEP, i));
		}
		for (int i = 0; i < LINES; i++) {
			addSlot(new RewardSlot(rewards, i, LINE_SLOT_X, LINE_SLOT_Y0 + i * LINE_STEP, i));
		}
		addStandardInventorySlots(inv, INV_X, INV_Y);
	}

	// ---- live server values ----

	private static DataSlot live(IntSupplier supplier) {
		return new DataSlot() {
			@Override
			public int get() {
				return supplier.getAsInt();
			}

			@Override
			public void set(int value) {}
		};
	}

	private int quest(long id, java.util.function.ToIntFunction<Quest> f) {
		Optional<Quest> q = Wanderer.quest(server);
		return q.isPresent() && q.get().id() == id ? f.applyAsInt(q.get()) : 0;
	}

	private int line(long id, int line, java.util.function.ToIntFunction<QuestLine> f) {
		return quest(id, q -> line < q.lines().size() ? f.applyAsInt(q.lines().get(line)) : 0);
	}

	// ---- state for the screen (same on both sides) ----

	public QuestSnapshot snapshot() {
		return snapshot;
	}

	public boolean isAccepted() {
		return accepted.get() != 0;
	}

	public int progressOf(int line) {
		return progress[line].get();
	}

	public boolean hasLine(int line) {
		return line >= 0 && line < snapshot.lines().size();
	}

	public boolean isDone(int line) {
		return hasLine(line) && progress[line].get() >= snapshot.lines().get(line).required();
	}

	public boolean isClaimed(int line) {
		return claimed[line].get() != 0;
	}

	public boolean eventRunning(int line) {
		return eventState[line].get() != 0;
	}

	public boolean depositActive(int line) {
		return isAccepted() && hasLine(line) && snapshot.lines().get(line).type() == QuestType.COLLECT && !isDone(line);
	}

	public boolean rewardActive(int line) {
		return isAccepted() && isDone(line) && !isClaimed(line);
	}

	// ---- slots ----

	private final class DepositSlot extends Slot {
		private final int line;

		DepositSlot(Container c, int index, int x, int y, int line) {
			super(c, index, x, y);
			this.line = line;
		}

		@Override
		public boolean isActive() {
			return depositActive(line);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (!depositActive(line) || stack.isEmpty()) {
				return false;
			}
			Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
			return id.toString().equals(snapshot.lines().get(line).target());
		}
	}

	private final class RewardSlot extends Slot {
		private final int line;

		RewardSlot(Container c, int index, int x, int y, int line) {
			super(c, index, x, y);
			this.line = line;
		}

		@Override
		public boolean isActive() {
			return rewardActive(line);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return !(slot instanceof RewardSlot) && super.canTakeItemForPickAll(stack, slot);
	}

	// ---- server behaviour ----

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (server != null && container == deposit && !processing) {
			processing = true;
			try {
				for (int i = 0; i < LINES; i++) {
					ItemStack s = deposit.getItem(i);
					if (!s.isEmpty() && depositActive(i)) {
						Wanderer.deposit(server, i, s);
						if (s.isEmpty()) {
							deposit.setItem(i, ItemStack.EMPTY);
						}
					}
				}
			} finally {
				processing = false;
			}
		}
	}

	@Override
	public void broadcastChanges() {
		if (server != null) {
			// Another player may have finished a line while our deposit slot still holds items: give them back.
			for (int i = 0; i < LINES; i++) {
				ItemStack s = deposit.getItem(i);
				if (!s.isEmpty() && !depositActive(i)) {
					SimpleContainer back = new SimpleContainer(s.copy());
					deposit.setItem(i, ItemStack.EMPTY);
					clearContainer(player, back);
				}
			}
		}
		super.broadcastChanges();
	}

	@Override
	public boolean clickMenuButton(Player p, int id) {
		if (server != null && id == 0) {
			Wanderer.accept(server);
			return true;
		}
		// 10 + line: the Start / Summon button of an event line.
		if (server != null && id >= 10 && id < 10 + LINES && p instanceof ServerPlayer sp && isAccepted()) {
			Wanderer.startEvent(server, id - 10, sp);
			return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		Slot slot = slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack original = slot.getItem();
		ItemStack copy = original.copy();
		if (index >= REWARD_BASE && index < INV_START) {
			// Reward: move a copy, and only when it fits do we empty (= claim) the slot.
			ItemStack moving = original.copy();
			if (!moveItemStackTo(moving, INV_START, INV_END, true) || !moving.isEmpty()) {
				return ItemStack.EMPTY;
			}
			slot.set(ItemStack.EMPTY);
			return copy;
		}
		if (index < REWARD_BASE) {
			if (!moveItemStackTo(original, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			boolean moved = false;
			for (int i = 0; i < LINES && !moved; i++) {
				if (slots.get(i).mayPlace(original)) {
					moved = moveItemStackTo(original, i, i + 1, false);
				}
			}
			if (!moved) {
				return ItemStack.EMPTY;
			}
		}
		if (original.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (original.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(p, original);
		return copy;
	}

	@Override
	public void removed(Player p) {
		super.removed(p);
		if (server != null) {
			clearContainer(p, deposit);
		}
	}

	@Override
	public boolean stillValid(Player p) {
		if (server == null) {
			return true;
		}
		WandererData d = Wanderer.data(server);
		return npc != null && !npc.isRemoved() && p.distanceToSqr(npc) <= 64.0
			&& d.phase() == Phase.PRESENT && d.quest().isPresent() && d.quest().get().id() == snapshot.questId();
	}
}
