package io.github.huydangtruong.immortalcultivation.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Areas left behind by techniques: the fire field, the frost area and the healing circle. They are plain server-side
 * records ticked once per server tick (no entity; the fields place no blocks themselves: the fire orb sets real fire on
 * impact, D55), kept in memory only.
 */
public final class TechniqueFields {
	public enum Kind { FIRE, ICE, HEAL }

	/** Lowest and highest reach of an area relative to its centre height (blocks). */
	private static final double BELOW = 1.0;
	private static final double ABOVE = 3.0;

	/** One area. start/end are game time of its level; techLevel is the caster's technique level. */
	public record Field(Kind kind, ResourceKey<Level> dimension, Vec3 center, double radius, long start, long end,
			int techLevel, UUID owner) {}

	private static final List<Field> FIELDS = new ArrayList<>();

	/**
	 * D94: entities burning because of a fire field (inside it or in the afterburn after leaving). The extra burn damage
	 * runs from here, once per second, until the entity stops burning, the afterburn ends or it dies/is removed.
	 */
	private static final class Burning {
		final LivingEntity entity;
		final UUID owner;
		int techLevel;
		long end;
		long nextDamage;

		Burning(LivingEntity entity, UUID owner, int techLevel, long end, long nextDamage) {
			this.entity = entity;
			this.owner = owner;
			this.techLevel = techLevel;
			this.end = end;
			this.nextDamage = nextDamage;
		}
	}

	private static final Map<UUID, Burning> BURNING = new HashMap<>();

	private TechniqueFields() {}

	public static void add(Kind kind, ServerLevel level, Vec3 center, double radius, double seconds, int techLevel, UUID owner) {
		long now = level.getGameTime();
		FIELDS.add(new Field(kind, level.dimension(), center, radius, now, now + Math.round(seconds * 20), techLevel, owner));
	}

	public static int count() {
		return FIELDS.size();
	}

	/** Entities currently tracked for the fire afterburn (tests). */
	public static int burningCount() {
		return BURNING.size();
	}

	public static void clear() {
		FIELDS.clear();
		BURNING.clear();
	}

	public static void tick(MinecraftServer server) {
		tickBurning(server);
		if (FIELDS.isEmpty()) {
			return;
		}
		for (Iterator<Field> it = FIELDS.iterator(); it.hasNext();) {
			Field f = it.next();
			ServerLevel level = server.getLevel(f.dimension());
			if (level == null || level.getGameTime() >= f.end()) {
				it.remove();
				continue;
			}
			tickField(level, f, server);
		}
	}

	private static void tickField(ServerLevel level, Field f, MinecraftServer server) {
		long age = level.getGameTime() - f.start();
		if (age % 2 == 0) {
			particles(level, f);
		}
		ServerPlayer owner = server.getPlayerList().getPlayer(f.owner());
		switch (f.kind()) {
			case FIRE -> {
				if (age % 10 == 0) {
					burn(level, f, owner);
				}
			}
			case ICE -> {
				if (age % 5 == 0) {
					freeze(level, f);
				}
			}
			case HEAL -> {
				if (age % 20 == 0) { // 10 steps over the default 10 s
					heal(level, f);
				}
			}
		}
	}

	/** Living entities whose feet are inside the area's circle (and within a few blocks of its height). */
	static List<LivingEntity> inside(ServerLevel level, Field f) {
		double r = f.radius();
		Vec3 c = f.center();
		AABB box = new AABB(c.x - r, c.y - BELOW, c.z - r, c.x + r, c.y + ABOVE, c.z + r);
		return level.getEntitiesOfClass(LivingEntity.class, box, e -> {
			double dx = e.getX() - c.x;
			double dz = e.getZ() - c.z;
			return dx * dx + dz * dz <= r * r && e.isAlive() && !e.isSpectator();
		});
	}

	private static void burn(ServerLevel level, Field f, ServerPlayer owner) {
		long now = level.getGameTime();
		long end = now + Math.round(TechniqueRules.fireAfterburnSeconds(f.techLevel()) * 20);
		for (LivingEntity e : inside(level, f)) {
			if (!TechniqueDamage.affectable(e, owner)) {
				continue;
			}
			if (e.isInWater()) {
				continue; // water puts it out at once
			}
			e.igniteForTicks((int) (end - now));
			if (!e.isOnFire()) {
				continue; // fire immune
			}
			Burning b = BURNING.get(e.getUUID());
			if (b == null) {
				BURNING.put(e.getUUID(), new Burning(e, f.owner(), f.techLevel(), end, now + 20));
			} else {
				b.techLevel = f.techLevel();
				b.end = end;
			}
		}
	}

	/** D94: vanilla fire hurts 1 per second; fireBurnPerSecond above 1 adds the rest on top, while the entity burns because of a field. */
	private static void tickBurning(MinecraftServer server) {
		if (BURNING.isEmpty()) {
			return;
		}
		for (Iterator<Burning> it = BURNING.values().iterator(); it.hasNext();) {
			Burning b = it.next();
			LivingEntity e = b.entity;
			if (e.isRemoved() || !e.isAlive() || !e.isOnFire() || !(e.level() instanceof ServerLevel level)) {
				it.remove();
				continue;
			}
			long now = level.getGameTime();
			if (now > b.end) {
				it.remove();
				continue;
			}
			if (now >= b.nextDamage) {
				b.nextDamage = now + 20;
				double extra = TechniqueRules.fireBurnPerSecond(b.techLevel) - 1;
				if (extra > 0) {
					ServerPlayer owner = server.getPlayerList().getPlayer(b.owner);
					e.setInvulnerableTime(0); // the vanilla tick may have just hurt it: don't let that swallow the extra
					TechniqueDamage.hurt(level, e, owner, null, DamageTypes.IN_FIRE, (float) extra);
				}
			}
		}
	}

	private static void freeze(ServerLevel level, Field f) {
		int amplifier = Frostbite.amplifierFor(TechniqueRules.iceSlow(f.techLevel()));
		int duration = (int) Math.round(TechniqueRules.iceLingerSeconds(f.techLevel()) * 20) + 5;
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(f.owner());
		for (LivingEntity e : inside(level, f)) {
			if (TechniqueDamage.affectable(e, owner)) {
				e.addEffect(new MobEffectInstance(Frostbite.EFFECT, duration, amplifier, false, true, true));
			}
		}
	}

	private static void heal(ServerLevel level, Field f) {
		float perStep = (float) (TechniqueRules.healingTotal(f.techLevel()) / TechniqueRules.healingSeconds(f.techLevel()));
		int healed = 0;
		for (LivingEntity e : inside(level, f)) {
			if (e.getHealth() < e.getMaxHealth()) {
				e.heal(perStep);
				healed++;
			}
		}
		if (healed > 0) {
			level.playSound(null, f.center().x, f.center().y, f.center().z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.4f);
		}
	}

	private static void particles(ServerLevel level, Field f) {
		Vec3 c = f.center();
		ParticleOptions edge = switch (f.kind()) {
			case FIRE -> ParticleTypes.FLAME;
			case ICE -> ParticleTypes.SNOWFLAKE;
			case HEAL -> ParticleTypes.END_ROD;
		};
		ParticleOptions fill = switch (f.kind()) {
			case FIRE -> ParticleTypes.SMALL_FLAME;
			case ICE -> ParticleTypes.SNOWFLAKE;
			case HEAL -> ParticleTypes.HAPPY_VILLAGER;
		};
		int points = Math.max(12, (int) (f.radius() * 8));
		double offset = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < points; i++) {
			double a = offset + Math.PI * 2 * i / points;
			level.sendParticles(edge, c.x + Math.cos(a) * f.radius(), c.y + 0.1, c.z + Math.sin(a) * f.radius(), 1, 0, 0.05, 0, 0.0);
		}
		int inner = Math.max(3, (int) (f.radius() * f.radius()));
		for (int i = 0; i < inner; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double d = Math.sqrt(level.getRandom().nextDouble()) * f.radius();
			level.sendParticles(fill, c.x + Math.cos(a) * d, c.y + 0.1, c.z + Math.sin(a) * d, 1, 0, f.kind() == Kind.HEAL ? 0.4 : 0.15, 0, 0.01);
		}
	}
}
