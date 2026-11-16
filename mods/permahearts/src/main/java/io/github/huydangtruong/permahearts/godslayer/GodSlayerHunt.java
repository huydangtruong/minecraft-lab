package io.github.huydangtruong.permahearts.godslayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import io.github.huydangtruong.permahearts.HeartRules;
import io.github.huydangtruong.permahearts.HeartsAttachment;
import io.github.huydangtruong.permahearts.Messages;
import io.github.huydangtruong.permahearts.godslayer.GodSlayerSavedData.Phase;
import io.github.huydangtruong.permahearts.reward.RewardBoxes;
import io.github.huydangtruong.permahearts.reward.RewardTier;
import io.github.huydangtruong.permahearts.wanderer.SpawnSpots;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The God Slayer hunt (P-032, D96): IDLE -> WARNING (60 s) -> HUNTING (10 min) -> IDLE. One hunt at a time. The boss
 * hunts one player after another (a player who died stays "killed" for the rest of the hunt). Ends when the boss dies
 * (reward), when time runs out or nobody is left (the boss wins) or by /boss giveup. The state is in
 * {@link GodSlayerSavedData}; a restart ends a running hunt (the boss is never saved) but keeps a warning.
 */
public final class GodSlayerHunt {
	private static final RandomGenerator RNG = new java.util.Random();
	private static final double BAR_RANGE = 96;
	private static final int APPEAR_MIN = 15, APPEAR_MAX = 20;

	private static ServerBossEvent bar;
	private static int missingChecks;
	private static int lastCountdown = -1;
	private static long lastBeat = 0;

	private GodSlayerHunt() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 5 == 0) {
				tick(server);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(GodSlayerHunt::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			bar = null;
			missingChecks = 0;
			lastCountdown = -1;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(GodSlayerHunt::onDeath);
	}

	// ---- state access ----

	public static GodSlayerSavedData data(MinecraftServer server) {
		return GodSlayerSavedData.of(server);
	}

	public static Phase phase(MinecraftServer server) {
		return data(server).phase;
	}

	public static int level(MinecraftServer server) {
		return data(server).level;
	}

	public static void setLevel(MinecraftServer server, int level) {
		GodSlayerSavedData d = data(server);
		d.level = GodSlayerRules.clampLevel(level);
		d.setDirty();
	}

	private static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	/** A player the boss may hunt: alive and not in creative or spectator mode. */
	static boolean huntable(ServerPlayer p) {
		return p.isAlive() && !p.isCreative() && !p.isSpectator();
	}

	static String name(ServerPlayer p) {
		return p.getGameProfile().name();
	}

	// ---- trigger from the Crimson Master ----

	/** The Crimson Master has arrived. Called by the scheduler. */
	public static void onCrimsonMasterVisit(MinecraftServer server) {
		onVisit(server, Wanderer.currentDay(server));
	}

	/** The visit logic with the day as a parameter, so tests can pass day 100. */
	public static void onVisit(MinecraftServer server, long day) {
		GodSlayerSavedData d = data(server);
		d.visitCount++;
		d.setDirty();
		if (!GodSlayerRules.isHuntVisit(day, d.visitCount) || d.phase != Phase.IDLE) {
			return;
		}
		List<ServerPlayer> strong = new ArrayList<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (huntable(p) && GodSlayerRules.heartsEnough(HeartRules.maxHearts(HeartsAttachment.get(p)))) {
				strong.add(p);
			}
		}
		if (strong.isEmpty()) {
			server.getPlayerList().broadcastSystemMessage(Messages.godSlayerWeaklings(), false);
			return;
		}
		start(server, strong.get(RNG.nextInt(strong.size())));
	}

	// ---- start, appear ----

	/** Starts the 60 second warning for this target. False if a hunt is already running. */
	public static boolean start(MinecraftServer server, ServerPlayer target) {
		GodSlayerSavedData d = data(server);
		if (d.phase != Phase.IDLE) {
			return false;
		}
		d.phase = Phase.WARNING;
		d.warningEndTick = now(server) + GodSlayerRules.WARNING_TICKS;
		d.huntEndTick = 0;
		d.bossUuid = "";
		d.currentTarget = target.getUUID().toString();
		d.firstTargetName = name(target);
		d.killed = new ArrayList<>();
		d.setDirty();
		lastCountdown = -1;
		lastBeat = 0;
		missingChecks = 0;
		server.getPlayerList().broadcastSystemMessage(Messages.godSlayerWarningChat(name(target)), false);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
			p.connection.send(new ClientboundSetSubtitleTextPacket(Messages.godSlayerSubtitle(name(target), 60)));
			p.connection.send(new ClientboundSetTitleTextPacket(Messages.godSlayerTitle()));
		}
		return true;
	}

	/** Test hook: ends the warning now and lets the boss appear on the next tick. */
	public static void skipWarning(MinecraftServer server) {
		GodSlayerSavedData d = data(server);
		if (d.phase == Phase.WARNING) {
			d.warningEndTick = now(server);
			d.setDirty();
		}
	}

	private static void appear(MinecraftServer server, GodSlayerSavedData d) {
		ServerPlayer t = playerOf(server, d.currentTarget);
		if (t == null || !t.isAlive() || d.killed.contains(d.currentTarget)) {
			t = pickNext(server, d);
		}
		if (t == null) {
			d.clearHunt();
			return;
		}
		ServerLevel level = t.level();
		GodSlayer boss = GodSlayer.TYPE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (boss == null) {
			d.clearHunt();
			return;
		}
		boss.setBossLevel(d.level);
		double ang = RNG.nextDouble() * Math.PI * 2;
		double r = APPEAR_MIN + RNG.nextDouble() * (APPEAR_MAX - APPEAR_MIN);
		BlockPos spot = SpawnSpots.standSpot(level, t.blockPosition(), (int) Math.round(Math.cos(ang) * r), (int) Math.round(Math.sin(ang) * r));
		boss.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
		boss.setHuntTarget(t.getUUID());
		level.addFreshEntity(boss);
		boss.faceTarget(t);
		boss.poof(level);
		d.phase = Phase.HUNTING;
		d.huntEndTick = now(server) + GodSlayerRules.HUNT_TICKS;
		d.bossUuid = boss.getUUID().toString();
		d.currentTarget = t.getUUID().toString();
		d.setDirty();
		missingChecks = 0;
		hideBar();
		bar = new ServerBossEvent(UUID.randomUUID(), Messages.godSlayerName(d.level), BossEvent.BossBarColor.RED,
			BossEvent.BossBarOverlay.NOTCHED_10);
		bar.setDarkenScreen(true);
	}

	// ---- tick ----

	static void tick(MinecraftServer server) {
		GodSlayerSavedData d = data(server);
		if (d.phase == Phase.WARNING) {
			tickWarning(server, d);
		} else if (d.phase == Phase.HUNTING) {
			tickHunting(server, d);
		}
	}

	private static void tickWarning(MinecraftServer server, GodSlayerSavedData d) {
		long left = d.warningEndTick - now(server);
		if (left <= 0) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				ServerPlayNetworking.send(p, new GodSlayerNet.Warning(0)); // the red edge ends
			}
			appear(server, d);
			return;
		}
		int secs = GodSlayerRules.secondsLeft(left);
		// heartbeat for the whole warning, faster toward the end (D102)
		long t = now(server);
		if (t - lastBeat >= GodSlayerRules.heartbeatInterval(secs)) {
			lastBeat = t;
			float pitch = 0.9f + 0.5f * (1f - secs / 60f);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				p.level().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, pitch);
			}
		}
		if (secs != lastCountdown) {
			lastCountdown = secs;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (secs <= 10) {
					p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 2f, 0.7f);
				}
				p.sendOverlayMessage(Messages.godSlayerCountdown(secs)); // red number for the whole 60 s
				ServerPlayNetworking.send(p, new GodSlayerNet.Warning((int) left)); // client interpolates the edge
			}
		}
	}

	private static void tickHunting(MinecraftServer server, GodSlayerSavedData d) {
		GodSlayer boss = findBoss(server, d.bossUuid);
		if (boss == null) {
			if (++missingChecks >= 5) {
				endQuiet(server, d);
			}
			return;
		}
		missingChecks = 0;
		if (boss.isDeadOrDying()) {
			return; // onDeath settles it
		}
		if (boss.isFrozen()) {
			d.huntEndTick++; // /boss freeze pauses the hunt timer (D104)
			d.setDirty();
			return;
		}
		if (now(server) >= d.huntEndTick) {
			won(server, d, boss);
			return;
		}
		ServerPlayer t = playerOf(server, d.currentTarget);
		if (t == null || !t.isAlive()) {
			if (!d.killed.contains(d.currentTarget)) {
				d.killed.add(d.currentTarget);
				d.setDirty();
			}
			advance(server, d, boss);
			boss = findBoss(server, d.bossUuid);
			if (d.phase != Phase.HUNTING || boss == null) {
				return;
			}
			t = playerOf(server, d.currentTarget);
			if (t == null) {
				return;
			}
		}
		if (boss.level() != t.level()) {
			boss = relocate(server, d, boss, t);
		} else if (!t.getUUID().equals(boss.huntTarget())) {
			boss.setHuntTarget(t.getUUID());
		}
		updateBar(server, boss);
	}

	private static void updateBar(MinecraftServer server, GodSlayer boss) {
		if (bar == null) {
			return;
		}
		bar.setProgress(Math.max(0f, Math.min(1f, boss.getHealth() / boss.getMaxHealth())));
		bar.setName(Messages.godSlayerName(boss.bossLevel()));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.level() == boss.level() && p.distanceTo(boss) <= BAR_RANGE) {
				bar.addPlayer(p);
			} else {
				bar.removePlayer(p);
			}
		}
	}

	private static void hideBar() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
	}

	// ---- switching targets, crossing dimensions ----

	/** Picks the next target (not killed, online, huntable); the boss turns to it. Boss wins if nobody is left. */
	private static void advance(MinecraftServer server, GodSlayerSavedData d, GodSlayer boss) {
		ServerPlayer n = pickNext(server, d);
		if (n == null) {
			won(server, d, boss);
			return;
		}
		d.currentTarget = n.getUUID().toString();
		d.setDirty();
		server.getPlayerList().broadcastSystemMessage(Messages.godSlayerNext(name(n)), false);
		if (boss.level() != n.level()) {
			relocate(server, d, boss, n);
		} else {
			boss.setHuntTarget(n.getUUID());
		}
	}

	private static ServerPlayer pickNext(MinecraftServer server, GodSlayerSavedData d) {
		List<ServerPlayer> open = new ArrayList<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (huntable(p) && !d.killed.contains(p.getUUID().toString())) {
				open.add(p);
			}
		}
		return open.isEmpty() ? null : open.get(RNG.nextInt(open.size()));
	}

	/** Moves the boss to another dimension next to the target: a copy is created there and the old one removed. */
	static GodSlayer relocate(MinecraftServer server, GodSlayerSavedData d, GodSlayer boss, ServerPlayer t) {
		ServerLevel dest = t.level();
		GodSlayer nb = GodSlayer.TYPE.create(dest, EntitySpawnReason.MOB_SUMMONED);
		if (nb == null) {
			return boss;
		}
		nb.setBossLevel(boss.bossLevel());
		nb.setHealth(boss.getHealth());
		nb.setHuntTarget(t.getUUID());
		Vec3 p = nb.findSpotNear(t);
		nb.snapTo(p.x, p.y, p.z, 0f, 0f);
		if (boss.level() instanceof ServerLevel old) {
			boss.poof(old);
		}
		boss.discard();
		dest.addFreshEntity(nb);
		nb.faceTarget(t);
		nb.poof(dest);
		d.bossUuid = nb.getUUID().toString();
		d.setDirty();
		return nb;
	}

	// ---- endings ----

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		MinecraftServer server = level.getServer();
		if (entity instanceof GodSlayer boss) {
			onBossDeath(server, boss);
		} else if (entity instanceof ServerPlayer p) {
			onPlayerDeath(server, p);
		}
	}

	private static void onBossDeath(MinecraftServer server, GodSlayer boss) {
		GodSlayerSavedData d = data(server);
		if (d.phase != Phase.HUNTING || !boss.getUUID().toString().equals(d.bossUuid)) {
			return;
		}
		hideBar();
		if (boss.getKillCredit() instanceof ServerPlayer killer) {
			d.level = Math.min(GodSlayerRules.MAX_LEVEL, d.level + 1);
			d.clearHunt();
			reward(killer);
			server.getPlayerList().broadcastSystemMessage(Messages.godSlayerFallen(d.level), false);
		} else {
			d.clearHunt(); // killed by something else (a command): no reward, the level stays
		}
	}

	private static void reward(ServerPlayer killer) {
		killer.giveExperienceLevels(GodSlayerRules.KILL_LEVELS);
		for (RewardTier tier : GodSlayerRules.rewardBoxes(RNG)) {
			ItemStack stack = RewardBoxes.stack(tier);
			if (!killer.getInventory().add(stack)) {
				killer.spawnAtLocation(killer.level(), stack);
			}
		}
		killer.sendSystemMessage(Messages.godSlayerReward(GodSlayerRules.KILL_LEVELS, GodSlayerRules.REWARD_BOXES));
		killer.level().playSound(null, killer.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
	}

	private static void onPlayerDeath(MinecraftServer server, ServerPlayer p) {
		GodSlayerSavedData d = data(server);
		if (d.phase == Phase.IDLE) {
			return;
		}
		String id = p.getUUID().toString();
		if (!d.killed.contains(id)) {
			d.killed.add(id);
			d.setDirty();
		}
		if (d.phase == Phase.HUNTING && id.equals(d.currentTarget)) {
			GodSlayer boss = findBoss(server, d.bossUuid);
			if (boss != null && !boss.isDeadOrDying()) {
				advance(server, d, boss);
			}
		}
	}

	/** Time is up or nobody is left: the boss taunts, leaves in smoke; the level stays. */
	private static void won(MinecraftServer server, GodSlayerSavedData d, GodSlayer boss) {
		server.getPlayerList().broadcastSystemMessage(Messages.godSlayerWon(), false);
		if (boss != null) {
			if (boss.level() instanceof ServerLevel sl) {
				boss.poof(sl);
			}
			boss.discard();
		}
		hideBar();
		d.clearHunt();
	}

	private static void endQuiet(MinecraftServer server, GodSlayerSavedData d) {
		GodSlayer boss = findBoss(server, d.bossUuid);
		if (boss != null) {
			boss.discard();
		}
		hideBar();
		d.clearHunt();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, new GodSlayerNet.Warning(0));
		}
	}

	/** /boss giveup: removes the boss and clears the state. False if nothing was running. */
	public static boolean giveUp(MinecraftServer server) {
		GodSlayerSavedData d = data(server);
		if (d.phase == Phase.IDLE) {
			return removeFrozenStray(server);
		}
		endQuiet(server, d);
		server.getPlayerList().broadcastSystemMessage(Messages.godSlayerGaveUp(), false);
		return true;
	}

	private static void onServerStart(MinecraftServer server) {
		GodSlayerSavedData d = data(server);
		if (d.phase == Phase.HUNTING) {
			d.clearHunt(); // the boss is never saved: nothing to hunt with
		}
	}

	// ---- lookups ----

	public static GodSlayer findBoss(MinecraftServer server, String uuid) {
		if (uuid == null || uuid.isEmpty()) {
			return null;
		}
		UUID id;
		try {
			id = UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			return null;
		}
		for (ServerLevel level : server.getAllLevels()) {
			Entity e = level.getEntity(id);
			if (e instanceof GodSlayer g) {
				return g;
			}
		}
		return null;
	}

	/** Any God Slayer in any dimension (the hunt boss first), or null. */
	public static GodSlayer anyBoss(MinecraftServer server) {
		GodSlayer hunt = currentBoss(server);
		if (hunt != null) {
			return hunt;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity e : level.getAllEntities()) {
				if (e instanceof GodSlayer g && g.isAlive()) {
					return g;
				}
			}
		}
		return null;
	}

	/** Removes God Slayers made by /boss freeze outside a hunt. True if there was one. */
	private static boolean removeFrozenStray(MinecraftServer server) {
		boolean any = false;
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity e : level.getAllEntities()) {
				if (e instanceof GodSlayer g && g.isAlive() && g.isFrozen()) {
					g.discard();
					any = true;
				}
			}
		}
		return any;
	}

	/** /boss freeze: freezes the boss (spawning one 6 blocks in front of the player outside any hunt if none exists). */
	public static GodSlayer freeze(CommandSourceStack source, int pose) {
		MinecraftServer server = source.getServer();
		GodSlayer boss = anyBoss(server);
		ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
		if (pose == GodSlayer.FREEZE_NONE) {
			if (boss == null || !boss.isFrozen()) {
				return null;
			}
			boolean stray = !boss.getUUID().toString().equals(data(server).bossUuid);
			boss.setFreezePose(GodSlayer.FREEZE_NONE);
			if (stray) {
				boss.discard(); // made by /boss freeze outside a hunt: it goes away
			}
			return boss;
		}
		if (boss == null) {
			if (player == null) {
				return null;
			}
			ServerLevel level = player.level();
			boss = GodSlayer.TYPE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (boss == null) {
				return null;
			}
			Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
			double x = player.getX() + look.x * 6;
			double z = player.getZ() + look.z * 6;
			boss.setBossLevel(data(server).level);
			boss.snapTo(x, player.getY(), z, 0f, 0f);
			boss.faceTarget(player);
			level.addFreshEntity(boss);
		} else if (!boss.isFrozen() && player != null && player.level() == boss.level()) {
			boss.faceTarget(player); // faces the player who froze it, once
		}
		boss.setFreezePose(pose);
		return boss;
	}

	/** The boss of the running hunt, or null. */
	public static GodSlayer currentBoss(MinecraftServer server) {
		return findBoss(server, data(server).bossUuid);
	}

	private static ServerPlayer playerOf(MinecraftServer server, String uuid) {
		if (uuid == null || uuid.isEmpty()) {
			return null;
		}
		try {
			return server.getPlayerList().getPlayer(UUID.fromString(uuid));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
