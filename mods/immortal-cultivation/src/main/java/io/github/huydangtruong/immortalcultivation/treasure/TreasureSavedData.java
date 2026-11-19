package io.github.huydangtruong.immortalcultivation.treasure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Deadly Treasure state (P-059), saved with the overworld. Times are on Wanderer.clockTime. */
public class TreasureSavedData extends SavedData {
	private static final Codec<TreasureSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.optionalFieldOf("phase", "IDLE").forGetter(d -> d.phase.name()),
		Codec.LONG.optionalFieldOf("start_at", -1L).forGetter(d -> d.startAt),
		Codec.LONG.optionalFieldOf("end_at", -1L).forGetter(d -> d.endAt),
		Codec.LONG.optionalFieldOf("id", 0L).forGetter(d -> d.id),
		Codec.INT.optionalFieldOf("x", 0).forGetter(d -> d.x),
		Codec.INT.optionalFieldOf("z", 0).forGetter(d -> d.z),
		Codec.LONG.optionalFieldOf("chest", 0L).forGetter(d -> d.chest),
		Codec.BOOL.optionalFieldOf("placed", false).forGetter(d -> d.placed),
		Codec.STRING.optionalFieldOf("group", "").forGetter(d -> d.group),
		Codec.STRING.listOf().optionalFieldOf("guards", List.of()).forGetter(d -> d.guards),
		Codec.LONG.listOf().optionalFieldOf("cleanup", List.of()).forGetter(d -> d.cleanup)
	).apply(i, TreasureSavedData::new));

	public static final SavedDataType<TreasureSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "deadly_treasure"), TreasureSavedData::new, CODEC, null);

	public TreasureRules.Phase phase = TreasureRules.Phase.IDLE;
	public long startAt = -1;
	public long endAt = -1;
	/** Counts up with every event; guards carry it, so stray guards of an old event can be removed. */
	public long id;
	public int x;
	public int z;
	/** Packed BlockPos of the chest once placed. */
	public long chest;
	public boolean placed;
	public String group = "";
	/** UUIDs of the guards still alive. */
	public final List<String> guards = new ArrayList<>();
	/** Chests of failed events to remove when their chunk loads. */
	public final List<Long> cleanup = new ArrayList<>();

	public TreasureSavedData() {}

	private TreasureSavedData(String phase, long startAt, long endAt, long id, int x, int z, long chest, boolean placed, String group,
			List<String> guards, List<Long> cleanup) {
		try {
			this.phase = TreasureRules.Phase.valueOf(phase);
		} catch (IllegalArgumentException e) {
			this.phase = TreasureRules.Phase.IDLE;
		}
		this.startAt = startAt;
		this.endAt = endAt;
		this.id = id;
		this.x = x;
		this.z = z;
		this.chest = chest;
		this.placed = placed;
		this.group = group;
		this.guards.addAll(guards);
		this.cleanup.addAll(cleanup);
	}

	/** Back to IDLE (keeps the id counter and the cleanup list). */
	public void clear() {
		phase = TreasureRules.Phase.IDLE;
		startAt = -1;
		endAt = -1;
		placed = false;
		chest = 0;
		group = "";
		guards.clear();
		setDirty();
	}
}
