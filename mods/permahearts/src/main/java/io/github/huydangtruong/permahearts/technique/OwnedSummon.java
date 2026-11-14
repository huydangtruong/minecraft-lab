package io.github.huydangtruong.permahearts.technique;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** A summoned fighter that belongs to a player (Violet Guardian, Warrior Angel): shared owner and kill credit logic. */
interface OwnedSummon {
	UUID owner();

	/** The owner if online in the same level as the summon, else null. */
	static Player ownerOf(Mob summon) {
		UUID id = ((OwnedSummon) summon).owner();
		return id == null ? null : summon.level().getPlayerByUUID(id);
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
