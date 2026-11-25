package io.github.huydangtruong.immortalcultivation.purgatory;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.danger.DangerMobs;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Back;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Owed;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Run;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Solo;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.DungeonRules;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Purgatory (D234-D240, P-082): solo and team floors fought in a generated dungeon of the `permahearts:purgatory` dimension.
 * Rooms wake when a fighter walks in; locked rooms seal until their mobs are dead; killing the boss in the last room wins.
 * Ten minutes at most. Death, Give up or running out of time loses the floor, nothing else. Fighters play in Adventure mode,
 * cannot leave their dungeon and go back to where they stood when it ends.
 */
public final class Purgatory {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "purgatory"));
	/** Every mob spawned by a box carries this tag. */
	public static final String TAG = "permahearts_purgatory";

	private static final Identifier HEALTH_ID = id("purgatory_health");
	private static final Identifier DAMAGE_ID = id("purgatory_damage");
	private static final Identifier ARMOR_ID = id("purgatory_armor");
	private static final Identifier SPEED_ID = id("purgatory_speed");
	private static final Identifier BOSS_ID = id("purgatory_boss");

	private static final RandomSource RNG = RandomSource.create();

	/** Team queue (not saved: a restart empties it). */
	private static final Set<UUID> QUEUE = new LinkedHashSet<>();
	private static int countdown = -1;
	/** Players who died this tick: handled on the next tick, outside the damage code. */
	private static final Set<UUID> DIED = new HashSet<>();
	/** Players who just joined: checked on the next tick. */
	private static final Set<UUID> JOINED = new HashSet<>();
	private static final Map<Integer, ServerBossEvent> BARS = new HashMap<>();
	/** Game tests turn the dimension off and use the given level instead. */
	public static ServerLevel testLevel;

	private Purgatory() {}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, path);
	}

	public static void register() {
		PurgatoryNet.register();
		ServerTickEvents.END_SERVER_TICK.register(Purgatory::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> forget());
		ServerPlayerEvents.JOIN.register(p -> JOINED.add(p.getUUID()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> QUEUE.remove(handler.player.getUUID()));
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer p && fighting(p) && !totemSaves(p, source)) {
				p.setHealth(p.getMaxHealth());
				p.removeAllEffects();
				p.clearFire();
				p.resetFallDistance();
				DIED.add(p.getUUID());
				return false; // D236: a loss costs nothing, no drops, no XP, no heart
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Mob m && m.entityTags().contains(PurgatoryFloor.BOSS_TAG) && m.level() instanceof ServerLevel sl) {
				PurgatoryFloor.bossDied(sl.getServer(), m);
			}
		});
	}

	/**
	 * D252: Fabric asks ALLOW_DEATH before vanilla tries the totem, so a fighter holding a totem must be let through: the totem
	 * then saves them and the run goes on. Damage that beats a totem (void, /kill) is still a loss, never a real death.
	 */
	static boolean totemSaves(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source) {
		if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
			if (p.getItemInHand(hand).has(net.minecraft.core.component.DataComponents.DEATH_PROTECTION)) {
				return true;
			}
		}
		return false;
	}

	/** Clears the state that lives outside the save (B6). */
	public static void forget() {
		QUEUE.clear();
		countdown = -1;
		DIED.clear();
		JOINED.clear();
		BARS.values().forEach(ServerBossEvent::removeAllPlayers);
		BARS.clear();
		PurgatoryFloor.forgetAll();
		testLevel = null;
	}

	// ---- state ----

	public static PurgatorySavedData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(PurgatorySavedData.TYPE);
	}

	/** D239: bosses and events wait while anyone fights in a box. */
	public static boolean busy(MinecraftServer server) {
		return !data(server).runs.isEmpty();
	}

	public static ServerLevel level(MinecraftServer server) {
		if (testLevel != null) {
			return testLevel;
		}
		ServerLevel l = server.getLevel(DIMENSION);
		return l != null ? l : server.overworld();
	}

	/** Alive fighter of a running box. */
	public static boolean fighting(ServerPlayer p) {
		Run r = data(p.level().getServer()).runOf(key(p));
		return r != null && r.alive(key(p));
	}

	private static String key(Player p) {
		return p.getUUID().toString();
	}

	static boolean queued(ServerPlayer p) {
		return QUEUE.contains(p.getUUID());
	}

	static int queueSize() {
		return QUEUE.size();
	}

	static int countdown() {
		return countdown;
	}

	// ---- geometry ----

	/** The whole dungeon grid of a slot, where fighters and watchers must stay. */
	public static AABB inside(int slot) {
		BlockPos o = DungeonBuilder.origin(slot);
		return new AABB(o.getX(), o.getY(), o.getZ(),
			o.getX() + DungeonRules.SIZE_X, o.getY() + DungeonRules.SIZE_Y, o.getZ() + DungeonRules.SIZE_Z);
	}

	/**
	 * D259: blocks of a running dungeon (and its bedrock skin) that skills must not break, burn or replace. True only in the
	 * Purgatory level, inside a run's grid or one block around it.
	 */
	public static boolean guarded(net.minecraft.world.level.Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel sl) || sl != level(sl.getServer())) {
			return false;
		}
		Vec3 c = Vec3.atCenterOf(pos);
		for (Run r : data(sl.getServer()).runs) {
			if (inside(r.slot).inflate(1.5).contains(c)) {
				return true;
			}
		}
		return false;
	}

	/** Where a run's fighters start (watchers float a little higher). */
	public static Vec3 start(Run r) {
		return PurgatoryFloor.start(r);
	}

	// ---- actions (null = done, else the reason) ----

	public static String startSolo(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		String why = canEnter(p);
		if (why != null) {
			return why;
		}
		PurgatorySavedData d = data(server);
		Solo s = d.solo(key(p));
		if (s.cooldown > 0) {
			return "The next floor opens in " + PurgatoryRules.clock(s.cooldown);
		}
		QUEUE.remove(p.getUUID());
		begin(server, d, false, s.floor, List.of(p));
		return null;
	}

	/** Joins or leaves the team queue. */
	public static String toggleTeam(ServerPlayer p) {
		if (QUEUE.remove(p.getUUID())) {
			return null;
		}
		String why = canEnter(p);
		if (why != null) {
			return why;
		}
		PurgatorySavedData d = data(p.level().getServer());
		if (d.teamCooldown > 0) {
			return "The next team floor opens in " + PurgatoryRules.clock(d.teamCooldown);
		}
		if (teamRun(d) != null) {
			return "A team floor is already being fought";
		}
		QUEUE.add(p.getUUID());
		countdown = PurgatoryRules.TEAM_COUNTDOWN_TICKS; // D237: someone new joined, wait 10 s again
		return null;
	}

	public static String giveUp(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		PurgatorySavedData d = data(server);
		Run r = d.runOf(key(p));
		if (r == null) {
			return "You are not in the Purgatory";
		}
		if (!r.team) {
			end(server, d, r, false);
			return null;
		}
		r.dead.remove(key(p));
		r.dead.add(key(p));
		r.players.remove(key(p)); // gave up: no reward, goes home now
		sendBack(server, d, p);
		tellRun(server, r, p.getName().getString() + " gave up");
		if (aliveCount(r) == 0) {
			end(server, d, r, false);
		}
		d.changed();
		return null;
	}

	/** D260: a winner leaves before the 30 seconds are up; the last one to leave closes the floor. */
	public static String comeBack(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		PurgatorySavedData d = data(server);
		Run r = d.runOf(key(p));
		if (r == null || !r.won()) {
			return "The floor is not cleared yet";
		}
		r.players.remove(key(p));
		r.dead.remove(key(p));
		sendBack(server, d, p);
		if (r.players.isEmpty()) {
			close(server, d, r);
		}
		d.changed();
		return null;
	}

	public static String watch(ServerPlayer p, int runId) {
		MinecraftServer server = p.level().getServer();
		PurgatorySavedData d = data(server);
		Run r = d.run(runId);
		if (r == null) {
			return "That floor is over";
		}
		if (d.runOf(key(p)) != null) {
			return "You are fighting in the Purgatory";
		}
		Integer old = d.watching.get(key(p));
		if (old == null) {
			d.back.put(key(p), backOf(p));
		}
		d.watching.put(key(p), runId);
		p.setGameMode(GameType.SPECTATOR);
		Vec3 c = start(r);
		p.teleportTo(level(server), c.x, c.y + 2, c.z, Set.of(), p.getYRot(), 30f, false);
		d.changed();
		return null;
	}

	public static String stopWatching(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		PurgatorySavedData d = data(server);
		if (!d.watching.containsKey(key(p))) {
			return "You are not watching a Purgatory floor";
		}
		sendBack(server, d, p);
		return null;
	}

	private static String canEnter(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		if (!ImmortalCultivation.enabled(server)) {
			return "Immortal Cultivation is off in this world";
		}
		PurgatorySavedData d = data(server);
		if (d.runOf(key(p)) != null) {
			return "You are already in the Purgatory";
		}
		if (d.watching.containsKey(key(p))) {
			return "Stop watching first";
		}
		if (p.isSpectator() || p.isCreative() && testLevel == null) {
			return "Only in Survival or Adventure";
		}
		if (!p.isAlive()) {
			return "You are dead";
		}
		return null;
	}

	// ---- start and end ----

	static Run begin(MinecraftServer server, PurgatorySavedData d, boolean team, int floor, List<ServerPlayer> players) {
		int slot = d.freeSlot();
		List<String> ids = new ArrayList<>();
		for (ServerPlayer p : players) {
			ids.add(key(p));
		}
		long seed = RNG.nextLong();
		Run r = new Run(d.nextId++, slot, team, floor, PurgatoryRules.RUN_TICKS, 0, ids, List.of(), seed == 0 ? 1 : seed, List.of(),
			List.of(), Run.BOSS_WAITING, 0);
		d.runs.add(r);
		ServerLevel level = level(server);
		DungeonBuilder.build(level, PurgatoryFloor.dungeon(r), slot);
		Vec3 c = start(r);
		for (ServerPlayer p : players) {
			d.back.put(key(p), backOf(p));
			p.stopRiding();
			p.setGameMode(GameType.ADVENTURE);
			p.teleportTo(level, c.x, c.y, c.z, Set.of(), p.getYRot(), 0f, false);
			p.setDeltaMovement(Vec3.ZERO);
			p.resetFallDistance();
			p.sendSystemMessage(Component.literal((team ? "Team " : "") + "Purgatory floor " + floor
				+ ": find and kill the boss within 10 minutes").withStyle(ChatFormatting.DARK_RED));
		}
		d.changed();
		return r;
	}

	/** Ends a run: settles it (unless it was already won, D260) and sends everyone home. */
	static void end(MinecraftServer server, PurgatorySavedData d, Run r, boolean won) {
		if (!r.won()) {
			settle(server, d, r, won);
		}
		close(server, d, r);
	}

	/** Floor, cooldown, rewards and the result line. */
	private static void settle(MinecraftServer server, PurgatorySavedData d, Run r, boolean won) {
		if (r.team) {
			if (won) {
				d.teamFloor = r.floor + 1;
			}
			d.teamCooldown = PurgatoryRules.COOLDOWN_TICKS;
		} else if (!r.players.isEmpty()) {
			Solo s = d.solo(r.players.get(0));
			if (won) {
				s.floor = r.floor + 1;
			}
			s.cooldown = PurgatoryRules.COOLDOWN_TICKS;
		}
		String result = (r.team ? "Team " : "") + "Purgatory floor " + r.floor + (won ? " cleared!" : r.ticksLeft <= 0 ? " lost: time is up" : " lost");
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (won) {
				Owed o = new Owed(PurgatoryRules.rewardLevels(r.floor), PurgatoryRules.rewardRunes(r.floor, r.players.size()));
				if (p != null) {
					pay(p, o);
				} else {
					d.owed.computeIfAbsent(id, k -> new ArrayList<>()).add(o);
				}
			}
			if (p != null) {
				p.sendSystemMessage(Component.literal(result).withStyle(won ? ChatFormatting.GOLD : ChatFormatting.RED));
			}
		}
		for (Map.Entry<String, Integer> w : d.watching.entrySet()) {
			if (w.getValue() == r.id) {
				ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(w.getKey()));
				if (p != null) {
					p.sendSystemMessage(Component.literal(result));
				}
			}
		}
		d.changed();
	}

	/** Removes the run, empties and clears its dungeon and sends fighters and watchers back. */
	private static void close(MinecraftServer server, PurgatorySavedData d, Run r) {
		d.runs.remove(r);
		ServerBossEvent bar = BARS.remove(r.id);
		if (bar != null) {
			bar.removeAllPlayers();
		}
		ServerLevel level = level(server);
		for (Entity e : level.getEntities((Entity) null, inside(r.slot).inflate(2), e -> !(e instanceof Player))) {
			e.discard(); // mobs, drops, arrows: the box is emptied
		}
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (p != null) {
				sendBack(server, d, p);
			} // offline: sent back when they join (the back point stays saved)
		}
		for (Map.Entry<String, Integer> w : new ArrayList<>(d.watching.entrySet())) {
			if (w.getValue() == r.id) {
				ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(w.getKey()));
				if (p != null) {
					sendBack(server, d, p);
				}
			}
		}
		if (r.seed != 0) {
			DungeonBuilder.clear(level, PurgatoryFloor.dungeon(r), r.slot);
		} else {
			DungeonBuilder.clearOldBox(level, r.slot);
		}
		PurgatoryFloor.forget(r);
		d.changed();
	}

	private static void pay(ServerPlayer p, Owed o) {
		p.giveExperienceLevels(o.levels());
		for (int i = 0; i < o.runes(); i++) {
			ItemStack rune = Runes.randomReward(p.level().registryAccess(), RNG);
			if (!rune.isEmpty() && !p.getInventory().add(rune)) {
				p.spawnAtLocation(p.level(), rune);
			}
		}
		p.sendSystemMessage(Component.literal("Reward: " + o.levels() + " levels and " + o.runes() + " rune" + (o.runes() == 1 ? "" : "s"))
			.withStyle(ChatFormatting.GOLD));
		p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
	}

	private static Back backOf(ServerPlayer p) {
		return new Back(p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(),
			p.gameMode.getGameModeForPlayer().getName());
	}

	/** Back to the saved spot and game mode; forgets the watch. */
	static void sendBack(MinecraftServer server, PurgatorySavedData d, ServerPlayer p) {
		d.watching.remove(key(p));
		Back b = d.back.remove(key(p));
		d.changed();
		if (b == null) {
			return;
		}
		ServerLevel to = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(b.dimension())));
		if (to == null) {
			to = server.overworld();
		}
		p.stopRiding();
		p.setGameMode(GameType.byName(b.gameMode(), GameType.SURVIVAL));
		p.teleportTo(to, b.x(), b.y(), b.z(), Set.of(), b.yRot(), b.xRot(), false);
		p.setDeltaMovement(Vec3.ZERO);
		p.resetFallDistance();
		p.clearFire();
	}

	// ---- tick ----

	private static void tick(MinecraftServer server) {
		if (!ImmortalCultivation.enabled(server)) {
			return;
		}
		PurgatorySavedData d = data(server);
		for (UUID u : new ArrayList<>(JOINED)) {
			ServerPlayer p = server.getPlayerList().getPlayer(u);
			if (p != null) {
				onJoin(server, d, p);
			}
		}
		JOINED.clear();
		for (UUID u : new ArrayList<>(DIED)) {
			ServerPlayer p = server.getPlayerList().getPlayer(u);
			if (p != null) {
				onDied(server, d, p);
			}
		}
		DIED.clear();
		for (Run r : new ArrayList<>(d.runs)) {
			tickRun(server, d, r);
		}
		tickWatchers(server, d);
		tickQueue(server, d);
		tickCooldowns(server, d);
		if (server.getTickCount() % 20 == 0) {
			PurgatoryNet.sync(server, d);
			if (server.getTickCount() % 200 == 0) {
				d.changed();
			}
		}
	}

	private static void onJoin(MinecraftServer server, PurgatorySavedData d, ServerPlayer p) {
		List<Owed> owed = d.owed.remove(key(p));
		if (owed != null) {
			owed.forEach(o -> pay(p, o));
			d.changed();
		}
		Run mine = d.runOf(key(p));
		Integer watched = d.watching.get(key(p));
		if (mine == null && (watched == null || d.run(watched) == null) && d.back.containsKey(key(p))) {
			sendBack(server, d, p); // the floor ended while they were away
		}
	}

	private static void onDied(MinecraftServer server, PurgatorySavedData d, ServerPlayer p) {
		Run r = d.runOf(key(p));
		if (r == null || !r.alive(key(p))) {
			return;
		}
		if (!r.team) {
			end(server, d, r, false);
			return;
		}
		r.dead.add(key(p));
		p.setGameMode(GameType.SPECTATOR); // D237: watches the rest of the floor inside the box
		tellRun(server, r, p.getName().getString() + " fell");
		if (aliveCount(r) == 0) {
			end(server, d, r, false);
		}
		d.changed();
	}

	private static int aliveCount(Run r) {
		int n = 0;
		for (String id : r.players) {
			n += r.dead.contains(id) ? 0 : 1;
		}
		return n;
	}

	private static List<ServerPlayer> onlineAlive(MinecraftServer server, Run r) {
		List<ServerPlayer> out = new ArrayList<>();
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (p != null && !r.dead.contains(id)) {
				out.add(p);
			}
		}
		return out;
	}

	static void tellRun(MinecraftServer server, Run r, String text) {
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (p != null) {
				p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GRAY));
			}
		}
	}

	private static void tickRun(MinecraftServer server, PurgatorySavedData d, Run r) {
		if (r.seed == 0) {
			end(server, d, r, false); // saved before the dungeons (P-082): nothing lost
			return;
		}
		ServerLevel level = level(server);
		AABB in = inside(r.slot);
		List<ServerPlayer> alive = onlineAlive(server, r);
		// keep everyone of this dungeon inside it
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (p != null) {
				keepInside(level, r, in, p, !r.dead.contains(id));
			}
		}
		if (r.boss == Run.BOSS_DEAD) {
			if (!r.won()) {
				win(server, d, level, r);
			} else if (--r.victoryTicks <= 0) {
				close(server, d, r);
				return;
			}
			if (server.getTickCount() % 20 == 0) {
				bar(server, d, r);
			}
			return; // D260: no clock, no mobs; the winners look around or leave
		}
		if (alive.isEmpty()) {
			return; // D236: the clock stops while the fighters are offline
		}
		r.ticksLeft--;
		if (r.ticksLeft <= 0) {
			end(server, d, r, false); // D255: the boss must die within the time
			return;
		}
		PurgatoryFloor.tick(server, level, r, alive);
		if (server.getTickCount() % 20 == 0) {
			bar(server, d, r);
			retarget(level, in, alive);
		}
	}

	/**
	 * D260: the boss is dead. The floor is paid at once, the mobs go, every gate opens; the fighters stay 30 seconds (loot,
	 * the treasure room) unless they press Come back now.
	 */
	private static void win(MinecraftServer server, PurgatorySavedData d, ServerLevel level, Run r) {
		settle(server, d, r, true);
		r.victoryTicks = DungeonRules.VICTORY_TICKS;
		for (Mob m : level.getEntitiesOfClass(Mob.class, inside(r.slot), m -> m.entityTags().contains(TAG))) {
			m.discard();
		}
		PurgatoryFloor.openAll(level, r);
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (p != null) {
				p.sendSystemMessage(Component.literal("Going back in " + DungeonRules.VICTORY_TICKS / 20
					+ " seconds (Come back now in the inventory leaves at once)").withStyle(ChatFormatting.GRAY));
			}
		}
		d.changed();
	}

	private static void keepInside(ServerLevel level, Run r, AABB in, ServerPlayer p, boolean fighter) {
		if (fighter && p.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
			p.setGameMode(GameType.ADVENTURE); // D256: no breaking or placing blocks in the dungeon
		}
		if (!fighter && !p.isSpectator()) {
			p.setGameMode(GameType.SPECTATOR);
		}
		if (p.level() == level && in.contains(p.position())) {
			return;
		}
		Vec3 c = start(r);
		p.stopRiding();
		p.teleportTo(level, c.x, c.y + (fighter ? 0 : 2), c.z, Set.of(), p.getYRot(), p.getXRot(), false);
		p.setDeltaMovement(Vec3.ZERO);
		p.resetFallDistance();
	}

	private static void tickWatchers(MinecraftServer server, PurgatorySavedData d) {
		ServerLevel level = level(server);
		for (Map.Entry<String, Integer> w : new ArrayList<>(d.watching.entrySet())) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(w.getKey()));
			if (p == null) {
				continue;
			}
			Run r = d.run(w.getValue());
			if (r == null) {
				sendBack(server, d, p);
				continue;
			}
			keepInside(level, r, inside(r.slot), p, false);
		}
	}

	private static void tickQueue(MinecraftServer server, PurgatorySavedData d) {
		QUEUE.removeIf(u -> {
			ServerPlayer p = server.getPlayerList().getPlayer(u);
			return p == null || canEnter(p) != null;
		});
		if (QUEUE.size() < PurgatoryRules.TEAM_MIN_PLAYERS || d.teamCooldown > 0 || teamRun(d) != null) {
			countdown = -1;
			return;
		}
		if (countdown < 0) {
			countdown = PurgatoryRules.TEAM_COUNTDOWN_TICKS;
		}
		int online = server.getPlayerList().getPlayers().size();
		if (QUEUE.size() >= online || --countdown <= 0) {
			List<ServerPlayer> team = new ArrayList<>();
			for (UUID u : QUEUE) {
				team.add(server.getPlayerList().getPlayer(u));
			}
			QUEUE.clear();
			countdown = -1;
			begin(server, d, true, d.teamFloor, team);
		}
	}

	private static void tickCooldowns(MinecraftServer server, PurgatorySavedData d) {
		if (d.teamCooldown > 0 && teamRun(d) == null) {
			d.teamCooldown--;
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Solo s = d.solo.get(key(p));
			if (s != null && s.cooldown > 0) {
				Run r = d.runOf(key(p));
				if (r == null || r.team) {
					s.cooldown--; // D236: only while online
				}
			}
		}
	}

	static Run teamRun(PurgatorySavedData d) {
		for (Run r : d.runs) {
			if (r.team) {
				return r;
			}
		}
		return null;
	}

	// ---- boss bar ----

	private static void bar(MinecraftServer server, PurgatorySavedData d, Run r) {
		ServerBossEvent bar = BARS.computeIfAbsent(r.id, k -> new ServerBossEvent(UUID.randomUUID(), Component.empty(),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10));
		if (r.won()) {
			bar.setColor(BossEvent.BossBarColor.YELLOW);
			bar.setName(Component.literal((r.team ? "Team " : "") + "Purgatory floor " + r.floor + " cleared  -  back in "
				+ PurgatoryRules.clock(r.victoryTicks)).withStyle(ChatFormatting.GOLD));
			bar.setProgress(Math.max(0f, Math.min(1f, r.victoryTicks / (float) DungeonRules.VICTORY_TICKS)));
		} else {
			bar.setName(Component.literal((r.team ? "Team " : "") + "Purgatory floor " + r.floor + "  -  " + PurgatoryRules.clock(r.ticksLeft))
				.withStyle(ChatFormatting.DARK_RED));
			bar.setProgress(Math.max(0f, Math.min(1f, r.ticksLeft / (float) PurgatoryRules.RUN_TICKS)));
		}
		Set<ServerPlayer> want = new HashSet<>();
		for (String id : r.players) {
			ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
			if (p != null) {
				want.add(p);
			}
		}
		d.watching.forEach((id, run) -> {
			ServerPlayer p = run == r.id ? server.getPlayerList().getPlayer(UUID.fromString(id)) : null;
			if (p != null) {
				want.add(p);
			}
		});
		for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
			if (!want.contains(p)) {
				bar.removePlayer(p);
			}
		}
		for (ServerPlayer p : want) {
			bar.addPlayer(p); // a respawned or rejoined player is a new object
		}
		PurgatoryFloor.bossBar(server, level(server), r, want);
	}

	// ---- mobs ----

	/**
	 * One floor mob at a feet position, or null when it does not fit there. health = extra health factor (bosses), name = a
	 * shown name (bosses) or null.
	 */
	static Mob spawnMob(ServerLevel level, Run r, String type, Vec3 at, List<ServerPlayer> alive, double health, String name) {
		EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(type));
		Entity e = et.create(level, EntitySpawnReason.EVENT);
		if (!(e instanceof Mob m)) {
			if (e != null) {
				e.discard();
			}
			return null;
		}
		m.snapTo(at.x, at.y, at.z, RNG.nextFloat() * 360f, 0f);
		if (!level.noCollision(m)) {
			m.discard();
			return null;
		}
		BlockPos pos = BlockPos.containing(at);
		m.addTag(TAG);
		m.addTag(DangerMobs.APPLIED_TAG); // the danger level does not touch Purgatory mobs: the floor decides
		m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		strengthen(m, r.floor, r.team ? r.players.size() : 1, health, name);
		if (name == null && RNG.nextDouble() < PurgatoryRules.eliteChance(r.floor)) {
			DangerMobs.makeElite(m, DangerRules.rollAffixes(Math.min(DangerRules.MAX_LEVEL, 20 + r.floor), new java.util.Random(RNG.nextLong())));
		}
		m.setPersistenceRequired();
		level.addFreshEntity(m);
		ServerPlayer target = nearest(m, alive);
		if (target != null) {
			m.setTarget(target);
		}
		return m;
	}

	/** Floor strength on top of the vanilla mob (the numbers are in PurgatoryRules). */
	public static void strengthen(Mob m, int floor, int players, double extraHealth, String name) {
		double health = PurgatoryRules.healthMultiplier(floor) * PurgatoryRules.teamHealthFactor(players) * extraHealth;
		add(m.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, health - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		add(m.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, PurgatoryRules.damageMultiplier(floor) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		add(m.getAttribute(Attributes.ARMOR), ARMOR_ID, PurgatoryRules.armorBonus(floor), AttributeModifier.Operation.ADD_VALUE);
		add(m.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, PurgatoryRules.speedBonus(floor), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		AttributeInstance follow = m.getAttribute(Attributes.FOLLOW_RANGE);
		if (follow != null) {
			follow.setBaseValue(Math.max(follow.getBaseValue(), 96.0));
		}
		if (name != null) {
			add(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE), BOSS_ID, 0.6, AttributeModifier.Operation.ADD_VALUE);
			m.setCustomName(Component.literal(name).withStyle(ChatFormatting.DARK_RED));
			m.setCustomNameVisible(true);
			m.setGlowingTag(true);
		}
		m.setHealth(m.getMaxHealth());
	}

	private static void add(AttributeInstance inst, Identifier id, double amount, AttributeModifier.Operation op) {
		if (inst != null && amount > 0 && !inst.hasModifier(id)) {
			inst.addPermanentModifier(new AttributeModifier(id, amount, op));
		}
	}

	private static ServerPlayer nearest(Entity e, List<ServerPlayer> players) {
		ServerPlayer best = null;
		double bestD = Double.MAX_VALUE;
		for (ServerPlayer p : players) {
			double dd = p.distanceToSqr(e);
			if (dd < bestD) {
				bestD = dd;
				best = p;
			}
		}
		return best;
	}

	/** Mobs that lost their target pick the nearest fighter again. */
	private static void retarget(ServerLevel level, AABB in, List<ServerPlayer> alive) {
		for (Mob m : level.getEntitiesOfClass(Mob.class, in, m -> m.entityTags().contains(TAG) && m.isAlive())) {
			if (m.getTarget() == null || !m.getTarget().isAlive() || m.getTarget() instanceof Player tp && tp.isSpectator()) {
				ServerPlayer t = nearest(m, alive);
				if (t != null) {
					m.setTarget(t);
				}
			}
		}
	}

	/** Test and command helper: ends the player's floor at once. */
	public static boolean finish(ServerPlayer p, boolean won) {
		MinecraftServer server = p.level().getServer();
		PurgatorySavedData d = data(server);
		Run r = d.runOf(key(p));
		if (r == null) {
			return false;
		}
		end(server, d, r, won);
		return true;
	}
}
