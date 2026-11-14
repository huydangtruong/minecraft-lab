package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Violet Guardian (D60): an iron golem that belongs to a player, like a tamed wolf. It follows the owner, fights the
 * monsters around the owner, never attacks a player and ignores the owner's own hits. It is never saved and vanishes
 * after its time, when its owner dies or leaves, or when the owner casts the technique again. The client draws it
 * with the vanilla iron golem model tinted violet.
 */
public final class VioletGuardian extends IronGolem {
	public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "violet_guardian"));
	public static final EntityType<VioletGuardian> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.<VioletGuardian>of(VioletGuardian::new, MobCategory.MISC).sized(1.4f, 2.7f).clientTrackingRange(10).noSave().noLootTable().build(KEY));

	/** Distance at which a guardian walks back to its owner, and the distance at which it teleports instead. */
	private static final double FOLLOW_FROM = 8, TELEPORT_FROM = 16;

	private UUID owner;
	private long endTick;

	public VioletGuardian(EntityType<? extends IronGolem> type, Level level) {
		super(type, level);
	}

	/** Registers the type and its attributes. Called once from PermaHearts (through TechniqueCasts.init). */
	public static void init() {
		FabricDefaultAttributeRegistry.register(TYPE, IronGolem.createAttributes());
	}

	// ---- summoning ----

	/** Summons the guardians of a level for the owner; the old ones are removed first. Returns the new ones. */
	public static List<VioletGuardian> summon(ServerPlayer owner, int level) {
		ServerLevel sl = owner.level();
		dismissAll(owner);
		int count = TechniqueRules.golemCount(level);
		double power = TechniqueRules.golemPower(level);
		long end = sl.getGameTime() + TechniqueRules.GOLEM_SECONDS * 20L;
		List<VioletGuardian> out = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			VioletGuardian g = TYPE.create(sl, EntitySpawnReason.MOB_SUMMONED);
			if (g == null) {
				continue;
			}
			double a = owner.getYRot() * Math.PI / 180 + Math.PI * 2 * i / count + Math.PI / count;
			Vec3 at = freeSpotNear(sl, g, owner.position().add(-Math.sin(a) * 2.5, 0, Math.cos(a) * 2.5), owner.position());
			g.snapTo(at.x, at.y, at.z, owner.getYRot(), 0f);
			g.owner = owner.getUUID();
			g.endTick = end;
			g.applyPower(power);
			g.setCustomName(Component.literal(owner.getGameProfile().name() + "'s Guardian"));
			g.setPersistenceRequired();
			sl.addFreshEntity(g);
			puff(sl, g);
			out.add(g);
		}
		return out;
	}

	private static Vec3 freeSpotNear(ServerLevel sl, VioletGuardian g, Vec3 want, Vec3 fallback) {
		for (int dy : new int[] {0, 1, 2, -1}) {
			Vec3 at = want.add(0, dy, 0);
			if (sl.noCollision(g, g.getDimensions(g.getPose()).makeBoundingBox(at))) {
				return at;
			}
		}
		return fallback;
	}

	/** Removes every guardian of the owner (with a puff). */
	public static int dismissAll(Player owner) {
		if (!(owner.level() instanceof ServerLevel sl)) {
			return 0;
		}
		List<? extends VioletGuardian> mine = guardiansOf(sl, owner.getUUID());
		for (VioletGuardian g : mine) {
			puff(sl, g);
			g.discard();
		}
		return mine.size();
	}

	public static List<? extends VioletGuardian> guardiansOf(ServerLevel sl, UUID owner) {
		return sl.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forExactClass(VioletGuardian.class), g -> owner.equals(g.owner) && g.isAlive());
	}

	private static void puff(ServerLevel sl, LivingEntity g) {
		sl.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + g.getBbHeight() / 2, g.getZ(), 25, 0.5, 0.8, 0.5, 0.05);
		sl.sendParticles(new DustParticleOptions(0xb45cff, 1.4f), g.getX(), g.getY() + g.getBbHeight() / 2, g.getZ(), 20, 0.6, 1.0, 0.6, 0.0);
		sl.playSound(null, g.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 1f, 1f);
	}

	/** Health and attack damage as a multiple of a vanilla iron golem's. */
	void applyPower(double power) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(TechniqueRules.GOLEM_BASE_HEALTH * power);
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TechniqueRules.GOLEM_BASE_DAMAGE * power);
		setHealth(getMaxHealth());
	}

	public UUID owner() {
		return owner;
	}

	/** Game time at which this guardian vanishes. */
	public long endTick() {
		return endTick;
	}

	/** Changes the vanishing time (used by game tests to fast-forward). */
	public void setEndTick(long tick) {
		endTick = tick;
	}

	// ---- behaviour ----

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(2, new FollowOwnerGoal(this));
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new DefendOwnerGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel sl) {
			Player o = owner == null ? null : sl.getPlayerByUUID(owner);
			if (o == null || !o.isAlive() || sl.getGameTime() >= endTick) {
				puff(sl, this);
				discard();
			} else if (tickCount % 10 == 0) {
				sl.sendParticles(new DustParticleOptions(0xb45cff, 0.8f), getX(), getY() + 1.6, getZ(), 2, 0.5, 0.9, 0.5, 0.0);
			}
		}
	}

	/** Never hurt by its owner (nothing to retaliate against). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Never attacks a player. */
	@Override
	public boolean canAttack(LivingEntity target) {
		return !(target instanceof Player) && !(target instanceof VioletGuardian) && super.canAttack(target);
	}

	private Player ownerPlayer() {
		return owner == null ? null : level().getPlayerByUUID(owner);
	}

	/** Walks back to the owner when far, teleports next to the owner when very far. */
	private static final class FollowOwnerGoal extends Goal {
		private final VioletGuardian g;

		FollowOwnerGoal(VioletGuardian g) {
			this.g = g;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			Player o = g.ownerPlayer();
			return o != null && g.getTarget() == null && g.distanceToSqr(o) > FOLLOW_FROM * FOLLOW_FROM;
		}

		@Override
		public boolean canContinueToUse() {
			Player o = g.ownerPlayer();
			return o != null && g.getTarget() == null && g.distanceToSqr(o) > 3 * 3;
		}

		@Override
		public void tick() {
			Player o = g.ownerPlayer();
			if (o == null) {
				return;
			}
			if (g.distanceToSqr(o) > TELEPORT_FROM * TELEPORT_FROM && g.level() instanceof ServerLevel sl) {
				Vec3 at = g.freeSpotNear(sl, g, o.position().add(g.random.nextDouble() * 4 - 2, 0, g.random.nextDouble() * 4 - 2), o.position());
				g.getNavigation().stop();
				g.snapTo(at.x, at.y, at.z);
				return;
			}
			if (g.tickCount % 10 == 0) {
				g.getNavigation().moveTo(o, 1.1);
			}
		}

		@Override
		public void stop() {
			g.getNavigation().stop();
		}
	}

	/** Targets what hurt the owner, what the owner hit and any monster close to the owner; never players. */
	private static final class DefendOwnerGoal extends TargetGoal {
		private final VioletGuardian g;
		private int wait;

		DefendOwnerGoal(VioletGuardian g) {
			super(g, false);
			this.g = g;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (--wait > 0) {
				return false;
			}
			wait = 10;
			Player o = g.ownerPlayer();
			if (o == null) {
				return false;
			}
			LivingEntity t = pick(o);
			if (t == null) {
				return false;
			}
			targetMob = t;
			return true;
		}

		private boolean valid(LivingEntity e, Player o) {
			return e != null && e.isAlive() && !(e instanceof Player) && !(e instanceof VioletGuardian) && !e.isSpectator()
				&& e.distanceToSqr(g) < 40 * 40 && e.distanceToSqr(o) < 32 * 32;
		}

		private LivingEntity pick(Player o) {
			if (o.tickCount - o.getLastHurtByMobTimestamp() < 100 && valid(o.getLastHurtByMob(), o)) {
				return o.getLastHurtByMob();
			}
			if (o.tickCount - o.getLastHurtMobTimestamp() < 100 && valid(o.getLastHurtMob(), o)) {
				return o.getLastHurtMob();
			}
			AABB box = o.getBoundingBox().inflate(12, 6, 12);
			Mob best = null;
			for (Mob m : g.level().getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && valid(m, o))) {
				if (best == null || m.distanceToSqr(g) < best.distanceToSqr(g)) {
					best = m;
				}
			}
			return best;
		}
	}
}
