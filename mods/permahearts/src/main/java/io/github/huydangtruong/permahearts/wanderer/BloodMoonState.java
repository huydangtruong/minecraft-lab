package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A running Blood Moon: which quest line it belongs to, who started it, and the overworld game time it started. */
public record BloodMoonState(int line, String starterUuid, long startGameTime) {
	public static final Codec<BloodMoonState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("line").forGetter(BloodMoonState::line),
		Codec.STRING.fieldOf("starter").forGetter(BloodMoonState::starterUuid),
		Codec.LONG.fieldOf("start").forGetter(BloodMoonState::startGameTime)
	).apply(i, BloodMoonState::new));
}
