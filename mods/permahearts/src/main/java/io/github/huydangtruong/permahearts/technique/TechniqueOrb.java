package io.github.huydangtruong.permahearts.technique;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The thrown Flame Burst / Frost Orb. It is a vanilla Snowball (so every client already renders it, here as a fire
 * charge or a snowball) with its own impact: damage to the entity hit and a field at the impact point; a fire orb also
 * sets the hit block side and flammable blocks in its field on fire (D55). Not saved.
 */
public final class TechniqueOrb extends Snowball {
	/** Slack around a target's box when sweeping, so a fast orb does not slip past (blocks). */
	private static final double HIT_MARGIN = 0.3;

	/** Ticks after which a flying orb ends where it is. */
	private static final int MAX_AGE = 200;

	private final Technique technique;
	private boolean landed;
	private final int techLevel;

	public TechniqueOrb(ServerLevel level, ServerPlayer caster, Technique technique, int techLevel) {
		super(level, caster, new ItemStack(technique == Technique.FIRE ? Items.FIRE_CHARGE : Items.SNOWBALL));
		this.technique = technique;
		this.techLevel = techLevel;
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel sl && sweepForTarget(sl)) {
			return;
		}
		super.tick();
		if (!isRemoved() && level() instanceof ServerLevel sl) {
			ParticleOptions trail = technique == Technique.FIRE ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
			sl.sendParticles(trail, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
			if (tickCount > MAX_AGE || isInWater() || getY() < sl.getMinY()) {
				endHere(sl); // lifetime end, splashed into water or fell out of the world: still lands where it is
			}
		}
	}

	/**
	 * Vanilla projectile hit detection let a slow orb slip past a mob standing right in its path in game tests, so the
	 * orb sweeps its own next step against every affectable living entity (and stops at a wall before them).
	 */
	private boolean sweepForTarget(ServerLevel sl) {
		Vec3 from = position();
		Vec3 step = getDeltaMovement();
		Vec3 to = from.add(step);
		double wall = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
			.getLocation().distanceToSqr(from);
		LivingEntity best = null;
		Vec3 bestAt = null;
		double bestDist = Double.MAX_VALUE;
		AABB area = getBoundingBox().expandTowards(step).inflate(1.0);
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area, e -> TechniqueDamage.affectable(e, getOwner()))) {
			AABB box = e.getBoundingBox().inflate(HIT_MARGIN);
			Optional<Vec3> at = box.contains(from) ? Optional.of(from) : box.clip(from, to);
			if (at.isPresent()) {
				double d = at.get().distanceToSqr(from);
				if (d < bestDist) {
					best = e;
					bestAt = at.get();
					bestDist = d;
				}
			}
		}
		if (best == null || bestDist > wall) {
			return false;
		}
		onHit(new EntityHitResult(best, bestAt));
		return true;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	/** Every end of an orb (block, entity, lifetime, water, void) goes through here: one impact at the last position. */
	private void endHere(ServerLevel sl) {
		if (landed) {
			return;
		}
		landed = true;
		double y = Math.max(getY(), sl.getMinY() + 1.0);
		impact(sl, getOwner() instanceof ServerPlayer p ? p : null, new Vec3(getX(), y, getZ()));
		discard();
	}

	@Override
	protected void onHit(HitResult hit) {
		if (!(level() instanceof ServerLevel sl) || landed) {
			return;
		}
		landed = true;
		ServerPlayer caster = getOwner() instanceof ServerPlayer p ? p : null;
		Vec3 at = hit.getLocation();
		if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target
			&& TechniqueDamage.affectable(target, getOwner())) {
			double damage = technique == Technique.FIRE
				? TechniqueRules.fireImpactDamage(techLevel) : TechniqueRules.iceImpactDamage(techLevel);
			TechniqueDamage.hurt(sl, target, caster, this, DamageTypes.MAGIC, (float) damage);
			at = target.position();
		}
		if (technique == Technique.FIRE && hit instanceof BlockHitResult bh) {
			// D55: like a fire charge, the block side that was hit catches fire
			placeFire(sl, bh.getBlockPos().relative(bh.getDirection()));
		}
		impact(sl, caster, at);
		discard();
	}

	/**
	 * D55: real fire on flammable blocks (wood, leaves, wool...) inside the fire field, like vanilla fire: it can spread
	 * and burn wooden builds (the doFireTick game rule stops spreading). Stone and other non-flammable blocks never burn.
	 */
	private static void igniteFlammable(ServerLevel sl, Vec3 at, double radius) {
		BlockPos c = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, 2, r))) {
			if (pos.distToCenterSqr(at) > radius * radius || !sl.getBlockState(pos).isAir()) {
				continue;
			}
			for (Direction d : Direction.values()) {
				if (sl.getBlockState(pos.relative(d)).ignitedByLava()) {
					placeFire(sl, pos.immutable());
					break;
				}
			}
		}
	}

	private static void placeFire(ServerLevel sl, BlockPos pos) {
		if (sl.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(sl, pos, Direction.UP)) {
			sl.setBlockAndUpdate(pos, BaseFireBlock.getState(sl, pos));
		}
	}

	private void impact(ServerLevel sl, ServerPlayer caster, Vec3 at) {
		UUID owner = caster != null ? caster.getUUID() : new UUID(0, 0);
		if (technique == Technique.FIRE) {
			TechniqueFields.add(TechniqueFields.Kind.FIRE, sl, at, TechniqueRules.fireFieldRadius(techLevel),
				TechniqueRules.fireFieldSeconds(techLevel), techLevel, owner);
			igniteFlammable(sl, at, TechniqueRules.fireFieldRadius(techLevel));
			sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 40, 0.4, 0.3, 0.4, 0.08);
			sl.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.8f);
		} else {
			TechniqueFields.add(TechniqueFields.Kind.ICE, sl, at, TechniqueRules.iceRadius(techLevel),
				TechniqueRules.iceAreaSeconds(techLevel), techLevel, owner);
			sl.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.2, at.z, 40, 0.4, 0.3, 0.4, 0.08);
			sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y + 0.2, at.z, 20, 0.3, 0.3, 0.3, 0.05);
			sl.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 1.3f);
		}
	}
}
