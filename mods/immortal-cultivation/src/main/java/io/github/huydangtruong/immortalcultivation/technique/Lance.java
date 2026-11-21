package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.passive.Passives;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * D157, D159 Solar Lance: a beam from the caster's hand to the crosshair (first block, at most 256 blocks) for up to 12 s
 * while the button that cast it stays held; letting go stops it. Every living entity on the line (not the caster's friends)
 * takes damage every 10 ticks that doubles every 3 s. Stopping for any reason starts the 40 s cooldown.
 */
public final class Lance {
	/** Server to client: the beam of one caster. active = false removes it. ticksLeft drives the bar. */
	public record State(int caster, boolean active, double x, double y, double z, int ticksLeft) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "lance_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::caster,
			ByteBufCodecs.BOOL, State::active,
			ByteBufCodecs.DOUBLE, State::x,
			ByteBufCodecs.DOUBLE, State::y,
			ByteBufCodecs.DOUBLE, State::z,
			ByteBufCodecs.VAR_INT, State::ticksLeft,
			State::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class Active {
		final ServerLevel level;
		final int techLevel;
		int ticks;
		Vec3 end = Vec3.ZERO;
		BlockPos mining;
		int mineTicks;

		Active(ServerLevel level, int techLevel) {
			this.level = level;
			this.techLevel = techLevel;
		}
	}

	private static final Map<UUID, Active> ACTIVE = new HashMap<>();
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xFF8A1E, 0.8f);

	private Lance() {}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Lance::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> ACTIVE.clear());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stop(handler.getPlayer()));
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer p) {
				stop(p);
			}
		});
	}

	public static boolean active(ServerPlayer p) {
		return ACTIVE.containsKey(p.getUUID());
	}

	/** The caster's friends: their own summons and the mobs they grasped. */
	static boolean isFriend(LivingEntity e, ServerPlayer caster) {
		return (e instanceof OwnedSummon s && caster.getUUID().equals(s.owner())) || Grasp.isGraspedBy(e, caster.getUUID());
	}

	/** Starts the beam. False while one is already running. */
	public static boolean cast(ServerPlayer p, int level) {
		if (ACTIVE.containsKey(p.getUUID())) {
			return false;
		}
		Active a = new Active(p.level(), level);
		ACTIVE.put(p.getUUID(), a);
		p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.9f);
		// D163: a harsh searing start
		p.level().playSound(null, p.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0f, 1.6f);
		p.level().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
		fire(p, a);
		return true;
	}

	/** The button was let go (TechniqueNet RELEASE): stops the beam and starts the cooldown. Does nothing when no beam runs. */
	public static void release(ServerPlayer p) {
		stop(p);
	}

	/** Ends the beam for any reason (release, 12 s, death, dimension change, logout) and starts the 40 s cooldown. */
	static void stop(ServerPlayer p) {
		Active a = ACTIVE.remove(p.getUUID());
		if (a == null) {
			return;
		}
		long now = p.level().getServer().overworld().getGameTime();
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.LANCE,
			now + Passives.cooldown(p, Technique.LANCE.cooldownTicks())));
		clearCrack(p, a);
		broadcast(p, new State(p.getId(), false, a.end.x, a.end.y, a.end.z, 0));
	}

	private static void broadcast(ServerPlayer p, State s) {
		ServerPlayNetworking.send(p, s);
		for (ServerPlayer o : PlayerLookup.tracking(p)) {
			ServerPlayNetworking.send(o, s);
		}
	}

	private static BlockHitResult beamHit(ServerPlayer p) {
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getViewVector(1f).scale(LanceRules.RANGE));
		return p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
	}

	/** Where the beam ends: the first block on the look ray, or 256 blocks out. */
	public static Vec3 beamEnd(ServerPlayer p) {
		BlockHitResult block = beamHit(p);
		return block.getType() == HitResult.Type.MISS ? p.getEyePosition().add(p.getViewVector(1f).scale(LanceRules.RANGE)) : block.getLocation();
	}

	private static void clearCrack(ServerPlayer p, Active a) {
		if (a.mining != null) {
			a.level.destroyBlockProgress(p.getId(), a.mining, -1);
			a.mining = null;
		}
		a.mineTicks = 0;
	}

	/** D161: the block the beam ends on cracks while the beam rests on it and breaks (dropping items) after 6 ticks. */
	private static void mine(ServerPlayer p, Active a, BlockHitResult hit) {
		ServerLevel sl = a.level;
		if (hit.getType() == HitResult.Type.MISS || !LanceRules.breakable(sl.getBlockState(hit.getBlockPos()).getDestroySpeed(sl, hit.getBlockPos()))) {
			clearCrack(p, a);
			return;
		}
		BlockPos pos = hit.getBlockPos();
		if (!pos.equals(a.mining)) {
			clearCrack(p, a);
			a.mining = pos;
		}
		a.mineTicks++;
		if (LanceRules.breaksBlock(a.mineTicks)) {
			sl.destroyBlockProgress(p.getId(), pos, -1);
			sl.destroyBlock(pos, true, p);
			a.mining = null;
			a.mineTicks = 0;
		} else {
			sl.destroyBlockProgress(p.getId(), pos, LanceRules.crackStage(a.mineTicks));
		}
	}

	private static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		for (UUID id : List.copyOf(ACTIVE.keySet())) {
			Active a = ACTIVE.get(id);
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (a == null) {
				continue;
			}
			if (p == null) {
				ACTIVE.remove(id);
				continue;
			}
			if (!p.isAlive() || p.level() != a.level) {
				stop(p);
				continue;
			}
			if (a.ticks >= LanceRules.DURATION_TICKS) {
				stop(p);
				continue;
			}
			fire(p, a);
		}
	}

	/** One tick of the beam: updates the end point, sends it, and every 10th tick hurts what is on the line. */
	private static void fire(ServerPlayer p, Active a) {
		ServerLevel sl = a.level;
		Vec3 eye = p.getEyePosition();
		BlockHitResult hit = beamHit(p);
		a.end = hit.getType() == HitResult.Type.MISS ? eye.add(p.getViewVector(1f).scale(LanceRules.RANGE)) : hit.getLocation();
		mine(p, a, hit);
		broadcast(p, new State(p.getId(), true, a.end.x, a.end.y, a.end.z, LanceRules.DURATION_TICKS - a.ticks));
		if (a.ticks % LanceRules.DAMAGE_INTERVAL == 0) {
			float dmg = (float) LanceRules.damagePerHit(a.techLevel, a.ticks);
			Vec3 end = a.end;
			for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0),
				e -> TechniqueDamage.affectable(e, p) && !isFriend(e, p) && e.getBoundingBox().inflate(0.2).clip(eye, end).isPresent())) {
				Vec3 velocity = e.getDeltaMovement();
				TechniqueDamage.hurt(sl, e, p, null, DamageTypes.MAGIC, dmg); // hurtTime restarts: the vanilla red hurt tint stays while hit
				e.setInvulnerableTime(0); // other damage is never held off by the beam's hits
				e.setDeltaMovement(velocity); // no knockback from a laser
				Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
				sl.sendParticles(SPARK, c.x, c.y, c.z, 6, e.getBbWidth() * 0.3, e.getBbHeight() * 0.25, e.getBbWidth() * 0.3, 0.1);
				sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 2, e.getBbWidth() * 0.25, e.getBbHeight() * 0.2, e.getBbWidth() * 0.25, 0.04);
			}
			// D163: harsher, crackling and sizzling, louder as the beam grows (never above 1.3)
			float grow = LanceRules.widthFactor(LanceRules.DURATION_TICKS - a.ticks);
			sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f, 2f);
			sl.playSound(null, p.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6f * grow, 1.4f);
			sl.playSound(null, p.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.5f * grow, 0.6f + 0.8f * sl.getRandom().nextFloat());
			sl.playSound(null, p.blockPosition(), SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 0.5f * grow, 1.8f);
		}
		a.ticks++;
	}
}
