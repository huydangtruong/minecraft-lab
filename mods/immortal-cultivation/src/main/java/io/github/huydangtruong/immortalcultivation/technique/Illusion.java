package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.technique.illusion.FightGoal;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IdleGoal;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionCarry;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionTargetGoal;
import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionJob;
import io.github.huydangtruong.immortalcultivation.technique.illusion.WorkGoal;
import io.github.huydangtruong.immortalcultivation.wanderer.SpawnSpots;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Illusion clone (D71): a copy of a player that fights for them. It is a PathfinderMob drawn on the client with the
 * player model and the owner's skin, name and gear. Its health, hit damage and armour are a share of the owner's; its
 * fights follow the owner's shared target list ({@link IllusionTargets}). Never saved; it vanishes after its time, when its
 * owner dies or leaves, or when the owner casts the technique again. Dying only makes a smoke puff.
 */
public final class Illusion extends PathfinderMob implements OwnedSummon {
	public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "illusion"));
	public static final EntityType<Illusion> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.<Illusion>of(Illusion::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
			.clientTrackingRange(8).noSave().noLootTable().build(KEY));

	/** The owner's UUID as text, so the client can find the owner's skin. */
	private static final EntityDataAccessor<String> OWNER_ID = SynchedEntityData.defineId(Illusion.class, EntityDataSerializers.STRING);
	private static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static final EntityDimensions CROUCHING = EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
	/** Distance beyond which an idle clone runs to its owner, and the distance at which it teleports instead. */
	public static final double FOLLOW_FROM = 6, TELEPORT_FROM = 24;

	private UUID owner;
	private String parsedFrom = "";
	private UUID parsed;
	private long endTick;
	private double factor = 0.2;
	private double fallbackDamage = 1.0;
	public IllusionJob job = IllusionJob.FIGHT;
	private BlockPos workCenter = BlockPos.ZERO;
	private double workRadius;
	private int workTicks = 20;
	private final IllusionCarry carry = new IllusionCarry();

	public Illusion(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	/** Registers the type attributes and the target hooks. Called once from ImmortalCultivation (through TechniqueCasts.init). */
	public static void init() {
		FabricDefaultAttributeRegistry.register(TYPE, Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.31).add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.FOLLOW_RANGE, 40.0));
		IllusionTargets.init();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OWNER_ID, "");
	}

	// ---- summoning ----

	/** Casts the technique: new clones around the owner (the old ones are removed first), then the owner and clones swap places at random. */
	public static List<Illusion> summon(ServerPlayer owner, int level) {
		ServerLevel sl = owner.level();
		dismissAll(owner);
		int count = TechniqueRules.illusionCount(level);
		double factor = TechniqueRules.illusionFactor(level);
		long end = sl.getGameTime() + Math.round(TechniqueRules.illusionSeconds(level) * 20);
		IllusionTargets.open(owner.getUUID());
		// D73/D85: the job is locked at the cast from the item in the owner's FIRST hotbar slot (inventory index 0): techniques are
		// cast with a technique selected, where the main hand is no real choice. An empty slot means FIGHT. The item is only read.
		ItemStack held = owner.getInventory().getItem(0);
		IllusionJob job = IllusionJob.fromItemId(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
		BlockPos center = owner.blockPosition();
		int feedTotal = job == IllusionJob.BREED ? held.getCount() : 0;
		// count + 1 spots on a ring 2-3 blocks away, evenly spread from a random start; the owner takes one at random.
		BlockPos origin = owner.blockPosition();
		List<Vec3> spots = new ArrayList<>();
		double start = owner.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i <= count; i++) {
			double ang = start + Math.PI * 2 * i / (count + 1);
			double r = 2.0 + owner.getRandom().nextDouble();
			BlockPos p = SpawnSpots.standSpot(sl, origin, (int) Math.round(Math.cos(ang) * r), (int) Math.round(Math.sin(ang) * r));
			spots.add(new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5));
		}
		Collections.shuffle(spots, new java.util.Random(owner.getRandom().nextLong()));
		List<Illusion> out = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Illusion c = TYPE.create(sl, EntitySpawnReason.MOB_SUMMONED);
			if (c == null) {
				continue;
			}
			Vec3 at = spots.get(i);
			c.snapTo(at.x, at.y, at.z, owner.getYRot(), 0f);
			c.setOwner(owner.getUUID());
			c.endTick = end;
			c.factor = factor;
			c.applyStats(owner);
			c.job = job;
			c.workCenter = center;
			c.workRadius = TechniqueRules.illusionWorkRadius(level);
			c.workTicks = TechniqueRules.illusionWorkTicks(level);
			c.setCustomName(Component.literal(owner.getGameProfile().name()));
			c.setCustomNameVisible(true);
			c.setPersistenceRequired();
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				c.setDropChance(slot, 0f);
			}
			if (job.works()) { // the main hand is locked to the cast item (looks only: no attribute modifiers)
				ItemStack lock = held.copyWithCount(1);
				lock.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
				c.setItemSlot(EquipmentSlot.MAINHAND, lock);
			}
			int foodShare = count > 0 ? feedTotal / count + (i < feedTotal % count ? 1 : 0) : 0;
			if (foodShare > 0) { // D109: the whole stack is split evenly between the clones
				c.carry.add(held.split(foodShare));
			}
			c.copyGear(owner);
			sl.addFreshEntity(c);
			puff(sl, c);
			out.add(c);
		}
		puff(sl, owner);
		Vec3 me = spots.get(count);
		owner.teleportTo(sl, me.x, me.y, me.z, Set.of(), owner.getYRot(), owner.getXRot(), true);
		puff(sl, owner);
		return out;
	}

	/** Removes every clone of the owner (with a puff) and closes the target list. */
	public static int dismissAll(Player owner) {
		if (!(owner.level() instanceof ServerLevel sl)) {
			return 0;
		}
		List<Illusion> mine = clonesOf(sl, owner.getUUID());
		for (Illusion c : mine) {
			puff(sl, c);
			c.discard();
		}
		IllusionTargets.close(owner.getUUID());
		return mine.size();
	}

	public static List<Illusion> clonesOf(ServerLevel sl, UUID owner) {
		return new ArrayList<>(sl.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forExactClass(Illusion.class),
			c -> owner.equals(c.owner()) && c.isAlive()));
	}

	private static void puff(ServerLevel sl, Entity e) {
		double y = e.getY() + e.getBbHeight() / 2;
		sl.sendParticles(ParticleTypes.POOF, e.getX(), y, e.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
		sl.sendParticles(ParticleTypes.END_ROD, e.getX(), y, e.getZ(), 8, 0.3, 0.6, 0.3, 0.02);
		sl.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 0.8f);
	}

	/** Health, armour and toughness as a share of the owner's. Melee damage is read from the owner at every hit. */
	void applyStats(Player o) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1.0, factor * o.getMaxHealth()));
		getAttribute(Attributes.ARMOR).setBaseValue(factor * o.getAttributeValue(Attributes.ARMOR));
		getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(factor * o.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		fallbackDamage = factor * o.getAttributeValue(Attributes.ATTACK_DAMAGE);
		setHealth(getMaxHealth());
	}

	/**
	 * Copies the owner's armour, main hand and off hand. The copies are for looks only: their attribute modifiers are
	 * removed (so they add nothing to the clone's stats) and they never drop. Silent, so no equip sounds.
	 */
	void copyGear(Player o) {
		setSilent(true);
		for (EquipmentSlot slot : GEAR) {
			if (slot == EquipmentSlot.MAINHAND && job.works()) {
				continue;
			}
			ItemStack want = o.getItemBySlot(slot).copy();
			if (!want.isEmpty()) {
				want.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			}
			if (!ItemStack.isSameItemSameComponents(getItemBySlot(slot), want)) {
				setItemSlot(slot, want);
			}
		}
		setSilent(false);
	}

	/** The owner id, set on the server and synced to the client. */
	void setOwner(UUID id) {
		owner = id;
		entityData.set(OWNER_ID, id == null ? "" : id.toString());
	}

	/** The owner's UUID; on the client read from the synced text. */
	@Override
	public UUID owner() {
		if (owner != null) {
			return owner;
		}
		String s = entityData.get(OWNER_ID);
		if (!s.equals(parsedFrom)) {
			parsedFrom = s;
			try {
				parsed = s.isEmpty() ? null : UUID.fromString(s);
			} catch (IllegalArgumentException e) {
				parsed = null;
			}
		}
		return parsed;
	}

	/** Most ticks a working clone may stay past its time to put what it carries in the chest (D197). */
	public static final int OVERTIME = 400;

	/** Game time at which this clone vanishes. */
	public long endTick() {
		return endTick;
	}

	/** Changes the vanishing time (used by game tests to fast-forward). */
	public void setEndTick(long tick) {
		endTick = tick;
	}

	/** The job locked at the cast. */
	public IllusionJob job() {
		return job;
	}

	/** The cast point, centre of the work area. */
	public BlockPos workCenter() {
		return workCenter;
	}

	public double workRadius() {
		return workRadius;
	}

	/** Ticks between two work actions. */
	public int workTicks() {
		return workTicks;
	}

	/** The work area as a box (the circle's bounding box, a bit taller). */
	public AABB workBox() {
		return new AABB(workCenter).inflate(workRadius, 6, workRadius);
	}

	public IllusionCarry carry() {
		return carry;
	}

	/** The item locked in the main hand (the tool of the job). */
	public ItemStack lockedItem() {
		return getMainHandItem();
	}

	public Player ownerPlayer() {
		return OwnedSummon.ownerOf(this);
	}

	/** Drops what the clone carries: at the owner's feet when the owner is in the work area, else where the clone stands. Never deletes items. */
	public void dropCarry() {
		if (carry.isEmpty() || !(level() instanceof ServerLevel sl)) {
			return;
		}
		Player o = ownerPlayer();
		Vec3 at = o != null && o.level() == sl && o.blockPosition().distSqr(workCenter) <= workRadius * workRadius ? o.position() : position();
		for (ItemStack s : carry.drain()) {
			net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(sl, at.x, at.y + 0.3, at.z, s);
			e.setDefaultPickUpDelay();
			if (o != null) {
				e.setThrower(o);
			}
			sl.addFreshEntity(e);
		}
	}

	/** The share of the owner's stats this clone has. */
	public double factor() {
		return factor;
	}

	// ---- behaviour ----

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new FightGoal(this));
		goalSelector.addGoal(2, new WorkGoal(this));
		goalSelector.addGoal(3, new IdleGoal(this));
		targetSelector.addGoal(1, new IllusionTargetGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel sl && isAlive()) {
			Player o = OwnedSummon.ownerOf(this);
			long now = sl.getGameTime();
			// D197: a working clone still carrying items when its time ends first takes them to the chest (at most OVERTIME more)
			boolean finishing = now >= endTick && now < endTick + OVERTIME && job.works() && !carry.isEmpty();
			if (o == null || !o.isAlive() || now >= endTick && !finishing) {
				puff(sl, this);
				discard();
			} else if ((tickCount + getId()) % 20 == 0) {
				copyGear(o);
			}
		}
	}

	/** Closes the owner's target list when the last clone is gone. */
	@Override
	public void remove(Entity.RemovalReason reason) {
		dropCarry();
		super.remove(reason);
		UUID id = owner;
		if (id != null && level() instanceof ServerLevel sl && IllusionTargets.active(id) && clonesOf(sl, id).isEmpty()) {
			IllusionTargets.close(id);
		}
	}

	/** Never hurt by its owner or the owner's other clones. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity by = source.getEntity();
		UUID id = owner();
		if (id != null && by != null && (id.equals(by.getUUID()) || by instanceof Illusion c && id.equals(c.owner()))) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Dying only makes a smoke puff: no death message, no drops. */
	@Override
	public void die(DamageSource source) {
		if (level() instanceof ServerLevel sl && !isRemoved()) {
			puff(sl, this);
			discard();
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	/** Never attacks the owner, the owner's summons or other clones of the owner. */
	@Override
	public boolean canAttack(LivingEntity target) {
		UUID id = owner();
		if (id != null && (id.equals(target.getUUID()) || target instanceof OwnedSummon s && id.equals(s.owner()))) {
			return false;
		}
		return super.canAttack(target);
	}

	/** Hits with the owner's current attack damage times the clone's share (D71); a mob it hurts counts as hurt by the owner (D62). */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		Player o = OwnedSummon.ownerOf(this);
		double damage = o != null ? factor * o.getAttributeValue(Attributes.ATTACK_DAMAGE) : fallbackDamage;
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
		OwnedSummon.giveKillCredit(this, target);
		swingForAttack(InteractionHand.MAIN_HAND);
		if (!onGround() && fallDistance > 0) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
		}
		return super.doHurtTarget(level, target);
	}

	/**
	 * A mob's speed attribute moves it much slower than a player's (its steady speed goes with the attribute squared), so the
	 * base 0.31 gives a walk of about 4.3 blocks/s and sprinting adds the smaller bonus that makes 5.6 (checked in IllusionGameTest).
	 */
	private static final Identifier SPRINT_ID = Identifier.fromNamespaceAndPath("permahearts", "illusion_sprint");
	public static final double SPRINT_BONUS = 0.14;

	@Override
	public void setSprinting(boolean sprinting) {
		super.setSprinting(sprinting);
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(Identifier.withDefaultNamespace("sprinting"));
			if (sprinting) {
				speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SPRINT_ID, SPRINT_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			} else {
				speed.removeModifier(SPRINT_ID);
			}
		}
	}

	/** Sneaks like a player: the pose and the shorter hitbox. */
	public void setCrouching(boolean on) {
		setShiftKeyDown(on);
		setPose(on ? Pose.CROUCHING : Pose.STANDING);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return pose == Pose.CROUCHING ? CROUCHING : super.getDefaultDimensions(pose);
	}

	public boolean holdsRanged() {
		ItemStack s = getMainHandItem();
		return s.is(Items.BOW) || s.is(Items.CROSSBOW);
	}

	/** Shoots one arrow at the target: damage is two (a vanilla arrow's base) times the clone's share. */
	public void shoot(ServerLevel sl, LivingEntity t) {
		Arrow arrow = new Arrow(sl, this, new ItemStack(Items.ARROW), getMainHandItem());
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		arrow.setBaseDamage(2.0 * factor);
		double dx = t.getX() - getX(), dz = t.getZ() - getZ();
		double dy = t.getY(0.33) - arrow.getY();
		arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.1, dz, 3.0f, 2.0f);
		OwnedSummon.giveKillCredit(this, t);
		swingForAttack(InteractionHand.MAIN_HAND);
		playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
		sl.addFreshEntity(arrow);
	}
}
