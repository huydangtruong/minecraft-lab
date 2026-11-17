package io.github.huydangtruong.immortalcultivation.godslayer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-wide God Slayer state (overworld data folder): the boss level, the Crimson Master visit count and the hunt in
 * progress. Plain mutable fields; call {@link #setDirty()} after changing them. Times are overworld game ticks.
 */
public class GodSlayerSavedData extends SavedData {
	public enum Phase { IDLE, WARNING, HUNTING }

	private static final Codec<Phase> PHASE_CODEC = Codec.STRING.xmap(Phase::valueOf, Phase::name);

	private static final Codec<GodSlayerSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
		Codec.INT.optionalFieldOf("visit_count", 0).forGetter(d -> d.visitCount),
		PHASE_CODEC.optionalFieldOf("phase", Phase.IDLE).forGetter(d -> d.phase),
		Codec.LONG.optionalFieldOf("warning_end", 0L).forGetter(d -> d.warningEndTick),
		Codec.LONG.optionalFieldOf("hunt_end", 0L).forGetter(d -> d.huntEndTick),
		Codec.STRING.optionalFieldOf("boss", "").forGetter(d -> d.bossUuid),
		Codec.STRING.optionalFieldOf("target", "").forGetter(d -> d.currentTarget),
		Codec.STRING.listOf().optionalFieldOf("killed", List.of()).forGetter(d -> d.killed),
		Codec.STRING.optionalFieldOf("first_target_name", "").forGetter(d -> d.firstTargetName)
	).apply(i, GodSlayerSavedData::new));

	public static final SavedDataType<GodSlayerSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "god_slayer"), GodSlayerSavedData::new, CODEC, null);

	public int level = 1;
	public int visitCount;
	public Phase phase = Phase.IDLE;
	public long warningEndTick;
	public long huntEndTick;
	/** UUID strings; empty = none. */
	public String bossUuid = "";
	public String currentTarget = "";
	public List<String> killed = new ArrayList<>();
	public String firstTargetName = "";

	public GodSlayerSavedData() {}

	private GodSlayerSavedData(int level, int visitCount, Phase phase, long warningEnd, long huntEnd, String boss,
							   String target, List<String> killed, String firstName) {
		this.level = GodSlayerRules.clampLevel(level);
		this.visitCount = visitCount;
		this.phase = phase;
		this.warningEndTick = warningEnd;
		this.huntEndTick = huntEnd;
		this.bossUuid = boss;
		this.currentTarget = target;
		this.killed = new ArrayList<>(killed);
		this.firstTargetName = firstName;
	}

	/** Ends any hunt: back to IDLE with the hunt fields cleared. Level and visit count stay. */
	public void clearHunt() {
		phase = Phase.IDLE;
		warningEndTick = 0;
		huntEndTick = 0;
		bossUuid = "";
		currentTarget = "";
		killed = new ArrayList<>();
		firstTargetName = "";
		setDirty();
	}

	public static GodSlayerSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
