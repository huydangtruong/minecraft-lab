package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.Messages;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

/**
 * The Blood Moon quest event. State lives in WandererData.bloodMoon (saved); the boss bar and the wave counter are
 * runtime only and rebuilt from the saved state. tick() runs once per second from the scheduler.
 */
public final class BloodMoon {
	public static final String TAG = "permahearts_blood_moon";
	/** Ticks between waves. */
	public static final long WAVE_TICKS = 200;
	public static final int PER_WAVE = 5;
	public static final int MAX_ALIVE = 30;
	/** Bar length in ticks (a Minecraft night is about this long). */
	public static final long BAR_TICKS = 12000;
	/** Safety end: the event always ends by itself. */
	public static final long HARD_END = 13000;
	/** Do not end at "dawn" before this many ticks have passed (the clock was just moved to night). */
	public static final long MIN_ELAPSED = 200;

	private static final RandomGenerator RNG = new java.util.Random();
	private static ServerBossEvent bar;
	private static long lastWave = -1;

	private BloodMoon() {}

	public static boolean running(MinecraftServer server) {
		return Wanderer.data(server).bloodMoon().isPresent();
	}

	/** Pure decision: should the event end with success? */
	public static boolean succeeded(long elapsed, boolean brightOutside) {
		return (elapsed >= MIN_ELAPSED && brightOutside) || elapsed >= HARD_END;
	}

	/** Pure decision: how many zombies to add for one player. */
	public static int toSpawn(int alive) {
		return Math.max(0, Math.min(PER_WAVE, MAX_ALIVE - alive));
	}

	// ---- start / end ----

	public static boolean start(MinecraftServer server, int line, ServerPlayer starter) {
		WandererData d = Wanderer.data(server);
		if (d.bloodMoon().isPresent() || d.quest().isEmpty() || !d.quest().get().accepted()) {
			return false;
		}
		Quest q = d.quest().get();
		if (line < 0 || line >= q.lines().size() || q.lines().get(line).type() != QuestType.BLOOD_MOON
			|| q.lines().get(line).done()) {
			return false;
		}
		ServerLevel ow = server.overworld();
		if (starter.level() != ow) {
			starter.sendOverlayMessage(Messages.eventNotHere());
			return false;
		}
		moveClock(server, true);
		Wanderer.save(server, d.withBloodMoon(Optional.of(new BloodMoonState(line, starter.getUUID().toString(), ow.getGameTime()))));
		lastWave = -1;
		ensureBar(server);
		for (ServerPlayer p : ow.players()) {
			bar.addPlayer(p);
		}
		server.getPlayerList().broadcastSystemMessage(Messages.bloodMoonStarted(), false);
		return true;
	}

	/** Moves the overworld clock to night (or day). Also used by the game tests to put it back. */
	public static void moveClock(MinecraftServer server, boolean night) {
		Holder<WorldClock> clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
		server.clockManager().moveToTimeMarker(clock, night ? ClockTimeMarkers.NIGHT : ClockTimeMarkers.DAY);
	}

	/** True right after the clock was moved to night (Level#isDarkOutside lags behind until the next real tick). */
	public static boolean isNight(MinecraftServer server) {
		Holder<WorldClock> clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
		return server.clockManager().isAtTimeMarker(clock, ClockTimeMarkers.NIGHT);
	}

	private static void ensureBar(MinecraftServer server) {
		if (bar == null) {
			bar = new ServerBossEvent(UUID.randomUUID(), Messages.bloodMoonBar(), BossEvent.BossBarColor.RED,
				BossEvent.BossBarOverlay.PROGRESS);
			bar.setDarkenScreen(true);
			bar.setCreateWorldFog(true);
		}
	}

	private static void clearBar() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
		lastWave = -1;
	}

	private static void end(MinecraftServer server) {
		Wanderer.save(server, Wanderer.data(server).withBloodMoon(Optional.empty()));
		clearBar();
	}

	/** Runtime cleanup when the whole Wanderer state is reset (admin, tests): bar, wave counter and leftover zombies. */
	static void onReset(MinecraftServer server) {
		clearBar();
		for (Zombie z : server.overworld().getEntities(EntityTypeTest.forClass(Zombie.class), e -> e.entityTags().contains(TAG))) {
			z.discard();
		}
	}

	// ---- tick ----

	public static void tick(MinecraftServer server) {
		WandererData d = Wanderer.data(server);
		if (d.bloodMoon().isEmpty()) {
			if (bar != null) {
				clearBar();
			}
			return;
		}
		BloodMoonState st = d.bloodMoon().get();
		ServerLevel ow = server.overworld();
		long elapsed = ow.getGameTime() - st.startGameTime();
		ensureBar(server);
		for (ServerPlayer p : ow.players()) {
			bar.addPlayer(p);
		}
		bar.setProgress(Math.max(0f, Math.min(1f, elapsed / (float) BAR_TICKS)));
		if (succeeded(elapsed, ow.isBrightOutside())) {
			end(server);
			Wanderer.eventDone(server, st.line());
			return;
		}
		long wave = Math.max(0, elapsed) / WAVE_TICKS;
		if (wave > lastWave) {
			lastWave = wave;
			wave(server, ow.players().stream().filter(BloodMoon::targetable).toList());
		}
	}

	/** Creative and spectator players are not attacked. */
	private static boolean targetable(ServerPlayer p) {
		return !p.isCreative() && !p.isSpectator();
	}

	/** One wave around each given player (the caller filters creative/spectator). Public for the game tests. */
	public static void wave(MinecraftServer server, Collection<ServerPlayer> players) {
		ServerLevel ow = server.overworld();
		int alive = ow.getEntities(EntityTypeTest.forClass(Zombie.class), e -> e.entityTags().contains(TAG)).size();
		for (ServerPlayer p : players) {
			if (p.level() != ow) {
				continue;
			}
			int n = toSpawn(alive);
			for (int i = 0; i < n; i++) {
				if (spawnZombie(ow, p)) {
					alive++;
				}
			}
		}
	}

	private static boolean spawnZombie(ServerLevel ow, ServerPlayer target) {
		double angle = RNG.nextDouble() * Math.PI * 2;
		double dist = 3 + RNG.nextDouble() * 3;
		int x = (int) Math.floor(target.getX() + Math.cos(angle) * dist);
		int z = (int) Math.floor(target.getZ() + Math.sin(angle) * dist);
		int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		Zombie zombie = EntityTypes.ZOMBIE.create(ow, EntitySpawnReason.EVENT);
		if (zombie == null) {
			return false;
		}
		BlockPos pos = new BlockPos(x, y, z);
		zombie.snapTo(x + 0.5, y, z + 0.5, RNG.nextFloat() * 360f, 0f);
		zombie.finalizeSpawn(ow, ow.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		zombie.addTag(TAG);
		zombie.setTarget(target);
		return ow.addFreshEntity(zombie);
	}

	// ---- failure and join hooks ----

	public static void onDeath(MinecraftServer server, LivingEntity entity) {
		Optional<BloodMoonState> st = Wanderer.data(server).bloodMoon();
		if (st.isPresent() && entity instanceof ServerPlayer p && p.getUUID().toString().equals(st.get().starterUuid())) {
			fail(server, p);
		}
	}

	public static void onLeave(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		Optional<BloodMoonState> st = Wanderer.data(server).bloodMoon();
		if (st.isPresent() && p.getUUID().toString().equals(st.get().starterUuid())) {
			fail(server, p);
		}
	}

	private static void fail(MinecraftServer server, ServerPlayer starter) {
		end(server);
		server.getPlayerList().broadcastSystemMessage(Messages.bloodMoonFailed(starter.getDisplayName()), false);
	}

	public static void onJoin(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		if (running(server) && p.level() == server.overworld()) {
			ensureBar(server);
			bar.addPlayer(p);
		}
	}
}
