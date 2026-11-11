package io.github.huydangtruong.permahearts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-player PermaHearts state. bonusHearts is the offset from 10 hearts (may be negative). heartXp is 0..499. */
public record HeartsData(int bonusHearts, int heartXp, int totalHostileKills, int totalDeaths) {
	public static final HeartsData DEFAULT = new HeartsData(0, 0, 0, 0);

	public static final Codec<HeartsData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("bonus_hearts", 0).forGetter(HeartsData::bonusHearts),
		Codec.INT.optionalFieldOf("heart_xp", 0).forGetter(HeartsData::heartXp),
		Codec.INT.optionalFieldOf("total_kills", 0).forGetter(HeartsData::totalHostileKills),
		Codec.INT.optionalFieldOf("total_deaths", 0).forGetter(HeartsData::totalDeaths)
	).apply(i, HeartsData::new));
}
