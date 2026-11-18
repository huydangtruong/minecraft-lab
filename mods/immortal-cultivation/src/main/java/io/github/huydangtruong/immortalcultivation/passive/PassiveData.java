package io.github.huydangtruong.immortalcultivation.passive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-player passives (D148). Pure Java, immutable.
 *
 * @param levels   passive id to level (missing = 0)
 * @param visionOn Increase vision switched on (the player toggles it in the tab)
 */
public record PassiveData(Map<String, Integer> levels, boolean visionOn) {
	public static final PassiveData DEFAULT = new PassiveData(Map.of(), true);

	public static final Codec<PassiveData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(PassiveData::levels),
		Codec.BOOL.optionalFieldOf("vision_on", true).forGetter(PassiveData::visionOn)
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
		return new PassiveData(m, visionOn);
	}

	public PassiveData withVisionOn(boolean on) {
		return new PassiveData(levels, on);
	}

	/** Vision strength in use: 0 when switched off. */
	public double activeVision() {
		return visionOn ? PassiveRules.vision(level(Passive.VISION)) : 0;
	}
}
