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
 * Crystal Prison cages that still stand (D143), so the crystal is removed on time even after a restart. Times are overworld
 * game ticks. Positions are packed BlockPos longs.
 */
public class CrystalSavedData extends SavedData {
	/** One cage: its dimension, when it ends, the crystal blocks placed and the interior box (min and max corner). */
	public static final class Entry {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(e -> e.dimension),
			Codec.LONG.fieldOf("expiry").forGetter(e -> e.expiry),
			Codec.LONG.listOf().fieldOf("blocks").forGetter(e -> e.blocks),
			Codec.LONG.fieldOf("min").forGetter(e -> e.min),
			Codec.LONG.fieldOf("max").forGetter(e -> e.max)
		).apply(i, Entry::new));

		public final String dimension;
		public final long expiry;
		/** Crystal blocks still to remove. */
		public List<Long> blocks;
		public final long min;
		public final long max;

		public Entry(String dimension, long expiry, List<Long> blocks, long min, long max) {
			this.dimension = dimension;
			this.expiry = expiry;
			this.blocks = new ArrayList<>(blocks);
			this.min = min;
			this.max = max;
		}
	}

	private static final Codec<CrystalSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Entry.CODEC.listOf().optionalFieldOf("cages", List.of()).forGetter(d -> d.cages)
	).apply(i, CrystalSavedData::new));

	public static final SavedDataType<CrystalSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "crystal_prison"), CrystalSavedData::new, CODEC, null);

	public final List<Entry> cages = new ArrayList<>();

	public CrystalSavedData() {}

	private CrystalSavedData(List<Entry> cages) {
		this.cages.addAll(cages);
	}

	public static CrystalSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
