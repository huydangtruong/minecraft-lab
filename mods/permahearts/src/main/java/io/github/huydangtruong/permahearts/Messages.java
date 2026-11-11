package io.github.huydangtruong.permahearts;

import net.minecraft.network.chat.Component;

public final class Messages {
	private Messages() {}

	public static Component lost(int maxHearts) {
		return Component.translatableWithFallback("permahearts.msg.lost", "You lost a heart. Max hearts: %s", maxHearts);
	}

	public static Component floor() {
		return Component.translatableWithFallback("permahearts.msg.floor", "You are at the minimum of 1 heart");
	}

	public static Component gained(int hearts, int maxHearts) {
		return Component.translatableWithFallback("permahearts.msg.gained", "+%s heart(s)! Max hearts: %s", hearts, maxHearts);
	}

	public static Component cap() {
		return Component.translatableWithFallback("permahearts.msg.cap", "You already have the maximum of 50 hearts");
	}

	public static Component xpGained(int xp) {
		return Component.translatableWithFallback("permahearts.msg.xp_gained", "+%s HeartXP", xp);
	}

	public static Component status(int hearts, int xp, int kills, int deaths) {
		return Component.translatableWithFallback("permahearts.msg.status",
			"Hearts %s/50 · HeartXP %s/500 · Kills %s · Deaths %s", hearts, xp, kills, deaths);
	}

	public static Component set(Component name, int hearts) {
		return Component.translatableWithFallback("permahearts.msg.set", "%s now has %s hearts", name, hearts);
	}

	public static Component xpSet(Component name, int xp) {
		return Component.translatableWithFallback("permahearts.msg.xp_set", "%s HeartXP set to %s", name, xp);
	}

	public static Component toggled(boolean on) {
		return on
			? Component.translatableWithFallback("permahearts.msg.on", "PermaHearts is now ON in this world")
			: Component.translatableWithFallback("permahearts.msg.off", "PermaHearts is now OFF in this world");
	}

	public static Component disabledHint() {
		return Component.translatableWithFallback("permahearts.msg.disabled",
			"PermaHearts is off in this world. Turn on: /gamerule permahearts:enabled true");
	}

	public static Component hudLabel(int xp) {
		return Component.translatableWithFallback("permahearts.hud.label", "HeartXP %s/500", xp);
	}

	public static Component hudMax() {
		return Component.translatableWithFallback("permahearts.hud.max", "HeartXP MAX");
	}
}
