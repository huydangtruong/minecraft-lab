package io.github.huydangtruong.immortalcultivation.treasure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Mega mobs (P-059, D150): treasure guards with x2..x4 health and damage, 1.3x size, half knockback and glowing eyes /
 * patterns (client: MegaGlowLayer draws textures/entity/mega/&lt;species&gt;.png with RenderTypes.eyes).
 */
public final class MegaMobs {
	/** species: glow texture name; mult: health and damage multiplier; treasure: event id the guard belongs to. */
	public record Mega(String species, double mult, long treasure) {
		public static final Codec<Mega> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("species").forGetter(Mega::species),
			Codec.DOUBLE.fieldOf("mult").forGetter(Mega::mult),
			Codec.LONG.optionalFieldOf("treasure", 0L).forGetter(Mega::treasure)
		).apply(i, Mega::new));
		public static final StreamCodec<ByteBuf, Mega> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Mega::species, ByteBufCodecs.DOUBLE, Mega::mult, ByteBufCodecs.VAR_LONG, Mega::treasure, Mega::new);
	}

	/** Synced to every client that sees the mob (the glow is drawn on the client). */
	public static final AttachmentType<Mega> MEGA = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "mega"),
		b -> b.persistent(Mega.CODEC).syncWith(Mega.STREAM_CODEC, AttachmentSyncPredicate.all()));

	public static final String TAG = "permahearts.mega";
	/** Name colour per species, the same as its glow. */
	private static final java.util.Map<String, Integer> COLORS = java.util.Map.ofEntries(
		java.util.Map.entry("piglin_brute", 0xFFB020), java.util.Map.entry("blaze", 0xFF4A10),
		java.util.Map.entry("wither_skeleton", 0xFF2A1A), java.util.Map.entry("breeze", 0x80F0FF),
		java.util.Map.entry("vindicator", 0xFF3030), java.util.Map.entry("evoker", 0xFFE060),
		java.util.Map.entry("ravager", 0xFF6A20), java.util.Map.entry("hoglin", 0xFF8A2A),
		java.util.Map.entry("zoglin", 0xA0FF40), java.util.Map.entry("enderman", 0xE070FF),
		java.util.Map.entry("witch", 0x60FF40), java.util.Map.entry("stray", 0x90E8FF),
		java.util.Map.entry("bogged", 0xC0FF40), java.util.Map.entry("drowned", 0x30FFE0),
		java.util.Map.entry("guardian", 0xFF9A20), java.util.Map.entry("elder_guardian", 0xFFE8A0));

	private MegaMobs() {}

	public static void init() {
		// loads the attachment type
	}

	public static Mega mega(Entity e) {
		return e.getAttached(MEGA);
	}

	public static boolean isMega(Entity e) {
		return e.hasAttached(MEGA);
	}

	/** Damage dealt by a Mega mob (melee, arrows, fireballs, potions, fangs, beams) times its multiplier. */
	public static float scaleDamage(DamageSource source, float amount) {
		Entity by = source.getEntity();
		Mega m = by == null ? null : by.getAttached(MEGA);
		return m == null ? amount : (float) (amount * m.mult());
	}

	/** Creates (does not add) a Mega guard at feet. Null if the type cannot be made. */
	public static Mob create(ServerLevel level, TreasureRules.Guard g, BlockPos feet, long treasure, BlockPos home) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(g.entity()));
		if (!(type.create(level, EntitySpawnReason.EVENT) instanceof Mob m)) {
			return null;
		}
		m.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
		m.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), EntitySpawnReason.EVENT, null);
		m.setAttached(MEGA, new Mega(g.species(), g.mult(), treasure));
		m.addTag(TAG);
		if (m instanceof net.minecraft.world.entity.monster.hoglin.Hoglin h) {
			h.setImmuneToZombification(true);
		}
		if (m instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin p) {
			p.setImmuneToZombification(true);
		}
		if (m instanceof net.minecraft.world.entity.monster.zombie.Drowned) {
			m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
			m.setDropChance(EquipmentSlot.MAINHAND, 0.1f);
		}
		scaleBase(m, Attributes.MAX_HEALTH, g.mult());
		scaleBase(m, Attributes.SCALE, TreasureRules.SCALE);
		AttributeInstance kb = m.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (kb != null) {
			kb.setBaseValue(Math.max(kb.getBaseValue(), TreasureRules.KNOCKBACK_RESISTANCE));
		}
		// D281: the danger level's strength (health, melee damage) on top of the Mega multiplier, like any hostile mob
		io.github.huydangtruong.immortalcultivation.danger.DangerMobs.applyStrength(m,
			io.github.huydangtruong.immortalcultivation.danger.Danger.level(level.getServer()));
		m.setHealth(m.getMaxHealth());
		m.setPersistenceRequired();
		m.setHomeTo(home, TreasureRules.HOME_RADIUS);
		m.setCustomName(Component.literal("Mega ").append(type.getDescription())
			.withStyle(s -> s.withColor(TextColor.fromRgb(COLORS.getOrDefault(g.species(), 0xFFFFFF)))));
		return m;
	}

	private static void scaleBase(Mob m, Holder<Attribute> attr, double factor) {
		AttributeInstance a = m.getAttribute(attr);
		if (a != null) {
			a.setBaseValue(a.getBaseValue() * factor);
		}
	}
}
