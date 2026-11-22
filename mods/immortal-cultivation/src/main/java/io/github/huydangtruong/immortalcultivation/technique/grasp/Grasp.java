package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer;
import io.github.huydangtruong.immortalcultivation.technique.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Soul Grasp, server side (D134, D135, D136, P-053): the held right click tethers a mob and tames it when the charge is
 * full, the list menu changes the mode of a held mob, calls it back or lets it go. Every request is checked again here.
 * The held mobs of each player live in {@link GraspData}; the mob itself carries the {@link GraspedMob} attachment.
 */
public final class Grasp {
	public static final Identifier BONUS_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_bonus");
	private static final Identifier SLOW_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_slow");

	public static final int START = 0, CANCEL = 1, LIST = 2, MODE = 3, CALL = 4, RELEASE = 5;
	/** D168: the "All" row. target is ignored; ALL_MODE carries the mode in arg. */
	public static final int ALL_MODE = 6, ALL_CALL = 7, ALL_RELEASE = 8;

	/** Client to server. kind: START (arg = entity id), CANCEL, LIST, MODE (target = mob uuid, arg = mode), CALL, RELEASE. */
	public record Request(int kind, String target, int arg) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Request::kind,
			ByteBufCodecs.STRING_UTF8, Request::target,
			ByteBufCodecs.VAR_INT, Request::arg,
			Request::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server to client: mobs held now, and the tether in progress (targetId -1 = none) with its length in ticks. */
	public record State(int held, int targetId, int duration) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::held,
			ByteBufCodecs.VAR_INT, State::targetId,
			ByteBufCodecs.VAR_INT, State::duration,
			State::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * One line of the list. dist is -1 for a mob that is not loaded (shown "far away"); armorGear and weaponGear are the
	 * tooltip lines (separated by newlines).
	 */
	public record Row(String id, String type, int number, int mode, boolean loaded, String dim, float dist, float hp, float maxHp,
			float bonus, float armor, float damage, String armorGear, String weaponGear, int callLeft) {
		static void write(RegistryFriendlyByteBuf b, Row r) {
			b.writeUtf(r.id);
			b.writeUtf(r.type);
			b.writeVarInt(r.number);
			b.writeVarInt(r.mode);
			b.writeBoolean(r.loaded);
			b.writeUtf(r.dim);
			b.writeFloat(r.dist);
			b.writeFloat(r.hp);
			b.writeFloat(r.maxHp);
			b.writeFloat(r.bonus);
			b.writeFloat(r.armor);
			b.writeFloat(r.damage);
			b.writeUtf(r.armorGear);
			b.writeUtf(r.weaponGear);
			b.writeVarInt(r.callLeft);
		}

		static Row read(RegistryFriendlyByteBuf b) {
			return new Row(b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readUtf(), b.readFloat(),
				b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readUtf(), b.readUtf(), b.readVarInt());
		}
	}

	/** Server to client: what the list screen shows. */
	public record Menu(List<Row> rows, int max, int level) implements CustomPacketPayload {
		public static final Type<Menu> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Menu> CODEC = StreamCodec.of((b, m) -> {
			b.writeVarInt(m.rows.size());
			for (Row r : m.rows) {
				Row.write(b, r);
			}
			b.writeVarInt(m.max);
			b.writeVarInt(m.level);
		}, b -> {
			int n = b.readVarInt();
			List<Row> rows = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				rows.add(Row.read(b));
			}
			return new Menu(rows, b.readVarInt(), b.readVarInt());
		});

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** A tether in progress (server memory only). */
	private static final class Tether {
		final Mob target;
		final int duration;
		final int level;
		int age;
		int noSight;

		Tether(Mob target, int duration, int level) {
			this.target = target;
			this.duration = duration;
			this.level = level;
		}
	}

	private static final Map<UUID, Tether> TETHERS = new HashMap<>();
	/** Loaded grasped mobs (added on load or tame, removed on unload). */
	private static final Set<Mob> LOADED = new HashSet<>();
	/** Released mobs that ignore their ex-owner for a while: mob uuid to (owner uuid, game time it ends). */
	private static final Map<UUID, Peace> PEACE = new HashMap<>();
	private static final Map<UUID, Long> CALL_READY = new HashMap<>();

	private record Peace(UUID owner, long until) {}

	private Grasp() {}

	public static void init() {
		GraspedMob.ATTACHMENT.identifier();
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Menu.TYPE, Menu.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, ctx) -> handle(ctx.player(), payload.kind(), payload.target(), payload.arg()));
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob m && m.hasAttached(GraspedMob.ATTACHMENT)) {
				onLoad(m, level);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Mob m && m.hasAttached(GraspedMob.ATTACHMENT)) {
				LOADED.remove(m);
				var why = m.getRemovalReason();
				if (why == Entity.RemovalReason.KILLED || why == Entity.RemovalReason.DISCARDED) {
					gone(m);
				}
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Mob m && m.hasAttached(GraspedMob.ATTACHMENT)) {
				gone(m);
			}
		});
		// D134: a grasped mob that hurts something gives its owner the kill credit, like a tamed wolf.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.getEntity() instanceof Mob m && !(entity instanceof Player)) {
				GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
				if (ga != null && m.level() instanceof ServerLevel sl) {
					ServerPlayer o = sl.getServer().getPlayerList().getPlayer(ga.owner());
					if (o != null && o.isAlive()) {
						entity.setLastHurtByPlayer(o, 100);
					}
				}
			}
			return true;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.getPlayer(), -1, 0));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> endTether(handler.getPlayer(), false));
		ServerTickEvents.END_SERVER_TICK.register(Grasp::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			TETHERS.clear();
			LOADED.clear();
			PEACE.clear();
			CALL_READY.clear();
		});
	}

	// ---- reads used by the mixins and goals ----

	public static boolean isGrasped(Entity e) {
		return e.hasAttached(GraspedMob.ATTACHMENT);
	}

	/** True when the entity is a mob grasped by this player: their clones, guardians and angels leave it alone. */
	public static boolean isGraspedBy(Entity e, UUID owner) {
		GraspedMob g = graspOf(e);
		return g != null && g.owner().equals(owner);
	}

	/** The grasp of a mob, or D192: of the grasped evoker that summoned this vex (its vexes are on the owner's side too). */
	static GraspedMob graspOf(Entity e) {
		GraspedMob g = e.getAttached(GraspedMob.ATTACHMENT);
		if (g == null && e instanceof net.minecraft.world.entity.monster.Vex v && v.getOwner() instanceof Mob o) {
			g = o.getAttached(GraspedMob.ATTACHMENT);
		}
		return g;
	}

	/** True when the attacker must not target the target (checked from Mob#canAttack). */
	public static boolean blocksAttack(Mob attacker, LivingEntity target) {
		GraspedMob mine = graspOf(attacker);
		if (mine != null) {
			if (target.getUUID().equals(mine.owner()) || graspOf(target) != null) {
				return true; // never its owner, never another grasped mob (nor a vex of a grasped evoker, D192)
			}
			if (attacker instanceof net.minecraft.world.entity.monster.Vex && !attacker.hasAttached(GraspedMob.ATTACHMENT)
				&& (target instanceof net.minecraft.world.entity.npc.villager.AbstractVillager || target instanceof Player && !isMatchOpponentOf(attacker, mine, target))) {
				return true; // D194: a grasped evoker's vex leaves villagers and other players alone (Death Match opponents excepted)
			}
		}
		if (target.hasAttached(GraspedMob.ATTACHMENT) && (attacker instanceof net.minecraft.world.entity.animal.golem.IronGolem
			|| attacker instanceof net.minecraft.world.entity.animal.golem.SnowGolem || attacker instanceof net.minecraft.world.entity.animal.wolf.Wolf)) {
			return true; // golems and wolves leave grasped mobs alone
		}
		if (!PEACE.isEmpty()) {
			Peace p = PEACE.get(attacker.getUUID());
			if (p != null && target.getUUID().equals(p.owner) && attacker.level().getGameTime() < p.until) {
				return true;
			}
		}
		return false;
	}

	private static boolean isMatchOpponentOf(Mob attacker, GraspedMob g, LivingEntity target) {
		ServerPlayer o = attacker.level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getPlayer(g.owner()) : null;
		return o != null && isMatchOpponent(o, target);
	}

	/** Death Match: any other player is an opponent of the owner of a grasped mob. */
	static boolean isMatchOpponent(ServerPlayer owner, Entity target) {
		return target instanceof ServerPlayer tp && tp != owner && tp.isAlive() && !tp.isSpectator() && !tp.isCreative()
			&& ImmortalCultivation.enabled(owner.level().getServer()) && DeathMatch.running(owner.level().getServer());
	}

	/** The facts of GraspRules about an entity. */
	public static boolean isBoss(Entity e) {
		return e instanceof GodSlayer || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
			|| e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.monster.warden.Warden
			|| e.entityTags().contains(QuestBoss.TAG) || e.entityTags().contains(QuestBoss.MINION_TAG);
	}

	public static GraspRules.Facts facts(Entity e) {
		boolean boss = isBoss(e);
		boolean owned = e.hasAttached(GraspedMob.ATTACHMENT)
			|| e instanceof OwnableEntity o && o.getOwnerReference() != null;
		return new GraspRules.Facts(e instanceof Player, boss, WandererNpc.isWanderer(e), e instanceof OwnedSummon, e instanceof Illusion,
			owned, e instanceof WanderingTrader, e instanceof WaterAnimal || e instanceof AgeableWaterCreature || e instanceof Guardian,
			e instanceof Slime);
	}

	public static int level(ServerPlayer p) {
		return TechniqueAttachment.get(p).level(Technique.GRASP);
	}

	public static int held(ServerPlayer p) {
		return GraspData.of(p.level().getServer()).held(p.getUUID());
	}

	/** The mobs of a player that are loaded right now. */
	public static List<Mob> loadedOf(UUID owner) {
		List<Mob> out = new ArrayList<>();
		for (Mob m : LOADED) {
			GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
			if (ga != null && ga.owner().equals(owner)) {
				out.add(m);
			}
		}
		return out;
	}

	public static boolean tethering(ServerPlayer p) {
		return TETHERS.containsKey(p.getUUID());
	}

	// ---- requests ----

	public static void handle(ServerPlayer p, int kind, String target, int arg) {
		MinecraftServer server = p.level().getServer();
		if (!ImmortalCultivation.enabled(server) || !p.isAlive() || p.isSpectator() || level(p) < 1) {
			endTether(p, true);
			return;
		}
		switch (kind) {
			case START -> startTether(p, arg);
			case CANCEL -> endTether(p, true);
			case LIST -> sendMenu(p);
			case MODE -> setMode(p, target, arg);
			case CALL -> call(p, target);
			case RELEASE -> releaseRequest(p, target);
			case ALL_MODE -> allMode(p, arg);
			case ALL_CALL -> allCall(p);
			case ALL_RELEASE -> allRelease(p);
			default -> {}
		}
	}

	private static void sendState(ServerPlayer p, int targetId, int duration) {
		ServerPlayNetworking.send(p, new State(held(p), targetId, duration));
	}

	// ---- the tether ----

	/** Starts a tether to the entity with this id. Refusals go to the overlay. Public for the game tests. */
	public static boolean startTether(ServerPlayer p, int entityId) {
		MinecraftServer server = p.level().getServer();
		TechniqueData d = TechniqueAttachment.get(p);
		long now = server.overworld().getGameTime();
		if (TETHERS.containsKey(p.getUUID())) {
			return false;
		}
		if (d.onCooldown(Technique.GRASP, now)) {
			p.sendOverlayMessage(Component.literal("On cooldown: " + (d.cooldownLeft(Technique.GRASP, now) + 19) / 20 + " s"));
			return false;
		}
		Entity e = p.level().getEntity(entityId);
		if (!(e instanceof Mob mob) || !mob.isAlive() || mob.distanceTo(p) > GraspRules.SELECT_RANGE + 2 || !p.hasLineOfSight(mob)) {
			p.sendOverlayMessage(Component.literal("Look at a mob within " + (int) GraspRules.SELECT_RANGE + " blocks"));
			return false;
		}
		int lvl = d.level(Technique.GRASP);
		GraspRules.Check c = GraspRules.check(facts(mob), mob.getMaxHealth(), lvl, held(p));
		if (!c.ok()) {
			p.sendOverlayMessage(Component.literal(GraspRules.message(c, lvl, held(p))));
			return false;
		}
		int duration = GraspRules.chargeTicks(mob.getMaxHealth());
		TETHERS.put(p.getUUID(), new Tether(mob, duration, lvl));
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		sendState(p, mob.getId(), duration);
		p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 0.6f);
		return true;
	}

	/** Ends the tether of a player without a cooldown. */
	public static void endTether(ServerPlayer p, boolean notify) {
		Tether t = TETHERS.remove(p.getUUID());
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW_ID);
		}
		if (t != null && notify && p.connection != null) {
			sendState(p, -1, 0);
		}
	}

	private static void tickTether(ServerPlayer p, Tether t) {
		Mob m = t.target;
		if (!p.isAlive() || p.isSpectator() || level(p) < 1 || m.isRemoved() || !m.isAlive() || m.level() != p.level()
			|| m.distanceTo(p) > GraspRules.BREAK_RANGE) {
			endTether(p, true);
			return;
		}
		if (p.hasLineOfSight(m)) {
			t.noSight = 0;
		} else if (++t.noSight > GraspRules.NO_SIGHT_TICKS) {
			endTether(p, true);
			return;
		}
		t.age++;
		if (t.age % 4 == 0 && p.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 1, 0.3, 0.3, 0.3, 0.01);
		}
		if (t.age >= t.duration) {
			complete(p, t);
		}
	}

	private static void complete(ServerPlayer p, Tether t) {
		Mob m = t.target;
		int lvl = level(p);
		GraspRules.Check c = GraspRules.check(facts(m), m.getMaxHealth(), lvl, held(p));
		endTether(p, true);
		if (!c.ok()) {
			p.sendOverlayMessage(Component.literal(GraspRules.message(c, lvl, held(p))));
			return;
		}
		tame(p, m, lvl);
		long now = p.level().getServer().overworld().getGameTime();
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.GRASP, now + io.github.huydangtruong.immortalcultivation.passive.Passives.cooldown(p, GraspRules.COOLDOWN_SECONDS * 20L)));
	}

	// ---- tame ----

	private static String typeId(Mob m) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString();
	}

	/** The yellow name "Zombie 1". */
	public static Component nameOf(EntityType<?> type, int number) {
		return Component.translatable(type.getDescriptionId()).append(" " + number).withStyle(ChatFormatting.YELLOW);
	}

	/** Makes the mob the player's: attachment, health bonus, name, persistence, goals, list entry. Public for the game tests. */
	public static void tame(ServerPlayer p, Mob mob, int level) {
		GraspData data = GraspData.of(p.level().getServer());
		int number = data.nextNumber(p.getUUID(), typeId(mob));
		var pos = mob.blockPosition();
		GraspedMob ga = new GraspedMob(p.getUUID(), number, GraspRules.Mode.FOLLOW.ordinal(), pos.getX(), pos.getY(), pos.getZ(), 0, List.of());
		mob.setAttached(GraspedMob.ATTACHMENT, ga);
		data.setLevel(p.getUUID(), level);
		applyBonus(mob, level, true);
		mob.setCustomName(nameOf(mob.getType(), number));
		mob.setCustomNameVisible(true);
		mob.setPersistenceRequired();
		mob.setTarget(null);
		leaveRaid(mob);
		GraspGoals.apply(mob);
		LOADED.add(mob);
		data.add(p.getUUID(), snapshot(mob, mob.getAttached(GraspedMob.ATTACHMENT)));
		if (p.level() instanceof ServerLevel sl) {
			double y = mob.getY() + mob.getBbHeight() / 2;
			sl.sendParticles(ParticleTypes.END_ROD, mob.getX(), y, mob.getZ(), 30, 0.5, 0.5, 0.5, 0.15);
			sl.sendParticles(ParticleTypes.GLOW, mob.getX(), y, mob.getZ(), 12, 0.5, 0.5, 0.5, 0.05);
			sl.playSound(null, mob.blockPosition(), SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.PLAYERS, 1f, 1.5f);
			sl.playSound(null, mob.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f);
		}
		sendState(p, -1, 0);
	}

	/**
	 * D185: a grasped raider (Vindicator, Pillager, Evoker, Witch, Ravager...) is no longer part of any raid: the raid
	 * finishes without it and it never joins a new one. Called at tame time and every second (worlds grasped before the fix).
	 */
	static void leaveRaid(Mob mob) {
		if (!(mob instanceof net.minecraft.world.entity.raid.Raider r)) {
			return;
		}
		if (r.canJoinRaid()) {
			r.setCanJoinRaid(false);
		}
		net.minecraft.world.entity.raid.Raid raid = r.getCurrentRaid();
		if (raid != null && r.level() instanceof ServerLevel sl) {
			if (raid.getLeader(r.getWave()) == r) {
				raid.removeLeader(r.getWave());
			}
			raid.removeFromRaid(sl, r, true);
			r.setCurrentRaid(null);
		}
	}

	/** Sets the health bonus of a level on the mob. heal = tame time: health back to full. Updates the stored level. */
	static void applyBonus(Mob mob, int level, boolean heal) {
		GraspedMob ga = mob.getAttached(GraspedMob.ATTACHMENT);
		AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
		if (ga == null || hp == null) {
			return;
		}
		float before = mob.getMaxHealth();
		hp.addOrReplacePermanentModifier(new AttributeModifier(BONUS_ID, GraspRules.bonusHealth(level), AttributeModifier.Operation.ADD_VALUE));
		if (heal) {
			mob.setHealth(mob.getMaxHealth());
		} else {
			mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + Math.max(0, mob.getMaxHealth() - before)));
		}
		mob.setAttached(GraspedMob.ATTACHMENT, ga.withBonus(level));
	}

	// ---- loading, level changes ----

	private static void onLoad(Mob m, ServerLevel level) {
		GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
		GraspData data = GraspData.of(level.getServer());
		if (data.find(ga.owner(), m.getUUID()) == null) {
			releaseMob(m, false); // let go while it was unloaded (or its record is gone): back to a normal mob
			return;
		}
		LOADED.add(m);
		GraspGoals.apply(m);
		m.setPersistenceRequired();
		int ownerLevel = Math.max(1, data.get(ga.owner()).level());
		if (ga.bonusApplied() != ownerLevel) {
			applyBonus(m, ownerLevel, false);
		}
	}

	// ---- death and removal ----

	/** The mob is dead or discarded: free the slot, tell the owner. Safe to call twice. */
	private static void gone(Mob m) {
		GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
		LOADED.remove(m);
		if (ga == null || !(m.level() instanceof ServerLevel sl)) {
			return;
		}
		MinecraftServer server = sl.getServer();
		GraspData.Entry e = GraspData.of(server).remove(ga.owner(), m.getUUID());
		if (e == null) {
			return;
		}
		ServerPlayer o = server.getPlayerList().getPlayer(ga.owner());
		if (o != null) {
			o.sendSystemMessage(Component.literal("Your ").append(nameOf(m.getType(), ga.number())).append(" died"));
			sendState(o, tethering(o) ? -2 : -1, 0);
		}
	}

	// ---- release ----

	private static void releaseRequest(ServerPlayer p, String target) {
		UUID id = parse(target);
		if (id != null && releaseOne(p, id)) {
			sendState(p, -1, 0);
			sendMenu(p);
		}
	}

	private static boolean releaseOne(ServerPlayer p, UUID id) {
		GraspData data = GraspData.of(p.level().getServer());
		if (data.find(p.getUUID(), id) == null) {
			return false;
		}
		data.remove(p.getUUID(), id);
		Entity e = p.level().getServer().overworld().getEntityInAnyDimension(id);
		if (e instanceof Mob m) {
			releaseMob(m, true);
		} // an unloaded mob lets itself go when it loads (its record is gone)
		return true;
	}

	/** D168: lets every mob of the player go. */
	private static void allRelease(ServerPlayer p) {
		for (GraspData.Entry e : new ArrayList<>(GraspData.of(p.level().getServer()).get(p.getUUID()).mobs())) {
			releaseOne(p, e.mob());
		}
		sendState(p, -1, 0);
		sendMenu(p);
	}

	/** Back to a normal mob: no attachment, bonus, name, persistence, given gear, vanilla goals; peace for a moment. */
	public static void releaseMob(Mob m, boolean peace) {
		GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
		if (ga == null) {
			return;
		}
		LOADED.remove(m);
		if (m.level() instanceof ServerLevel sl) {
			GraspGear.dropGiven(sl, m, ga);
			if (peace) {
				PEACE.put(m.getUUID(), new Peace(ga.owner(), sl.getGameTime() + GraspRules.RELEASE_PEACE_TICKS));
			}
		}
		m.removeAttached(GraspedMob.ATTACHMENT);
		AttributeInstance hp = m.getAttribute(Attributes.MAX_HEALTH);
		if (hp != null) {
			hp.removeModifier(BONUS_ID);
		}
		if (m.getHealth() > m.getMaxHealth()) {
			m.setHealth(m.getMaxHealth());
		}
		m.setCustomName(null);
		m.setCustomNameVisible(false);
		((io.github.huydangtruong.immortalcultivation.mixin.MobGoalAccess) m).permahearts$setPersistenceRequired(false);
		m.setCanPickUpLoot(false);
		m.setTarget(null);
		GraspGoals.restore(m);
	}

	// ---- mode, call ----

	private static UUID parse(String s) {
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static void setMode(ServerPlayer p, String target, int mode) {
		if (setModeOne(p, parse(target), mode)) {
			sendMenu(p);
		}
	}

	/** D168: the same mode for every mob of the player. */
	private static void allMode(ServerPlayer p, int mode) {
		for (GraspData.Entry e : new ArrayList<>(GraspData.of(p.level().getServer()).get(p.getUUID()).mobs())) {
			setModeOne(p, e.mob(), mode);
		}
		sendMenu(p);
	}

	private static boolean setModeOne(ServerPlayer p, UUID id, int mode) {
		GraspData data = GraspData.of(p.level().getServer());
		GraspData.Entry e = id == null ? null : data.find(p.getUUID(), id);
		if (e == null || mode < 0 || mode >= GraspRules.Mode.values().length) {
			return false;
		}
		data.replace(p.getUUID(), e.withMode(mode));
		if (p.level().getServer().overworld().getEntityInAnyDimension(id) instanceof Mob m && m.getAttached(GraspedMob.ATTACHMENT) != null) {
			GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
			var pos = m.blockPosition();
			m.setAttached(GraspedMob.ATTACHMENT, ga.withMode(mode, pos.getX(), pos.getY(), pos.getZ()));
			m.getNavigation().stop();
			m.setTarget(null);
		}
		return true;
	}

	private static void call(ServerPlayer p, String target) {
		UUID id = parse(target);
		GraspData.Entry e = id == null ? null : GraspData.of(p.level().getServer()).find(p.getUUID(), id);
		if (e == null) {
			return;
		}
		long now = p.level().getGameTime();
		if (CALL_READY.getOrDefault(id, 0L) > now) {
			p.sendOverlayMessage(Component.literal("Call again in " + (CALL_READY.get(id) - now + 19) / 20 + " s"));
			return;
		}
		if (!(p.level().getEntity(id) instanceof Mob m) || !m.isAlive()) {
			p.sendOverlayMessage(Component.literal("It is too far away to hear you"));
			return;
		}
		callTo(p, m, p.position().add(p.getLookAngle().multiply(2, 0, 2)), now);
		sendMenu(p);
	}

	/** Teleports a mob to a spot near a point and starts its Call cooldown. */
	private static void callTo(ServerPlayer p, Mob m, Vec3 want, long now) {
		Vec3 at = GraspGoals.freeSpotNear(p.level(), m, want);
		m.getNavigation().stop();
		m.snapTo(at.x, at.y, at.z);
		m.setTarget(null);
		CALL_READY.put(m.getUUID(), now + GraspRules.CALL_COOLDOWN_TICKS);
		p.level().sendParticles(ParticleTypes.END_ROD, at.x, at.y + m.getBbHeight() / 2, at.z, 12, 0.4, 0.4, 0.4, 0.05);
	}

	/** D168: calls every mob whose cooldown is ready, each to its own ring spot (D165); says how many came. */
	private static void allCall(ServerPlayer p) {
		long now = p.level().getGameTime();
		int came = 0, waiting = 0, far = 0;
		for (GraspData.Entry e : new ArrayList<>(GraspData.of(p.level().getServer()).get(p.getUUID()).mobs())) {
			if (CALL_READY.getOrDefault(e.mob(), 0L) > now) {
				waiting++;
			} else if (p.level().getEntity(e.mob()) instanceof Mob m && m.isAlive()) {
				callTo(p, m, GraspGoals.followSpot(m, p), now);
				came++;
			} else {
				far++;
			}
		}
		String msg = came + (came == 1 ? " mob came" : " mobs came");
		if (waiting > 0) {
			msg += ", " + waiting + " still waiting";
		}
		if (far > 0) {
			msg += ", " + far + " too far away";
		}
		p.sendOverlayMessage(Component.literal(msg));
		sendMenu(p);
	}

	// ---- snapshot ----

	private static GraspData.Entry snapshot(Mob m, GraspedMob ga) {
		float bonus = (float) (m.getAttribute(Attributes.MAX_HEALTH) == null ? 0 : m.getAttribute(Attributes.MAX_HEALTH).getModifier(BONUS_ID) == null
			? 0 : m.getAttribute(Attributes.MAX_HEALTH).getModifier(BONUS_ID).amount());
		return new GraspData.Entry(m.getUUID(), typeId(m), ga.number(), ga.mode(), m.level().dimension().identifier().toString(), m.getX(), m.getY(), m.getZ(),
			m.getHealth(), m.getMaxHealth(), bonus, (float) m.getArmorValue(), (float) GraspGoals.damageOf(m), GraspGear.armorLines(m), GraspGear.heldLines(m));
	}

	private static void sendMenu(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		GraspData data = GraspData.of(server);
		long now = p.level().getGameTime();
		List<Row> rows = new ArrayList<>();
		for (GraspData.Entry e : data.get(p.getUUID()).mobs()) {
			Entity live = server.overworld().getEntityInAnyDimension(e.mob());
			boolean loaded = live instanceof Mob m && m.isAlive() && m.hasAttached(GraspedMob.ATTACHMENT);
			GraspData.Entry v = loaded ? snapshot((Mob) live, ((Mob) live).getAttached(GraspedMob.ATTACHMENT)) : e;
			float dist = loaded && live.level() == p.level() ? (float) live.distanceTo(p) : -1f;
			int callLeft = (int) Math.max(0, CALL_READY.getOrDefault(e.mob(), 0L) - now);
			rows.add(new Row(e.mob().toString(), v.type(), v.number(), e.mode(), loaded, v.dim(), dist, v.hp(), v.maxHp(), v.bonus(), v.armor(),
				v.damage(), v.armorGear(), v.weaponGear(), callLeft));
		}
		int lvl = level(p);
		ServerPlayNetworking.send(p, new Menu(rows, GraspRules.maxMobs(lvl), lvl));
	}

	// ---- tick ----

	private static void tick(MinecraftServer server) {
		for (var it = new ArrayList<>(TETHERS.entrySet()).iterator(); it.hasNext();) {
			var en = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
			if (p == null) {
				TETHERS.remove(en.getKey());
			} else if (TETHERS.get(en.getKey()) == en.getValue()) {
				tickTether(p, en.getValue());
			}
		}
		long now = server.overworld().getGameTime();
		if (!PEACE.isEmpty() && now % 20 == 0) {
			PEACE.values().removeIf(pe -> pe.until < now);
		}
		if (server.getTickCount() % 20 == 0) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				int lvl = level(p);
				if (lvl > 0 || GraspData.of(server).get(p.getUUID()).level() > 0) {
					GraspData.of(server).setLevel(p.getUUID(), lvl);
				}
			}
		}
		for (Mob m : new ArrayList<>(LOADED)) {
			if (m.isRemoved() || !m.isAlive()) {
				continue;
			}
			GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
			if (ga == null || !(m.level() instanceof ServerLevel sl)) {
				continue;
			}
			tickMob(server, sl, m, ga);
		}
	}

	private static void tickMob(MinecraftServer server, ServerLevel sl, Mob m, GraspedMob ga) {
		GraspData data = GraspData.of(server);
		int ownerLevel = Math.max(1, data.get(ga.owner()).level());
		if (ga.bonusApplied() != ownerLevel) {
			applyBonus(m, ownerLevel, false); // level up: the bonus follows at once
			ga = m.getAttached(GraspedMob.ATTACHMENT);
		}
		int t = m.tickCount;
		if (t % 4 == 0 && m.isOnFire() && sl.isBrightOutside() && !m.isInLava() && !m.isInWater() && m.getRemainingFireTicks() <= 200) {
			m.clearFire(); // no burning in sunlight
		}
		ServerPlayer o = server.getPlayerList().getPlayer(ga.owner());
		if (t % 10 == 0 && o != null && o.isAlive() && o.level() == sl && GraspRules.Mode.of(ga.mode()) == GraspRules.Mode.FOLLOW
			&& m.distanceToSqr(o) > GraspRules.FOLLOW_TELEPORT * GraspRules.FOLLOW_TELEPORT
			&& !io.github.huydangtruong.immortalcultivation.technique.PhaseState.phasing(o)) { // D169: not while the owner is in the walls
			Vec3 at = io.github.huydangtruong.immortalcultivation.technique.SafeTeleport.spotOrNull(sl, m, GraspGoals.followSpot(m, o)); // D165: its own spot; D170: safe ground only
			if (at != null) { // D169: never into a block
				m.getNavigation().stop();
				m.snapTo(at.x, at.y, at.z);
			}
		}
		if (o != null && o.isAlive() && o.level() == sl && m.getTarget() == null && GraspRules.Mode.of(ga.mode()) == GraspRules.Mode.FOLLOW) {
			// D165: a follower inside 2 blocks of its owner is gently pushed out, so a crowd never stands on the owner
			double dx = m.getX() - o.getX(), dz = m.getZ() - o.getZ();
			double h2 = dx * dx + dz * dz;
			if (h2 < GraspRules.FOLLOW_KEEP_OUT * GraspRules.FOLLOW_KEEP_OUT && Math.abs(m.getY() - o.getY()) < 2) {
				double h = Math.sqrt(h2);
				if (h < 1e-3) {
					double a = m.getUUID().hashCode() * 2.399963229728653;
					dx = Math.cos(a);
					dz = Math.sin(a);
					h = 1;
				}
				m.setDeltaMovement(m.getDeltaMovement().add(dx / h * 0.08, 0, dz / h * 0.08));
			}
		}
		if (t % 5 == 0 && o != null) {
			AABB box = m.getBoundingBox().inflate(2.5, 1.5, 2.5);
			UUID ownerId = ga.owner();
			for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, box, i -> i.isAlive() && i.getOwner() != null && i.getOwner().getUUID().equals(ownerId))) {
				if (GraspGear.pickUp(sl, m, m.getAttached(GraspedMob.ATTACHMENT), ie)) {
					break;
				}
			}
		}
		if (t % 20 == 0) {
			leaveRaid(m);
			GraspedMob cur = m.getAttached(GraspedMob.ATTACHMENT);
			if (cur != null && data.find(cur.owner(), m.getUUID()) != null) {
				data.replace(cur.owner(), snapshot(m, cur));
			}
		}
	}

	/** Test helper: forgets everything held in memory. */
	public static void clearMemory() {
		TETHERS.clear();
		LOADED.clear();
		PEACE.clear();
		CALL_READY.clear();
	}

	/** The mob's label text for chat and tests: "Skeleton 7". */
	public static MutableComponent label(Mob m) {
		GraspedMob ga = m.getAttached(GraspedMob.ATTACHMENT);
		return ga == null ? Component.empty() : nameOf(m.getType(), ga.number()).copy();
	}
}
