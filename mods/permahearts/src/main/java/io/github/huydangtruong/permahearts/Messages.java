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

	public static Component gained(int maxHearts) {
		return Component.translatableWithFallback("permahearts.msg.gained", "Hostile mobs defeated: +1 heart! Max hearts: %s", maxHearts);
	}

	public static Component cap() {
		return Component.translatableWithFallback("permahearts.msg.cap", "You already have the maximum of 20 hearts");
	}

	public static Component progress(int current, int total) {
		return Component.translatableWithFallback("permahearts.msg.progress", "Hostile %s/%s", current, total);
	}

	public static Component status(int hearts, int progress, int kills, int deaths) {
		return Component.translatableWithFallback("permahearts.msg.status",
			"Hearts %s/20 · Hostile %s/100 · Kills %s · Deaths %s", hearts, progress, kills, deaths);
	}

	public static Component set(Component name, int hearts) {
		return Component.translatableWithFallback("permahearts.msg.set", "%s now has %s hearts", name, hearts);
	}

	public static Component progressSet(Component name, int progress) {
		return Component.translatableWithFallback("permahearts.msg.progress_set", "%s progress set to %s", name, progress);
	}
}
