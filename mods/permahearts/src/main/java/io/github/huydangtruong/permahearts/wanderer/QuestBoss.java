package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.Messages;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The quest boss: a giant zombie, skeleton, evoker, blaze or vindicator with 150 health and four minions
 * (an evoker brings vindicators, so the fight is not a swarm of vexes). The boss UUID is saved in
 * WandererData.boss; the boss bar is runtime only. tick() runs once per second from the scheduler.
 */
public final class QuestBoss {
	public static final String TAG = "permahearts_boss";
	public static final String MINION_TAG = "permahearts_boss_minion";
	public static final float HEALTH = 150f;
	public static final double SCALE = 2.0;
	public static final int MINIONS = 4;
	private static final double BAR_RANGE = 64.0;

	private static final RandomGenerator RNG = new java.util.Random();
	private static ServerBossEvent bar;

	private QuestBoss() {}

	public static boolean running(MinecraftServer server) {
		return Wanderer.data(server).boss().isPresent();
	}

	/** The live boss entity, or null (not spawned yet, unloaded, or dead). */
	private static Entity liveBoss(MinecraftServer server, BossState st) {
		try {
			Entity e = server.overworld().getEntity(UUID.fromString(st.bossUuid()));
			return e != null && e.isAlive() ? e : null;
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public static boolean summon(MinecraftServer server, int line, ServerPlayer by) {
		WandererData d = Wanderer.data(server);
		if (d.quest().isEmpty() || !d.quest().get().accepted()) {
			return false;
		}
		Quest q = d.quest().get();
		if (line < 0 || line >= q.lines().size() || q.lines().get(line).type() != QuestType.BOSS || q.lines().get(line).done()) {
			return false;
		}
		Entity npc = Wanderer.npc(server);
		if (npc == null || npc.isRemoved()) {
			return false;
		}
		if (d.boss().isPresent() && liveBoss(server, d.boss().get()) != null) {
			by.sendOverlayMessage(Messages.bossAlreadyHere());
			return false;
		}
		ServerLevel ow = server.overworld();
		if (npc.level() != ow) {
			return false;
		}
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(q.lines().get(line).target()));
		// 5 blocks from the NPC, towards the summoner when possible.
		double dx = by.getX() - npc.getX();
		double dz = by.getZ() - npc.getZ();
		double len = Math.hypot(dx, dz);
		if (len < 0.5) {
			dx = 1;
			dz = 0;
			len = 1;
		}
		BlockPos spot = surface(ow, npc.getX() + dx / len * 5, npc.getZ() + dz / len * 5);
		if (!(type.create(ow, EntitySpawnReason.EVENT) instanceof Mob boss)) {
			return false;
		}
		Component typeName = type.getDescription();
		boss.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
		boss.finalizeSpawn(ow, ow.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
		if (boss instanceof Zombie z) {
			z.setBaby(false);
		}
		equip(boss);
		set(boss, Attributes.MAX_HEALTH, HEALTH);
		boss.setHealth(HEALTH);
		set(boss, Attributes.SCALE, SCALE);
		AttributeInstance atk = boss.getAttribute(Attributes.ATTACK_DAMAGE);
		if (atk != null) {
			atk.setBaseValue(atk.getBaseValue() * 1.5);
		}
		boss.setPersistenceRequired();
		boss.setCustomName(Messages.bossName(typeName));
		boss.setCustomNameVisible(true);
		boss.addTag(TAG);
		boss.setTarget(by);
		ow.addFreshEntity(boss);
		EntityType<?> minionType = type == EntityTypes.EVOKER ? EntityTypes.VINDICATOR : type;
		for (int i = 0; i < MINIONS; i++) {
			BlockPos m = surface(ow, spot.getX() + (RNG.nextDouble() * 6 - 3), spot.getZ() + (RNG.nextDouble() * 6 - 3));
			if (minionType.create(ow, EntitySpawnReason.EVENT) instanceof Mob minion) {
				minion.snapTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
				minion.finalizeSpawn(ow, ow.getCurrentDifficultyAt(m), EntitySpawnReason.EVENT, null);
				if (minion instanceof Zombie z) {
					z.setBaby(false);
				}
				minion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
				minion.setDropChance(EquipmentSlot.HEAD, 0f);
				minion.addTag(MINION_TAG);
				minion.setTarget(by);
				ow.addFreshEntity(minion);
			}
		}
		Wanderer.save(server, Wanderer.data(server).withBoss(Optional.of(new BossState(line, boss.getUUID().toString()))));
		clearBar();
		server.getPlayerList().broadcastSystemMessage(Messages.bossSummoned(Messages.bossName(typeName)), false);
		return true;
	}

	private static BlockPos surface(ServerLevel level, double x, double z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		return new BlockPos(bx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz), bz);
	}

	private static void set(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double v) {
		AttributeInstance a = e.getAttribute(attr);
		if (a != null) {
			a.setBaseValue(v);
		}
	}

	/**
	 * Iron armour with a diamond helmet (a helmet also stops sun burning; armour still protects mobs whose model does
	 * not draw it); a sword for zombies, a bow for skeletons, an axe for vindicators, empty hands for casters/blazes.
	 */
	private static void equip(Mob boss) {
		boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
		boss.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		boss.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
		boss.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
		ItemStack weapon = boss instanceof Zombie ? new ItemStack(Items.IRON_SWORD)
			: boss instanceof AbstractSkeleton ? new ItemStack(Items.BOW)
			: boss instanceof Vindicator ? new ItemStack(Items.DIAMOND_AXE)
			: ItemStack.EMPTY;
		boss.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
			boss.setDropChance(s, 0f);
		}
	}

	// ---- tick, death, reset ----

	public static void tick(MinecraftServer server) {
		Optional<BossState> st = Wanderer.data(server).boss();
		if (st.isEmpty()) {
			clearBar();
			return;
		}
		Entity boss = liveBoss(server, st.get());
		if (!(boss instanceof LivingEntity living)) {
			// Unloaded or lost: hide the bar; the next summon press replaces the boss.
			clearBar();
			return;
		}
		if (bar == null) {
			bar = new ServerBossEvent(UUID.randomUUID(), living.getDisplayName(), BossEvent.BossBarColor.PURPLE,
				BossEvent.BossBarOverlay.PROGRESS);
		}
		bar.setProgress(Math.max(0f, Math.min(1f, living.getHealth() / living.getMaxHealth())));
		for (ServerPlayer p : server.overworld().players()) {
			if (p.distanceToSqr(living) <= BAR_RANGE * BAR_RANGE) {
				bar.addPlayer(p);
			} else {
				bar.removePlayer(p);
			}
		}
	}

	public static void onDeath(MinecraftServer server, LivingEntity entity) {
		Optional<BossState> st = Wanderer.data(server).boss();
		if (st.isPresent() && entity.getUUID().toString().equals(st.get().bossUuid())) {
			Wanderer.save(server, Wanderer.data(server).withBoss(Optional.empty()));
			clearBar();
			Wanderer.eventDone(server, st.get().line());
		}
	}

	private static void clearBar() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
	}

	/** Runtime cleanup when the whole Wanderer state is reset: bar, boss and minions. */
	static void onReset(MinecraftServer server) {
		clearBar();
		for (Mob m : server.overworld().getEntities(EntityTypeTest.forClass(Mob.class),
			e -> e.entityTags().contains(TAG) || e.entityTags().contains(MINION_TAG))) {
			m.discard();
		}
	}
}
