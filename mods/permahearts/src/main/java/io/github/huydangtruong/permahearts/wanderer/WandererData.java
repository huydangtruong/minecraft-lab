package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * leaveAt: overworld clock time when this visit ends (-1 = not set yet, e.g. a 0.7.0 save). warned: the
 * "leaving soon" chat line was already sent for this visit.
 */
public record WandererData(Phase phase, long nextDay, int x, int y, int z, Optional<String> npcUuid,
						   Optional<Quest> quest, long questCounter,
						   Optional<BloodMoonState> bloodMoon, Optional<BossState> boss,
						   long leaveAt, boolean warned, Optional<DeathMatchState> deathMatch) {
	public static final WandererData DEFAULT = new WandererData(Phase.WAITING, -1, 0, 0, 0, Optional.empty(), Optional.empty(), 0,
		Optional.empty(), Optional.empty(), -1L, false, Optional.empty());

	private static final Codec<Phase> PHASE_CODEC = Codec.STRING.xmap(Phase::valueOf, Phase::name);

	public static final Codec<WandererData> CODEC = RecordCodecBuilder.create(i -> i.group(
		PHASE_CODEC.optionalFieldOf("phase", Phase.WAITING).forGetter(WandererData::phase),
		Codec.LONG.optionalFieldOf("next_day", -1L).forGetter(WandererData::nextDay),
		Codec.INT.optionalFieldOf("x", 0).forGetter(WandererData::x),
		Codec.INT.optionalFieldOf("y", 0).forGetter(WandererData::y),
		Codec.INT.optionalFieldOf("z", 0).forGetter(WandererData::z),
		Codec.STRING.optionalFieldOf("npc_uuid").forGetter(WandererData::npcUuid),
		Quest.CODEC.optionalFieldOf("quest").forGetter(WandererData::quest),
		Codec.LONG.optionalFieldOf("quest_counter", 0L).forGetter(WandererData::questCounter),
		BloodMoonState.CODEC.optionalFieldOf("blood_moon").forGetter(WandererData::bloodMoon),
		BossState.CODEC.optionalFieldOf("boss").forGetter(WandererData::boss),
		Codec.LONG.optionalFieldOf("leave_at", -1L).forGetter(WandererData::leaveAt),
		Codec.BOOL.optionalFieldOf("leave_warned", false).forGetter(WandererData::warned),
		DeathMatchState.CODEC.optionalFieldOf("death_match").forGetter(WandererData::deathMatch)
	).apply(i, WandererData::new));

	public WandererData withPhase(Phase p) {
		return new WandererData(p, nextDay, x, y, z, npcUuid, quest, questCounter, bloodMoon, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withNextDay(long d) {
		return new WandererData(phase, d, x, y, z, npcUuid, quest, questCounter, bloodMoon, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withPos(int nx, int ny, int nz) {
		return new WandererData(phase, nextDay, nx, ny, nz, npcUuid, quest, questCounter, bloodMoon, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withNpc(Optional<String> uuid) {
		return new WandererData(phase, nextDay, x, y, z, uuid, quest, questCounter, bloodMoon, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withQuest(Optional<Quest> q) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, q, questCounter, bloodMoon, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withQuestCounter(long c) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, c, bloodMoon, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withBloodMoon(Optional<BloodMoonState> s) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, questCounter, s, boss, leaveAt, warned, deathMatch);
	}

	public WandererData withBoss(Optional<BossState> s) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, questCounter, bloodMoon, s, leaveAt, warned, deathMatch);
	}

	public WandererData withLeaveAt(long t) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, questCounter, bloodMoon, boss, t, warned, deathMatch);
	}

	public WandererData withWarned(boolean w) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, questCounter, bloodMoon, boss, leaveAt, w, deathMatch);
	}

	public WandererData withDeathMatch(Optional<DeathMatchState> s) {
		return new WandererData(phase, nextDay, x, y, z, npcUuid, quest, questCounter, bloodMoon, boss, leaveAt, warned, s);
	}
}
