package io.github.huydangtruong.immortalcultivation.technique;

import com.mojang.math.Transformation;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.mixin.BlockDisplayAccess;
import io.github.huydangtruong.immortalcultivation.mixin.DisplayAccess;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * D142 Seism. Seismic Impact: a boulder (block display) falls on the aimed point for 10 ticks (0.5 s) while the point follows
 * the crosshair, then hurts hostile non-boss mobs around it. Seismic Wave (Lv 3+, held 1 s): three quake pulses around
 * the caster that lift and hurt everything on the ground. Runs from the server tick; the rules are in SeismRules.
 */
public final class Seism {
	private static final Identifier SLOW_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "seism_slow");
	private static final int SPIKE_LIFE = 12;

	/** Server to client: a short camera shake. */
	public record Shake(float strength, int ticks) implements CustomPacketPayload {
		public static final Type<Shake> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "seism_shake"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Shake> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Shake::strength,
			ByteBufCodecs.VAR_INT, Shake::ticks,
			Shake::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class Fall {
		final ServerLevel level;
		final UUID caster;
		final int techLevel;
		final boolean follow;
		Vec3 point;
		int age;
		Display.BlockDisplay boulder;

		Fall(ServerLevel level, UUID caster, int techLevel, Vec3 point, boolean follow) {
			this.level = level;
			this.caster = caster;
			this.techLevel = techLevel;
			this.point = point;
			this.follow = follow;
		}
	}

	private static final class Wave {
		final ServerLevel level;
		final UUID caster;
		final int techLevel;
		final long start;

		Wave(ServerLevel level, UUID caster, int techLevel, long start) {
			this.level = level;
			this.caster = caster;
			this.techLevel = techLevel;
			this.start = start;
		}
	}

	private static final class Spike {
		final Display.BlockDisplay display;
		final long start;

		Spike(Display.BlockDisplay display, long start) {
			this.display = display;
			this.start = start;
		}
	}

	private static final List<Fall> FALLS = new ArrayList<>();
	private static final List<Wave> WAVES = new ArrayList<>();
	private static final List<Spike> SPIKES = new ArrayList<>();

	private Seism() {}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Shake.TYPE, Shake.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Seism::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> clear());
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer p) {
				endWaves(p.getUUID(), p);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> endWaves(handler.getPlayer().getUUID(), handler.getPlayer()));
	}

	private static void clear() {
		for (Fall f : FALLS) {
			if (f.boulder != null) {
				f.boulder.discard();
			}
		}
		for (Spike s : SPIKES) {
			s.display.discard();
		}
		FALLS.clear();
		WAVES.clear();
		SPIKES.clear();
	}

	// ---- cast ----

	/** Wave when the level and the charge allow it (Lv 3+, full hold), else Impact on the aimed point. */
	public static boolean cast(ServerPlayer p, int level, float charge) {
		if (SeismRules.waveCast(level, charge)) {
			return wave(p, level);
		}
		return impact(p, level, null);
	}

	/** The aim point: the first block on the look ray within 48 blocks; the sky is dropped to the ground. */
	public static Vec3 aim(ServerPlayer p) {
		ServerLevel sl = p.level();
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getViewVector(1f).scale(SeismRules.AIM_RANGE));
		BlockHitResult block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (block.getType() != HitResult.Type.MISS) {
			return block.getLocation();
		}
		BlockHitResult down = sl.clip(new ClipContext(end, new Vec3(end.x, sl.getMinY(), end.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		return down.getType() == HitResult.Type.MISS ? end : down.getLocation();
	}

	/**
	 * Seismic Impact. {@code fixed} null: the point follows the caster's crosshair during the fall; else the boulder falls on
	 * that point (the game tests use it).
	 */
	public static boolean impact(ServerPlayer p, int level, Vec3 fixed) {
		ServerLevel sl = p.level();
		Fall f = new Fall(sl, p.getUUID(), level, fixed != null ? fixed : aim(p), fixed == null);
		f.boulder = spawnBlock(sl, Blocks.COBBLESTONE.defaultBlockState(), f.point.add(0, SeismRules.FALL_HEIGHT, 0));
		placeBoulder(f);
		FALLS.add(f);
		sl.playSound(null, BlockPos.containing(f.point), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.5f);
		return true;
	}

	/** Seismic Wave: slows the caster and starts the three pulses (the first one at once). */
	public static boolean wave(ServerPlayer p, int level) {
		ServerLevel sl = p.level();
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, SeismRules.WAVE_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		Wave w = new Wave(sl, p.getUUID(), level, sl.getGameTime());
		WAVES.add(w);
		pulse(w, p, 0);
		return true;
	}

	private static void endWaves(UUID caster, ServerPlayer p) {
		WAVES.removeIf(w -> w.caster.equals(caster));
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW_ID);
		}
	}

	/** True while the player is slowed by a running Wave (tests). */
	public static boolean slowed(ServerPlayer p) {
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		return speed != null && speed.hasModifier(SLOW_ID);
	}

	// ---- tick ----

	private static void tick(MinecraftServer server) {
		if (FALLS.isEmpty() && WAVES.isEmpty() && SPIKES.isEmpty()) {
			return;
		}
		for (Iterator<Fall> it = FALLS.iterator(); it.hasNext();) {
			Fall f = it.next();
			f.age++;
			if (f.follow && f.level.getServer().getPlayerList().getPlayer(f.caster) instanceof ServerPlayer c && c.isAlive() && c.level() == f.level) {
				f.point = aim(c);
			}
			if (f.age < SeismRules.FALL_TICKS) {
				placeBoulder(f);
				continue;
			}
			it.remove();
			if (f.boulder != null) {
				f.boulder.discard();
			}
			if (f.level.isLoaded(BlockPos.containing(f.point))) {
				land(f);
			}
		}
		for (Iterator<Wave> it = WAVES.iterator(); it.hasNext();) {
			Wave w = it.next();
			ServerPlayer caster = server.getPlayerList().getPlayer(w.caster);
			if (caster == null || !caster.isAlive() || caster.level() != w.level) {
				it.remove();
				if (caster != null) {
					endWaves(w.caster, caster);
				}
				continue;
			}
			long elapsed = w.level.getGameTime() - w.start;
			if (elapsed >= SeismRules.WAVE_SLOW_TICKS) {
				it.remove();
				endWaves(w.caster, caster);
			} else if (elapsed > 0 && elapsed % SeismRules.WAVE_PULSE_TICKS == 0) {
				pulse(w, caster, (int) (elapsed / SeismRules.WAVE_PULSE_TICKS));
			}
		}
		for (Iterator<Spike> it = SPIKES.iterator(); it.hasNext();) {
			Spike s = it.next();
			long age = s.display.level().getGameTime() - s.start;
			if (age >= SPIKE_LIFE || s.display.isRemoved()) {
				s.display.discard();
				it.remove();
			} else {
				transform(s.display, new Vector3f(-0.45f, 0f, -0.45f), new Vector3f(0.9f, spikeHeight((int) age), 0.9f));
			}
		}
	}

	// ---- Impact ----

	private static void placeBoulder(Fall f) {
		if (f.boulder == null) {
			return;
		}
		float progress = SeismRules.fallProgress(f.age);
		float size = SeismRules.boulderScale(progress) * 2.5f;
		Vec3 at = f.point.add(0, SeismRules.FALL_HEIGHT * (1 - progress), 0);
		f.boulder.setPos(at.x, at.y, at.z);
		transform(f.boulder, new Vector3f(-size / 2, 0f, -size / 2), new Vector3f(size, size, size));
	}

	private static void land(Fall f) {
		ServerLevel sl = f.level;
		Vec3 c = f.point;
		ServerPlayer caster = sl.getServer().getPlayerList().getPlayer(f.caster);
		float damage = SeismRules.impactHearts(f.techLevel) * 2f;
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(SeismRules.IMPACT_RADIUS + 1),
			e -> e instanceof Enemy && e.isAlive() && !e.isSpectator() && !Grasp.isBoss(e) && !Grasp.isGrasped(e)
				&& !(e instanceof OwnedSummon) && !e.getUUID().equals(f.caster) && distance(e, c) <= SeismRules.IMPACT_RADIUS)) {
			TechniqueDamage.hurt(sl, e, caster, null, DamageTypes.MAGIC, damage);
		}
		BlockState rock = Blocks.STONE.defaultBlockState();
		sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, rock), c.x, c.y + 0.2, c.z, 60, 1.4, 0.3, 1.4, 0.15);
		sl.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.3, c.z, 24, 1.5, 0.2, 1.5, 0.05);
		sl.playSound(null, BlockPos.containing(c), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 0.6f);
		sl.playSound(null, BlockPos.containing(c), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
		shake(sl, c, 0.5f, 8);
	}

	// ---- Wave ----

	private static void pulse(Wave w, ServerPlayer caster, int index) {
		ServerLevel sl = w.level;
		Vec3 c = caster.position();
		double r = SeismRules.waveRadius(index);
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r + 1),
			e -> TechniqueDamage.affectable(e, caster) && !(e instanceof Player pl && pl.isCreative()) && distance(e, c) <= r)) {
			if (!e.onGround() && !e.isInWater()) {
				continue; // already airborne (for instance launched by an earlier pulse)
			}
			int dy = SeismRules.heightAbove(e.getY(), caster.getY());
			int hearts = SeismRules.heartsAt(w.techLevel, dy);
			int lift = SeismRules.liftAt(w.techLevel, dy);
			if (hearts > 0) {
				TechniqueDamage.hurt(sl, e, caster, null, DamageTypes.MAGIC, hearts * 2f);
			}
			if (Grasp.isBoss(e) || !e.isAlive()) {
				continue; // bosses take the damage only
			}
			double dx = e.getX() - c.x;
			double dz = e.getZ() - c.z;
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len < 1e-4) {
				dx = 1;
				dz = 0;
				len = 1;
			}
			double kb = SeismRules.knockbackSpeed(SeismRules.WAVE_KNOCKBACK);
			double vy = lift > 0 ? SeismRules.liftVelocity(lift) : e.getDeltaMovement().y;
			e.setDeltaMovement(dx / len * kb, vy, dz / len * kb);
			e.needsSync = true;
		}
		spikes(sl, c, r, caster.getY());
		sl.playSound(null, caster.blockPosition(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.5f, 0.6f);
		sl.playSound(null, caster.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1f, 0.7f);
		sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), c.x, c.y + 0.2, c.z, 40, r / 2, 0.2, r / 2, 0.1);
		shake(sl, c, 0.35f, 8);
	}

	private static void spikes(ServerLevel sl, Vec3 c, double r, double casterY) {
		int count = (int) Math.max(8, Math.round(Math.PI * 2 * r * 1.2));
		double offset = sl.getRandom().nextDouble() * Math.PI * 2;
		BlockState spike = Blocks.POINTED_DRIPSTONE.defaultBlockState();
		for (int i = 0; i < count; i++) {
			double a = offset + Math.PI * 2 * i / count;
			double sr = r * (i % 2 == 0 ? 1.0 : 0.6);
			double x = c.x + Math.cos(a) * sr;
			double z = c.z + Math.sin(a) * sr;
			Display.BlockDisplay d = spawnBlock(sl, spike, new Vec3(x, groundY(sl, x, casterY, z), z));
			if (d != null) {
				transform(d, new Vector3f(-0.45f, 0f, -0.45f), new Vector3f(0.9f, 0.01f, 0.9f));
				SPIKES.add(new Spike(d, sl.getGameTime()));
			}
		}
	}

	/** Height of a 2-block spike over its life: rises in 3 ticks, holds, sinks. */
	static float spikeHeight(int age) {
		if (age < 3) {
			return 2f * Math.max(0.05f, age / 3f);
		}
		if (age < 7) {
			return 2f;
		}
		return Math.max(0.05f, 2f * (SPIKE_LIFE - age) / (SPIKE_LIFE - 7f));
	}

	private static double groundY(ServerLevel sl, double x, double y, double z) {
		int by = (int) Math.floor(y) + 1;
		for (int i = 0; i < 6; i++) {
			BlockPos pos = new BlockPos((int) Math.floor(x), by - i, (int) Math.floor(z));
			var shape = sl.getBlockState(pos).getCollisionShape(sl, pos);
			if (!shape.isEmpty()) {
				return pos.getY() + shape.max(Direction.Axis.Y);
			}
		}
		return y;
	}

	// ---- helpers ----

	private static Display.BlockDisplay spawnBlock(ServerLevel sl, BlockState state, Vec3 at) {
		Entity e = EntityTypes.BLOCK_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
		if (!(e instanceof Display.BlockDisplay d)) {
			return null;
		}
		((BlockDisplayAccess) d).permahearts$setBlockState(state);
		((DisplayAccess) d).permahearts$setViewRange(2f);
		d.setPos(at.x, at.y, at.z);
		d.setNoGravity(true);
		sl.addFreshEntity(d);
		return d;
	}

	private static void transform(Display.BlockDisplay d, Vector3f translation, Vector3f scale) {
		((DisplayAccess) d).permahearts$setTransformation(new Transformation(translation, new Quaternionf(), scale, new Quaternionf()));
	}

	private static void shake(ServerLevel sl, Vec3 at, float strength, int ticks) {
		for (ServerPlayer p : sl.players()) {
			if (p.position().distanceToSqr(at) <= 64) {
				ServerPlayNetworking.send(p, new Shake(strength, ticks));
			}
		}
	}

	/** Distance from the point to the closest point of the entity's box. */
	private static double distance(Entity e, Vec3 c) {
		AABB b = e.getBoundingBox();
		double dx = Math.max(Math.max(b.minX - c.x, 0), c.x - b.maxX);
		double dy = Math.max(Math.max(b.minY - c.y, 0), c.y - b.maxY);
		double dz = Math.max(Math.max(b.minZ - c.z, 0), c.z - b.maxZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Number of boulders still falling and spikes still alive (tests). */
	public static int activeVisuals() {
		return FALLS.size() + SPIKES.size();
	}
}
