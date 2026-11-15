package io.github.huydangtruong.permahearts.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Texts of the reward boxes (D76); English only like the rest of the mod (D16). */
public final class RewardMessages {
	private RewardMessages() {}

	public static Component boxTooltip(int cards) {
		return Component.translatableWithFallback("permahearts.reward.tooltip", "Right-click to open: choose 1 of %s rewards", cards)
			.withStyle(ChatFormatting.GRAY);
	}

	public static Component claim() {
		return Component.translatableWithFallback("permahearts.reward.claim", "Claim");
	}

	public static Component close() {
		return Component.translatableWithFallback("permahearts.reward.close", "Close");
	}

	public static Component hint(int picks) {
		return Component.translatableWithFallback("permahearts.reward.hint", "Choose %s reward(s)", picks);
	}

	public static Component claimed() {
		return Component.translatableWithFallback("permahearts.reward.claimed", "You claimed your reward");
	}
}
