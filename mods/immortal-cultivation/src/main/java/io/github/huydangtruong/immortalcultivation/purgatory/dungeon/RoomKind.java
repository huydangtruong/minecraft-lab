package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/** What happens in a room (mob numbers: {@link DungeonRules}). */
public enum RoomKind {
	START(false), CORRIDOR(false), OPEN(false), HAZARD(false), TRAP(false), DARK(false), REST(false), STAIRS(false),
	/** Sealed until every mob in it is dead. */
	LOCKED(true),
	/** D265: sealed until the Seal Pillar somewhere in it is destroyed; mobs keep coming while it stands. */
	PILLAR(true),
	/** Sealed fight, then a chest. */
	TREASURE(true),
	/** Sealed, the floor boss: killing it wins the floor. */
	BOSS(true);

	public final boolean locks;

	RoomKind(boolean locks) {
		this.locks = locks;
	}
}
