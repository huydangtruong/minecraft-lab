package io.github.huydangtruong.immortalcultivation.danger;

import com.mojang.serialization.Codec;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules.Affix;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer;
import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspedMob;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** P-054 (D137, D138): what the danger level does to a hostile mob when it enters the world: strength, gear, elite affixes. */
public final class DangerMobs {
	/** Entity tag: the danger level was applied to this mob once, never again. */
	public static final String APPLIED_TAG = "permahearts_danger";
	/** Entity tag: spawned by the daylight/rain relaxation, never burns in the sun. */
	public static final String DAY_TAG = "permahearts_day_spawn";

	/** Elite affixes by enum name. Saved with the mob (and kept by a Soul Grasp tame). */
	public static final AttachmentType<List<String>> AFFIXES = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "elite_affixes"),
		b -> b.persistent(Codec.STRING.listOf()));

	private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "danger_health");
	private static final Identifier DAMAGE_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "danger_damage");
	private static final Identifier ELITE_HEALTH_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "elite_health");
	private static final Identifier ELITE_SPEED_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "elite_swift");
	private static final java.util.Random RNG = new java.util.Random();
	private static final DustParticleOptions ORANGE = new DustParticleOptions(0xFF8800, 1.0f);

	/** Elites that are loaded right now (for regeneration and particles). */
	private static final Set<Mob> ELITES = new HashSet<>();

	private DangerMobs() {}

	public static void init() {
		AFFIXES.identifier();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> ELITES.clear()); // B6: stale Mob refs
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob m) || !(entity instanceof Enemy)) {
				return;
			}
			if (!m.entityTags().contains(APPLIED_TAG) && !excluded(m)) {
				m.addTag(APPLIED_TAG);
				if (DangerSpawn.consumeDayMark(m, level.getGameTime())) {
					m.addTag(DAY_TAG);
				}
				int lvl = Danger.level(level.getServer());
				if (lvl > 0) {
					applyOnSpawn(level, m, lvl);
				}
			}
			if (m.hasAttached(AFFIXES)) {
				ELITES.add(m);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Mob m) {
				ELITES.remove(m);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0 && !ELITES.isEmpty()) {
				eliteSecond();
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
			if (blocked || taken <= 0 || !(source.getEntity() instanceof Mob attacker) || victim == attacker) {
				return;
			}
			Set<Affix> a = affixes(attacker);
			if (a.contains(Affix.BLAZING)) {
				victim.igniteForSeconds(DangerRules.BLAZING_FIRE_SECONDS);
			}
			if (a.contains(Affix.VENOMOUS)) {
				victim.addEffect(new MobEffectInstance(MobEffects.POISON, DangerRules.VENOMOUS_POISON_SECONDS * 20, 0));
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel sl && entity.hasAttached(AFFIXES) && RNG.nextBoolean()) {
				entity.spawnAtLocation(sl, new ItemStack(Items.EMERALD, 1 + RNG.nextInt(3)));
			}
		});
	}

	/** Mobs the danger level never touches: bosses, chieftains, tamed and summoned allies, clones. */
	public static boolean excluded(Entity e) {
		return e instanceof GodSlayer || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
			|| e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
			|| e instanceof net.minecraft.world.entity.monster.warden.Warden
			|| e instanceof OwnedSummon || e instanceof Illusion || WandererNpc.isWanderer(e)
			|| e.hasAttached(GraspedMob.ATTACHMENT) || io.github.huydangtruong.immortalcultivation.treasure.MegaMobs.isMega(e)
			|| e.entityTags().contains(QuestBoss.TAG) || e.entityTags().contains(QuestBoss.MINION_TAG);
	}

	/** Strength, gear and the elite roll for a fresh mob at the given level. */
	public static void applyOnSpawn(ServerLevel level, Mob m, int lvl) {
		applyStrength(m, lvl);
		applyGear(level, m, lvl);
		if (RNG.nextDouble() < DangerRules.eliteChance(lvl)) {
			makeElite(m, DangerRules.rollAffixes(lvl, RNG));
		}
		if (m instanceof net.minecraft.world.entity.monster.piglin.PiglinBrute b) {
			b.setImmuneToZombification(true);
		}
		m.setHealth(m.getMaxHealth());
	}

	public static void applyStrength(Mob m, int lvl) {
		double bonus = DangerRules.strengthBonus(lvl);
		if (bonus <= 0) {
			return;
		}
		add(m.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		add(m.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		m.setHealth(m.getMaxHealth());
	}

	private static void add(AttributeInstance inst, Identifier id, double amount, AttributeModifier.Operation op) {
		if (inst != null && !inst.hasModifier(id)) {
			inst.addPermanentModifier(new AttributeModifier(id, amount, op));
		}
	}

	// ---- Gear ----

	private static boolean armorCapable(Mob m) {
		return m instanceof Zombie || m instanceof AbstractSkeleton || m instanceof AbstractPiglin || m instanceof AbstractIllager;
	}

	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private static Item armorItem(DangerRules.Tier tier, int slot) {
		return switch (tier) {
			case IRON -> new Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS}[slot];
			case DIAMOND -> new Item[] {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}[slot];
			case NETHERITE -> new Item[] {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}[slot];
			default -> null;
		};
	}

	public static void applyGear(ServerLevel level, Mob m, int lvl) {
		if (lvl < DangerRules.GEAR_START_LEVEL || !armorCapable(m)) {
			return;
		}
		DangerRules.GearPlan plan = DangerRules.gearRoll(lvl, RNG);
		Holder<Enchantment> protection = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		for (int i = 0; i < 4; i++) {
			Item item = armorItem(plan.armor()[i], i);
			if (item == null) {
				continue;
			}
			ItemStack stack = new ItemStack(item);
			if (plan.enchanted()[i]) {
				stack.enchant(protection, 1 + RNG.nextInt(lvl >= DangerRules.PROTECTION_4_LEVEL ? 4 : 3));
			}
			m.setItemSlot(ARMOR_SLOTS[i], stack);
			m.setDropChance(ARMOR_SLOTS[i], 0.085f);
		}
	}

	// ---- Elites ----

	public static Set<Affix> affixes(Entity e) {
		List<String> names = e.getAttached(AFFIXES);
		if (names == null || names.isEmpty()) {
			return EnumSet.noneOf(Affix.class);
		}
		Set<Affix> out = EnumSet.noneOf(Affix.class);
		for (String n : names) {
			Affix a = Affix.byName(n);
			if (a != null) {
				out.add(a);
			}
		}
		return out;
	}

	public static boolean isElite(Entity e) {
		return e.hasAttached(AFFIXES);
	}

	/** Makes the mob an elite: x2 max health on top of everything, affixes, orange name. Heals to full. */
	public static void makeElite(Mob m, List<Affix> list) {
		List<String> names = new ArrayList<>();
		MutableComponent name = Component.empty();
		for (Affix a : list) {
			names.add(a.name());
			name.append(a.label + " ");
		}
		name.append(m.getType().getDescription());
		m.setAttached(AFFIXES, names);
		m.addTag(APPLIED_TAG);
		add(m.getAttribute(Attributes.MAX_HEALTH), ELITE_HEALTH_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		if (list.contains(Affix.SWIFT)) {
			add(m.getAttribute(Attributes.MOVEMENT_SPEED), ELITE_SPEED_ID, DangerRules.SWIFT_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		}
		m.setCustomName(name.withStyle(ChatFormatting.GOLD));
		m.setCustomNameVisible(true);
		m.setHealth(m.getMaxHealth());
		ELITES.add(m);
	}

	private static void eliteSecond() {
		for (Mob m : new ArrayList<>(ELITES)) {
			if (m.isRemoved() || !m.isAlive() || !(m.level() instanceof ServerLevel sl)) {
				ELITES.remove(m);
				continue;
			}
			if (affixes(m).contains(Affix.REGENERATING) && m.getHealth() < m.getMaxHealth()) {
				m.heal((float) (m.getMaxHealth() * DangerRules.REGEN_FRACTION_PER_SECOND));
			}
			sl.sendParticles(ORANGE, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
		}
	}

	/** Hook of the hurt mixin: damage an elite takes after Ironhide. */
	public static float adjustDamage(LivingEntity e, float amount) {
		return e.hasAttached(AFFIXES) && affixes(e).contains(Affix.IRONHIDE) ? (float) (amount * DangerRules.IRONHIDE_DAMAGE_TAKEN) : amount;
	}

	/** Hook of the hurt mixin: Blazing elites ignore fire damage. */
	public static boolean ignoresFire(LivingEntity e) {
		return e.hasAttached(AFFIXES) && affixes(e).contains(Affix.BLAZING);
	}

	/** D207: XP multiplier of a mob from its danger health boost (strength modifier and elite doubling); 1 when unboosted. */
	public static double xpFactor(LivingEntity e) {
		AttributeInstance inst = e.getAttribute(Attributes.MAX_HEALTH);
		if (inst == null) {
			return 1.0;
		}
		AttributeModifier strength = inst.getModifier(HEALTH_ID);
		return DangerRules.xpFactor(strength == null ? 0 : strength.amount(), inst.hasModifier(ELITE_HEALTH_ID));
	}

	/** D282: 1 + the danger strength this mob got when it spawned (kept after a Soul Grasp tame); 1 when unboosted. */
	public static double strengthFactor(Entity e) {
		AttributeInstance inst = e instanceof LivingEntity l ? l.getAttribute(Attributes.MAX_HEALTH) : null;
		AttributeModifier strength = inst == null ? null : inst.getModifier(HEALTH_ID);
		return strength == null ? 1.0 : 1.0 + strength.amount();
	}

	public static boolean isDaySpawn(Entity e) {
		return e.entityTags().contains(DAY_TAG);
	}
}
