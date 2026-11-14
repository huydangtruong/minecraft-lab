package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.ArrayList;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Warrior Angel (D63): a vex that belongs to a player. It flies through walls (the vex already does), follows the
 * owner, attacks the monsters around the owner, never attacks a player and ignores the owner's own hits. Never saved;
 * it vanishes after its time, when its owner dies or leaves, or when the owner casts the technique again. The client
 * draws it with the vex model and an original gold and white texture.
 */
public final class WarriorAngel extends Vex implements OwnedSummon {
	public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "warrior_angel"));
	public static final EntityType<WarriorAngel> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.<WarriorAngel>of(WarriorAngel::new, MobCategory.MISC).sized(0.4f, 0.8f).eyeHeight(0.51375f)
			.ridingOffset(0.04f).clientTrackingRange(8).noSave().noLootTable().build(KEY));

	/** Distance beyond which an idle angel flies back to its owner, and the distance at which it teleports instead. */
	private static final double FOLLOW_FROM = 6, TELEPORT_FROM = 24;

	private UUID owner;
	private long endTick;

	public WarriorAngel(EntityType<? extends Vex> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	/** Registers the type and its attributes (the vanilla vex ones). Called once from PermaHearts (through TechniqueCasts.init). */
	public static void init() {
		FabricDefaultAttributeRegistry.register(TYPE, Vex.createAttributes());
	}

	// ---- summoning ----

	/** Summons the angels of a level for the owner; the old ones are removed first. Returns the new ones. */
	public static List<WarriorAngel> summon(ServerPlayer owner, int level) {
		ServerLevel sl = owner.level();
		dismissAll(owner);
		int count = TechniqueRules.angelCount(level);
		long end = sl.getGameTime() + TechniqueRules.ANGEL_SECONDS * 20L;
		List<WarriorAngel> out = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			WarriorAngel a = TYPE.create(sl, EntitySpawnReason.MOB_SUMMONED);
			if (a == null) {
				continue;
			}
			double ang = owner.getYRot() * Math.PI / 180 + Math.PI * 2 * i / count + Math.PI / count;
			Vec3 at = owner.position().add(-Math.sin(ang) * 2.0, 1.2 + (i % 2) * 0.6, Math.cos(ang) * 2.0);
			a.snapTo(at.x, at.y, at.z, owner.getYRot(), 0f);
			a.owner = owner.getUUID();
			a.endTick = end;
			a.applyLevel(level);
			a.setCustomName(Component.literal(owner.getGameProfile().name() + "'s Angel"));
			a.setPersistenceRequired();
			sl.addFreshEntity(a);
			puff(sl, a);
			out.add(a);
		}
		return out;
	}

	/** Removes every angel of the owner (with a puff). */
	public static int dismissAll(Player owner) {
		if (!(owner.level() instanceof ServerLevel sl)) {
			return 0;
		}
		List<? extends WarriorAngel> mine = angelsOf(sl, owner.getUUID());
		for (WarriorAngel a : mine) {
			puff(sl, a);
			a.discard();
		}
		return mine.size();
	}

	public static List<? extends WarriorAngel> angelsOf(ServerLevel sl, UUID owner) {
		return sl.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forExactClass(WarriorAngel.class), a -> owner.equals(a.owner) && a.isAlive());
	}

	private static void puff(ServerLevel sl, LivingEntity a) {
		sl.sendParticles(ParticleTypes.POOF, a.getX(), a.getY() + a.getBbHeight() / 2, a.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
		sl.sendParticles(new DustParticleOptions(0xffd24a, 1.2f), a.getX(), a.getY() + a.getBbHeight() / 2, a.getZ(), 15, 0.4, 0.5, 0.4, 0.0);
		sl.playSound(null, a.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 1.4f);
	}

	/** Health and attack damage as a multiple of a vanilla vex's, by technique level. */
	void applyLevel(int level) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(TechniqueRules.angelHealth(level));
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TechniqueRules.angelDamage(level));
		setHealth(getMaxHealth());
	}

	@Override
	public UUID owner() {
		return owner;
	}

	/** Game time at which this angel vanishes. */
	public long endTick() {
		return endTick;
	}

	/** Changes the vanishing time (used by game tests to fast-forward). */
	public void setEndTick(long tick) {
		endTick = tick;
	}

	// ---- behaviour ----

	/** Replaces the vex goals: nothing here targets players or copies an evoker's target. The vex's own limited life stays off. */
	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new AttackGoal(this));
		goalSelector.addGoal(2, new HoverNearOwnerGoal(this));
		targetSelector.addGoal(1, new DefendOwnerGoal<>(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel sl) {
			Player o = OwnedSummon.ownerOf(this);
			if (o == null || !o.isAlive() || sl.getGameTime() >= endTick) {
				puff(sl, this);
				discard();
			} else if (tickCount % 10 == 0) {
				sl.sendParticles(new DustParticleOptions(0xffd24a, 0.7f), getX(), getY() + 0.4, getZ(), 2, 0.25, 0.3, 0.25, 0.0);
			}
		}
	}

	/** Never hurt by its owner. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Never attacks a player or another summon. */
	@Override
	public boolean canAttack(LivingEntity target) {
		return !(target instanceof Player) && !(target instanceof OwnedSummon) && super.canAttack(target);
	}

	/** D62: a monster this angel hurts counts as hurt by the owner, so a kill is the owner's kill. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		OwnedSummon.giveKillCredit(this, target);
		return super.doHurtTarget(level, target);
	}

	/** Flies into the target and hits it on contact, then charges again after a short pause. */
	private static final class AttackGoal extends Goal {
		private final WarriorAngel a;
		private int cooldown;

		AttackGoal(WarriorAngel a) {
			this.a = a;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = a.getTarget();
			return t != null && t.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void start() {
			a.setIsCharging(true);
			a.playSound(SoundEvents.VEX_CHARGE, 1f, 1.5f);
		}

		@Override
		public void stop() {
			a.setIsCharging(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity t = a.getTarget();
			if (t == null) {
				return;
			}
			cooldown--;
			if (a.getBoundingBox().inflate(0.15).intersects(t.getBoundingBox())) {
				if (cooldown <= 0 && a.level() instanceof ServerLevel sl) {
					a.doHurtTarget(sl, t);
					cooldown = 12;
				}
			} else {
				Vec3 p = t.getEyePosition();
				a.getMoveControl().setWantedPosition(p.x, p.y, p.z, 1.0);
			}
		}
	}

	/** Without a target: stays within a few blocks of the owner, drifting to a new spot now and then; teleports if left far behind. */
	private static final class HoverNearOwnerGoal extends Goal {
		private final WarriorAngel a;

		HoverNearOwnerGoal(WarriorAngel a) {
			this.a = a;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return a.getTarget() == null && OwnedSummon.ownerOf(a) != null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			Player o = OwnedSummon.ownerOf(a);
			if (o == null) {
				return;
			}
			if (a.distanceToSqr(o) > TELEPORT_FROM * TELEPORT_FROM) {
				a.snapTo(o.getX() + a.random.nextDouble() * 3 - 1.5, o.getY() + 1.5, o.getZ() + a.random.nextDouble() * 3 - 1.5);
				return;
			}
			boolean far = a.distanceToSqr(o) > FOLLOW_FROM * FOLLOW_FROM;
			if (far || (!a.getMoveControl().hasWanted() && a.random.nextInt(20) == 0)) {
				double r = far ? 2.5 : 5;
				a.getMoveControl().setWantedPosition(o.getX() + (a.random.nextDouble() - 0.5) * r,
					o.getY() + 1.0 + a.random.nextDouble() * 2.0, o.getZ() + (a.random.nextDouble() - 0.5) * r, far ? 1.3 : 0.8);
			}
		}
	}
}
