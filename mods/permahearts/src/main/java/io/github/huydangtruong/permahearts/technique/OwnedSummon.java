package io.github.huydangtruong.permahearts.technique;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** A summoned fighter that belongs to a player (Violet Guardian, Warrior Angel): shared owner and kill credit logic. */
public interface OwnedSummon {
	UUID owner();

	/** The owner if online in the same level as the summon, else null. */
	static Player ownerOf(Mob summon) {
		UUID id = ((OwnedSummon) summon).owner();
		return id == null ? null : summon.level().getPlayerByUUID(id);
	}

	/**
	 * P-043: during a Death Match every other player is the owner's opponent: the summon may target and hit that player,
	 * and only then (outside a match summons never attack players).
	 */
	static boolean isMatchOpponent(Mob summon, Entity target) {
		if (!(target instanceof net.minecraft.server.level.ServerPlayer tp) || !tp.isAlive() || tp.isSpectator() || tp.isCreative()
			|| !(summon.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
			return false;
		}
		Player owner = ownerOf(summon);
		var server = sl.getServer();
		return owner != null && tp != owner && io.github.huydangtruong.permahearts.PermaHearts.enabled(server)
			&& io.github.huydangtruong.permahearts.wanderer.DeathMatch.running(server);
	}

	/** The nearest opponent of the owner in range of the summon during a Death Match, or null. */
	static LivingEntity nearestMatchOpponent(Mob summon, double range) {
		LivingEntity best = null;
		for (Player p : summon.level().players()) {
			if (isMatchOpponent(summon, p) && p.distanceToSqr(summon) < range * range
				&& (best == null || p.distanceToSqr(summon) < best.distanceToSqr(summon))) {
				best = p;
			}
		}
		return best;
	}

	/**
	 * D62: before a summon hurts a mob, the owner gets the vanilla player kill credit on it, like a tamed wolf's owner.
	 * Then {@code getKillCredit()} returns the owner: kill quests, +1 heart per 100 kills and HeartXP all count it.
	 */
	static void giveKillCredit(Mob summon, Entity target) {
		Player owner = ownerOf(summon);
		if (owner != null && owner.isAlive() && target instanceof LivingEntity living && !(target instanceof Player)) {
			living.setLastHurtByPlayer(owner, 100);
		}
	}
}
