package io.github.huydangtruong.permahearts;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;

/** Elixir levels per stat id. A map keyed by id so new stats can be added later; unknown keys are kept. */
public record CultivationData(Map<String, Integer> levels) {
	public static final CultivationData DEFAULT = new CultivationData(Map.of());

	public static final Codec<CultivationData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
		.xmap(CultivationData::new, CultivationData::levels);

	public int level(ElixirStat stat) {
		return levels.getOrDefault(stat.id(), 0);
	}

	public CultivationData plus(ElixirStat stat, int n) {
		Map<String, Integer> m = new HashMap<>(levels);
		m.put(stat.id(), Math.max(0, level(stat) + n));
		return new CultivationData(Map.copyOf(m));
	}
}
