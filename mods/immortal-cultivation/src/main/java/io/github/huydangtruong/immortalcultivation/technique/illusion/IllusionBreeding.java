package io.github.huydangtruong.immortalcultivation.technique.illusion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.animal.Animal;

/**
 * Remembers animals that an Illusion clone put into love mode (D73). Their offspring drops no breeding XP orb (D140): the orb in
 * {@code Animal#finalizeSpawnChildFromBreeding} is skipped when the animal is marked here and its XP goes straight to the owner (nothing when offline).
 */
public final class IllusionBreeding {
	/** A mark lasts a bit longer than the longest love mode (30 s). */
	private static final long MARK_TICKS = 20 * 60;
	private static final Map<UUID, Mark> MARKED = new HashMap<>();

	private record Mark(long until, UUID owner) {}

	private IllusionBreeding() {}

	public static void mark(Animal a, UUID owner) {
		long now = a.level().getGameTime();
		if (MARKED.size() > 200) {
			MARKED.values().removeIf(m -> m.until() < now);
		}
		MARKED.put(a.getUUID(), new Mark(now + MARK_TICKS, owner));
	}

	/** True when a clone fed this animal and the mark has not expired. */
	public static boolean isMarked(Animal a) {
		Mark m = MARKED.get(a.getUUID());
		return m != null && m.until() >= a.level().getGameTime();
	}

	/** D140: gives the breeding XP to the owner of the clone that fed this animal, if online. The caller drops no orb. */
	public static void giveXpToOwner(Animal a, int xp) {
		Mark m = MARKED.get(a.getUUID());
		if (m != null && m.owner() != null && a.level().getPlayerByUUID(m.owner()) instanceof net.minecraft.server.level.ServerPlayer p) {
			p.giveExperiencePoints(io.github.huydangtruong.immortalcultivation.passive.Passives.bonusXp(p, xp)); // D148 XP gain bonus
		}
	}
}
