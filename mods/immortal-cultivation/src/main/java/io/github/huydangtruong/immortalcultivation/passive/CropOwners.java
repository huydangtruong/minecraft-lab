package io.github.huydangtruong.immortalcultivation.passive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Crop growth (P-058), saved per dimension: who planted the crop at each position (packed BlockPos). The overworld copy
 * also remembers each owner's Crop growth level in use, so crops keep growing faster while the owner is offline.
 */
public class CropOwners extends SavedData {
	private record Planted(long pos, UUID owner) {
		static final Codec<Planted> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("pos").forGetter(Planted::pos),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Planted::owner)
		).apply(i, Planted::new));
	}

	private static final Codec<CropOwners> CODEC = RecordCodecBuilder.create(i -> i.group(
		Planted.CODEC.listOf().optionalFieldOf("crops", List.of()).forGetter(CropOwners::planted),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(d -> d.levelsById())
	).apply(i, CropOwners::new));

	public static final SavedDataType<CropOwners> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "crop_owners"), CropOwners::new, CODEC, null);

	private final Map<Long, UUID> owners = new HashMap<>();
	private final Map<UUID, Integer> levels = new HashMap<>();

	public CropOwners() {}

	private CropOwners(List<Planted> crops, Map<String, Integer> lv) {
		for (Planted p : crops) {
			owners.put(p.pos(), p.owner());
		}
		lv.forEach((k, v) -> {
			try {
				levels.put(UUID.fromString(k), v);
			} catch (IllegalArgumentException ignored) {
				// a bad key is dropped
			}
		});
	}

	private List<Planted> planted() {
		List<Planted> out = new ArrayList<>();
		owners.forEach((k, v) -> out.add(new Planted(k, v)));
		return out;
	}

	private Map<String, Integer> levelsById() {
		Map<String, Integer> out = new HashMap<>();
		levels.forEach((k, v) -> out.put(k.toString(), v));
		return out;
	}

	public UUID owner(long pos) {
		return owners.get(pos);
	}

	public void put(long pos, UUID owner) {
		if (!owner.equals(owners.put(pos, owner))) {
			setDirty();
		}
	}

	public void remove(long pos) {
		if (owners.remove(pos) != null) {
			setDirty();
		}
	}

	public int size() {
		return owners.size();
	}

	/** Last known Crop growth level in use (0 = off or never seen). */
	public int level(UUID owner) {
		return levels.getOrDefault(owner, 0);
	}

	public void setLevel(UUID owner, int level) {
		Integer old = level == 0 ? levels.remove(owner) : levels.put(owner, level);
		if (old == null ? level != 0 : old != level) {
			setDirty();
		}
	}
}
