package io.github.huydangtruong.immortalcultivation.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Texts of the reward boxes (D76); English only like the rest of the mod (D16). */
public final class RewardMessages {
	private RewardMessages() {}

	public static Component boxTooltip(int cards, int picks) {
		if (picks == 1) {
			return Component.translatableWithFallback("permahearts.reward.tooltip", "Right-click to open: choose 1 of %s rewards", cards)
				.withStyle(ChatFormatting.GRAY);
		}
		return Component.translatableWithFallback("permahearts.reward.tooltip_multi", "Right-click to open: choose %s of %s rewards", picks, cards)
			.withStyle(ChatFormatting.GRAY);
	}

	public static Component bottleTooltip(int levels) {
		return Component.translatableWithFallback("permahearts.reward.bottle", "Drink: +%s levels", levels).withStyle(ChatFormatting.GRAY);
	}

	public static Component mysteryTooltip() {
		return Component.translatableWithFallback("permahearts.reward.mystery", "Random reward box").withStyle(ChatFormatting.GRAY);
	}

	public static Component claim() {
		return Component.translatableWithFallback("permahearts.reward.claim", "Claim");
	}

	public static Component close() {
		return Component.translatableWithFallback("permahearts.reward.close", "Close");
	}

	/** The line under the title: picks to make and how many are selected now. */
	public static Component hint(int picks, int selected) {
		if (picks == 1) {
			return Component.translatableWithFallback("permahearts.reward.hint", "Choose %s reward(s)", picks);
		}
		if (selected == 0) {
			return Component.translatableWithFallback("permahearts.reward.hint_multi", "Pick %s rewards", picks);
		}
		if (selected < picks) {
			return Component.translatableWithFallback("permahearts.reward.hint_left", "%s left", picks - selected);
		}
		return Component.translatableWithFallback("permahearts.reward.hint_ready", "Press Claim to take them");
	}

	public static Component claimed() {
		return Component.translatableWithFallback("permahearts.reward.claimed", "You claimed your reward");
	}
}
