package io.github.huydangtruong.immortalcultivation.reward;

/** Which cards are selected, as a bit mask (bit i = card i). The server checks it again on Claim. No Minecraft imports. */
public final class RewardSelection {
	private RewardSelection() {}

	/**
	 * A click on a card. picks == 1: the click moves the selection to that card (clicking the selected card keeps it).
	 * picks > 1: toggles the card; a new card is ignored while picks cards are already selected. A card out of range is ignored.
	 */
	public static int toggle(int mask, int index, int picks, int optionCount) {
		if (index < 0 || index >= optionCount) {
			return mask;
		}
		int bit = 1 << index;
		if (picks == 1) {
			return bit;
		}
		if ((mask & bit) != 0) {
			return mask & ~bit;
		}
		return Integer.bitCount(mask) < picks ? mask | bit : mask;
	}

	/** Exactly picks cards selected (what enables the Claim button). */
	public static boolean complete(int mask, int picks) {
		return Integer.bitCount(mask) == picks;
	}

	/** Exactly picks cards selected, all inside 0..optionCount-1. */
	public static boolean valid(int mask, int picks, int optionCount) {
		return mask > 0 && complete(mask, picks) && (mask >>> optionCount) == 0;
	}

	public static boolean isSelected(int mask, int index) {
		return index >= 0 && index < 31 && (mask & (1 << index)) != 0;
	}
}
