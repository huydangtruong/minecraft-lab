package io.github.huydangtruong.permahearts.reward;

import io.github.huydangtruong.permahearts.wanderer.QuestType;

/** Reward box tiers (D76). options = cards on the screen, picks = how many the player takes (1 for every tier for now). */
public enum RewardTier {
	SILVER("silver", 3, 1),
	GOLD("gold", 4, 1),
	DIAMOND("diamond", 5, 1);

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

	/** The box a quest line pays (D76). */
	public static RewardTier forLine(QuestType type) {
		return switch (type) {
			case COLLECT, KILL -> SILVER;
			case EXPLORE, BLOOD_MOON -> GOLD;
			case BOSS, DEATH_MATCH -> DIAMOND;
		};
	}
}
