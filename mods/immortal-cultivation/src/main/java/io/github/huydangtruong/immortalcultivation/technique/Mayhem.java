package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.passive.Passives;
import io.github.huydangtruong.immortalcultivation.technique.MayhemRules.P;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * D174 Crystal Mayhem. Left click: Shard Projection, one amethyst shard along the crosshair (hurts more in light, leaves a
 * cluster for 10 s). Hold right click 1 s: Light Strike Reflection, which turns clusters into budding amethyst that lights up,
 * slows and burns near the player for 20 s and refills the shards. After the 20 s everything is removed and the 30 s cooldown runs.
 * Both buttons come through {@link #input}; the cooldown and the shard count are kept here, not by TechniqueNet.
 */
public final class Mayhem {
	/** Server to the owner: shards left (-1 = nothing running) and ticks left of the 20 s window (0 = none). */
	public record State(int shards, int windowTicks) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "mayhem_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::shards,
			ByteBufCodecs.VAR_INT, State::windowTicks,
			State::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class Run {
		final ServerLevel level;
		int techLevel;
		int shards = MayhemRules.SHARDS;
		/** Shards of the first batch leave clusters; the refill after the channel does not. */
		boolean fresh = true;
		boolean window;
		long windowEnd;
		/** The first batch is spent and the cooldown runs, but the channel may still be used once. */
		boolean spent;
		long lastShot = -1000;

		Run(ServerLevel level, int techLevel) {
			this.level = level;
			this.techLevel = techLevel;
		}
	}

	public static final double RANGE = 64;
	private static final Map<UUID, Run> RUNS = new HashMap<>();
	private static final DustParticleOptions SHARD = new DustParticleOptions(0xB06CFF, 0.8f);
	private static final DustParticleOptions SPARKLE = new DustParticleOptions(0xD9A8FF, 0.7f);

	private Mayhem() {}

	public static void init() {
		MayhemBlocks.init();
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Mayhem::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> RUNS.clear());
		// blocks left over from a run that a restart cut short go as soon as their chunk is loaded
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MayhemSavedData data = MayhemSavedData.of(server);
			for (MayhemSavedData.Entry e : data.blocks) {
				e.expiry = 0;
			}
			data.setDirty();
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.getPlayer()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer p) {
				end(p);
			}
		});
	}

	public static boolean running(ServerPlayer p) {
		return RUNS.containsKey(p.getUUID());
	}

	public static int shardsLeft(ServerPlayer p) {
		Run r = RUNS.get(p.getUUID());
		return r == null ? -1 : r.shards;
	}

	public static boolean windowActive(ServerPlayer p) {
		Run r = RUNS.get(p.getUUID());
		return r != null && r.window;
	}

	private static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	/** Left click = a shard (charge below 1), a full right click hold (charge 1) = the channel. Returns null when it worked, else why not. */
	public static String input(ServerPlayer p, int level, float charge) {
		float c = Float.isNaN(charge) ? 1f : charge;
		return c >= 0.99f ? channel(p, level) : shoot(p, level);
	}

	private static String cooldownMessage(ServerPlayer p, long now) {
		return "On cooldown: " + (TechniqueAttachment.get(p).cooldownLeft(Technique.MAYHEM, now) + 19) / 20 + " s";
	}

	// ---- Shard Projection ----

	private static String shoot(ServerPlayer p, int level) {
		ServerLevel sl = p.level();
		long now = now(sl.getServer());
		Run r = RUNS.get(p.getUUID());
		if (r == null) {
			if (TechniqueAttachment.get(p).onCooldown(Technique.MAYHEM, now)) {
				return cooldownMessage(p, now);
			}
			r = new Run(sl, level);
			RUNS.put(p.getUUID(), r);
		}
		if (r.shards <= 0) {
			return r.window ? "No shards left" : cooldownMessage(p, now);
		}
		r.techLevel = level;
		if (now - r.lastShot < MayhemRules.shardDelayTicks(level)) {
			return null; // the next shard is not ready yet: a click too early does nothing
		}
		r.lastShot = now;
		r.shards--;
		fire(p, r, now);
		if (r.shards == 0 && !r.window) { // the batch is spent: the 30 s cooldown starts, the channel stays open
			r.spent = true;
			TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.MAYHEM,
				now + Passives.cooldown(p, Technique.MAYHEM.cooldownTicks())));
		}
		send(p, r, now);
		return null;
	}

	private static void fire(ServerPlayer p, Run r, long now) {
		ServerLevel sl = r.level;
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getViewVector(1f).scale(RANGE));
		BlockHitResult block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, p, eye, limit, new AABB(eye, limit).inflate(1.0),
			e -> e instanceof LivingEntity le && hits(le, p), 0.3f);
		Vec3 stop = hit != null ? hit.getLocation() : limit;
		trail(sl, eye.add(0, -0.2, 0), stop);
		sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
		if (hit != null && hit.getEntity() instanceof LivingEntity target) {
			BlockPos at = target.blockPosition();
			int sky = MayhemRules.skyLevel(sl.getBrightness(LightLayer.SKY, at), sl.getSkyDarken(), sl.getOverworldClockTime());
			int blockLight = sl.getBrightness(LightLayer.BLOCK, at);
			TechniqueDamage.hurt(sl, target, p, null, DamageTypes.MAGIC, (float) MayhemRules.shardDamage(r.techLevel, sky, blockLight));
			Vec3 c = target.getBoundingBox().getCenter();
			sl.sendParticles(SHARD, c.x, c.y, c.z, 8, 0.25, 0.3, 0.25, 0.05);
			if (r.fresh) {
				placeUnder(p, sl, target, now);
			}
		} else if (block.getType() != HitResult.Type.MISS && r.fresh) {
			BlockPos pos = block.getBlockPos().relative(block.getDirection());
			placeCluster(p, sl, pos, block.getDirection(), now);
		}
	}

	private static void trail(ServerLevel sl, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		int steps = (int) Math.min(48, Math.max(2, d.length() * 1.5));
		for (int i = 0; i < steps; i++) {
			Vec3 q = from.add(d.scale(i / (double) steps));
			sl.sendParticles(SHARD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
		}
	}

	/** On the ground under the mob (up to 8 blocks down), pointing up. */
	private static void placeUnder(ServerPlayer p, ServerLevel sl, LivingEntity target, long now) {
		BlockPos pos = target.blockPosition();
		for (int i = 0; i < 8; i++) {
			BlockPos below = pos.below();
			if (sl.getBlockState(below).isFaceSturdy(sl, below, Direction.UP)) {
				placeCluster(p, sl, pos, Direction.UP, now);
				return;
			}
			pos = below;
		}
	}

	public static void placeCluster(ServerPlayer p, ServerLevel sl, BlockPos pos, Direction facing, long now) {
		if (!sl.getBlockState(pos).isAir()) {
			return;
		}
		BlockState state = MayhemBlocks.cluster(facing, false);
		if (!state.canSurvive(sl, pos)) {
			return;
		}
		sl.setBlock(pos, state, 3);
		MayhemSavedData data = MayhemSavedData.of(sl.getServer());
		data.blocks.add(new MayhemSavedData.Entry(p.getUUID().toString(), dim(sl), pos.asLong(), MayhemSavedData.CLUSTER, now + MayhemRules.CLUSTER_TICKS));
		data.setDirty();
	}

	// ---- Light Strike Reflection ----

	private static String channel(ServerPlayer p, int level) {
		ServerLevel sl = p.level();
		long now = now(sl.getServer());
		Run r = RUNS.get(p.getUUID());
		if (r != null && r.window) {
			return "Light Strike Reflection is already active";
		}
		if (r == null) {
			if (TechniqueAttachment.get(p).onCooldown(Technique.MAYHEM, now)) {
				return cooldownMessage(p, now);
			}
			r = new Run(sl, level);
			RUNS.put(p.getUUID(), r);
		} else if (now - r.lastShot < MayhemRules.shardDelayTicks(level)) {
			return "Wait for the next shard";
		}
		r.techLevel = level;
		r.window = true;
		r.windowEnd = now + MayhemRules.WINDOW_TICKS;
		r.shards = MayhemRules.SHARDS;
		r.fresh = false;
		r.spent = false;
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.MAYHEM, 0)); // using it cancels the cooldown
		sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 0.9f);
		sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f);
		MayhemSavedData data = MayhemSavedData.of(sl.getServer());
		convert(p, r, data, now);
		refreshLights(p, r, data);
		send(p, r, now);
		return null;
	}

	private static List<MayhemSavedData.Entry> entries(MayhemSavedData data, ServerPlayer p, ServerLevel sl, int kind) {
		List<MayhemSavedData.Entry> out = new ArrayList<>();
		String owner = p.getUUID().toString();
		String dim = dim(sl);
		for (MayhemSavedData.Entry e : data.blocks) {
			if (e.kind == kind && e.owner.equals(owner) && e.dimension.equals(dim)) {
				out.add(e);
			}
		}
		return out;
	}

	private static P centre(BlockPos pos) {
		return new P(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	/** Turns clusters into buddings, one at a time in the order of MayhemRules.pickNext, until the cap or nothing is left in reach. */
	private static void convert(ServerPlayer p, Run r, MayhemSavedData data, long now) {
		ServerLevel sl = r.level;
		int cap = MayhemRules.maxBuddings(r.techLevel);
		Vec3 body = p.getBoundingBox().getCenter();
		P player = new P(body.x, body.y, body.z);
		boolean changed = false;
		while (true) {
			List<MayhemSavedData.Entry> buddings = entries(data, p, sl, MayhemSavedData.BUDDING);
			if (buddings.size() >= cap) {
				break;
			}
			List<MayhemSavedData.Entry> clusters = entries(data, p, sl, MayhemSavedData.CLUSTER);
			clusters.removeIf(e -> !MayhemBlocks.isChainCluster(sl.getBlockState(BlockPos.of(e.pos))));
			List<P> bp = new ArrayList<>();
			for (MayhemSavedData.Entry b : buddings) {
				bp.add(centre(BlockPos.of(b.pos)));
			}
			List<P> cp = new ArrayList<>();
			for (MayhemSavedData.Entry c : clusters) {
				cp.add(centre(BlockPos.of(c.pos)));
			}
			int i = MayhemRules.pickNext(player, bp, cp);
			if (i < 0) {
				break;
			}
			becomeBudding(sl, clusters.get(i), p, data);
			changed = true;
		}
		if (changed) {
			data.setDirty();
		}
	}

	private static void becomeBudding(ServerLevel sl, MayhemSavedData.Entry e, ServerPlayer p, MayhemSavedData data) {
		BlockPos pos = BlockPos.of(e.pos);
		sl.setBlock(pos, MayhemBlocks.BUDDING.defaultBlockState(), 3);
		e.kind = MayhemSavedData.BUDDING;
		e.expiry = Long.MAX_VALUE; // no more 10 s timer; the window end removes it
		for (Direction d : Direction.values()) {
			BlockPos n = pos.relative(d);
			if (sl.getBlockState(n).isAir()) {
				sl.setBlock(n, MayhemBlocks.cluster(d, true), 3);
				data.blocks.add(new MayhemSavedData.Entry(e.owner, e.dimension, n.asLong(), MayhemSavedData.DECOR, Long.MAX_VALUE));
			}
		}
		sl.sendParticles(SPARKLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.5, 0.5, 0.5, 0.05);
		sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 1.2f);
	}

	/** Which buddings are active now (linked to the player) and the lit state of each block follows. Returns the active positions. */
	private static List<BlockPos> refreshLights(ServerPlayer p, Run r, MayhemSavedData data) {
		ServerLevel sl = r.level;
		List<MayhemSavedData.Entry> buddings = entries(data, p, sl, MayhemSavedData.BUDDING);
		List<BlockPos> live = new ArrayList<>();
		List<P> ps = new ArrayList<>();
		for (MayhemSavedData.Entry b : buddings) {
			BlockPos pos = BlockPos.of(b.pos);
			if (sl.getBlockState(pos).is(MayhemBlocks.BUDDING)) {
				live.add(pos);
				ps.add(centre(pos));
			}
		}
		Vec3 body = p.getBoundingBox().getCenter();
		boolean[] on = MayhemRules.active(new P(body.x, body.y, body.z), ps);
		List<BlockPos> active = new ArrayList<>();
		for (int i = 0; i < live.size(); i++) {
			BlockState s = sl.getBlockState(live.get(i));
			if (s.getValue(MayhemBlocks.Budding.LIT) != on[i]) {
				sl.setBlock(live.get(i), s.setValue(MayhemBlocks.Budding.LIT, on[i]), 3);
			}
			if (on[i]) {
				active.add(live.get(i));
			}
		}
		return active;
	}

	/** Once a second: slowness and burn damage for everything near an active budding, each target once. */
	/**
	 * Targets of the shards and the burn: the usual technique filter, and D182: other players only when the PvP game rule
	 * is on (a Death Match needs PvP on, so its opponents stay targets). With PvP off a shard flies through players.
	 */
	public static boolean hits(LivingEntity e, ServerPlayer p) {
		return TechniqueDamage.affectable(e, p) && !Lance.isFriend(e, p)
			&& (!(e instanceof net.minecraft.world.entity.player.Player) || io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch.pvpAllowed(p.level().getServer()));
	}

	private static void burn(ServerPlayer p, Run r, List<BlockPos> active) {
		ServerLevel sl = r.level;
		double radius = MayhemRules.burnRadius(r.techLevel);
		float damage = (float) MayhemRules.burnDamage(r.techLevel);
		Set<LivingEntity> targets = new LinkedHashSet<>();
		for (BlockPos pos : active) {
			Vec3 c = Vec3.atCenterOf(pos);
			targets.addAll(sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius),
				e -> hits(e, p) && e.getBoundingBox().getCenter().distanceTo(c) <= radius));
			sl.sendParticles(SPARKLE, c.x, c.y + 0.6, c.z, 3, 0.4, 0.4, 0.4, 0.02);
		}
		for (LivingEntity e : targets) {
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, false, true, true));
			TechniqueDamage.hurt(sl, e, p, null, DamageTypes.MAGIC, damage);
			Vec3 c = e.getBoundingBox().getCenter();
			sl.sendParticles(SPARKLE, c.x, c.y, c.z, 4, e.getBbWidth() * 0.3, e.getBbHeight() * 0.3, e.getBbWidth() * 0.3, 0.02);
		}
	}

	// ---- ending ----

	/** Ends the run for any reason (window over, death, dimension change, logout): removes the blocks, starts the 30 s cooldown. */
	public static void end(ServerPlayer p) {
		Run r = RUNS.remove(p.getUUID());
		if (r == null) {
			return;
		}
		MinecraftServer server = r.level.getServer();
		long now = now(server);
		purge(server, e -> e.owner.equals(p.getUUID().toString()), now, true);
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.MAYHEM,
			now + Passives.cooldown(p, Technique.MAYHEM.cooldownTicks())));
		if (p.connection != null) {
			ServerPlayNetworking.send(p, new State(-1, 0));
		}
	}

	/** Removes the blocks of the entries that match (all of them with force, else only those whose time is up). */
	private static void purge(MinecraftServer server, java.util.function.Predicate<MayhemSavedData.Entry> which, long now, boolean force) {
		MayhemSavedData data = MayhemSavedData.of(server);
		boolean changed = false;
		for (Iterator<MayhemSavedData.Entry> it = data.blocks.iterator(); it.hasNext();) {
			MayhemSavedData.Entry e = it.next();
			if (!which.test(e) || (!force && now < e.expiry)) {
				continue;
			}
			ServerLevel sl = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(e.dimension)));
			if (sl == null) {
				it.remove();
				changed = true;
				continue;
			}
			BlockPos pos = BlockPos.of(e.pos);
			if (!sl.isLoaded(pos)) {
				e.expiry = 0; // chunk not loaded: it goes as soon as it is
				changed = true;
				continue;
			}
			BlockState s = sl.getBlockState(pos);
			if (s.is(MayhemBlocks.CLUSTER) || s.is(MayhemBlocks.BUDDING)) {
				if (e.kind == MayhemSavedData.DECOR) {
					sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				} else {
					sl.destroyBlock(pos, false);
				}
			}
			it.remove();
			changed = true;
		}
		if (changed) {
			data.setDirty();
		}
	}

	// ---- ticking ----

	private static void tick(MinecraftServer server) {
		long now = now(server);
		for (UUID id : List.copyOf(RUNS.keySet())) {
			Run r = RUNS.get(id);
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (r == null) {
				continue;
			}
			if (p == null) {
				RUNS.remove(id);
				purge(server, e -> e.owner.equals(id.toString()), now, true);
				continue;
			}
			if (!p.isAlive() || p.level() != r.level || (r.window && now >= r.windowEnd)) {
				end(p);
				continue;
			}
			if (r.spent && !r.window && !TechniqueAttachment.get(p).onCooldown(Technique.MAYHEM, now)) {
				RUNS.remove(id); // the batch was spent and the cooldown ran out without a channel: a fresh batch of 20 (clusters again)
				ServerPlayNetworking.send(p, new State(-1, 0));
				continue;
			}
			if (r.window) {
				long left = r.windowEnd - now;
				MayhemSavedData data = MayhemSavedData.of(server);
				if (left % MayhemRules.CONVERT_CHECK_TICKS == 0) {
					convert(p, r, data, now);
					List<BlockPos> active = refreshLights(p, r, data);
					if (left % 20 == 0) {
						burn(p, r, active);
					}
				}
				if (left % 20 == 0) {
					send(p, r, now);
				}
			}
		}
		if (now % 5 == 0) {
			MayhemSavedData data = MayhemSavedData.of(server);
			if (!data.blocks.isEmpty()) {
				purge(server, e -> true, now, false);
			}
		}
	}

	private static void send(ServerPlayer p, Run r, long now) {
		ServerPlayNetworking.send(p, new State(r.shards, r.window ? (int) Math.max(0, r.windowEnd - now) : 0));
	}

	private static String dim(ServerLevel sl) {
		return sl.dimension().identifier().toString();
	}
}
