package io.github.huydangtruong.immortalcultivation.trash;

/** D217: when a click on the trash slot destroys the carried stack. Pure rule, no Minecraft imports. */
public final class TrashRules {
	private TrashRules() {}

	/** Destroy only a non-empty carried stack, and never for a spectator. */
	public static boolean shouldDestroy(boolean carriedEmpty, boolean spectator) {
		return !carriedEmpty && !spectator;
	}
}
