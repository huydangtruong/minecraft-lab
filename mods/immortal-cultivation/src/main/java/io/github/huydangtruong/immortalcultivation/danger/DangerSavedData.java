package io.github.huydangtruong.immortalcultivation.danger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * P-054: the world danger level and the game tick at which the next +1 is due (0 = none pending). Old worlds: level 0.
 * D206: format 2 = level on the 0..50 scale. A file without "format" (or 1) holds the old 0..30 scale and is converted
 * on load (30 becomes 50); a new world starts at the current format, so nothing is converted twice.
 */
public class DangerSavedData extends SavedData {
	private static final Codec<DangerSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("level", 0).forGetter(d -> d.level),
		Codec.LONG.optionalFieldOf("pending_at", 0L).forGetter(d -> d.pendingAtTick),
		Codec.INT.optionalFieldOf("format", 1).forGetter(d -> DangerSavedData.FORMAT)
	).apply(i, DangerSavedData::load));

	/** Current stored format (D206): level on the 0..50 scale. */
	public static final int FORMAT = 2;

	public static final SavedDataType<DangerSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "danger"), DangerSavedData::new, CODEC, null);

	public int level;
	public long pendingAtTick;

	public DangerSavedData() {}

	private DangerSavedData(int level, long pendingAtTick) {
		this.level = DangerRules.clamp(level);
		this.pendingAtTick = pendingAtTick;
	}

	/** Reads stored data: a format below 2 holds the old 0..30 scale. Public with plain values for tests. */
	public static DangerSavedData load(int level, long pendingAtTick, int format) {
		return new DangerSavedData(format < FORMAT ? DangerRules.fromOldScale(level) : level, pendingAtTick);
	}

	public static DangerSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
