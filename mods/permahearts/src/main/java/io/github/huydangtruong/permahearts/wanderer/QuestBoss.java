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
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The quest boss: a giant zombie, skeleton, evoker, blaze, vindicator or ghast with 150 health and four minions
 * (an evoker brings pillagers with crossbows, D34; a ghast brings phantoms, D35). The boss UUID is saved in
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

	/** True while the saved boss entity exists and is alive. */
	public static boolean bossAlive(MinecraftServer server) {
		Optional<BossState> st = Wanderer.data(server).boss();
		return st.isPresent() && liveBoss(server, st.get()) != null;
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
		// 3..5 blocks from the NPC, towards the summoner, at the NPC's height (caves included).
		double dx = by.getX() - npc.getX();
		double dz = by.getZ() - npc.getZ();
		double len = Math.hypot(dx, dz);
		if (len < 0.5) {
			dx = 1;
			dz = 0;
			len = 1;
		}
		double reach = 3 + RNG.nextDouble() * 2;
		BlockPos spot = SpawnSpots.standSpot(ow, npc.blockPosition(), (int) Math.round(dx / len * reach), (int) Math.round(dz / len * reach));
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
		if (boss instanceof Ghast) {
			liftIntoSky(ow, boss); // 8 blocks wide: never in a cave, floats above the ground near the NPC (D35)
		}
		AttributeInstance atk = boss.getAttribute(Attributes.ATTACK_DAMAGE);
		if (atk != null) {
			atk.setBaseValue(atk.getBaseValue() * 1.5);
		}
		boss.setPersistenceRequired();
		boss.setCustomName(Messages.bossName(typeName));
		boss.setCustomNameVisible(true);
		boss.addTag(TAG);
		boss.setTarget(by);
		// Save the boss first: the ENTITY_LOAD hook discards tagged bosses that are not the saved one.
		Optional<BossState> previous = d.boss();
		Wanderer.save(server, Wanderer.data(server).withBoss(Optional.of(new BossState(line, boss.getUUID().toString()))));
		if (!ow.addFreshEntity(boss)) {
			Wanderer.save(server, Wanderer.data(server).withBoss(previous));
			return false;
		}
		EntityType<?> minionType = type == EntityTypes.EVOKER ? EntityTypes.PILLAGER // D34
			: type == EntityTypes.GHAST ? EntityTypes.PHANTOM // D35
			: type;
		for (int i = 0; i < MINIONS; i++) {
			BlockPos m = minionType == EntityTypes.PHANTOM
				? BlockPos.containing(boss.getX() + RNG.nextInt(13) - 6, boss.getY() + RNG.nextInt(5), boss.getZ() + RNG.nextInt(13) - 6)
				: SpawnSpots.standSpot(ow, spot, RNG.nextInt(7) - 3, RNG.nextInt(7) - 3);
			if (minionType.create(ow, EntitySpawnReason.EVENT) instanceof Mob minion) {
				minion.snapTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
				minion.finalizeSpawn(ow, ow.getCurrentDifficultyAt(m), EntitySpawnReason.EVENT, null);
				if (minion instanceof Zombie z) {
					z.setBaby(false);
				}
				minion.setItemSlot(EquipmentSlot.HEAD, helmet(Items.IRON_HELMET));
				minion.setDropChance(EquipmentSlot.HEAD, 0f);
				minion.addTag(MINION_TAG);
				minion.setTarget(by);
				ow.addFreshEntity(minion);
			}
		}
		clearBar();
		server.getPlayerList().broadcastSystemMessage(Messages.bossSummoned(Messages.bossName(typeName)), false);
		return true;
	}

	/** Moves the Ghast boss above the ground at its x/z, then higher until it touches nothing. */
	private static void liftIntoSky(ServerLevel ow, Mob boss) {
		double x = boss.getX();
		double z = boss.getZ();
		double y = Math.max(boss.getY(), ow.getHeight(Heightmap.Types.MOTION_BLOCKING, boss.getBlockX(), boss.getBlockZ()))
			+ GhastLeash.SPAWN_ABOVE;
		boss.setPos(x, y, z);
		for (int i = 0; i < 16 && !ow.noCollision(boss); i++) {
			y += 2;
			boss.setPos(x, y, z);
		}
	}

	/**
	 * Once a second: a Ghast boss that floated more than 24 blocks from the Crimson Master, or more than 16 above the
	 * nearest player (or the ground, when the player is under it), is sent back to hover 8-12 blocks over that player.
	 */
	public static void leash(MinecraftServer server, Ghast ghast) {
		Entity npc = Wanderer.npc(server);
		if (npc == null || npc.level() != ghast.level()) {
			return;
		}
		ServerLevel level = (ServerLevel) ghast.level();
		ServerPlayer near = null;
		for (ServerPlayer p : level.players()) {
			if (!p.isSpectator() && p.isAlive() && p.distanceToSqr(npc) <= BAR_RANGE * BAR_RANGE
				&& (near == null || p.distanceToSqr(ghast) < near.distanceToSqr(ghast))) {
				near = p;
			}
		}
		// Vanilla ghasts only pick targets within 4 blocks of their own height; hovering above the player, the boss
		// would never find one again after losing sight of it.
		if (near != null && !near.isCreative() && (ghast.getTarget() == null || !ghast.getTarget().isAlive())) {
			ghast.setTarget(near);
		}
		double px = near == null ? npc.getX() : near.getX();
		double pz = near == null ? npc.getZ() : near.getZ();
		double py = near == null ? npc.getY() : near.getY();
		double groundHere = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ghast.getBlockX(), ghast.getBlockZ());
		if (!GhastLeash.outOfBounds(ghast.getX(), ghast.getY(), ghast.getZ(), npc.getX(), npc.getZ(), Math.max(py, groundHere))) {
			return;
		}
		double groundThere = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(px), (int) Math.floor(pz));
		double[] to = GhastLeash.returnPoint(px, pz, Math.max(py, groundThere), npc.getX(), npc.getZ(),
			GhastLeash.HOVER_MIN + RNG.nextDouble() * (GhastLeash.HOVER_MAX - GhastLeash.HOVER_MIN),
			RNG.nextDouble() * 8 - 4, RNG.nextDouble() * 8 - 4);
		ghast.getMoveControl().setWantedPosition(to[0], to[1], to[2], 1.0);
	}

	/** An unbreakable helmet: sunlight damages helmets of zombies and skeletons, and they burn once it breaks. */
	static ItemStack helmet(net.minecraft.world.item.Item item) {
		ItemStack h = new ItemStack(item);
		h.set(net.minecraft.core.component.DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
		return h;
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
		boss.setItemSlot(EquipmentSlot.HEAD, helmet(Items.DIAMOND_HELMET));
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
		if (living instanceof Ghast ghast) {
			leash(server, ghast);
		}
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
