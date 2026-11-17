package io.github.huydangtruong.immortalcultivation.godslayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** Pure rules of the God Slayer skill director (D111): unlock by level and a shuffled bag of special skills. */
public final class GodSlayerDirector {
	public enum Kind {
		FIREBALL(2), KATANA_THROW(3), FLAME_BREATH(4), EYE_BEAM(5);

		public final int unlockLevel;

		Kind(int unlockLevel) {
			this.unlockLevel = unlockLevel;
		}
	}

	/** Skills that are finished and wired in. A skill not listed here is never unlocked. */
	private static final List<Kind> IMPLEMENTED = List.of(Kind.FIREBALL, Kind.KATANA_THROW, Kind.FLAME_BREATH, Kind.EYE_BEAM);

	public static List<Kind> unlocked(int level) {
		List<Kind> out = new ArrayList<>();
		for (Kind k : Kind.values()) {
			if (k.unlockLevel <= level && IMPLEMENTED.contains(k)) {
				out.add(k);
			}
		}
		return out;
	}

	private final List<Kind> bag = new ArrayList<>();
	private Kind last;

	/** The next skill that fits now, or null. Never the same twice in a row (unless it is the only one). */
	public Kind pick(RandomGenerator rng, int level, Predicate<Kind> fits) {
		List<Kind> all = unlocked(level);
		if (all.isEmpty()) {
			return null;
		}
		bag.retainAll(all);
		if (bag.isEmpty()) {
			bag.addAll(all);
			Collections.shuffle(bag, new java.util.Random(rng.nextLong()));
		}
		for (Kind k : bag) {
			if ((k != last || all.size() == 1) && fits.test(k)) {
				bag.remove(k);
				last = k;
				return k;
			}
		}
		return null;
	}

	/** Melee pressure before the next skill: 4 to 8 seconds, in ticks. */
	public static int pressureTicks(RandomGenerator rng) {
		return 80 + rng.nextInt(81);
	}
}
