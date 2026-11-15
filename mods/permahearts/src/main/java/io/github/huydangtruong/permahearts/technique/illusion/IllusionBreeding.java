package io.github.huydangtruong.permahearts.technique.illusion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.animal.Animal;

/**
 * Remembers animals that an Illusion clone put into love mode (D73). Their offspring gives no breeding XP: the XP orb in
 * {@code Animal#finalizeSpawnChildFromBreeding} is skipped when the animal is marked here.
 */
public final class IllusionBreeding {
	/** A mark lasts a bit longer than the longest love mode (30 s). */
	private static final long MARK_TICKS = 20 * 60;
	private static final Map<UUID, Long> MARKED = new HashMap<>();

	private IllusionBreeding() {}

	public static void mark(Animal a) {
		long now = a.level().getGameTime();
		if (MARKED.size() > 200) {
			MARKED.values().removeIf(t -> t < now);
		}
		MARKED.put(a.getUUID(), now + MARK_TICKS);
	}

	/** True when a clone fed this animal and the mark has not expired. */
	public static boolean isMarked(Animal a) {
		Long until = MARKED.get(a.getUUID());
		return until != null && until >= a.level().getGameTime();
	}
}
