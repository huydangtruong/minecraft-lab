package io.github.huydangtruong.permahearts.wanderer;

/** Pure Death Match rules (D25). */
public final class DeathMatchRules {
	/** The quest is only offered, and can only start, with at least this many players online. */
	public static final int MIN_ONLINE = 2;
	/** Quest line target: every Death Match line has this target, so the duplicate-target rule keeps it unique. */
	public static final String TARGET = "minecraft:player";

	private DeathMatchRules() {}

	/** Players that must fall: half of those online, rounded up. 0 = the match cannot start. */
	public static int needed(int online) {
		return online < MIN_ONLINE ? 0 : (online + 1) / 2;
	}

	public static boolean canOffer(int online) {
		return online >= MIN_ONLINE;
	}
}
