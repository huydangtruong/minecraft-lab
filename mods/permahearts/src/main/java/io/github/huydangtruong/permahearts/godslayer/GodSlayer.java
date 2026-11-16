package io.github.huydangtruong.permahearts.godslayer;

import io.github.huydangtruong.permahearts.PermaHearts;
import io.github.huydangtruong.permahearts.godslayer.GodSlayerRules.Mode;
import io.github.huydangtruong.permahearts.technique.Frostbite;
import io.github.huydangtruong.permahearts.technique.Stun;
import io.github.huydangtruong.permahearts.technique.OwnedSummon;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The God Slayer (P-032): an Oni with two katanas, 5 blocks tall at every level (D98), that hunts one player at a time.
 * It runs on the ground, flies after a target in the air (it does not pass walls), swims at three times a dolphin,
 * breaks blocks in its way like a Wither (when mob griefing is on) and teleports next to its target when it cannot
 * reach it. Only damage caused by a player (or the player's projectile or summon) hurts it, one hit takes at most 40.
 * It has no goals: all the behaviour is in {@link #customServerAiStep}. The hunt itself lives in {@link GodSlayerHunt}.
 */
public final class GodSlayer extends PathfinderMob {
	public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "god_slayer"));
	public static final EntityType<GodSlayer> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.<GodSlayer>of(GodSlayer::new, MobCategory.MISC).sized(1.6f, 4.8f).eyeHeight(4.3f)
			.clientTrackingRange(10).updateInterval(1).fireImmune().noSave().noLootTable().build(KEY));

	/** True while the boss is in the flying mode: the client shows the wings and the forward lean (D101). */
	private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(GodSlayer.class, EntityDataSerializers.BOOLEAN);

	private int bossLevel = 1;
	private UUID targetId;
	private List<GodSlayerSkill> skills;
	private GodSlayerSkill active;
	private Mode mode = Mode.GROUND;
	private LivingEntity progressTarget;
	private double bestDistance = Double.MAX_VALUE;
	private int noProgress;
	/** Ticks the boss has had no path to the target or has not moved itself, out of reach (D100). Drives the teleport. */
	private int stuckTicks;
	private Vec3 lastPos = Vec3.ZERO;
	/** Entity tick of the last hit by a player; far in the past at the start. */
	private int lastHitTick = Integer.MIN_VALUE / 2;

	public GodSlayer(EntityType<? extends GodSlayer> type, Level level) {
		super(type, level);
		xpReward = 0;
		setPersistenceRequired();
		skills = GodSlayerSkills.forLevel(1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FLYING, false);
	}

	/** Synced: is the boss in the flying mode (also true on the client). */
	public boolean isFlyingMode() {
		return entityData.get(FLYING);
	}

	/** Forces the flying state; for tests with no AI. */
	public void setFlyingMode(boolean flying) {
		entityData.set(FLYING, flying);
	}

	/** Registers the attributes. Called once from PermaHearts. */
	public static void init() {
		GodSlayerItems.init();
		FabricDefaultAttributeRegistry.register(TYPE, Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, GodSlayerRules.BASE_HEALTH)
			.add(Attributes.MOVEMENT_SPEED, GodSlayerRules.GROUND_SPEED_ATTRIBUTE)
			.add(Attributes.FOLLOW_RANGE, 128)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.ATTACK_DAMAGE, GodSlayerRules.BASE_SLASH_DAMAGE)
			.add(Attributes.STEP_HEIGHT, 1.5)
			.add(Attributes.SAFE_FALL_DISTANCE, 64));
	}

	// ---- level and target ----

	public int bossLevel() {
		return bossLevel;
	}

	/** Sets the boss level: health, damage, body size and the skills it may use. Heals to full. */
	public void setBossLevel(int level) {
		bossLevel = GodSlayerRules.clampLevel(level);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(GodSlayerRules.maxHealth(bossLevel));
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(GodSlayerRules.slashDamage(bossLevel));
		setHealth(getMaxHealth());
		skills = GodSlayerSkills.forLevel(bossLevel);
		active = null;
	}

	public void setHuntTarget(UUID id) {
		if (targetId == null || !targetId.equals(id)) {
			resetProgress();
		}
		targetId = id;
	}

	public UUID huntTarget() {
		return targetId;
	}

	public Mode mode() {
		return mode;
	}

	/** The hunted player if online, alive and in this boss's dimension, else null. */
	private ServerPlayer target() {
		if (targetId == null || !(level() instanceof ServerLevel sl)) {
			return null;
		}
		ServerPlayer p = sl.getServer().getPlayerList().getPlayer(targetId);
		return p != null && p.isAlive() && p.level() == sl ? p : null;
	}

	/** Is the target within the slash reach (3.5 blocks from the boss's body)? */
	public boolean inReachOf(LivingEntity t) {
		return getBoundingBox().inflate(GodSlayerRules.SLASH_REACH).intersects(t.getBoundingBox());
	}

	// ---- damage rules ----

	/** Only a player, a player's projectile or a player's summon can hurt it; one hit takes at most 40. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean kill = source.is(DamageTypes.GENERIC_KILL);
		Entity cause = source.getEntity();
		boolean player = cause instanceof net.minecraft.world.entity.player.Player
			|| (cause instanceof Mob m && m instanceof OwnedSummon os && os.owner() != null);
		if (!kill && !player) {
			return false;
		}
		if (cause instanceof Mob m && m instanceof OwnedSummon) {
			OwnedSummon.giveKillCredit(m, this);
		}
		boolean hurt = super.hurtServer(level, source, kill ? amount : GodSlayerRules.clampDamage(amount));
		if (hurt && !kill) {
			lastHitTick = tickCount;
		}
		return hurt;
	}

	/** Frostbite slows it a tenth as much. */
	@Override
	public boolean addEffect(MobEffectInstance effect, Entity source) {
		if (effect.getEffect().equals(Frostbite.EFFECT)) {
			int percent = GodSlayerRules.frostPercent(effect.getAmplifier() + 1);
			effect = new MobEffectInstance(Frostbite.EFFECT, effect.getDuration(), percent - 1, effect.isAmbient(),
				effect.isVisible(), effect.showIcon());
		}
		if (effect.getEffect().equals(Stun.EFFECT)) {
			effect = new MobEffectInstance(Stun.EFFECT, GodSlayerRules.stunTicks(effect.getDuration()), effect.getAmplifier(),
				effect.isAmbient(), effect.isVisible(), effect.showIcon());
		}
		return super.addEffect(effect, source);
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isAffectedByFluids() {
		return false;
	}

	@Override
	public boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public void checkDespawn() {}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	// ---- behaviour ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		ServerPlayer t = target();
		if (t == null) {
			mode = Mode.GROUND;
			setNoGravity(false);
			setFlyingMode(false);
			active = null;
			return;
		}
		if (progressTarget != t) {
			progressTarget = t;
			resetProgress();
		}
		double dist = distanceTo(t);
		boolean reach = inReachOf(t);
		trackProgress(dist, reach);
		if (GodSlayerRules.shouldTeleport(dist, true, stuckTicks)) {
			teleportNear(t);
			return;
		}
		boolean inWater = isInWater() && getFluidHeight(FluidTags.WATER) >= 1.0;
		mode = GodSlayerRules.chooseMode(inWater, heightAboveGround(t), t.getY() - getY(), noProgress, mode);
		setNoGravity(mode != Mode.GROUND);
		setFlyingMode(mode == Mode.AIR);
		move(level, t, dist, reach);
		trackStuck(reach);
		if (tickCount % 4 == 0 && !reach && (horizontalCollision || noProgress >= 20)) {
			breakBlocks(level, t);
		}
		fight(t);
		if (tickCount % 20 == 0 && getHealth() < getMaxHealth()
			&& GodSlayerRules.regenActive(tickCount - lastHitTick, noProgress, reach)) {
			heal(GodSlayerRules.regenAmount(getMaxHealth()));
		}
	}

	private void resetProgress() {
		bestDistance = Double.MAX_VALUE;
		noProgress = 0;
		stuckTicks = 0;
		lastPos = position();
	}

	/** Stuck = out of reach and (on the ground: no path) or (not moving by itself). Running after a fast target is not stuck. */
	private void trackStuck(boolean reach) {
		Vec3 now = position();
		boolean moved = now.distanceToSqr(lastPos) > 0.01 * 0.01;
		lastPos = now;
		boolean noPath = mode == Mode.GROUND && getNavigation().getPath() == null;
		if (!reach && (noPath || !moved)) {
			stuckTicks++;
		} else {
			stuckTicks = 0;
		}
	}

	/** Counts the ticks without getting closer to the target. Being in reach is progress. */
	private void trackProgress(double dist, boolean reach) {
		if (reach || dist < bestDistance - 0.05) {
			bestDistance = dist;
			noProgress = 0;
		} else {
			noProgress++;
		}
	}

	private void move(ServerLevel level, LivingEntity t, double dist, boolean reach) {
		if (mode == Mode.GROUND) {
			if (reach) {
				getNavigation().stop();
				faceTarget(t);
			} else if (tickCount % 5 == 0 || getNavigation().isDone()) {
				getNavigation().moveTo(t, 1.0);
			}
			return;
		}
		getNavigation().stop();
		faceTarget(t);
		if (reach) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		double base = getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
		double slow = base <= 0 ? 1 : Mth.clamp(getAttributeValue(Attributes.MOVEMENT_SPEED) / base, 0.05, 1);
		double speed = (mode == Mode.WATER ? GodSlayerRules.WATER_SPEED : GodSlayerRules.AIR_SPEED) * slow;
		Vec3 aim = t.position().add(0, t.getBbHeight() * 0.5, 0);
		Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
		Vec3 d = aim.subtract(from);
		double len = d.length();
		if (len > 1e-4) {
			setDeltaMovement(d.scale(Math.min(speed, len) / len));
		}
	}

	/** In the air and in water the movement is exactly the wanted velocity: no gravity, no drag, but walls still stop it. */
	@Override
	public void travel(Vec3 input) {
		if (mode == Mode.GROUND) {
			super.travel(input);
			return;
		}
		move(MoverType.SELF, getDeltaMovement());
	}

	public void faceTarget(LivingEntity t) {
		double dx = t.getX() - getX();
		double dz = t.getZ() - getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * 180 / Math.PI) - 90f;
		setYRot(yaw);
		setYHeadRot(yaw);
		setYBodyRot(yaw);
	}

	private void fight(LivingEntity t) {
		if (active != null) {
			active.tick(this, t);
			if (active.isDone()) {
				active = null;
			}
			return;
		}
		for (GodSlayerSkill s : skills) {
			if (s.canStart(this, t)) {
				active = s;
				s.tick(this, t);
				if (s.isDone()) {
					active = null;
				}
				return;
			}
		}
	}

	/** How far the target hovers above solid ground (0 on the ground or in water), up to 8. */
	private double heightAboveGround(LivingEntity t) {
		if (t.onGround() || t.isInWater()) {
			return 0;
		}
		BlockPos.MutableBlockPos p = t.blockPosition().mutable();
		for (int i = 0; i < 8; i++) {
			if (!level().getBlockState(p).getCollisionShape(level(), p).isEmpty()) {
				return t.getY() - (p.getY() + 1);
			}
			p.move(0, -1, 0);
		}
		return 8;
	}

	// ---- block breaking (like the Wither) ----

	private void breakBlocks(ServerLevel level, LivingEntity t) {
		if (!level.getServer().getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		Vec3 dir = t.position().subtract(position());
		double hl = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		Vec3 push = hl < 1e-4 ? Vec3.ZERO : new Vec3(dir.x / hl, 0, dir.z / hl);
		AABB box = getBoundingBox().inflate(0.6).move(push.scale(0.8));
		int floorY = Mth.floor(getY());
		int broken = 0;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
			BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			if (mode == Mode.GROUND && pos.getY() < floorY) {
				continue; // never dig the floor it walks on
			}
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getBlock() instanceof LiquidBlock || !WitherBoss.canDestroy(state)
				|| state.getDestroySpeed(level, pos) < 0) {
				continue;
			}
			if (level.destroyBlock(pos, false, this, 512) && ++broken >= 40) {
				break;
			}
		}
		if (broken > 0) {
			level.playSound(null, blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 0.8f, 0.8f);
		}
	}

	// ---- teleport ----

	/** A free spot 3 to 5 blocks from the target (on the ground if there is one), for this boss's body. */
	public Vec3 findSpotNear(LivingEntity t) {
		Vec3 base = position();
		int[] ups = {0, 1, -1, 2, -2, 3};
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < 24; i++) {
				double ang = random.nextDouble() * Math.PI * 2;
				double r = 3 + random.nextDouble() * 2 + getBbWidth() / 2;
				double x = t.getX() + Math.cos(ang) * r;
				double z = t.getZ() + Math.sin(ang) * r;
				for (int dy : ups) {
					Vec3 p = new Vec3(x, t.getY() + dy, z);
					AABB box = getBoundingBox().move(p.subtract(base));
					if (level().noCollision(this, box)
						&& (pass == 1 || !level().noCollision(this, box.move(0, -0.5, 0)))) {
						return p;
					}
				}
			}
		}
		return t.position();
	}

	/** Teleports next to the target in the same dimension, with smoke and a sound at both ends. */
	public void teleportNear(LivingEntity t) {
		if (level() instanceof ServerLevel sl) {
			poof(sl);
			Vec3 p = findSpotNear(t);
			teleportTo(p.x, p.y, p.z);
			getNavigation().stop();
			setDeltaMovement(Vec3.ZERO);
			poof(sl);
			resetProgress();
		}
	}

	/** Smoke and a teleport sound where the boss stands. */
	public void poof(ServerLevel sl) {
		sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 30, getBbWidth() / 2,
			getBbHeight() / 3, getBbWidth() / 2, 0.02);
		sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + getBbHeight() / 2, getZ(), 30, getBbWidth() / 2,
			getBbHeight() / 3, getBbWidth() / 2, 0.1);
		sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.5f);
	}
}
