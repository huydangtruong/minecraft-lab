package io.github.huydangtruong.immortalcultivation;

import net.minecraft.ChatFormatting;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerLines;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;

/** God Slayer messages (split from Messages, P-076). */
public final class GodSlayerMessages {
	private GodSlayerMessages() {}

	// ---- God Slayer (P-032) ----

	public static Component godSlayerName(int level) {
		return Component.translatableWithFallback("permahearts.god_slayer.bar", "God Slayer \u2014 Lv %s", level);
	}

	public static Component godSlayerTitle() {
		return Component.translatableWithFallback("permahearts.god_slayer.title", "God Slayer")
			.withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD);
	}

	public static Component godSlayerSubtitle(String name, int seconds) {
		return Component.translatableWithFallback("permahearts.god_slayer.subtitle", "is coming for %s in %ss", name, seconds).withStyle(net.minecraft.ChatFormatting.RED);
	}

	public static Component godSlayerWarningChat(String name) {
		return Component.translatableWithFallback("permahearts.god_slayer.warning_chat",
			"God Slayer is coming for %s. It arrives in 60 seconds.", name).withStyle(net.minecraft.ChatFormatting.RED);
	}

	public static Component godSlayerFrozen(int pose) {
		String key = switch (pose) {
			case 1 -> "stand";
			case 2 -> "fly";
			case 3 -> "fly_forward";
			default -> "off";
		};
		return Component.translatableWithFallback("permahearts.god_slayer.frozen." + key, switch (pose) {
			case 1 -> "God Slayer frozen: standing";
			case 2 -> "God Slayer frozen: hovering";
			case 3 -> "God Slayer frozen: flying forward";
			default -> "God Slayer unfrozen";
		});
	}

	public static Component godSlayerNoFrozen() {
		return Component.translatableWithFallback("permahearts.god_slayer.no_frozen", "No frozen God Slayer to release");
	}

	public static Component godSlayerCountdown(int seconds) {
		return Component.translatableWithFallback("permahearts.god_slayer.countdown", "God Slayer arrives in %s...", seconds).withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD);
	}

	/** A random scorn line (D113) when nobody has enough hearts. */
	public static Component godSlayerWeaklings() {
		return godSlayerSays(GodSlayerLines.scorn(ThreadLocalRandom.current()));
	}

	/** A random line (D113) when the boss falls. */
	public static Component godSlayerLost() {
		return godSlayerSays(GodSlayerLines.lose(ThreadLocalRandom.current()));
	}

	private static Component godSlayerSays(String line) {
		return Component.literal("God Slayer: " + line).withStyle(ChatFormatting.RED);
	}

	public static Component godSlayerNext(String name) {
		return Component.translatableWithFallback("permahearts.god_slayer.next", "God Slayer turns to %s.", name);
	}

	public static Component godSlayerFallen(int level) {
		return Component.translatableWithFallback("permahearts.god_slayer.fallen",
			"God Slayer has fallen. It will return stronger. (Lv %s)", level);
	}

	/** A random victory line (D113). */
	public static Component godSlayerWon() {
		return godSlayerSays(GodSlayerLines.win(ThreadLocalRandom.current()));
	}

	public static Component godSlayerGaveUp() {
		return Component.translatableWithFallback("permahearts.god_slayer.gave_up", "God Slayer left.");
	}

	public static Component godSlayerReward(int levels, int boxes) {
		return Component.translatableWithFallback("permahearts.god_slayer.reward",
			"You slew the God Slayer: +%s levels and %s reward boxes!", levels, boxes);
	}

	public static Component godSlayerBusy() {
		return Component.translatableWithFallback("permahearts.god_slayer.busy", "God Slayer is already coming or hunting");
	}

	public static Component godSlayerNoPlayer() {
		return Component.translatableWithFallback("permahearts.god_slayer.no_player", "No player to hunt");
	}

	public static Component godSlayerSummoned(String name) {
		return Component.translatableWithFallback("permahearts.god_slayer.summoned", "God Slayer will hunt %s", name);
	}

	public static Component godSlayerNoHunt() {
		return Component.translatableWithFallback("permahearts.god_slayer.no_hunt", "No God Slayer hunt is running");
	}

	public static Component godSlayerLevelSet(int level) {
		return Component.translatableWithFallback("permahearts.god_slayer.level_set", "God Slayer level is now %s", level);
	}

	public static Component godSlayerLevelIs(int level) {
		return Component.translatableWithFallback("permahearts.god_slayer.level_is", "God Slayer level is %s", level);
	}
}
