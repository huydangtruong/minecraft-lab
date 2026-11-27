package io.github.huydangtruong.immortalcultivation.technique.chrono;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.mixin.ArrowChronoAccess;
import io.github.huydangtruong.immortalcultivation.passive.Passives;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Time Rewind on the server (D268). While the technique sits in a skill slot, every tick records the mobs and projectiles
 * around the caster, the blocks that change near them and the caster's health, food and armor. The cast stops time in a
 * 16-block box; the wheel ({@link ChronoNet.Scroll}) plays the recording backward and forward; a recast or the end of the
 * duration lets time run again from where it is shown, and the 140 s cooldown starts then.
 */
public final class Chrono {
	/** Per entity frame: x, y, z (from the recorder's anchor), yRot, xRot, head, body, health, vx, vy, vz, fire ticks. */
	private static final int WIDTH = 12;
	/** Per caster frame: health, food, saturation, (item id, damage) for feet, legs, chest, head, then x, y, z, fall distance. */
	private static final int SELF = 15;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
	private static final int MAX_BLOCK_CHANGES = 20_000;
	private static final double SYNC_RANGE = 192;

	private static final Map<UUID, Recorder> RECORDERS = new HashMap<>();
	/** Recorders with a running time stop, looked at by {@link #frozen} for every entity tick. */
	private static final List<Recorder> ACTIVE = new ArrayList<>();
	/** True while this class changes the world itself (its own spawns, discards and block changes are not recorded). */
	private static boolean applying;

	private Chrono() {}

	private record Gone(EntityType<?> type, CompoundTag data, int frame) {}

	private record BlockChange(int frame, BlockPos pos, BlockState from, BlockState to) {}

	private static final class Recorder {
		final ServerLevel level;
		final int lvl;
		final int capacity;
		final double ax;
		final double ay;
		final double az;
		Vec3 centre;
		int frame = -1;
		int count;
		final Map<UUID, FrameRing> tracks = new HashMap<>();
		final Map<UUID, Integer> born = new HashMap<>();
		final Map<UUID, Gone> gone = new HashMap<>();
		final List<BlockChange> blocks = new ArrayList<>();
		final FrameRing self;
		Zone zone;

		Recorder(ServerPlayer p, int lvl) {
			this.level = p.level();
			this.lvl = lvl;
			this.capacity = TechniqueRules.chronoRewindTicks(lvl) + 1;
			this.ax = Math.floor(p.getX());
			this.ay = Math.floor(p.getY());
			this.az = Math.floor(p.getZ());
			this.centre = p.position();
			this.self = new FrameRing(capacity, SELF);
		}
	}

	private static final class Zone {
		final AABB box;
		final int minX;
		final int minY;
		final int minZ;
		final long end;
		final int castFrame;
		final ChronoCursor cursor;
		final Set<UUID> held;
		final Map<UUID, Gone> hidden = new HashMap<>();
		int blockFrame;
		/** D272: the caster stands still in the stop and moves with the recording until they move or click. */
		boolean casterHeld = true;

		Zone(int[] min, long end, int castFrame, ChronoCursor cursor, Set<UUID> held) {
			this.minX = min[0];
			this.minY = min[1];
			this.minZ = min[2];
			int s = TechniqueRules.CHRONO_BOX;
			this.box = new AABB(min[0], min[1], min[2], min[0] + s, min[1] + s, min[2] + s);
			this.end = end;
			this.castFrame = castFrame;
			this.cursor = cursor;
			this.held = held;
			this.blockFrame = castFrame;
		}
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(ChronoNet.Zone.TYPE, ChronoNet.Zone.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChronoNet.Scroll.TYPE, ChronoNet.Scroll.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChronoNet.Release.TYPE, ChronoNet.Release.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ChronoNet.Scroll.TYPE, (payload, ctx) -> scroll(ctx.player(), payload.notches()));
		ServerPlayNetworking.registerGlobalReceiver(ChronoNet.Release.TYPE, (payload, ctx) -> release(ctx.player()));
		ServerTickEvents.END_SERVER_TICK.register(Chrono::tick);
		ServerEntityEvents.ENTITY_LOAD.register(Chrono::loaded);
		ServerEntityEvents.ENTITY_UNLOAD.register(Chrono::unloaded);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> drop(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			RECORDERS.clear();
			ACTIVE.clear();
		});
	}

	// ---- what is recorded ----

	/** Mobs and projectiles are rewound; players, dropped items, XP, armor stands and riders are not (D268, D270). */
	static boolean rewindable(Entity e) {
		return !(e instanceof Player) && !e.isPassenger()
			&& (e instanceof LivingEntity && !(e instanceof ArmorStand) || e instanceof Projectile);
	}

	/** Level of Time Rewind when it sits in an open slot of a player who can use it, else 0. */
	private static int slotted(ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator() || !ImmortalCultivation.enabled(p.level().getServer())) {
			return 0;
		}
		TechniqueData d = TechniqueAttachment.get(p);
		int slot = d.slotOf(Technique.CHRONO);
		return slot >= 0 && slot < d.slotsOpen() ? d.level(Technique.CHRONO) : 0;
	}

	private static void tick(MinecraftServer server) {
		long now = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Recorder r = RECORDERS.get(p.getUUID());
			int lvl = slotted(p);
			if (r != null && r.zone != null) {
				if (lvl == 0 || p.level() != r.level || now >= r.zone.end) {
					resume(p, r);
				} else {
					tickZone(p, r, now);
				}
				continue;
			}
			if (r != null && (lvl == 0 || r.level != p.level() || r.lvl != lvl)) {
				RECORDERS.remove(p.getUUID());
				r = null;
			}
			if (lvl > 0) {
				if (r == null) {
					r = new Recorder(p, lvl);
					RECORDERS.put(p.getUUID(), r);
				}
				record(p, r);
			}
		}
		RECORDERS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		ACTIVE.removeIf(r -> r.zone == null || !RECORDERS.containsValue(r));
	}

	private static void record(ServerPlayer p, Recorder r) {
		r.frame++;
		r.count = Math.min(r.count + 1, r.capacity);
		r.centre = p.position();
		float[] v = new float[WIDTH];
		for (Entity e : r.level.getEntities((Entity) null, p.getBoundingBox().inflate(TechniqueRules.CHRONO_RECORD_RADIUS), Chrono::rewindable)) {
			values(r, e, v);
			r.tracks.computeIfAbsent(e.getUUID(), k -> new FrameRing(r.capacity, WIDTH)).put(r.frame, v);
		}
		float[] sv = selfValues(p);
		sv[11] = (float) (p.getX() - r.ax);
		sv[12] = (float) (p.getY() - r.ay);
		sv[13] = (float) (p.getZ() - r.az);
		sv[14] = (float) p.fallDistance;
		r.self.put(r.frame, sv);
		if (r.frame % 20 == 0) {
			prune(r);
		}
	}

	private static void prune(Recorder r) {
		int oldest = r.frame - r.capacity + 1;
		r.tracks.values().removeIf(t -> t.last() < oldest);
		r.born.values().removeIf(f -> f < oldest);
		r.gone.values().removeIf(g -> g.frame < oldest);
		int cut = 0;
		while (cut < r.blocks.size() && (r.blocks.get(cut).frame <= oldest || r.blocks.size() - cut > MAX_BLOCK_CHANGES)) {
			cut++;
		}
		r.blocks.subList(0, cut).clear();
	}

	private static void values(Recorder r, Entity e, float[] v) {
		v[0] = (float) (e.getX() - r.ax);
		v[1] = (float) (e.getY() - r.ay);
		v[2] = (float) (e.getZ() - r.az);
		v[3] = e.getYRot();
		v[4] = e.getXRot();
		Vec3 m = e.getDeltaMovement();
		v[8] = (float) m.x;
		v[9] = (float) m.y;
		v[10] = (float) m.z;
		v[11] = e.getRemainingFireTicks();
		if (e instanceof LivingEntity le) {
			v[5] = le.getYHeadRot();
			v[6] = le.yBodyRot;
			v[7] = le.getHealth();
		} else {
			v[5] = v[3];
			v[6] = v[3];
			v[7] = -1;
		}
	}

	private static float[] selfValues(ServerPlayer p) {
		float[] v = new float[SELF];
		v[0] = p.getHealth();
		v[1] = p.getFoodData().getFoodLevel();
		v[2] = p.getFoodData().getSaturationLevel();
		for (int i = 0; i < ARMOR.length; i++) {
			ItemStack s = p.getItemBySlot(ARMOR[i]);
			boolean worn = !s.isEmpty() && s.isDamageableItem();
			v[3 + i * 2] = worn ? BuiltInRegistries.ITEM.getId(s.getItem()) : -1;
			v[4 + i * 2] = worn ? s.getDamageValue() : -1;
		}
		return v;
	}

	private static Recorder near(Level level, Vec3 pos) {
		for (Recorder r : RECORDERS.values()) {
			if (r.level == level && r.zone == null && r.centre.distanceToSqr(pos) <= sq(TechniqueRules.CHRONO_RECORD_RADIUS + 4)) {
				return r;
			}
		}
		return null;
	}

	private static double sq(double d) {
		return d * d;
	}

	private static void loaded(Entity e, ServerLevel level) {
		if (applying || RECORDERS.isEmpty() || !rewindable(e)) {
			return;
		}
		for (Recorder r : RECORDERS.values()) {
			if (r.level == level && r.zone == null && r.centre.distanceToSqr(e.position()) <= sq(TechniqueRules.CHRONO_RECORD_RADIUS + 4)) {
				r.born.put(e.getUUID(), r.frame + 1); // D268: rewound past this, it was never there
			}
		}
	}

	private static void unloaded(Entity e, ServerLevel level) {
		Entity.RemovalReason why = e.getRemovalReason();
		if (applying || RECORDERS.isEmpty() || why != Entity.RemovalReason.KILLED && why != Entity.RemovalReason.DISCARDED) {
			return;
		}
		CompoundTag data = null;
		for (Recorder r : RECORDERS.values()) {
			if (r.level == level && r.tracks.containsKey(e.getUUID())) {
				if (data == null) {
					data = save(e);
				}
				r.gone.put(e.getUUID(), new Gone(e.getType(), data, r.frame)); // D268: rewound before this, it comes back
			}
		}
	}

	/** Called by the Level mixin before a block changes on the server. */
	public static void blockChanging(Level level, BlockPos pos, BlockState next) {
		if (applying || RECORDERS.isEmpty() || !(level instanceof ServerLevel)) {
			return;
		}
		Recorder r = near(level, Vec3.atCenterOf(pos));
		if (r == null) {
			return;
		}
		BlockState prev = level.getBlockState(pos);
		if (prev == next) {
			return;
		}
		for (Recorder each : RECORDERS.values()) {
			if (each.level == level && each.zone == null && each.centre.distanceToSqr(Vec3.atCenterOf(pos)) <= sq(TechniqueRules.CHRONO_RECORD_RADIUS + 4)) {
				each.blocks.add(new BlockChange(each.frame + 1, pos.immutable(), prev, next));
			}
		}
	}

	private static CompoundTag save(Entity e) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, e.registryAccess());
		e.saveWithoutId(out);
		return out.buildResult();
	}

	// ---- the time stop ----

	/** True while the player's time stop runs. */
	public static boolean active(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		return r != null && r.zone != null;
	}

	/** Ticks the player's time stop can go back now (0 when none runs). For tests. */
	public static int usable(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		return r == null || r.zone == null ? 0 : r.zone.cursor.usable();
	}

	/** Ticks back from the cast the player's time stop shows now. For tests. */
	public static int shown(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		return r == null || r.zone == null ? 0 : r.zone.cursor.shown();
	}

	/** Ticks recorded so far for the player (0 when nothing records). For tests and the cast check. */
	public static int recorded(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		return r == null ? 0 : r.count;
	}

	/** Stops time around the caster, or lets it run again when it is already stopped. */
	public static boolean cast(ServerPlayer p, int lvl) {
		Recorder r = RECORDERS.get(p.getUUID());
		if (r != null && r.zone != null) {
			resume(p, r);
			return true;
		}
		if (r == null || r.level != p.level()) {
			r = new Recorder(p, lvl); // cast by the op command without a slot: nothing to rewind yet
			RECORDERS.put(p.getUUID(), r);
			record(p, r);
		}
		ServerLevel sl = p.level();
		long now = sl.getServer().getTickCount();
		int[] min = TechniqueRules.chronoBoxMin(p.getX(), p.getY(), p.getZ());
		Zone z = new Zone(min, now + TechniqueRules.chronoDurationTicks(lvl), r.frame,
			new ChronoCursor(ChronoCursor.usable(r.count - 1, lvl)), new HashSet<>());
		for (Entity e : sl.getEntities((Entity) null, z.box, Chrono::rewindable)) {
			z.held.add(e.getUUID());
		}
		float[] v = new float[WIDTH];
		for (Map.Entry<UUID, Gone> g : r.gone.entrySet()) {
			FrameRing t = r.tracks.get(g.getKey());
			if (t != null && t.get(t.last(), v) && z.box.contains(r.ax + v[0], r.ay + v[1], r.az + v[2])) {
				z.held.add(g.getKey()); // died in the box a moment ago: rewinding brings it back
			}
		}
		r.zone = z;
		ACTIVE.add(r);
		sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 0.5f);
		sl.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8f, 1.6f);
		broadcast(p, r, now);
		return true;
	}

	/** True while the player's time stop still holds them (they have not moved or clicked yet). For tests. */
	public static boolean held(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		return r != null && r.zone != null && r.zone.casterHeld;
	}

	/** D272: the caster moved or clicked: they leave the stop and act again, the box stays stopped. */
	public static void release(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		if (r != null && r.zone != null && r.zone.casterHeld) {
			r.zone.casterHeld = false;
			broadcast(p, r, r.level.getServer().getTickCount());
		}
	}

	/** Wheel notches from the caster (positive = back). */
	public static void scroll(ServerPlayer p, int notches) {
		Recorder r = RECORDERS.get(p.getUUID());
		if (r != null && r.zone != null && notches != 0) {
			r.zone.cursor.scroll(Math.max(-10, Math.min(10, notches)));
		}
	}

	private static void tickZone(ServerPlayer p, Recorder r, long now) {
		Zone z = r.zone;
		if (z.cursor.step()) {
			show(p, r, z.castFrame - z.cursor.shown());
		}
		// frozen mobs can be hit again and again (their hurt cooldown would count down in their own tick)
		for (LivingEntity e : r.level.getEntitiesOfClass(LivingEntity.class, z.box.inflate(8), e -> frozen(e))) {
			if (e.damageCooldownTime > 0) {
				e.damageCooldownTime--;
			}
			if (e.hurtTime > 0) {
				e.hurtTime--;
			}
		}
		if (now % 10 == 0) {
			RandomEdge.particles(r.level, z.box);
		}
		broadcast(p, r, now);
	}

	/** Shows the recording at a frame: mobs, projectiles, blocks of the box and the caster's health, food and armor. */
	private static void show(ServerPlayer p, Recorder r, int f) {
		Zone z = r.zone;
		applying = true;
		try {
			showBlocks(r, z, f);
			float[] v = new float[WIDTH];
			for (UUID id : z.held) {
				FrameRing track = r.tracks.get(id);
				Integer born = r.born.get(id);
				Gone gone = r.gone.get(id);
				boolean there = (born == null || f >= born) && (gone == null || f <= gone.frame);
				Entity e = r.level.getEntity(id);
				if (there) {
					if (e == null) {
						Gone src = z.hidden.remove(id);
						e = recreate(r.level, src != null ? src : gone);
					}
					if (e != null && track != null && (track.get(f, v) || f < track.first() && track.get(track.first(), v))) {
						if (e instanceof LivingEntity le && v[7] > 0 && (le.isDeadOrDying() || le.deathTime > 0)) {
							e = fresh(r.level, e); // D272: a living copy, so clients drop the dying (lying, red) one
						}
						if (e != null) {
							apply(r, e, v);
						}
					}
				} else if (e != null) {
					if (gone == null) {
						z.hidden.put(id, new Gone(e.getType(), save(e), -1)); // spawned after this moment: gone until time moves on
					}
					e.discard();
				}
			}
			float[] s = new float[SELF];
			if (z.casterHeld && p.isAlive() && r.self.get(f, s)) {
				p.setHealth(Math.min(s[0], p.getMaxHealth()));
				p.getFoodData().setFoodLevel((int) s[1]);
				p.getFoodData().setSaturation(s[2]);
				for (int i = 0; i < ARMOR.length; i++) {
					ItemStack worn = p.getItemBySlot(ARMOR[i]);
					if (s[3 + i * 2] >= 0 && !worn.isEmpty() && worn.isDamageableItem()
						&& BuiltInRegistries.ITEM.getId(worn.getItem()) == (int) s[3 + i * 2]) {
						worn.setDamageValue((int) s[4 + i * 2]);
					}
				}
				// D272: the held caster goes where they were (velocity from the frame before, so letting go carries on)
				float[] prev = new float[SELF];
				Vec3 at = new Vec3(r.ax + s[11], r.ay + s[12], r.az + s[13]);
				Vec3 vel = r.self.get(f - 1, prev) ? at.subtract(r.ax + prev[11], r.ay + prev[12], r.az + prev[13]) : Vec3.ZERO;
				p.connection.teleport(new PositionMoveRotation(at, vel, 0, 0), Relative.ROTATION);
				p.fallDistance = s[14];
			}
		} finally {
			applying = false;
		}
	}

	private static void showBlocks(Recorder r, Zone z, int f) {
		if (f < z.blockFrame) {
			for (int i = r.blocks.size() - 1; i >= 0; i--) {
				BlockChange c = r.blocks.get(i);
				if (c.frame > f && c.frame <= z.blockFrame && z.box.contains(Vec3.atCenterOf(c.pos))
					&& r.level.getBlockState(c.pos) == c.to) { // D268: a block changed during the stop stays as it is
					r.level.setBlock(c.pos, c.from, 3);
				}
			}
		} else if (f > z.blockFrame) {
			for (BlockChange c : r.blocks) {
				if (c.frame > z.blockFrame && c.frame <= f && z.box.contains(Vec3.atCenterOf(c.pos)) && r.level.getBlockState(c.pos) == c.from) {
					r.level.setBlock(c.pos, c.to, 3);
				}
			}
		}
		z.blockFrame = f;
	}

	/** Replaces a dying or hurt mob by a fresh copy (same UUID): its death and hurt state live on the clients too. */
	private static Entity fresh(ServerLevel level, Entity e) {
		CompoundTag t = save(e);
		e.discard();
		return recreate(level, new Gone(e.getType(), t, -1));
	}

	private static Entity recreate(ServerLevel level, Gone g) {
		if (g == null) {
			return null;
		}
		CompoundTag data = g.data.copy();
		if (data.contains("Health")) { // D272: it comes back alive, not dying (the frame sets its real health)
			data.putFloat("Health", Math.max(1f, data.getFloatOr("Health", 1f)));
			data.putShort("DeathTime", (short) 0);
			data.putShort("HurtTime", (short) 0);
		}
		Entity e = EntityType.create(g.type, TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data), level,
			EntitySpawnReason.LOAD).orElse(null);
		if (e != null && !level.addFreshEntity(e)) {
			return null;
		}
		return e;
	}

	private static void apply(Recorder r, Entity e, float[] v) {
		e.snapTo(r.ax + v[0], r.ay + v[1], r.az + v[2], v[3], v[4]);
		e.setDeltaMovement(v[8], v[9], v[10]);
		e.setRemainingFireTicks((int) v[11]);
		e.setSharedFlagOnFire(v[11] > 0); // (a frozen entity does not refresh the flag clients draw the fire from)
		if (e instanceof AbstractArrow a && v[8] * v[8] + v[9] * v[9] + v[10] * v[10] > 1e-4) {
			((ArrowChronoAccess) a).permahearts$setInGround(false); // back in flight
		}
		if (e instanceof LivingEntity le) {
			le.setYHeadRot(v[5]);
			le.setYBodyRot(v[6]);
			if (v[7] > 0) {
				le.setHealth(Math.min(v[7], le.getMaxHealth()));
			}
		}
	}

	/** Time runs again from the moment shown: what happened after it is forgotten and the 140 s cooldown starts. */
	private static void resume(ServerPlayer p, Recorder r) {
		Zone z = r.zone;
		if (z == null) {
			return;
		}
		int f = z.castFrame - z.cursor.shown();
		r.zone = null;
		ACTIVE.remove(r);
		r.frame = f;
		r.count = Math.max(1, r.count - z.cursor.shown());
		for (FrameRing t : r.tracks.values()) {
			t.truncate(f);
		}
		r.self.truncate(f);
		r.born.values().removeIf(b -> b > f);
		// alive again when back in the world; a mob that died out of the box stays dead, its death now at the frame shown
		r.gone.entrySet().removeIf(g -> g.getValue().frame >= f && r.level.getEntity(g.getKey()) != null);
		r.gone.replaceAll((id, g) -> g.frame > f ? new Gone(g.type, g.data, f) : g);
		r.blocks.removeIf(c -> c.frame > f);
		ServerLevel sl = r.level;
		long now = sl.getServer().getTickCount();
		long overworld = sl.getServer().overworld().getGameTime();
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.CHRONO,
			overworld + Passives.cooldown(p, TechniqueRules.cooldownTicks(Technique.CHRONO, r.lvl))));
		sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.4f);
		ChronoNet.Zone end = new ChronoNet.Zone(p.getId(), z.minX, z.minY, z.minZ, 0, 0, 0, 0, 0, false);
		for (ServerPlayer o : sl.players()) {
			ServerPlayNetworking.send(o, end);
		}
	}

	private static void resume(ServerPlayer p) {
		Recorder r = RECORDERS.get(p.getUUID());
		if (r != null) {
			resume(p, r);
		}
	}

	/** Ends the player's time stop (if any) and forgets the recording: they left. */
	private static void drop(ServerPlayer p) {
		resume(p);
		Recorder r = RECORDERS.remove(p.getUUID());
		ACTIVE.remove(r);
	}

	private static void broadcast(ServerPlayer p, Recorder r, long now) {
		Zone z = r.zone;
		ChronoNet.Zone msg = new ChronoNet.Zone(p.getId(), z.minX, z.minY, z.minZ, TechniqueRules.CHRONO_BOX, (int) Math.max(0, z.end - now),
			z.cursor.shown(), z.cursor.target(), z.cursor.usable(), z.casterHeld);
		Vec3 c = z.box.getCenter();
		for (ServerPlayer o : r.level.players()) {
			if (o == p || o.position().distanceToSqr(c) <= SYNC_RANGE * SYNC_RANGE) {
				ServerPlayNetworking.send(o, msg);
			}
		}
	}

	/** Server side of {@link ChronoFreeze}: players never stop; mobs held by a stop or anything inside its box does. */
	static boolean frozen(Entity e) {
		if (ACTIVE.isEmpty() || e instanceof Player) {
			return false;
		}
		for (Recorder r : ACTIVE) {
			Zone z = r.zone;
			if (z != null && r.level == e.level() && (z.box.contains(e.position()) || z.held.contains(e.getUUID()))) {
				return true;
			}
		}
		return false;
	}

	/** A few motes on the edges of the box so it can be found when the grey does not show (far away). */
	private static final class RandomEdge {
		static void particles(ServerLevel sl, AABB box) {
			var rnd = sl.getRandom();
			for (int i = 0; i < 6; i++) {
				double x = rnd.nextBoolean() ? box.minX : box.maxX;
				double z = rnd.nextBoolean() ? box.minZ : box.maxZ;
				double y = box.minY + rnd.nextDouble() * (box.maxY - box.minY);
				sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
			}
		}
	}
}
