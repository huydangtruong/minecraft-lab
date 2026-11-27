package io.github.huydangtruong.immortalcultivation.technique;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * D86 Thunder Strike. The caster aims, the point is marked for 1 s (particle ring), then a visual-only lightning bolt
 * and a creeper-style explosion hit it. The strike is stored by caster id and runs from the server tick, so it still
 * happens when the caster dies or logs out. Damage comes from the technique (the explosion itself hurts nobody).
 */
public final class Thunder {
	private record Strike(ServerLevel level, Vec3 center, UUID caster, int techLevel, long at) {}

	private static final List<Strike> PENDING = new ArrayList<>();

	private Thunder() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Thunder::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> PENDING.clear());
	}

	/**
	 * The point to strike: the first block or entity on the look ray within the range; with nothing hit, the end of the
	 * ray dropped straight down to the first solid ground (the point itself over the void).
	 */
	public static Vec3 aim(ServerPlayer p) {
		ServerLevel sl = p.level();
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getViewVector(1f).scale(TechniqueRules.THUNDER_RANGE));
		BlockHitResult block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
		EntityHitResult ent = ProjectileUtil.getEntityHitResult(sl, p, eye, limit, new AABB(eye, limit).inflate(1.0),
			e -> e instanceof LivingEntity le && TechniqueDamage.affectable(le, p) && !isOwnSummon(le, p.getUUID()), 0.3f);
		if (ent != null) {
			return ent.getLocation();
		}
		if (block.getType() != HitResult.Type.MISS) {
			return block.getLocation();
		}
		BlockHitResult down = sl.clip(new ClipContext(end, new Vec3(end.x, sl.getMinY(), end.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		return down.getType() == HitResult.Type.MISS ? end : down.getLocation();
	}

	/** Casts: marks the point and schedules the strike. Always works (the cooldown starts in TechniqueNet). */
	public static boolean cast(ServerPlayer p, int level) {
		return cast(p, level, aim(p));
	}

	public static boolean cast(ServerPlayer p, int level, Vec3 at) {
		ServerLevel sl = p.level();
		PENDING.add(new Strike(sl, at, p.getUUID(), level, sl.getGameTime() + TechniqueRules.THUNDER_WARN_TICKS));
		sl.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4f, 2f);
		ring(sl, at, level);
		return true;
	}

	private static boolean isOwnSummon(Entity e, UUID caster) {
		return e instanceof OwnedSummon s && caster.equals(s.owner());
	}

	private static void ring(ServerLevel sl, Vec3 c, int level) {
		double r = TechniqueRules.thunderRadius(level);
		int points = Math.max(24, (int) (r * 10));
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0.02, 0.05, 0.02, 0.0);
			if (i % 4 == 0) {
				sl.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.0);
			}
		}
		for (int i = 0; i < 10; i++) {
			double a = sl.getRandom().nextDouble() * Math.PI * 2;
			double d = Math.sqrt(sl.getRandom().nextDouble()) * r;
			sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * d, c.y + 0.1, c.z + Math.sin(a) * d, 1, 0, 0.1, 0, 0.0);
		}
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Iterator<Strike> it = PENDING.iterator(); it.hasNext();) {
			Strike s = it.next();
			long now = s.level().getGameTime();
			if (now < s.at()) {
				if ((s.at() - now) % 4 == 0) {
					ring(s.level(), s.center(), s.techLevel());
				}
				continue;
			}
			it.remove();
			if (s.level().isLoaded(BlockPos.containing(s.center()))) {
				strike(s);
			}
		}
	}

	private static void strike(Strike s) {
		ServerLevel sl = s.level();
		Vec3 c = s.center();
		int lvl = s.techLevel();
		double r = TechniqueRules.thunderRadius(lvl);
		ServerPlayer caster = sl.getServer().getPlayerList().getPlayer(s.caster());
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(sl, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setPos(c.x, c.y, c.z);
			bolt.setVisualOnly(true); // no fire, no charged creepers or pigs from the bolt itself
			bolt.setCause(caster);
			sl.addFreshEntity(bolt);
			// the vanilla transformations, for every such mob in the blast radius (before the damage)
			for (LivingEntity e : near(sl, c, r, caster)) {
				if (e instanceof Pig || e instanceof Villager || e instanceof Creeper || e instanceof MushroomCow || e instanceof CopperGolem) {
					e.thunderHit(sl, bolt);
					e.clearFire(); // the generic part of thunderHit sets fire
				}
			}
		}
		sl.explode(null, null, new NoEntityDamage(sl), c.x, c.y, c.z, (float) TechniqueRules.thunderPower(lvl), false,
			Level.ExplosionInteraction.MOB); // D269: in the Purgatory dungeon module shells stay whole (NoEntityDamage)
		for (LivingEntity e : near(sl, c, r, caster)) {
			double d = distance(e, c);
			float dmg = (float) TechniqueRules.thunderDamageAt(lvl, d);
			if (dmg <= 0) {
				continue;
			}
			TechniqueDamage.hurt(sl, e, caster, null, DamageTypes.MAGIC, dmg);
			double dx = e.getX() - c.x;
			double dz = e.getZ() - c.z;
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len < 1e-4) {
				dx = 1;
				dz = 0;
				len = 1;
			}
			double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
			double push = 1.2 * (1 - d / r) * resist;
			e.setDeltaMovement(e.getDeltaMovement().add(dx / len * push, 0.4 * resist, dz / len * push));
			e.needsSync = true;
		}
	}

	/** Living entities the strike may touch: not the caster, not the caster's summons. */
	private static List<LivingEntity> near(ServerLevel sl, Vec3 c, double r, ServerPlayer caster) {
		return sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r + 1),
			e -> TechniqueDamage.foe(e, caster) && distance(e, c) <= r);
	}

	/** Distance from the point to the closest point of the entity's box. */
	private static double distance(Entity e, Vec3 c) {
		AABB b = e.getBoundingBox();
		double dx = Math.max(Math.max(b.minX - c.x, 0), c.x - b.maxX);
		double dy = Math.max(Math.max(b.minY - c.y, 0), c.y - b.maxY);
		double dz = Math.max(Math.max(b.minZ - c.z, 0), c.z - b.maxZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** The explosion breaks blocks like a creeper but hurts nobody: the technique deals the damage. */
	private static final class NoEntityDamage extends ExplosionDamageCalculator {
		private final ServerLevel level;

		NoEntityDamage(ServerLevel level) {
			this.level = level;
		}

		@Override
		public boolean shouldBlockExplode(Explosion explosion, net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos pos,
				net.minecraft.world.level.block.state.BlockState state, float power) {
			return super.shouldBlockExplode(explosion, getter, pos, state, power)
				&& !io.github.huydangtruong.immortalcultivation.purgatory.Purgatory.guarded(level, pos);
		}

		@Override
		public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
			return false;
		}
	}
}
