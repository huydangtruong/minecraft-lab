package io.github.huydangtruong.permahearts;

import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

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
			? Component.translatableWithFallback("permahearts.msg.on", "ImmortalCultivation is now ON in this world")
			: Component.translatableWithFallback("permahearts.msg.off", "ImmortalCultivation is now OFF in this world");
	}

	public static Component disabledHint() {
		return Component.translatableWithFallback("permahearts.msg.disabled",
			"ImmortalCultivation is off in this world. Turn on: /gamerule permahearts:immortal_cultivation true");
	}

	public static Component hudLabel(int xp) {
		return Component.translatableWithFallback("permahearts.hud.label", "HeartXP %s/500", xp);
	}

	public static Component hudMax() {
		return Component.translatableWithFallback("permahearts.hud.max", "HeartXP MAX");
	}

	public static Component statName(ElixirStat stat) {
		return Component.translatable("permahearts.stat." + stat.id());
	}

	public static Component elixirDrunk(ElixirStat stat, int newLevel) {
		return Component.translatableWithFallback("permahearts.msg.elixir_drunk", "%s %s! Total: %s",
			stat.format(stat.step()), statName(stat), stat.format(stat.bonus(newLevel)));
	}

	public static Component elixirTooltip(ElixirStat stat) {
		return Component.translatableWithFallback("permahearts.elixir.tooltip", "%s %s, permanent",
			stat.format(stat.step()), statName(stat));
	}

	public static Component elixirDisabled() {
		return Component.translatableWithFallback("permahearts.msg.elixir_disabled", "ImmortalCultivation is off in this world");
	}

	public static Component cultivationStatus(CultivationData data) {
		List<Component> parts = new ArrayList<>();
		for (ElixirStat stat : ElixirStat.values()) {
			int level = data.level(stat);
			if (level > 0) {
				parts.add(Component.translatable("permahearts.msg.cultivation_part", statName(stat), stat.format(stat.bonus(level))));
			}
		}
		if (parts.isEmpty()) {
			return Component.translatableWithFallback("permahearts.msg.cultivation_none", "No elixirs yet");
		}
		Component line = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				line = Component.empty().append(line).append(Component.literal(" · "));
			}
			line = Component.empty().append(line).append(parts.get(i));
		}
		return line;
	}

	// ---- Wanderer (0.5.0) ----

	public static Component wandererName() {
		return Component.translatableWithFallback("permahearts.wanderer.name", "Wanderer");
	}

	public static Component wandererPlanned(int x, int z) {
		return Component.translatableWithFallback("permahearts.wanderer.planned", "A Wanderer is waiting at X %s, Z %s", x, z);
	}

	public static Component wandererIntro() {
		return Component.translatableWithFallback("permahearts.wanderer.intro",
			"I am a traveller of many realms. Help me with a few tasks and I will give you cultivation elixirs.");
	}

	public static Component questsTitle() {
		return Component.translatableWithFallback("permahearts.wanderer.quests_title", "Quests");
	}

	public static Component questOffer() {
		return Component.translatableWithFallback("permahearts.wanderer.offer", "Rewards are shared: first come, first served.");
	}

	public static Component questSummary(int done, int total) {
		return Component.translatableWithFallback("permahearts.wanderer.summary", "Done: %s/%s\nRewards: first come, first served", done, total);
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
			"Quest line done: %s. Reward waits at the Wanderer", name);
	}

	public static Component farewell() {
		return Component.translatableWithFallback("permahearts.wanderer.farewell", "The Wanderer thanks you and moves on");
	}

	public static Component questWhere(int x, int z, int distance) {
		return Component.translatableWithFallback("permahearts.wanderer.where", "Wanderer at X %s, Z %s (%s blocks away)", x, z, distance);
	}

	public static Component questNext(long days) {
		return Component.translatableWithFallback("permahearts.wanderer.next", "Next Wanderer in about %s day(s)", days);
	}

	public static Component questNoLines() {
		return Component.translatableWithFallback("permahearts.wanderer.not_accepted", "Quest not accepted yet: talk to the Wanderer");
	}

	public static Component questLineStatus(int number, Component text) {
		return Component.translatableWithFallback("permahearts.wanderer.status_line", "%s. %s", number, text);
	}

	public static Component questDone() {
		return Component.translatableWithFallback("permahearts.wanderer.done", "done");
	}

	public static Component questAdmin(String text) {
		return Component.literal(text);
	}

	// ---- Quest events (0.6.0) ----

	public static Component lineExplore(Component name) {
		return Component.translatableWithFallback("permahearts.wanderer.line_explore", "Reach %s", name);
	}

	public static Component lineBloodMoon() {
		return Component.translatableWithFallback("permahearts.wanderer.line_blood_moon", "Survive the Blood Moon night");
	}

	public static Component lineBoss(Component name) {
		return Component.translatableWithFallback("permahearts.wanderer.line_boss", "Defeat %s", name);
	}

	public static Component start() {
		return Component.translatableWithFallback("permahearts.wanderer.start", "Start");
	}

	public static Component summon() {
		return Component.translatableWithFallback("permahearts.wanderer.summon", "Summon");
	}

	public static Component running() {
		return Component.translatableWithFallback("permahearts.wanderer.running", "In progress…");
	}

	public static Component fighting() {
		return Component.translatableWithFallback("permahearts.wanderer.fighting", "Fighting…");
	}

	public static Component bloodMoonStarted() {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_started", "The Blood Moon rises! Survive until dawn");
	}

	public static Component bloodMoonFailed(Component player) {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_failed", "The Blood Moon claimed %s. Try again", player);
	}

	public static Component bloodMoonNoSleep() {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_no_sleep", "You cannot sleep under the Blood Moon");
	}

	public static Component bloodMoonBar() {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_bar", "Blood Moon");
	}

	public static Component bossName(Component entity) {
		return Component.translatableWithFallback("permahearts.wanderer.boss_name", "%s Chieftain", entity);
	}

	public static Component bossSummoned(Component name) {
		return Component.translatableWithFallback("permahearts.wanderer.boss_summoned", "%s has appeared!", name);
	}

	public static Component bossAlreadyHere() {
		return Component.translatableWithFallback("permahearts.wanderer.boss_already_here", "The chieftain is already here");
	}

	public static Component eventNotHere() {
		return Component.translatableWithFallback("permahearts.wanderer.event_not_here", "This can only be started in the Overworld, near the Wanderer");
	}

	/** id is a structure id or "#tag" id, e.g. "#minecraft:village". */
	public static Component structureName(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		return Component.translatableWithFallback("permahearts.structure." + path, path);
	}
}
