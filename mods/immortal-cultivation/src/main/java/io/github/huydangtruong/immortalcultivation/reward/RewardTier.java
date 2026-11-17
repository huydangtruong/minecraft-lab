package io.github.huydangtruong.immortalcultivation.reward;

import io.github.huydangtruong.immortalcultivation.wanderer.QuestType;
import java.util.random.RandomGenerator;

/** Reward box tiers (D76, D89). options = cards on the screen, picks = how many the player takes (1, the emerald box 2). */
public enum RewardTier {
	SILVER("silver", 3, 1),
	GOLD("gold", 4, 1),
	DIAMOND("diamond", 5, 1),
	EMERALD("emerald", 6, 2);

	private final String id;
	private final int options;
	private final int picks;

	RewardTier(String id, int options, int picks) {
		this.id = id;
		this.options = options;
		this.picks = picks;
	}

	public String id() {
		return id;
	}

	public int options() {
		return options;
	}

	public int picks() {
		return picks;
	}

	/** Item name of the box of this tier. */
	public String itemName() {
		return id + "_reward_box";
	}

	/** Odds in percent of SILVER, GOLD, DIAMOND, EMERALD for most lines (D90). */
	private static final int[] COMMON_ODDS = {40, 30, 20, 10};
	/** Odds in percent for a boss or Blood Moon line (D90). */
	private static final int[] HARD_ODDS = {20, 30, 30, 20};

	/** Percent chance (0..100) that a line of this type pays a box of the tier (D90). */
	public static int oddsPercent(QuestType type, RewardTier tier) {
		return (type == QuestType.BOSS || type == QuestType.BLOOD_MOON ? HARD_ODDS : COMMON_ODDS)[tier.ordinal()];
	}

	/** Rolls the box a quest line pays, when the line is claimed (D90). The player does not see it beforehand. */
	public static RewardTier roll(QuestType type, RandomGenerator rng) {
		int r = rng.nextInt(100);
		for (RewardTier t : values()) {
			r -= oddsPercent(type, t);
			if (r < 0) {
				return t;
			}
		}
		return SILVER;
	}
}
