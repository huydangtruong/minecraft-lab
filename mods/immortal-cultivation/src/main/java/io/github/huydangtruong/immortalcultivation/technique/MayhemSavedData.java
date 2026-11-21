package io.github.huydangtruong.immortalcultivation.technique;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Crystal Mayhem blocks that still stand (D174), so none stays behind after a restart. Times are overworld game ticks.
 * Positions are packed BlockPos longs.
 */
public class MayhemSavedData extends SavedData {
	public static final int CLUSTER = 0;
	public static final int BUDDING = 1;
	public static final int DECOR = 2;

	/** One placed block: whose it is, where, what kind, and (clusters only) when it goes. */
	public static final class Entry {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("owner").forGetter(e -> e.owner),
			Codec.STRING.fieldOf("dimension").forGetter(e -> e.dimension),
			Codec.LONG.fieldOf("pos").forGetter(e -> e.pos),
			Codec.INT.fieldOf("kind").forGetter(e -> e.kind),
			Codec.LONG.fieldOf("expiry").forGetter(e -> e.expiry)
		).apply(i, Entry::new));

		public final String owner;
		public final String dimension;
		public final long pos;
		public int kind;
		public long expiry;

		public Entry(String owner, String dimension, long pos, int kind, long expiry) {
			this.owner = owner;
			this.dimension = dimension;
			this.pos = pos;
			this.kind = kind;
			this.expiry = expiry;
		}
	}

	private static final Codec<MayhemSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Entry.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(d -> d.blocks)
	).apply(i, MayhemSavedData::new));

	public static final SavedDataType<MayhemSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "crystal_mayhem"), MayhemSavedData::new, CODEC, null);

	public final List<Entry> blocks = new ArrayList<>();

	public MayhemSavedData() {}

	private MayhemSavedData(List<Entry> blocks) {
		this.blocks.addAll(blocks);
	}

	public static MayhemSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
