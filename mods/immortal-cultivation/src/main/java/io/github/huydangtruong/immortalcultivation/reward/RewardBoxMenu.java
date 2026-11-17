package io.github.huydangtruong.immortalcultivation.reward;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The choose screen of a reward box. No item slots: the cards are drawn by the screen and the selection is a bit mask
 * in a data slot. Menu buttons: 0..7 = click card i, CLAIM_BUTTON = Claim. The server keeps the box in the hand
 * it was opened from and re-checks everything on Claim.
 */
public class RewardBoxMenu extends AbstractContainerMenu {
	public static final int CLAIM_BUTTON = 20;

	private final RewardTier tier;
	private final List<RewardOption> options;
	private final InteractionHand hand;
	private final Player player;
	private final DataSlot selected = DataSlot.standalone();

	/** Client constructor (the type's factory). */
	public RewardBoxMenu(int id, Inventory inv, RewardBoxes.OpenData data) {
		super(RewardBoxes.MENU, id);
		this.tier = data.tier();
		this.options = data.options();
		this.hand = data.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		this.player = inv.player;
		addDataSlot(selected);
	}

	public RewardTier tier() {
		return tier;
	}

	public List<RewardOption> options() {
		return options;
	}

	/** Bit mask of the selected cards. */
	public int selected() {
		return selected.get();
	}

	public boolean canClaim() {
		return RewardSelection.valid(selected.get(), tier.picks(), options.size());
	}

	@Override
	public boolean clickMenuButton(Player p, int id) {
		if (!(p instanceof ServerPlayer sp)) {
			return false;
		}
		if (id >= 0 && id < options.size()) {
			selected.set(RewardSelection.toggle(selected.get(), id, tier.picks(), options.size()));
			return true;
		}
		if (id == CLAIM_BUTTON && canClaim() && RewardBoxes.claim(sp, hand, selected.get())) {
			sp.closeContainer();
			return true;
		}
		return false; // out of range or not complete: nothing happens, the box is kept
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		return ItemStack.EMPTY;
	}

	/** Open only while the same rolled box is still in the hand it came from. */
	@Override
	public boolean stillValid(Player p) {
		ItemStack stack = p.getItemInHand(hand);
		var data = stack.get(RewardBoxes.DATA);
		return RewardBoxes.tierOf(stack).filter(t -> t == tier).isPresent() && data != null && data.options().equals(options);
	}
}
