package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A summoned quest boss: the quest line and the boss entity's UUID. */
public record BossState(int line, String bossUuid) {
	public static final Codec<BossState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("line").forGetter(BossState::line),
		Codec.STRING.fieldOf("boss").forGetter(BossState::bossUuid)
	).apply(i, BossState::new));
}
