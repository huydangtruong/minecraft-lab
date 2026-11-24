package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/** The look of a room: which real blocks its {@link Mat}s become (Minecraft side: DungeonPalette). */
public enum Style {
	/** Stone brick keep. */
	BASE,
	/** Abandoned mineshaft: planks, logs, rails, cobwebs. */
	MINE,
	/** Trial chamber: tuff and copper. */
	TRIAL,
	/** End city: purpur and end stone. */
	END,
	/** Ancient city: deepslate and sculk. */
	DEEP,
	/** Nether fortress: nether bricks, blackstone, lava. */
	NETHER,
	/** Ocean monument: prismarine and sea lanterns. */
	OCEAN,
	/** Library: dark oak and bookshelves. */
	LIBRARY,
	/** Overgrown garden: moss and azalea. */
	GARDEN;

	static final Style[] VALUES = values();
}
