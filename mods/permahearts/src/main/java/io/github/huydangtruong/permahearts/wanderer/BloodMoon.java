package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.Messages;
import java.util.Collection;
import java.util.List;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
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
	public static final int PER_WAVE = 3;
	public static final int MAX_ALIVE = 30;
	/** Bar length in ticks (a Minecraft night is about this long). */
	public static final long BAR_TICKS = 12000;
	/** Safety end: the event always ends by itself. */
	public static final long HARD_END = 13000;
	/** Do not end at "dawn" before this many ticks have passed (the clock was just moved to night). */
	public static final long MIN_ELAPSED = 200;

	private static final RandomGenerator RNG = new java.util.Random();
	/** Arena side of each player during the event (D33): inside or not, and the last position on that side. Runtime only. */
	private record Side(boolean inside, double x, double y, double z) {}
	private static final java.util.Map<UUID, Side> SIDES = new java.util.HashMap<>();
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

	/** The mob type of a Blood Moon line target (zombie, piglin brute or creeper; "" = zombie). */
	public static EntityType<?> mobType(String target) {
		return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(QuestGenerator.bloodMoonMob(target)));
	}

	/** Display name of a Blood Moon mob: "Zombie Horseman" for horsemen, else the entity name. */
	public static net.minecraft.network.chat.Component mobName(String target) {
		return QuestGenerator.HORSEMAN.equals(QuestGenerator.bloodMoonMob(target)) ? Messages.zombieHorseman() : mobType(target).getDescription();
	}

	/** The mob name of the running event. */
	private static net.minecraft.network.chat.Component runningName(MinecraftServer server) {
		return mobName(EntityType.getKey(runningMob(server)).toString());
	}

	/** The mob of the running event (zombie if the line is gone). */
	private static EntityType<?> runningMob(MinecraftServer server) {
		WandererData d = Wanderer.data(server);
		if (d.bloodMoon().isPresent() && d.quest().isPresent()) {
			int line = d.bloodMoon().get().line();
			List<QuestLine> lines = d.quest().get().lines();
			if (line >= 0 && line < lines.size()) {
				return mobType(lines.get(line).target());
			}
		}
		return mobType("");
	}

	/** Pure decision: how many mobs to add for one player. */
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
		server.getPlayerList().broadcastSystemMessage(Messages.bloodMoonStarted(runningName(server)), false);
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
			bar = new ServerBossEvent(UUID.randomUUID(), Messages.bloodMoonBar(runningName(server)), BossEvent.BossBarColor.RED,
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
		SIDES.clear();
	}

	private static void end(MinecraftServer server) {
		Wanderer.save(server, Wanderer.data(server).withBloodMoon(Optional.empty()));
		clearBar();
	}

	/** Runtime cleanup when the whole Wanderer state is reset (admin, tests): bar, wave counter and leftover mobs. */
	static void onReset(MinecraftServer server) {
		clearBar();
		for (Mob m : alive(server.overworld())) {
			m.discard();
		}
	}

	/** Blood Moon attackers for the 30 cap: a horse that still carries its rider does not count twice. */
	private static int attackers(ServerLevel ow) {
		return (int) alive(ow).stream().filter(m -> !m.isVehicle()).count();
	}

	/** Living Blood Moon mobs (any variant, horses included). Public for the game tests. */
	public static List<? extends Mob> alive(ServerLevel ow) {
		return ow.getEntities(EntityTypeTest.forClass(Mob.class), e -> e.entityTags().contains(TAG));
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
			// Only players in the arena are attacked (D33).
			wave(server, ow.players().stream().filter(BloodMoon::targetable).filter(p -> inArena(d, p)).toList());
		}
		showEdges(ow, d, ow.players());
	}

	private static boolean inArena(WandererData d, ServerPlayer p) {
		return BloodMoonArena.inside(p.getX(), p.getZ(), d.x(), d.z());
	}

	// ---- arena barrier (D33) ----

	/** Every tick from the scheduler. */
	public static void barrierTick(MinecraftServer server) {
		if (!running(server)) {
			SIDES.clear();
			return;
		}
		barrier(server, server.overworld().players(), true);
	}

	/**
	 * Keeps each given player on the side of the arena edge where it was first seen during this event: a player who
	 * crosses is put back where it last stood on its side. Creative / spectator players and other dimensions are free.
	 * Public for the game tests (mock players are not in the overworld player list and always count as creative, so
	 * the tests pass checkGameMode = false).
	 */
	public static void barrier(MinecraftServer server, Collection<ServerPlayer> players, boolean checkGameMode) {
		WandererData d = Wanderer.data(server);
		ServerLevel ow = server.overworld();
		for (ServerPlayer p : players) {
			if (p.level() != ow || (checkGameMode && !targetable(p)) || !p.isAlive()) {
				continue;
			}
			boolean in = inArena(d, p);
			Side side = SIDES.get(p.getUUID());
			if (side == null || side.inside() == in) {
				SIDES.put(p.getUUID(), new Side(in, p.getX(), p.getY(), p.getZ()));
				continue;
			}
			p.stopRiding();
			p.teleportTo(ow, side.x(), side.y(), side.z(), java.util.Set.of(), p.getYRot(), p.getXRot(), false);
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			p.sendOverlayMessage(side.inside() ? Messages.bloodMoonHeldIn() : Messages.bloodMoonKeptOut());
		}
	}

	/** A respawned player starts fresh: the side of its respawn point counts. */
	public static void onRespawn(ServerPlayer p) {
		SIDES.remove(p.getUUID());
	}

	/** Red dust along the arena edge near players who are close to it. */
	private static void showEdges(ServerLevel ow, WandererData d, Collection<ServerPlayer> players) {
		var dust = new net.minecraft.core.particles.DustParticleOptions(0xC00000, 1.5f);
		double cx = d.x() + 0.5;
		double cz = d.z() + 0.5;
		int h = BloodMoonArena.HALF;
		int s = BloodMoonArena.SHOW_EDGE;
		for (ServerPlayer p : players) {
			if (BloodMoonArena.edgeDistance(p.getX(), p.getZ(), d.x(), d.z()) > s) {
				continue;
			}
			for (int t = -s; t <= s; t++) {
				// the four edges, only the points near the player and on the edge segment
				for (double[] pt : new double[][] {
					{cx - h, Math.floor(p.getZ()) + t}, {cx + h, Math.floor(p.getZ()) + t},
					{Math.floor(p.getX()) + t, cz - h}, {Math.floor(p.getX()) + t, cz + h}}) {
					if (Math.abs(pt[0] - cx) <= h + 0.01 && Math.abs(pt[1] - cz) <= h + 0.01
						&& Math.hypot(pt[0] - p.getX(), pt[1] - p.getZ()) <= s + 1) {
						ow.sendParticles(dust, pt[0], p.getY() + 1, pt[1], 3, 0, 0.8, 0, 0);
					}
				}
			}
		}
	}

	/** Creative and spectator players are not attacked. */
	private static boolean targetable(ServerPlayer p) {
		return !p.isCreative() && !p.isSpectator();
	}

	/** One wave around each given player (the caller filters creative/spectator). Public for the game tests. */
	public static void wave(MinecraftServer server, Collection<ServerPlayer> players) {
		ServerLevel ow = server.overworld();
		EntityType<?> type = runningMob(server);
		int alive = attackers(ow);
		for (ServerPlayer p : players) {
			if (p.level() != ow) {
				continue;
			}
			int n = toSpawn(alive);
			for (int i = 0; i < n; i++) {
				if (spawn(ow, type, p)) {
					alive++;
				}
			}
		}
	}

	private static boolean spawn(ServerLevel ow, EntityType<?> type, ServerPlayer target) {
		double angle = RNG.nextDouble() * Math.PI * 2;
		double dist = 3 + RNG.nextDouble() * 3;
		BlockPos pos = SpawnSpots.standSpot(ow, target.blockPosition(), (int) Math.round(Math.cos(angle) * dist),
			(int) Math.round(Math.sin(angle) * dist));
		if (!(type.create(ow, EntitySpawnReason.EVENT) instanceof Mob mob)) {
			return false;
		}
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
		mob.finalizeSpawn(ow, ow.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		if (mob instanceof AbstractPiglin piglin) {
			piglin.setImmuneToZombification(true); // piglin brutes would turn into zombified piglins in the overworld
		}
		mob.addTag(TAG);
		mob.setTarget(target);
		if (mob instanceof ZombieHorse horse) {
			return horseman(ow, horse, target);
		}
		return ow.addFreshEntity(mob);
	}

	/**
	 * A zombie horseman, like vanilla's natural one (ZombieHorse#finalizeSpawn with NATURAL): a zombie with an iron
	 * spear rides the horse. Both carry the tag so give up / reset removes them.
	 */
	private static boolean horseman(ServerLevel ow, ZombieHorse horse, ServerPlayer target) {
		Zombie rider = EntityTypes.ZOMBIE.create(ow, EntitySpawnReason.JOCKEY);
		if (rider == null) {
			return ow.addFreshEntity(horse);
		}
		rider.snapTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), 0f);
		rider.finalizeSpawn(ow, ow.getCurrentDifficultyAt(horse.blockPosition()), EntitySpawnReason.JOCKEY, null);
		rider.setBaby(false); // a baby would not hold the spear on a horse well
		rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		rider.addTag(TAG);
		rider.setTarget(target);
		rider.startRiding(horse, false, false);
		return ow.tryAddFreshEntityWithPassengers(horse);
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
