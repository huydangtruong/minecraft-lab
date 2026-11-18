package io.github.huydangtruong.immortalcultivation.danger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** P-054: the world danger level and the game tick at which the next +1 is due (0 = none pending). Old worlds: level 0. */
public class DangerSavedData extends SavedData {
	private static final Codec<DangerSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("level", 0).forGetter(d -> d.level),
		Codec.LONG.optionalFieldOf("pending_at", 0L).forGetter(d -> d.pendingAtTick)
	).apply(i, DangerSavedData::new));

	public static final SavedDataType<DangerSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "danger"), DangerSavedData::new, CODEC, null);

	public int level;
	public long pendingAtTick;

	public DangerSavedData() {}

	private DangerSavedData(int level, long pendingAtTick) {
		this.level = DangerRules.clamp(level);
		this.pendingAtTick = pendingAtTick;
	}

	public static DangerSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
