package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/** A running Death Match: which quest line it belongs to and the UUIDs (as strings) of the players that fell so far. */
public record DeathMatchState(int line, List<String> fallen) {
	public static final Codec<DeathMatchState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("line").forGetter(DeathMatchState::line),
		Codec.STRING.listOf().optionalFieldOf("fallen", List.of()).forGetter(DeathMatchState::fallen)
	).apply(i, DeathMatchState::new));

	/** A copy with this victim added; unchanged if the victim already fell. */
	public DeathMatchState with(String victimUuid) {
		if (fallen.contains(victimUuid)) {
			return this;
		}
		ArrayList<String> copy = new ArrayList<>(fallen);
		copy.add(victimUuid);
		return new DeathMatchState(line, List.copyOf(copy));
	}
}
