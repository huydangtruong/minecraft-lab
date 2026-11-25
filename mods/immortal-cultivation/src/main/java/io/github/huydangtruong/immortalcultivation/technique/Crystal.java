package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.technique.CrystalRules.Cell;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/**
 * D143 Crystal Prison. A 3x3x3 hollow cage of crystal closes around up to two living things. Left click cages the mob or
 * player you aim at (or, aiming at a block, the spot in front of it); right click cages you and the nearest other one.
 * Hostile mobs inside cannot attack for a while; the crystal vanishes when the cage time is over (CrystalSavedData).
 */
public final class Crystal {
	private static final Random RNG = new Random();

	private Crystal() {}

	public static void init() {
		CrystalBlock.init();
		CrystalDisarm.init();
		ServerTickEvents.END_SERVER_TICK.register(Crystal::tick);
		// A disarmed player (Death Match opponent) cannot hit or shoot, but keeps their techniques (Huy, D146).
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
			player instanceof ServerPlayer sp && CrystalDisarm.disarmed(sp) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(source.getEntity() instanceof ServerPlayer sp && CrystalDisarm.disarmed(sp) && !TechniqueDamage.active() && isWeaponHit(sp, source)));
	}

	/** A hit with the hand or a held weapon, or an arrow or trident: what a disarmed player may not do. */
	static boolean isWeaponHit(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source) {
		var direct = source.getDirectEntity();
		return direct == p || direct instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow;
	}

	// ---- cast ----

	public static boolean cast(ServerPlayer p, int level, float charge) {
		ServerLevel sl = p.level();
		boolean self = charge >= 0.5f;
		List<LivingEntity> must = new ArrayList<>();
		int cx;
		int cz;
		int floorY;
		if (self) {
			must.add(p);
			cx = p.getBlockX();
			cz = p.getBlockZ();
			floorY = p.getBlockY() - 1;
			LivingEntity near = nearest(sl, p, new Vec3(cx + 0.5, p.getY(), cz + 0.5));
			if (near != null) {
				must.add(near);
			}
		} else {
			Vec3 eye = p.getEyePosition();
			Vec3 end = eye.add(p.getViewVector(1f).scale(CrystalRules.AIM_RANGE));
			BlockHitResult block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
			Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
			EntityHitResult ent = ProjectileUtil.getEntityHitResult(sl, p, eye, limit, new AABB(eye, limit).inflate(1.0),
				e -> e instanceof LivingEntity le && TechniqueDamage.affectable(le, p), 0.3f);
			if (ent != null && ent.getEntity() instanceof LivingEntity target) {
				if (CrystalRules.tooBig(target.getBbWidth(), target.getBbHeight())) {
					return false;
				}
				must.add(target);
				cx = target.getBlockX();
				cz = target.getBlockZ();
				floorY = target.getBlockY() - 1;
			} else if (block.getType() != HitResult.Type.MISS) {
				BlockPos at = block.getBlockPos().relative(block.getDirection());
				cx = at.getX();
				cz = at.getZ();
				floorY = at.getY() - 1;
			} else {
				return false;
			}
		}
		if (floorY < sl.getMinY() || floorY + 4 >= sl.getMaxY()) {
			return false;
		}
		build(p, sl, level, cx, floorY, cz, must);
		return true;
	}

	/** The nearest other living thing that could share the cage of a caster standing at {@code at}. */
	private static LivingEntity nearest(ServerLevel sl, ServerPlayer p, Vec3 at) {
		AABB zone = new AABB(at.x - 2.5, at.y - 1, at.z - 2.5, at.x + 2.5, at.y + 3, at.z + 2.5);
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, zone, e -> TechniqueDamage.affectable(e, p))) {
			if (CrystalRules.tooBig(e.getBbWidth(), e.getBbHeight())) {
				continue;
			}
			double d = e.distanceToSqr(p);
			if (d < bestDist) {
				bestDist = d;
				best = e;
			}
		}
		return best;
	}

	private static void build(ServerPlayer p, ServerLevel sl, int level, int cx, int floorY, int cz, List<LivingEntity> must) {
		CrystalRules.Cage cage = CrystalRules.cage(cx, floorY, cz);
		double mx = cx + 0.5;
		double mz = cz + 0.5;
		AABB zone = new AABB(cx - 2, floorY, cz - 2, cx + 3, floorY + 5, cz + 3);
		List<LivingEntity> inZone = sl.getEntitiesOfClass(LivingEntity.class, zone, e -> TechniqueDamage.affectable(e, p));
		for (LivingEntity m : must) {
			if (!inZone.contains(m)) {
				inZone.add(m);
			}
		}
		// who stays: the ones asked for first, then a random pick among the others standing in the interior footprint
		List<LivingEntity> kept = new ArrayList<>();
		for (LivingEntity m : must) {
			if (kept.size() < CrystalRules.MAX_CAGED && !CrystalRules.tooBig(m.getBbWidth(), m.getBbHeight())) {
				kept.add(m);
			}
		}
		List<LivingEntity> others = new ArrayList<>();
		for (LivingEntity e : inZone) {
			if (!kept.contains(e) && !must.contains(e) && !CrystalRules.tooBig(e.getBbWidth(), e.getBbHeight())
				&& Math.abs(e.getX() - mx) < 1.5 + e.getBbWidth() / 2 && Math.abs(e.getZ() - mz) < 1.5 + e.getBbWidth() / 2) {
				others.add(e);
			}
		}
		int slots = CrystalRules.MAX_CAGED - kept.size();
		if (slots > 0 && !others.isEmpty()) {
			for (int idx : CrystalRules.pickKept(others.size(), RNG)) {
				if (kept.size() < CrystalRules.MAX_CAGED) {
					kept.add(others.get(idx));
				}
			}
		}
		// everyone else goes outside the shell first, while the space is still free
		for (LivingEntity e : inZone) {
			if (!kept.contains(e)) {
				pushOut(sl, e, mx, mz);
			}
		}
		// the cage
		BlockState air = Blocks.AIR.defaultBlockState();
		for (Cell c : cage.interior()) {
			BlockPos pos = new BlockPos(c.x(), c.y(), c.z());
			if (replaceable(sl, pos)) {
				clear(sl, pos);
				sl.setBlock(pos, air, 3);
			}
		}
		List<Long> placed = new ArrayList<>();
		BlockState crystal = CrystalBlock.state(level);
		for (Cell c : cage.shell()) {
			BlockPos pos = new BlockPos(c.x(), c.y(), c.z());
			if (replaceable(sl, pos)) {
				clear(sl, pos);
				sl.setBlock(pos, crystal, 3);
				placed.add(pos.asLong());
			}
		}
		long expiry = sl.getServer().overworld().getGameTime() + Math.round(CrystalRules.cageSeconds(level) * 20);
		BlockPos min = new BlockPos(cx - 1, floorY + 1, cz - 1);
		BlockPos max = new BlockPos(cx + 1, floorY + 3, cz + 1);
		CrystalSavedData data = CrystalSavedData.of(sl.getServer());
		data.cages.add(new CrystalSavedData.Entry(sl.dimension().identifier().toString(), expiry, placed, min.asLong(), max.asLong()));
		data.setDirty();
		// move the kept ones into the middle of the hollow and disarm the hostile ones
		int disarmTicks = (int) Math.round(CrystalRules.disarmSeconds(level) * 20);
		for (LivingEntity e : kept) {
			double hw = e.getBbWidth() / 2;
			double room = Math.max(0, 1.5 - hw);
			double x = Math.max(mx - room, Math.min(mx + room, e.getX()));
			double z = Math.max(mz - room, Math.min(mz + room, e.getZ()));
			e.teleportTo(sl, x, floorY + 1, z, Set.of(), e.getYRot(), e.getXRot(), false);
			e.resetFallDistance();
			e.setDeltaMovement(Vec3.ZERO);
			if (shouldDisarm(p, e)) {
				e.addEffect(new MobEffectInstance(CrystalDisarm.EFFECT, disarmTicks, 0, false, true, true));
			}
		}
		for (Cell c : cage.shell()) {
			if (RNG.nextInt(3) == 0) {
				sl.sendParticles(ParticleTypes.END_ROD, c.x() + 0.5, c.y() + 0.5, c.z() + 0.5, 1, 0.3, 0.3, 0.3, 0.02);
			}
		}
		sl.playSound(null, mx, floorY + 2, mz, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 0.8f);
		sl.playSound(null, mx, floorY + 2, mz, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
	}

	/** Unbreakable blocks (bedrock, barriers) stay. */
	private static boolean replaceable(ServerLevel sl, BlockPos pos) {
		BlockState s = sl.getBlockState(pos);
		return s.isAir() || s.getDestroySpeed(sl, pos) >= 0 && !io.github.huydangtruong.immortalcultivation.purgatory.Purgatory.guarded(sl, pos); // D259: not the dungeon's blocks
	}

	/** A container in the way spills its contents instead of deleting them. */
	private static void clear(ServerLevel sl, BlockPos pos) {
		BlockEntity be = sl.getBlockEntity(pos);
		if (be instanceof Container c) {
			Containers.dropContents(sl, pos, c);
			c.clearContent();
		}
	}

	private static void pushOut(ServerLevel sl, LivingEntity e, double mx, double mz) {
		double[] off = CrystalRules.pushOutOffset(e.getX() - mx, e.getZ() - mz, e.getBbWidth());
		double x = mx + off[0];
		double z = mz + off[1];
		for (int up = 0; up <= 5; up++) {
			double y = e.getY() + up;
			AABB box = e.getBoundingBox().move(x - e.getX(), y - e.getY(), z - e.getZ());
			if (sl.noCollision(e, box)) {
				e.teleportTo(sl, x, y, z, Set.of(), e.getYRot(), e.getXRot(), false);
				e.setDeltaMovement(Vec3.ZERO);
				return;
			}
		}
	}

	/** Hostile mobs and the Death Match opponent are disarmed; allies, pets, grasped mobs and bystander players are not. */
	static boolean shouldDisarm(ServerPlayer caster, LivingEntity e) {
		if (e == caster || !e.isAlive()) {
			return false;
		}
		if (e instanceof ServerPlayer tp) {
			MinecraftServer server = caster.level().getServer();
			return DeathMatch.running(server) && !tp.isCreative() && !tp.isSpectator();
		}
		return e instanceof Mob m && e instanceof Enemy && !(e instanceof OwnedSummon) && !Grasp.isGrasped(m);
	}

	// ---- expiry ----

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) {
			return;
		}
		CrystalSavedData data = CrystalSavedData.of(server);
		if (data.cages.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		boolean changed = false;
		for (Iterator<CrystalSavedData.Entry> it = data.cages.iterator(); it.hasNext(); ) {
			CrystalSavedData.Entry c = it.next();
			if (now < c.expiry) {
				continue;
			}
			ServerLevel sl = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(c.dimension)));
			if (sl == null) {
				it.remove();
				changed = true;
				continue;
			}
			List<Long> left = new ArrayList<>();
			for (long packed : c.blocks) {
				BlockPos pos = BlockPos.of(packed);
				if (!sl.isLoaded(pos)) {
					left.add(packed); // chunk not loaded: try again later
					continue;
				}
				if (sl.getBlockState(pos).getBlock() instanceof CrystalBlock) {
					sl.destroyBlock(pos, false);
				}
			}
			changed = true;
			if (left.isEmpty()) {
				it.remove();
			} else {
				c.blocks = left;
			}
		}
		if (changed) {
			data.setDirty();
		}
	}

	/** True when the entity stands in the hollow of a cage that is still up (Enderman teleport guard). */
	public static boolean inCage(Entity e) {
		if (!(e.level() instanceof ServerLevel sl)) {
			return false;
		}
		MinecraftServer server = sl.getServer();
		CrystalSavedData data = CrystalSavedData.of(server);
		if (data.cages.isEmpty()) {
			return false;
		}
		long now = server.overworld().getGameTime();
		String dim = sl.dimension().identifier().toString();
		for (CrystalSavedData.Entry c : data.cages) {
			if (now >= c.expiry || !c.dimension.equals(dim)) {
				continue;
			}
			BlockPos min = BlockPos.of(c.min);
			BlockPos max = BlockPos.of(c.max);
			if (new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1).inflate(0.5).contains(e.position())) {
				return true;
			}
		}
		return false;
	}
}
