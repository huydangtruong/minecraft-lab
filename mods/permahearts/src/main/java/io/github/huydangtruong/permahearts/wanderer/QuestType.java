package io.github.huydangtruong.permahearts.wanderer;

/** Kinds of quest line. Saved data uses the names, so only ever append. */
public enum QuestType {
	COLLECT,
	KILL,
	BLOOD_MOON,
	EXPLORE,
	BOSS;

	/** Event lines have no item slot; they are started or completed by something happening in the world. */
	public boolean isEvent() {
		return this != COLLECT && this != KILL;
	}
}
