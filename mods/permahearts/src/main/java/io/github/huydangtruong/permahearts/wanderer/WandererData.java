package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record WandererData(Phase phase, long nextDay, int x, int y, int z, Optional<String> npcUuid,
						   Optional<Quest> quest, long questCounter) {
	public static final WandererData DEFAULT = new WandererData(Phase.WAITING, -1, 0, 0, 0, Optional.empty(), Optional.empty(), 0);

	private static final Codec<Phase> PHASE_CODEC = Codec.STRING.xmap(Phase::valueOf, Phase::name);

	public static final Codec<WandererData> CODEC = RecordCodecBuilder.create(i -> i.group(
		PHASE_CODEC.optionalFieldOf("phase", Phase.WAITING).forGetter(WandererData::phase),
		Codec.LONG.optionalFieldOf("next_day", -1L).forGetter(WandererData::nextDay),
		Codec.INT.optionalFieldOf("x", 0).forGetter(WandererData::x),
		Codec.INT.optionalFieldOf("y", 0).forGetter(WandererData::y),
		Codec.INT.optionalFieldOf("z", 0).forGetter(WandererData::z),
		Codec.STRING.optionalFieldOf("npc_uuid").forGetter(WandererData::npcUuid),
		Quest.CODEC.optionalFieldOf("quest").forGetter(WandererData::quest),
		Codec.LONG.optionalFieldOf("quest_counter", 0L).forGetter(WandererData::questCounter)
	).apply(i, WandererData::new));

	public WandererData withPhase(Phase p) {
		return new WandererData(p, nextDay, x, y, z, npcUuid, quest, questCounter);
	}

	public WandererData withNextDay(long d) {
		return new WandererData(phase, d, x, y, z, npcUuid, quest, questCounter);
	}

	public WandererData withPos(int nx, int ny, int nz) {
		return new WandererData(phase, nextDay, nx, ny, nz, npcUuid, quest, questCounter);
	}

	public WandererData withNpc(Optional<String> uuid) {
		return new WandererData(phase, nextDay, x, y, z, uuid, quest, questCounter);
	}

	public WandererData withQuest(Optional<Quest> q) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, q, questCounter);
	}

	public WandererData withQuestCounter(long c) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, c);
	}
}
