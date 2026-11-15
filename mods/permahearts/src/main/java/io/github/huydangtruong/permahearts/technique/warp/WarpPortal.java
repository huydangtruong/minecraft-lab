package io.github.huydangtruong.permahearts.technique.warp;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Cosmic Warping portal (D75): a harmless standing marker that stays forever. Nothing removes it except the server
 * code that handles the X button of the warp menu (and the stale-copy check, see {@link Warp#init}). Its list entry
 * lives in {@link WarpSavedData}; this entity only carries the inner colour so every client can draw it.
 */
public final class WarpPortal extends Entity {
	public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "warp_portal"));
	public static final EntityType<WarpPortal> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.<WarpPortal>of(WarpPortal::new, MobCategory.MISC).sized(1.0f, 1.5f).clientTrackingRange(10)
			.updateInterval(20).fireImmune().noSummon().build(KEY));

	/** Inner colour 0xRRGGBB, synced to every client. */
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(WarpPortal.class, EntityDataSerializers.INT);

	public WarpPortal(EntityType<? extends WarpPortal> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
		setPermanentlyInvulnerable(true);
	}

	/** Touches the class so the type is registered. */
	public static void init() {}

	public int color() {
		return this.entityData.get(COLOR);
	}

	public void setColor(int rgb) {
		this.entityData.set(COLOR, rgb);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(COLOR, 0x40E0D0);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		setColor(in.getIntOr("color", 0x40E0D0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		out.putInt("color", color());
	}

	// ---- unbreakable and untouchable ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	/** Not even /kill removes it (the X in the menu and the stale-copy check call discard()). */
	@Override
	public void kill(ServerLevel level) {}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean skipAttackInteraction(Entity attacker) {
		return true;
	}

	@Override
	public boolean isPickable() {
		return false; // clicks and hits go through to what is behind it
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith(Entity other) {
		return false;
	}

	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		return true;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	// ---- looks ----

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			clientParticles();
		}
	}

	/** A few purple sparks on the rim and some coloured ones inside; the quads are drawn by the renderer. */
	private void clientParticles() {
		var rnd = level().getRandom();
		if (tickCount % 3 != 0) {
			return;
		}
		double yaw = Math.toRadians(getYRot());
		double sx = Math.cos(yaw); // the portal plane runs along (cos, sin) of the yaw
		double sz = Math.sin(yaw);
		double a = rnd.nextDouble() * Math.PI * 2;
		double ex = Math.cos(a) * 0.5, ey = 0.75 + Math.sin(a) * 0.75;
		level().addParticle(ParticleTypes.PORTAL, getX() + sx * ex, getY() + ey, getZ() + sz * ex, 0, 0.02, 0);
		if (rnd.nextInt(2) == 0) {
			double b = rnd.nextDouble() * Math.PI * 2, r = rnd.nextDouble() * 0.4;
			level().addParticle(new DustParticleOptions(color(), 0.7f), getX() + sx * Math.cos(b) * r,
				getY() + 0.75 + Math.sin(b) * r * 1.5, getZ() + sz * Math.cos(b) * r, 0, 0.01, 0);
		}
	}
}
