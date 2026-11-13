package io.github.huydangtruong.permahearts;

import java.util.Locale;
import java.util.Optional;

/** The permanent stats an elixir can raise. No Minecraft imports so JUnit can test it. */
public enum ElixirStat {
	STRENGTH("strength", 0.5, false),
	FURY("fury", 0.10, true),
	SWIFTNESS("swiftness", 0.05, true),
	IRON_SKIN("iron_skin", 1.0, false),
	REACH("reach", 0.5, false),
	MINING("mining", 0.10, true),
	/** Not an attribute: each meal heals extra health = food points x bonus (D30). */
	VITALITY("vitality", 0.25, true);

	private final String id;
	private final double step;
	private final boolean multiplier;

	ElixirStat(String id, double step, boolean multiplier) {
		this.id = id;
		this.step = step;
		this.multiplier = multiplier;
	}

	public String id() {
		return id;
	}

	public double step() {
		return step;
	}

	/** True: the bonus is a fraction of the base value (ADD_MULTIPLIED_BASE); false: a flat addition. */
	public boolean multiplier() {
		return multiplier;
	}

	public double bonus(int level) {
		return level * step;
	}

	/** "+1.5" for flat stats, "+20%" for multiplier stats. */
	public String format(double bonus) {
		if (multiplier) {
			return "+" + Math.round(bonus * 100) + "%";
		}
		return "+" + String.format(Locale.ROOT, "%.1f", bonus);
	}

	/** Extra health (half-hearts) a meal of the given food points heals at this Vitality level. */
	public static float foodHeal(int nutrition, int vitalityLevel) {
		return (float) (Math.max(0, nutrition) * VITALITY.bonus(Math.max(0, vitalityLevel)));
	}

	public static Optional<ElixirStat> byId(String id) {
		for (ElixirStat s : values()) {
			if (s.id.equals(id)) {
				return Optional.of(s);
			}
		}
		return Optional.empty();
	}
}
