package io.github.huydangtruong.immortalcultivation.wanderer;

import io.github.huydangtruong.immortalcultivation.QuestEventMessages;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspedMob;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * The Death Match quest event (D25). State lives in WandererData.deathMatch (saved). Players that fall to another
 * player while it runs count once each; such deaths keep hearts, items and XP (see ImmortalCultivation and the mixin).
 */
public final class DeathMatch {
	/** Test hook: when set, this is the number of players "online" (mock players are not in the player list). */
	private static Integer onlineOverride;

	/** D116: true on a player while a Death Match runs; synced to that player's client, which paints the sky purple. Not saved. */
	public static final AttachmentType<Boolean> SKY = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "death_match_sky"),
		b -> b.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly()));

	private DeathMatch() {}

	/** Once a second: sets the sky flag on every player (only writes on a change, so nothing is sent otherwise). */
	public static void tickSky(MinecraftServer server) {
		boolean on = ImmortalCultivation.enabled(server) && running(server);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (Boolean.TRUE.equals(p.getAttached(SKY)) != on) {
				if (on) {
					p.setAttached(SKY, true);
				} else {
					p.removeAttached(SKY);
				}
			}
		}
	}

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
			starter.sendOverlayMessage(QuestEventMessages.deathMatchNeedPlayers());
			return false;
		}
		if (!pvpAllowed(server)) {
			starter.sendOverlayMessage(QuestEventMessages.deathMatchPvpOff());
			return false;
		}
		int needed = DeathMatchRules.needed(online);
		Quest started = QuestLogic.deathMatchStarted(q, line, needed);
		Wanderer.save(server, d.withQuest(Optional.of(started))
			.withDeathMatch(Optional.of(new DeathMatchState(line, List.of()))));
		server.getPlayerList().broadcastSystemMessage(QuestEventMessages.deathMatchStarted(needed, online), false);
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
		if (!running(server) || !ImmortalCultivation.enabled(server)) {
			return null;
		}
		ServerPlayer p = playerBehind(server, victim.getKillCredit(), 0);
		if (p != null && p != victim) {
			return p;
		}
		p = source == null ? null : playerBehind(server, source.getEntity(), 0);
		return p != null && p != victim ? p : null;
	}

	/**
	 * D275: the player behind a killer: the player, or the owner of their summon (Warrior Angel, Violet Guardian, Illusion
	 * clone), of their grasped mob, or of the projectile any of these shot.
	 */
	static ServerPlayer playerBehind(MinecraftServer server, Entity e, int depth) {
		if (e == null || depth > 2) {
			return null;
		}
		if (e instanceof ServerPlayer sp) {
			return sp;
		}
		if (e instanceof Projectile pr) {
			return playerBehind(server, pr.getOwner(), depth + 1);
		}
		UUID owner = null;
		if (e instanceof OwnedSummon s) {
			owner = s.owner();
		} else if (e.getAttached(GraspedMob.ATTACHMENT) instanceof GraspedMob g) {
			owner = g.owner();
		}
		return owner != null && e.level().getPlayerByUUID(owner) instanceof ServerPlayer sp ? sp : null;
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
			QuestEventMessages.deathMatchFell(victim.getDisplayName(), killer.getDisplayName(), line.progress(), line.required()), false);
		if (finished) {
			Wanderer.announceIfDone(server, before, after, st.line());
		}
	}
}
