package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.Messages;
import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * The Death Match quest event (D25). State lives in WandererData.deathMatch (saved). Players that fall to another
 * player while it runs count once each; such deaths keep hearts, items and XP (see PermaHearts and the mixin).
 */
public final class DeathMatch {
	/** Test hook: when set, this is the number of players "online" (mock players are not in the player list). */
	private static Integer onlineOverride;

	private DeathMatch() {}

	public static void setOnlineForTest(Integer online) {
		onlineOverride = online;
	}

	public static int online(MinecraftServer server) {
		return onlineOverride != null ? onlineOverride : server.getPlayerList().getPlayerCount();
	}

	/** PvP is a game rule in 26.3 (GameRules.PVP). */
	public static boolean pvpAllowed(MinecraftServer server) {
		return server.getGameRules().get(GameRules.PVP);
	}

	public static boolean running(MinecraftServer server) {
		return Wanderer.data(server).deathMatch().isPresent();
	}

	/** True when the match of this line is running. */
	static boolean runningFor(WandererData d, int line) {
		return d.deathMatch().isPresent() && d.deathMatch().get().line() == line;
	}

	// ---- start / end ----

	public static boolean start(MinecraftServer server, int line, ServerPlayer starter) {
		WandererData d = Wanderer.data(server);
		if (d.deathMatch().isPresent() || d.quest().isEmpty() || !d.quest().get().accepted()) {
			return false;
		}
		Quest q = d.quest().get();
		if (line < 0 || line >= q.lines().size() || q.lines().get(line).type() != QuestType.DEATH_MATCH
			|| q.lines().get(line).done()) {
			return false;
		}
		int online = online(server);
		if (!DeathMatchRules.canOffer(online)) {
			starter.sendOverlayMessage(Messages.deathMatchNeedPlayers());
			return false;
		}
		if (!pvpAllowed(server)) {
			starter.sendOverlayMessage(Messages.deathMatchPvpOff());
			return false;
		}
		int needed = DeathMatchRules.needed(online);
		Quest started = QuestLogic.deathMatchStarted(q, line, needed);
		Wanderer.save(server, d.withQuest(Optional.of(started))
			.withDeathMatch(Optional.of(new DeathMatchState(line, List.of()))));
		server.getPlayerList().broadcastSystemMessage(Messages.deathMatchStarted(needed, online), false);
		return true;
	}

	/** Clears the match (Give up, the Master leaving, /quest reset). Callers that save a stale WandererData afterwards must also clear it there. */
	static void onReset(MinecraftServer server) {
		WandererData d = Wanderer.data(server);
		if (d.deathMatch().isPresent()) {
			Wanderer.save(server, d.withDeathMatch(Optional.empty()));
		}
	}

	// ---- deaths ----

	/**
	 * The player that killed the victim, if this death counts for the match: the match runs, the switch is on and the
	 * kill credit (or, when that is not set yet, the damage source's entity) is another player. Null otherwise.
	 */
	static ServerPlayer pvpKiller(MinecraftServer server, ServerPlayer victim, DamageSource source) {
		if (!running(server) || !PermaHearts.enabled(server)) {
			return null;
		}
		if (victim.getKillCredit() instanceof ServerPlayer sp && sp != victim) {
			return sp;
		}
		if (source != null && source.getEntity() instanceof ServerPlayer sp && sp != victim) {
			return sp;
		}
		return null;
	}

	/** A death that keeps hearts, items and XP. */
	public static boolean isProtectedDeath(MinecraftServer server, ServerPlayer victim, DamageSource source) {
		return pvpKiller(server, victim, source) != null;
	}

	/** From AFTER_DEATH: a protected death counts the victim once and may finish the match. */
	public static void onPlayerKilled(MinecraftServer server, ServerPlayer victim, DamageSource source) {
		ServerPlayer killer = pvpKiller(server, victim, source);
		if (killer == null) {
			return;
		}
		WandererData d = Wanderer.data(server);
		DeathMatchState st = d.deathMatch().orElseThrow();
		DeathMatchState next = st.with(victim.getUUID().toString());
		if (next == st || d.quest().isEmpty()) {
			return; // already counted: protected, but no progress
		}
		Quest before = d.quest().get();
		Quest after = QuestLogic.deathMatchProgress(before, st.line(), next.fallen().size());
		QuestLine line = after.lines().get(st.line());
		boolean finished = line.done();
		Wanderer.save(server, d.withQuest(Optional.of(after))
			.withDeathMatch(finished ? Optional.empty() : Optional.of(next)));
		server.getPlayerList().broadcastSystemMessage(
			Messages.deathMatchFell(victim.getDisplayName(), killer.getDisplayName(), line.progress(), line.required()), false);
		if (finished) {
			Wanderer.announceIfDone(server, before, after, st.line());
		}
	}
}
