package io.github.huydangtruong.immortalcultivation;

import net.minecraft.network.chat.Component;

/** Quest event and Death Match messages (split from Messages, P-076). */
public final class QuestEventMessages {
	private QuestEventMessages() {}

	// ---- Quest events (0.6.0) ----

	public static Component lineExplore(Component name) {
		return Component.translatableWithFallback("permahearts.wanderer.line_explore", "Reach %s", name);
	}

	public static Component lineBloodMoon(Component mob) {
		return Component.translatableWithFallback("permahearts.wanderer.line_blood_moon", "Survive the %s Blood Moon", mob);
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
		return Component.translatableWithFallback("permahearts.wanderer.running", "Running…");
	}

	public static Component fighting() {
		return Component.translatableWithFallback("permahearts.wanderer.fighting", "Fighting…");
	}

	public static Component bloodMoonStarted(Component mob) {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_started", "The %s Blood Moon rises! Survive until dawn", mob);
	}

	public static Component bloodMoonFailed(Component player) {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_failed", "The Blood Moon claimed %s. Try again", player);
	}

	public static Component bloodMoonNoSleep() {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_no_sleep", "You cannot sleep under the Blood Moon");
	}

	public static Component zombieHorseman() {
		return Component.translatableWithFallback("permahearts.wanderer.zombie_horseman", "Zombie Horseman");
	}

	public static Component bloodMoonHeldIn() {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_held_in", "The Blood Moon holds you near the Crimson Master until dawn");
	}

	public static Component bloodMoonKeptOut() {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_kept_out", "The Blood Moon keeps you out until dawn");
	}

	public static Component bloodMoonBar(Component mob) {
		return Component.translatableWithFallback("permahearts.wanderer.blood_moon_bar", "%s Blood Moon", mob);
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

	// ---- Death Match (0.9.0, D25) ----

	public static Component deathMatchName() {
		return Component.translatableWithFallback("permahearts.wanderer.death_match", "Death Match");
	}

	public static Component lineDeathMatch() {
		return Component.translatableWithFallback("permahearts.wanderer.line_death_match", "Death Match (PvP)");
	}

	public static Component lineDeathMatchRunning(int fallen, int needed) {
		return Component.translatableWithFallback("permahearts.wanderer.line_death_match_running", "Death Match %s/%s fallen", fallen, needed);
	}

	public static Component deathMatchStarted(int needed, int online) {
		return Component.translatableWithFallback("permahearts.wanderer.death_match_started",
			"Death Match! %1$s of %2$s players must fall to another player. Deaths in this match cost no hearts and no items.", needed, online);
	}

	public static Component deathMatchFell(Component victim, Component killer, int fallen, int needed) {
		return Component.translatableWithFallback("permahearts.wanderer.death_match_fell",
			"%1$s fell to %2$s (Death Match %3$s/%4$s)", victim, killer, fallen, needed);
	}

	public static Component deathMatchProtected() {
		return Component.translatableWithFallback("permahearts.wanderer.death_match_protected", "You fell in the Death Match: no heart lost, items kept.");
	}

	public static Component deathMatchNeedPlayers() {
		return Component.translatableWithFallback("permahearts.wanderer.death_match_need_players", "Needs at least 2 players online");
	}

	public static Component deathMatchPvpOff() {
		return Component.translatableWithFallback("permahearts.wanderer.death_match_pvp_off", "PvP is off on this server");
	}

	public static Component eventNotHere() {
		return Component.translatableWithFallback("permahearts.wanderer.event_not_here", "This can only be started in the Overworld, near the Crimson Master");
	}

	/** id is a structure id or "#tag" id, e.g. "#minecraft:village". */
	public static Component structureName(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		return Component.translatableWithFallback("permahearts.structure." + path, path);
	}
}
