package io.github.huydangtruong.immortalcultivation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Wanderer and Crimson Master messages (split from Messages, P-076). */
public final class WandererMessages {
	private WandererMessages() {}

	// ---- Wanderer (0.5.0) ----

	public static Component wandererName() {
		return Component.translatableWithFallback("permahearts.wanderer.name", "Crimson Master");
	}

	public static Component wandererPlanned(int x, int z) {
		return Component.translatableWithFallback("permahearts.wanderer.planned", "The Crimson Master is waiting at X %s, Z %s", x, z).withStyle(net.minecraft.ChatFormatting.GOLD);
	}

	public static Component wandererIntro() {
		return Component.translatableWithFallback("permahearts.wanderer.intro",
			"I am a traveller of many realms. Help me with a few tasks and I will give you cultivation elixirs.");
	}

	public static Component questsTitle() {
		return Component.translatableWithFallback("permahearts.wanderer.quests_title", "Quests");
	}

	public static Component questOffer() {
		return Component.translatableWithFallback("permahearts.wanderer.offer", "First come, first served.");
	}

	public static Component questSummary(int done, int total) {
		return Component.translatableWithFallback("permahearts.wanderer.summary", "Done: %s/%s", done, total);
	}

	public static Component accept() {
		return Component.translatableWithFallback("permahearts.wanderer.accept", "Accept");
	}

	public static Component lineCollect(int required, Component name, int progress) {
		return Component.translatableWithFallback("permahearts.wanderer.line_collect", "Find %s %s: %s/%s", required, name, progress, required);
	}

	public static Component lineKill(int required, Component name, int progress) {
		return Component.translatableWithFallback("permahearts.wanderer.line_kill", "Kill %s %s: %s/%s", required, name, progress, required);
	}

	public static Component lineDone(Component reward) {
		return Component.translatableWithFallback("permahearts.wanderer.line_done", "Reward: %s", reward);
	}

	public static Component lineClaimed() {
		return Component.translatableWithFallback("permahearts.wanderer.line_claimed", "Claimed");
	}

	public static Component killProgress(Component name, int progress, int required) {
		return Component.translatableWithFallback("permahearts.wanderer.kill_progress", "Quest: %s %s/%s", name, progress, required);
	}

	public static Component lineCompleted(Component name) {
		return Component.translatableWithFallback("permahearts.wanderer.line_completed",
			"Quest line done: %s. Reward waits at the Crimson Master", name);
	}

	public static Component farewell() {
		return Component.translatableWithFallback("permahearts.wanderer.farewell", "The Crimson Master thanks you and moves on");
	}

	public static Component unclaimedChest(int boxes, int x, int y, int z) {
		return Component.translatableWithFallback("permahearts.wanderer.unclaimed_chest",
			"The Crimson Master left %s unclaimed reward box(es) in a chest at %s %s %s", boxes, x, y, z).withStyle(net.minecraft.ChatFormatting.GOLD);
	}

	public static Component farewellLevels(int levels, int quests) {
		return Component.translatableWithFallback(quests == 1 ? "permahearts.wanderer.farewell_levels_one" : "permahearts.wanderer.farewell_levels",
			quests == 1 ? "Everyone receives %s levels for 1 completed quest" : "Everyone receives %s levels for %s completed quests", levels, quests);
	}

	public static Component sharedNoteAdded(Component player) {
		return Component.translatableWithFallback("permahearts.notes.shared_added", "%s added a note to the shared notes", player)
			.withStyle(net.minecraft.ChatFormatting.GRAY);
	}

	public static Component giveUp() {
		return Component.translatableWithFallback("permahearts.wanderer.give_up", "Give up");
	}

	public static Component giveUpConfirm() {
		return Component.translatableWithFallback("permahearts.wanderer.give_up_confirm", "Sure? Click again");
	}

	public static Component gaveUp(Component player) {
		return Component.translatableWithFallback("permahearts.wanderer.gave_up", "%s gave up the Crimson Master's quests. The Crimson Master moves on", player);
	}

	public static Component nothingToGiveUp() {
		return Component.translatableWithFallback("permahearts.wanderer.nothing_to_give_up", "There is no Crimson Master to give up right now");
	}

	public static Component leavesIn(String time) {
		return Component.translatableWithFallback("permahearts.wanderer.leaves_in", "Leaves in %s", time);
	}

	public static Component leavingSoon() {
		return Component.translatableWithFallback("permahearts.wanderer.leaving_soon", "The Crimson Master will leave soon");
	}

	// ---- Crimson Master announcements (0.8.1, D26) ----

	public static Component masterTitle() {
		return Component.translatableWithFallback("permahearts.wanderer.title", "Crimson Master")
			.withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
	}

	public static Component masterArrivedSubtitle(int x, int z) {
		return Component.translatableWithFallback("permahearts.wanderer.title_arrived", "is waiting at X %s, Z %s", x, z)
			.withStyle(net.minecraft.ChatFormatting.GOLD);
	}

	public static Component masterLeavingSubtitle(int x, int z) {
		return Component.translatableWithFallback("permahearts.wanderer.title_leaving", "leaves soon! X %s, Z %s", x, z)
			.withStyle(net.minecraft.ChatFormatting.GOLD);
	}

	public static Component masterBar(int x, int z, String timeLeft) {
		return Component.translatableWithFallback("permahearts.wanderer.bar", "Crimson Master: X %s, Z %s · leaves in %s", x, z, timeLeft).withStyle(net.minecraft.ChatFormatting.GOLD);
	}

	public static Component leftAfterStay() {
		return Component.translatableWithFallback("permahearts.wanderer.left_after_stay", "The Crimson Master's time is up. The Crimson Master moves on");
	}

	public static Component questWhere(int x, int z, int distance) {
		return Component.translatableWithFallback("permahearts.wanderer.where", "Crimson Master at X %s, Z %s (%s blocks away)", x, z, distance);
	}

	public static Component questNext(long days) {
		return Component.translatableWithFallback("permahearts.wanderer.next", "Next Crimson Master in about %s day(s)", days);
	}

	public static Component questNoLines() {
		return Component.translatableWithFallback("permahearts.wanderer.not_accepted", "Quest not accepted yet: talk to the Crimson Master");
	}

	public static Component questLineStatus(int number, Component text) {
		return Component.translatableWithFallback("permahearts.wanderer.status_line", "%s. %s", number, text);
	}

	public static Component questAdmin(String text) {
		return Component.literal(text);
	}
}
