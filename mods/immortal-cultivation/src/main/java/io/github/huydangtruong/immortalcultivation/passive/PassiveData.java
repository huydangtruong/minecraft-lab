package io.github.huydangtruong.immortalcultivation.passive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-player passives (D148). Pure Java, immutable.
 *
 * @param levels   passive id to level (missing = 0)
 * @param visionOn Increase vision switched on (the switch in the tab)
 * @param growthOn Crop growth switched on (the switch in the tab)
 * @param guardOn  Item guard switched on (the switch in the tab, D176)
 */
public record PassiveData(Map<String, Integer> levels, boolean visionOn, boolean growthOn, boolean guardOn) {
	public static final PassiveData DEFAULT = new PassiveData(Map.of(), true, true, true);

	public static final Codec<PassiveData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(PassiveData::levels),
		Codec.BOOL.optionalFieldOf("vision_on", true).forGetter(PassiveData::visionOn),
		Codec.BOOL.optionalFieldOf("growth_on", true).forGetter(PassiveData::growthOn),
		Codec.BOOL.optionalFieldOf("guard_on", true).forGetter(PassiveData::guardOn)
	).apply(i, PassiveData::new));

	public PassiveData {
		levels = Map.copyOf(levels);
	}

	public int level(Passive p) {
		return PassiveRules.clamp(levels.getOrDefault(p.id(), 0));
	}

	public PassiveData withLevel(Passive p, int level) {
		Map<String, Integer> m = new HashMap<>(levels);
		m.put(p.id(), PassiveRules.clamp(level));
		return new PassiveData(m, visionOn, growthOn, guardOn);
	}

	/** True unless the passive has a switch that is off. */
	public boolean isOn(Passive p) {
		return switch (p) {
			case VISION -> visionOn;
			case GROWTH -> growthOn;
			case GUARD -> guardOn;
			default -> true;
		};
	}

	public PassiveData withOn(Passive p, boolean on) {
		return switch (p) {
			case VISION -> new PassiveData(levels, on, growthOn, guardOn);
			case GROWTH -> new PassiveData(levels, visionOn, on, guardOn);
			case GUARD -> new PassiveData(levels, visionOn, growthOn, on);
			default -> this;
		};
	}

	public PassiveData withVisionOn(boolean on) {
		return withOn(Passive.VISION, on);
	}

	/** Level in use: 0 when switched off. */
	public int activeLevel(Passive p) {
		return isOn(p) ? level(p) : 0;
	}

	/** Vision strength in use: 0 when switched off. */
	public double activeVision() {
		return PassiveRules.vision(activeLevel(Passive.VISION));
	}
}
