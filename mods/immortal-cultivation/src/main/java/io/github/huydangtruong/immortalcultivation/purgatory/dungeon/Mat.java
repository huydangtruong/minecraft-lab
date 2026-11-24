package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/**
 * What a dungeon voxel is, before a {@link Style} turns it into a real block. {@link Move} is how the stuck checker
 * ({@link Walker}) treats it. {@link #SHELL} is first so a fresh {@link Grid} is solid rock.
 */
public enum Mat {
	SHELL(Move.SOLID), AIR(Move.PASS), FLOOR(Move.SOLID), FLOOR2(Move.SOLID), WALL(Move.SOLID), WALL2(Move.SOLID),
	PILLAR(Move.SOLID), CEILING(Move.SOLID), LIGHT(Move.SOLID), LANTERN(Move.SOLID), STAIR(Move.SOLID), FENCE(Move.FENCE),
	BARS(Move.FENCE),
	/** A doorway of a sealing room: air while open, bars while the room is sealed (the checker sees it open). */
	GATE(Move.PASS), WATER(Move.WATER), LAVA(Move.LAVA), CARPET(Move.PASS), COBWEB(Move.PASS), LADDER(Move.LADDER),
	RAIL(Move.PASS), CHEST(Move.SOLID), DISPENSER(Move.SOLID), PLATE(Move.PASS), CAMPFIRE(Move.SOLID), SHELF(Move.SOLID),
	LEAVES(Move.SOLID), SCULK(Move.SOLID), MAGMA(Move.SOLID), DECOR(Move.SOLID), ACCENT(Move.SOLID);

	public enum Move {
		/** Walk through it. */
		PASS,
		/** Stand on it, it blocks the way. */
		SOLID,
		/** Blocks the way and is too tall to jump on (fences, bars). */
		FENCE,
		/** Swim in it. */
		WATER,
		/** Never enter (it hurts; it is not "stuck"). */
		LAVA,
		/** Climb it. */
		LADDER
	}

	static final Mat[] VALUES = values();

	public final Move move;

	Mat(Move move) {
		this.move = move;
	}

	/** Feet and head may be here. */
	public boolean passable() {
		return move == Move.PASS || move == Move.WATER || move == Move.LADDER;
	}

	/** Something to stand on. */
	public boolean supports() {
		return move == Move.SOLID || move == Move.FENCE;
	}
}
