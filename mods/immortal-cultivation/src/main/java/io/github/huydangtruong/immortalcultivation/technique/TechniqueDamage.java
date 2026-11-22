package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Damage from techniques. D44: it must reach players even when the server's PvP is off. */
final class TechniqueDamage {
	private TechniqueDamage() {}

	/** True for entities a technique may affect: living, not the caster, not spectating, not the Wanderer NPC. */
	static boolean affectable(LivingEntity e, Entity caster) {
		return e != caster && e.isAlive() && !e.isSpectator() && !WandererNpc.isWanderer(e);
	}

	/**
	 * True for entities an area technique may hurt or slow (P-077 B11): affectable, not the caster's friends (summons,
	 * grasped mobs), and other players only when the PvP game rule is on (D182). An owner that is not a player (an orb
	 * of a mob, a caster who logged out) has no friends to spare and no PvP rule to follow.
	 */
	static boolean foe(LivingEntity e, Entity caster) {
		if (!affectable(e, caster)) {
			return false;
		}
		if (!(caster instanceof ServerPlayer p)) {
			return true;
		}
		return !Lance.isFriend(e, p)
			&& (!(e instanceof Player) || io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch.pvpAllowed(p.level().getServer()));
	}

	/**
	 * Hurts target. ServerPlayer#hurtServer refuses any damage whose causing entity is a Player when PvP is off (or the
	 * players share a team), so for player targets the causing entity is left empty: the damage then counts as magic
	 * from the technique (direct entity = the orb or nothing). Trade-off: a player killed this way credits nobody and
	 * the death message names no attacker. Mobs get the caster as the cause (kill credit, XP, aggro).
	 */
	static boolean hurt(ServerLevel level, LivingEntity target, ServerPlayer caster, Entity direct, ResourceKey<DamageType> type, float amount) {
		var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);
		Entity cause = target instanceof Player ? null : caster;
		depth++;
		try {
			return target.hurtServer(level, new DamageSource(holder, direct, cause), amount);
		} finally {
			depth--;
		}
	}

	private static int depth; // > 0 while a technique deals damage (server thread only)

	/** True while TechniqueDamage.hurt runs: a Crystal Prison disarm never blocks technique damage (D146). */
	static boolean active() {
		return depth > 0;
	}
}
