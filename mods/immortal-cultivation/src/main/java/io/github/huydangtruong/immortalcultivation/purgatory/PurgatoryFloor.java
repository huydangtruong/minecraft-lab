package io.github.huydangtruong.immortalcultivation.purgatory;

import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Run;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Dungeon;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.DungeonRules;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Layout;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Room;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.RoomKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The rooms of a Purgatory dungeon floor at play (P-082): a room wakes when a fighter walks in; locked rooms seal their doors
 * with iron bars until their mobs are dead; the boss room holds the floor's boss, whose death wins the floor.
 */
public final class PurgatoryFloor {
	public static final String BOSS_TAG = "permahearts_purgatory_boss";
	private static final Map<Integer, Dungeon> CACHE = new HashMap<>();
	private static final Map<Integer, ServerBossEvent> BOSS_BARS = new HashMap<>();
	private static final RandomSource RNG = RandomSource.create();

	private PurgatoryFloor() {}

	/** The floor of a run (the same seed always gives the same floor, so it is not saved). */
	public static Dungeon dungeon(Run r) {
		return CACHE.computeIfAbsent(r.id, k -> Layout.generate(r.seed, r.floor));
	}

	static void forget(Run r) {
		CACHE.remove(r.id);
		ServerBossEvent bar = BOSS_BARS.remove(r.id);
		if (bar != null) {
			bar.removeAllPlayers();
		}
	}

	static void forgetAll() {
		CACHE.clear();
		BOSS_BARS.values().forEach(ServerBossEvent::removeAllPlayers);
		BOSS_BARS.clear();
	}

	/** Feet position at the middle of the start room. */
	static Vec3 start(Run r) {
		return feet(r.slot, dungeon(r).start().centre);
	}

	public static Vec3 feet(int slot, int[] g) {
		BlockPos w = DungeonBuilder.world(slot, g);
		return new Vec3(w.getX() + 0.5, w.getY(), w.getZ() + 0.5);
	}

	/** The walkable inside of a room in world coordinates. */
	static AABB box(int slot, Room room) {
		BlockPos o = DungeonBuilder.origin(slot);
		return new AABB(o.getX() + room.x0() + 1, o.getY() + room.y0() + 1, o.getZ() + room.z0() + 1,
			o.getX() + room.x1() - 1, o.getY() + room.y1() - 1, o.getZ() + room.z1() - 1);
	}

	/** The room a fighter stands in, or null (in a doorway). */
	static Room roomOf(Run r, ServerPlayer p) {
		BlockPos o = DungeonBuilder.origin(r.slot);
		BlockPos b = p.blockPosition();
		return dungeon(r).roomAt(b.getX() - o.getX(), b.getY() - o.getY(), b.getZ() - o.getZ());
	}

	/** Well inside a room: past the 2 blocks just behind its doors, so a gate never shuts on the fighter. */
	private static boolean deepIn(Run r, Room room, ServerPlayer p) {
		BlockPos o = DungeonBuilder.origin(r.slot);
		int x = p.getBlockX() - o.getX();
		int z = p.getBlockZ() - o.getZ();
		return x >= room.x0() + 3 && x < room.x1() - 3 && z >= room.z0() + 3 && z < room.z1() - 3;
	}

	// ---- tick ----

	static void tick(MinecraftServer server, ServerLevel level, Run r, List<ServerPlayer> alive) {
		Dungeon d = dungeon(r);
		Map<Room, List<ServerPlayer>> where = new HashMap<>();
		for (ServerPlayer p : alive) {
			Room room = roomOf(r, p);
			if (room != null) {
				where.computeIfAbsent(room, k -> new ArrayList<>()).add(p);
			}
		}
		boolean second = server.getTickCount() % 20 == 0;
		for (Room room : d.rooms()) {
			List<ServerPlayer> in = where.getOrDefault(room, List.of());
			int state = r.room(room.index);
			if (state == Run.IDLE) {
				for (ServerPlayer p : in) {
					if (deepIn(r, room, p)) {
						wake(server, level, r, room, alive);
						break;
					}
				}
			} else if ((state == Run.SEALED || state == Run.SEALED_LAST) && room.kind != RoomKind.BOSS) {
				int t = r.tickRoom(room.index);
				if (t % 10 == 0) {
					List<Mob> mobs = mobs(level, box(r.slot, room).inflate(0.5));
					if (mobs.isEmpty() && state == Run.SEALED && DungeonRules.waves(room.kind) > 1) {
						secondWave(server, level, r, room, alive); // D259
					} else if (mobs.isEmpty() || t >= DungeonRules.OPEN_AFTER_TICKS) {
						open(level, r, room);
					} else if (t >= DungeonRules.GLOW_AFTER_TICKS) {
						mobs.forEach(m -> m.setGlowingTag(true)); // D257: the last ones are easy to find
					}
				}
			}
			if (second && !in.isEmpty()) {
				effects(level, r, room, in);
			}
		}
		if (r.boss == Run.BOSS_ALIVE) {
			tickBoss(level, r, d.boss(), alive, second);
		}
	}

	private static List<Mob> mobs(ServerLevel level, AABB box) {
		return level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m.entityTags().contains(Purgatory.TAG));
	}

	private static void wake(MinecraftServer server, ServerLevel level, Run r, Room room, List<ServerPlayer> alive) {
		int players = r.team ? r.players.size() : 1;
		if (room.kind.locks) {
			seal(level, r, room);
			r.room(room.index, Run.SEALED);
			level.playSound(null, BlockPos.containing(box(r.slot, room).getCenter()), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 2f, 0.6f);
		} else {
			r.room(room.index, Run.DONE);
		}
		if (room.kind == RoomKind.BOSS) {
			pullIn(level, r, room, alive);
			spawnBoss(level, r, room, alive);
			Purgatory.tellRun(server, r, "The boss wakes!");
		} else if (room.kind.locks) {
			Purgatory.tellRun(server, r, "The doors seal: kill every mob to open them");
		}
		spawnWave(level, r, room, alive, DungeonRules.wave(room.kind, r.floor, players));
	}

	private static void secondWave(MinecraftServer server, ServerLevel level, Run r, Room room, List<ServerPlayer> alive) {
		r.room(room.index, Run.SEALED_LAST);
		r.resetRoom(room.index);
		List<ServerPlayer> in = new ArrayList<>();
		AABB b = box(r.slot, room);
		for (ServerPlayer p : alive) {
			if (b.contains(p.position())) {
				in.add(p);
			}
		}
		level.playSound(null, BlockPos.containing(b.getCenter()), SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.HOSTILE, 2f, 0.8f);
		spawnWave(level, r, room, in.isEmpty() ? alive : in, DungeonRules.wave(room.kind, r.floor, r.team ? r.players.size() : 1));
		Purgatory.tellRun(server, r, "A second wave comes!");
	}

	/** D260: the floor is won: every sealed gate opens. */
	static void openAll(ServerLevel level, Run r) {
		for (Room room : dungeon(r).rooms()) {
			int state = r.room(room.index);
			if (state == Run.SEALED || state == Run.SEALED_LAST) {
				open(level, r, room);
			}
		}
	}

	/** Team: the fighters outside the boss room join the fight. */
	private static void pullIn(ServerLevel level, Run r, Room room, List<ServerPlayer> alive) {
		AABB b = box(r.slot, room);
		Vec3 c = feet(r.slot, room.centre);
		for (ServerPlayer p : alive) {
			if (!b.contains(p.position())) {
				p.stopRiding();
				p.teleportTo(level, c.x, c.y, c.z, Set.of(), p.getYRot(), 0f, false);
				p.setDeltaMovement(Vec3.ZERO);
				p.resetFallDistance();
			}
		}
	}

	private static void seal(ServerLevel level, Run r, Room room) {
		BlockState bars = Blocks.IRON_BARS.defaultBlockState();
		List<BlockPos> placed = new ArrayList<>();
		for (int[] g : room.gates) {
			BlockPos p = DungeonBuilder.world(r.slot, g);
			if (level.getEntitiesOfClass(ServerPlayer.class, new AABB(p)).isEmpty() && level.getBlockState(p).isAir()) {
				level.setBlock(p, bars, Block.UPDATE_CLIENTS);
				placed.add(p);
			}
		}
		for (BlockPos p : placed) {
			BlockState s = level.getBlockState(p);
			BlockState shaped = Block.updateFromNeighbourShapes(s, level, p);
			if (shaped != s) {
				level.setBlock(p, shaped, Block.UPDATE_CLIENTS);
			}
		}
	}

	private static void open(ServerLevel level, Run r, Room room) {
		for (int[] g : room.gates) {
			BlockPos p = DungeonBuilder.world(r.slot, g);
			if (level.getBlockState(p).is(Blocks.IRON_BARS)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		r.room(room.index, Run.DONE);
		level.playSound(null, BlockPos.containing(box(r.slot, room).getCenter()), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 1f);
	}

	private static void spawnWave(ServerLevel level, Run r, Room room, List<ServerPlayer> alive, int n) {
		if (n <= 0 || room.spawns.isEmpty()) {
			return;
		}
		int room0 = mobs(level, Purgatory.inside(r.slot)).size();
		List<int[]> far = new ArrayList<>();
		List<int[]> near = new ArrayList<>();
		double min = PurgatoryRules.SPAWN_MIN_DISTANCE * (double) PurgatoryRules.SPAWN_MIN_DISTANCE;
		for (int[] g : room.spawns) {
			Vec3 v = feet(r.slot, g);
			boolean ok = true;
			for (ServerPlayer p : alive) {
				ok &= p.distanceToSqr(v) >= min;
			}
			(ok ? far : near).add(g);
		}
		far.addAll(near);
		List<String> pool = PurgatoryRules.mobPool(r.floor);
		int made = 0;
		for (int i = 0; made < n && i < far.size() && room0 + made < PurgatoryRules.MAX_ALIVE; i++) {
			String type = pool.get(RNG.nextInt(pool.size()));
			if (Purgatory.spawnMob(level, r, type, feet(r.slot, far.get(i)), alive, 1.0, null) != null) {
				made++;
			}
		}
	}

	// ---- boss ----

	private static void spawnBoss(ServerLevel level, Run r, Room room, List<ServerPlayer> alive) {
		boolean warden = r.floor % PurgatoryRules.WARDEN_EVERY == 0;
		String type = warden ? PurgatoryRules.wardenType(r.floor) : DungeonRules.boss(r.floor, RNG.nextInt(1000));
		double health = DungeonRules.BOSS_HEALTH * (warden ? 1.5 : 1.0);
		Mob m = Purgatory.spawnMob(level, r, type, feet(r.slot, room.boss), alive, health, warden ? "Floor Warden" : "Floor Boss");
		if (m == null) {
			m = Purgatory.spawnMob(level, r, type, feet(r.slot, room.centre), alive, health, warden ? "Floor Warden" : "Floor Boss");
		}
		if (m != null) {
			m.addTag(BOSS_TAG);
			r.boss = Run.BOSS_ALIVE;
			r.spawnTimer = DungeonRules.BOSS_ADDS_EVERY_TICKS;
		}
	}

	private static Mob findBoss(ServerLevel level, Run r) {
		List<Mob> found = level.getEntitiesOfClass(Mob.class, Purgatory.inside(r.slot), m -> m.entityTags().contains(BOSS_TAG));
		return found.isEmpty() ? null : found.get(0);
	}

	private static void tickBoss(ServerLevel level, Run r, Room room, List<ServerPlayer> alive, boolean second) {
		if (--r.spawnTimer <= 0) {
			r.spawnTimer = DungeonRules.BOSS_ADDS_EVERY_TICKS;
			spawnWave(level, r, room, alive, DungeonRules.bossAdds(r.floor, r.team ? r.players.size() : 1));
		}
		if (second && findBoss(level, r) == null) {
			spawnBoss(level, r, room, alive); // lost somehow (never by a kill: that wins at once)
		}
	}

	/** The run whose dungeon holds a dead boss wins (called from the death event). */
	static void bossDied(MinecraftServer server, Mob boss) {
		for (Run r : Purgatory.data(server).runs) {
			if (r.boss == Run.BOSS_ALIVE && Purgatory.inside(r.slot).contains(boss.position())) {
				r.boss = Run.BOSS_DEAD;
			}
		}
	}

	static void bossBar(MinecraftServer server, ServerLevel level, Run r, Set<ServerPlayer> viewers) {
		Mob boss = r.boss == Run.BOSS_ALIVE ? findBoss(level, r) : null;
		if (boss == null) {
			ServerBossEvent bar = BOSS_BARS.remove(r.id);
			if (bar != null) {
				bar.removeAllPlayers();
			}
			return;
		}
		ServerBossEvent bar = BOSS_BARS.computeIfAbsent(r.id, k -> new ServerBossEvent(UUID.randomUUID(), Component.empty(),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS));
		bar.setName(boss.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
		bar.setProgress(Math.max(0f, Math.min(1f, boss.getHealth() / boss.getMaxHealth())));
		for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
			if (!viewers.contains(p)) {
				bar.removePlayer(p);
			}
		}
		viewers.forEach(bar::addPlayer);
	}

	// ---- room effects ----

	private static void effects(ServerLevel level, Run r, Room room, List<ServerPlayer> in) {
		switch (room.kind) {
			case DARK -> in.forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false)));
			case REST -> in.forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 1, true, true)));
			case TRAP -> traps(level, r, room, in);
			default -> {
			}
		}
	}

	/** A fighter on a pressure plate makes every dispenser of the corridor shoot at them. */
	private static void traps(ServerLevel level, Run r, Room room, List<ServerPlayer> in) {
		Set<BlockPos> plates = new HashSet<>();
		for (int[] g : room.plates) {
			plates.add(DungeonBuilder.world(r.slot, g));
		}
		for (ServerPlayer p : in) {
			if (!plates.contains(p.blockPosition())) {
				continue;
			}
			for (int[] g : room.dispensers) {
				BlockPos b = DungeonBuilder.world(r.slot, g);
				Direction f = switch (g[3]) {
					case 0 -> Direction.NORTH;
					case 1 -> Direction.EAST;
					case 2 -> Direction.SOUTH;
					default -> Direction.WEST;
				};
				Vec3 from = Vec3.atCenterOf(b).add(f.getStepX() * 0.7, 0, f.getStepZ() * 0.7);
				Vec3 to = p.getEyePosition().subtract(from);
				Arrow a = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
				a.pickup = AbstractArrow.Pickup.DISALLOWED;
				a.setBaseDamage(2.0 + r.floor * 0.15);
				a.shoot(to.x, to.y + to.horizontalDistance() * 0.1, to.z, 1.4f, 6f);
				level.addFreshEntity(a);
			}
			level.playSound(null, p.blockPosition(), SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1f, 1f);
		}
	}
}
