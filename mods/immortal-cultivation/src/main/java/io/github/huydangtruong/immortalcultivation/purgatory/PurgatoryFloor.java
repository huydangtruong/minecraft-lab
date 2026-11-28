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
	/** D265: the Seal Pillar (its hit box mob and the blocks it looks like). */
	public static final String PILLAR_TAG = "permahearts_purgatory_pillar";
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
	public static Room roomOf(Run r, ServerPlayer p) {
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
		boolean tenth = server.getTickCount() % 10 == 0;
		for (Room room : d.rooms()) {
			List<ServerPlayer> in = where.getOrDefault(room, List.of());
			int state = r.room(room.index);
			if (Run.sealed(state) && tenth) {
				closeWhenClear(level, r, d, room, alive); // D283: the doors shut once no teammate is left just outside
			}
			if (state == Run.IDLE && room.kind == RoomKind.BOSS && roomsLeftBeforeBoss(r, d) > 0) {
				bossGateShut(level, r, d, room, alive, in, tenth, second); // D284
			} else if (state == Run.IDLE) {
				if (room.kind == RoomKind.BOSS && tenth) {
					bossGateOpen(server, level, r, room);
				}
				for (ServerPlayer p : in) {
					if (deepIn(r, room, p)) {
						wake(server, level, r, room, alive);
						break;
					}
				}
			} else if (state == Run.SEALED && room.kind == RoomKind.PILLAR) {
				tickPillar(server, level, r, room, alive);
			} else if (Run.sealed(state) && room.kind != RoomKind.BOSS) {
				int t = r.tickRoom(room.index);
				if (t % 10 == 0) {
					List<Mob> mobs = mobs(level, box(r.slot, room).inflate(0.5));
					int waves = DungeonRules.waves(room.kind);
					int sent = Run.wavesSent(state, waves);
					if (mobs.isEmpty() && sent < waves) {
						nextWave(server, level, r, room, alive, sent + 1, waves); // D259, D285
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
		boolean shut = true;
		if (room.kind.locks) {
			r.room(room.index, Run.SEALED);
			if (room.kind == RoomKind.BOSS) {
				pullIn(level, r, room, alive);
			}
			shut = closeWhenClear(level, r, dungeon(r), room, alive);
		} else {
			r.room(room.index, Run.DONE);
		}
		String later = shut ? "" : " (they shut once every fighter just outside is in)";
		if (room.kind == RoomKind.BOSS) {
			spawnBoss(level, r, room, alive);
			Purgatory.tellRun(server, r, "The boss wakes!");
		} else if (room.kind == RoomKind.PILLAR) {
			spawnPillar(level, r, room);
			Purgatory.tellRun(server, r, "The doors seal: destroy the glowing Seal Pillar to open them!" + later);
		} else if (room.kind == RoomKind.GRAND) {
			Purgatory.tellRun(server, r, "The great hall seals: survive " + DungeonRules.GRAND_WAVES + " waves!" + later);
		} else if (room.kind.locks) {
			Purgatory.tellRun(server, r, "The doors seal: kill every mob to open them" + later);
		}
		spawnWave(level, r, room, alive, DungeonRules.wave(room.kind, r.floor, players));
	}

	/**
	 * D283: bars a sealed room's open gates unless a living fighter is still just outside it (in a room joined to it by a
	 * door, or in its doorways), so nobody is shut out of the fight or locked in the room behind. True when every gate is
	 * barred (or there was nothing to bar).
	 */
	private static boolean closeWhenClear(ServerLevel level, Run r, Dungeon d, Room room, List<ServerPlayer> alive) {
		boolean open = false;
		for (int[] g : room.gates) {
			if (level.getBlockState(DungeonBuilder.world(r.slot, g)).isAir()) {
				open = true;
				break;
			}
		}
		if (!open) {
			return true;
		}
		if (room.kind != RoomKind.BOSS && fighterJustOutside(r, d, room, alive)) {
			return false;
		}
		if (seal(level, r, room) > 0) {
			level.playSound(null, BlockPos.containing(box(r.slot, room).getCenter()), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 2f, 0.6f);
		}
		return true;
	}

	/** D283: a living fighter stands in a room joined to this one by a door, or in its doorways. */
	static boolean fighterJustOutside(Run r, Dungeon d, Room room, List<ServerPlayer> alive) {
		List<Room> next = d.neighbours(room);
		AABB doorways = box(r.slot, room).inflate(2.0, 0, 2.0);
		for (ServerPlayer p : alive) {
			Room at = roomOf(r, p);
			if (at == room) {
				continue;
			}
			if (at == null ? doorways.contains(p.position()) : next.contains(at)) {
				return true;
			}
		}
		return false;
	}

	/** D284: sealing rooms of the floor (the boss room aside) that are not cleared yet. */
	static int roomsLeftBeforeBoss(Run r, Dungeon d) {
		List<RoomKind> kinds = new ArrayList<>();
		List<Boolean> done = new ArrayList<>();
		for (Room room : d.rooms()) {
			kinds.add(room.kind);
			done.add(r.room(room.index) == Run.DONE);
		}
		return DungeonRules.roomsLeftBeforeBoss(kinds, done);
	}

	/**
	 * D284: the boss room stays barred while sealing rooms are left: its door is barred, a fighter who got in anyway is put
	 * back in the room before it, and fighters near the door are told how many rooms are left.
	 */
	private static void bossGateShut(ServerLevel level, Run r, Dungeon d, Room boss, List<ServerPlayer> alive, List<ServerPlayer> in,
			boolean tenth, boolean second) {
		if (tenth) {
			seal(level, r, boss);
		}
		List<Room> next = d.neighbours(boss);
		for (ServerPlayer p : in) {
			if (!next.isEmpty() && next.get(0).centre != null) {
				Vec3 c = feet(r.slot, next.get(0).centre);
				p.stopRiding();
				p.teleportTo(level, c.x, c.y, c.z, Set.of(), p.getYRot(), p.getXRot(), false);
				p.setDeltaMovement(Vec3.ZERO);
				p.resetFallDistance();
			}
		}
		if (second) {
			int left = roomsLeftBeforeBoss(r, d);
			AABB near = box(r.slot, boss).inflate(8.0, 2.0, 8.0);
			for (ServerPlayer p : alive) {
				if (near.contains(p.position())) {
					p.sendOverlayMessage(Component.literal("The boss room is sealed: clear " + left + " more sealed room" + (left == 1 ? "" : "s"))
						.withStyle(ChatFormatting.RED));
				}
			}
		}
	}

	/** D284: every sealing room is cleared: the boss room's bars go (once). */
	private static void bossGateOpen(MinecraftServer server, ServerLevel level, Run r, Room boss) {
		boolean barred = false;
		for (int[] g : boss.gates) {
			BlockPos p = DungeonBuilder.world(r.slot, g);
			if (level.getBlockState(p).is(Blocks.IRON_BARS)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				barred = true;
			}
		}
		if (barred) {
			level.playSound(null, BlockPos.containing(box(r.slot, boss).getCenter()), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.8f);
			Purgatory.tellRun(server, r, "Every sealed room is cleared: the boss room opens!");
		}
	}

	private static void nextWave(MinecraftServer server, ServerLevel level, Run r, Room room, List<ServerPlayer> alive, int wave, int waves) {
		r.room(room.index, Run.afterWave(wave, waves));
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
		Purgatory.tellRun(server, r, waves == 2 ? "A second wave comes!" : "Wave " + wave + " of " + waves + " comes!");
	}

	// ---- Seal Pillar (D265) ----

	private static final net.minecraft.resources.Identifier PILLAR_HEALTH_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(
		io.github.huydangtruong.immortalcultivation.ImmortalCultivation.NAMESPACE, "purgatory_pillar_health");

	/**
	 * A 2-block pillar of crying obsidian and a charged respawn anchor (block displays, glowing so it is found through walls)
	 * around a still, silent wither skeleton that takes the hits (30 hearts). It never dies: at 0 health it shatters.
	 */
	private static void spawnPillar(ServerLevel level, Run r, Room room) {
		int[] g = room.pillar != null ? room.pillar : room.centre;
		Vec3 at = feet(r.slot, g);
		if (!level.getEntitiesOfClass(Mob.class, box(r.slot, room), m -> m.entityTags().contains(PILLAR_TAG)).isEmpty()) {
			return;
		}
		if (!(net.minecraft.world.entity.EntityTypes.WITHER_SKELETON.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT)
				instanceof Mob m)) {
			return;
		}
		m.snapTo(at.x, at.y, at.z, 0f, 0f);
		m.setNoAi(true);
		m.setSilent(true);
		m.setPersistenceRequired();
		m.addTag(PILLAR_TAG);
		m.addTag(io.github.huydangtruong.immortalcultivation.danger.DangerMobs.APPLIED_TAG);
		var health = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		if (health != null) {
			health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(PILLAR_HEALTH_ID,
				DungeonRules.PILLAR_HEALTH - health.getBaseValue(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
		}
		var push = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
		if (push != null) {
			push.setBaseValue(1.0);
		}
		var scale = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(0.82); // 2 blocks tall, hidden inside the blocks
		}
		m.setHealth(m.getMaxHealth());
		m.setCustomNameVisible(true);
		pillarName(m);
		level.addFreshEntity(m);
		pillarBlock(level, at, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 0f);
		pillarBlock(level, at, Blocks.RESPAWN_ANCHOR.defaultBlockState()
			.setValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE, 4), 1f);
		level.playSound(null, BlockPos.containing(at), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 2f, 0.7f);
	}

	private static void pillarBlock(ServerLevel level, Vec3 at, BlockState state, float y) {
		if (!(net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)
				instanceof net.minecraft.world.entity.Display.BlockDisplay d)) {
			return;
		}
		((io.github.huydangtruong.immortalcultivation.mixin.BlockDisplayAccess) d).permahearts$setBlockState(state);
		((io.github.huydangtruong.immortalcultivation.mixin.DisplayAccess) d).permahearts$setTransformation(new com.mojang.math.Transformation(
			new org.joml.Vector3f(-0.45f, y, -0.45f), new org.joml.Quaternionf(), new org.joml.Vector3f(0.9f, 1f, 0.9f), new org.joml.Quaternionf()));
		d.setPos(at.x, at.y, at.z);
		d.setNoGravity(true);
		d.addTag(PILLAR_TAG);
		d.setGlowingTag(true);
		level.addFreshEntity(d);
	}

	private static void pillarName(Mob m) {
		int hearts = (int) Math.ceil(m.getHealth() / 2.0);
		m.setCustomName(Component.literal("Seal Pillar ").withStyle(ChatFormatting.LIGHT_PURPLE)
			.append(Component.literal("\u2764 " + hearts).withStyle(ChatFormatting.RED)));
	}

	static boolean isPillar(net.minecraft.world.entity.Entity e) {
		return e.entityTags().contains(PILLAR_TAG);
	}

	/** The pillar's health hit 0 (death event): it shatters instead of dying, no drops. */
	static void shatter(ServerLevel level, Mob m) {
		Vec3 at = m.position();
		for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null,
				new AABB(at, at).inflate(1.0, 3.0, 1.0), PurgatoryFloor::isPillar)) {
			e.discard();
		}
		m.discard();
		level.playSound(null, BlockPos.containing(at), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 2f, 0.8f);
		level.playSound(null, BlockPos.containing(at), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2f, 0.6f);
		level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
			Blocks.CRYING_OBSIDIAN.defaultBlockState()), at.x, at.y + 1, at.z, 60, 0.4, 0.8, 0.4, 0.1);
	}

	private static void tickPillar(MinecraftServer server, ServerLevel level, Run r, Room room, List<ServerPlayer> alive) {
		int t = r.tickRoom(room.index);
		if (t % 10 != 0) {
			return;
		}
		AABB b = box(r.slot, room).inflate(0.5);
		List<Mob> pillar = level.getEntitiesOfClass(Mob.class, b, m -> isPillar(m) && m.isAlive());
		if (pillar.isEmpty()) {
			for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, b, PurgatoryFloor::isPillar)) {
				e.discard();
			}
			open(level, r, room);
			Purgatory.tellRun(server, r, "The seal breaks: the doors open");
			return;
		}
		Mob m = pillar.get(0);
		Vec3 home = feet(r.slot, room.pillar != null ? room.pillar : room.centre);
		if (m.position().distanceToSqr(home) > 0.25) {
			m.teleportTo(home.x, home.y, home.z); // never pushed off its spot
			m.setDeltaMovement(Vec3.ZERO);
		}
		pillarName(m);
		if (t % DungeonRules.PILLAR_REINFORCE_TICKS == 0) {
			int players = r.team ? r.players.size() : 1;
			int alreadyIn = mobs(level, b).size();
			int add = Math.min(DungeonRules.reinforcements(r.floor, players), DungeonRules.wave(room.kind, r.floor, players) - alreadyIn);
			if (add > 0) {
				level.playSound(null, BlockPos.containing(home), SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.HOSTILE, 2f, 0.8f);
				spawnWave(level, r, room, alive, add);
			}
		}
	}

	/** D260: the floor is won: every sealed gate opens. */
	static void openAll(ServerLevel level, Run r) {
		for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, Purgatory.inside(r.slot),
				PurgatoryFloor::isPillar)) {
			e.discard(); // D265
		}
		for (Room room : dungeon(r).rooms()) {
			if (Run.sealed(r.room(room.index))) {
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

	/** Bars every open gate no fighter stands in; returns how many it barred. */
	private static int seal(ServerLevel level, Run r, Room room) {
		BlockState bars = Blocks.IRON_BARS.defaultBlockState();
		List<BlockPos> placed = new ArrayList<>();
		for (int[] g : room.gates) {
			BlockPos p = DungeonBuilder.world(r.slot, g);
			if (level.getBlockState(p).isAir() && level.getEntitiesOfClass(ServerPlayer.class, new AABB(p)).isEmpty()) {
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
		return placed.size();
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
			Mob m = Purgatory.spawnMob(level, r, type, feet(r.slot, far.get(i)), alive, 1.0, null);
			if (m != null) {
				fireproof(m, room);
				made++;
			}
		}
	}

	/** D290: mobs of a room with lava do not burn in it (they used to walk in and die). */
	static void fireproof(Mob m, Room room) {
		if (room.lava) {
			m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
				net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false));
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
			fireproof(m, room);
			bossBody(m);
			m.addTag(BOSS_TAG);
			r.boss = Run.BOSS_ALIVE;
			r.spawnTimer = DungeonRules.BOSS_ADDS_EVERY_TICKS;
		}
	}

	private static final net.minecraft.resources.Identifier BOSS_SIZE_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(
		io.github.huydangtruong.immortalcultivation.ImmortalCultivation.NAMESPACE, "purgatory_boss_size");
	private static final net.minecraft.resources.Identifier BOSS_HIT_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(
		io.github.huydangtruong.immortalcultivation.ImmortalCultivation.NAMESPACE, "purgatory_boss_hit");

	/** D261: twice the size, 20% more damage (on top of the floor's damage). */
	static void bossBody(Mob m) {
		var scale = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
		if (scale != null && !scale.hasModifier(BOSS_SIZE_ID)) {
			scale.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BOSS_SIZE_ID, DungeonRules.BOSS_SCALE - 1.0,
				net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		var hit = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		if (hit != null && !hit.hasModifier(BOSS_HIT_ID)) {
			hit.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BOSS_HIT_ID, DungeonRules.BOSS_DAMAGE - 1.0,
				net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
				Purgatory.bossFell(r, boss.position());
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
