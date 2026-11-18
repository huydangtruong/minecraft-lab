package io.github.huydangtruong.immortalcultivation.technique.grasp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every player's Soul Grasp record (D134): the held mobs, the per-type counters and the technique level, kept in the
 * overworld data folder so mobs can be added, removed and updated while their owner is offline (a deliberate step away from
 * a player attachment). Entries keep the last known numbers so the list shows unloaded mobs too.
 */
public class GraspData extends SavedData {
	/** One held mob. dim is the dimension id, x/y/z the last known position, the rest the last known stats. */
	public record Entry(UUID mob, String type, int number, int mode, String dim, double x, double y, double z,
			float hp, float maxHp, float bonus, float armor, float damage, String armorGear, String weaponGear) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("mob").forGetter(Entry::mob),
			Codec.STRING.optionalFieldOf("type", "minecraft:zombie").forGetter(Entry::type),
			Codec.INT.optionalFieldOf("number", 1).forGetter(Entry::number),
			Codec.INT.optionalFieldOf("mode", 0).forGetter(Entry::mode),
			Codec.STRING.optionalFieldOf("dim", "minecraft:overworld").forGetter(Entry::dim),
			Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Entry::x),
			Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(Entry::y),
			Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(Entry::z),
			Codec.FLOAT.optionalFieldOf("hp", 20f).forGetter(Entry::hp),
			Codec.FLOAT.optionalFieldOf("max_hp", 20f).forGetter(Entry::maxHp),
			Codec.FLOAT.optionalFieldOf("bonus", 0f).forGetter(Entry::bonus),
			Codec.FLOAT.optionalFieldOf("armor", 0f).forGetter(Entry::armor),
			Codec.FLOAT.optionalFieldOf("damage", 0f).forGetter(Entry::damage),
			Codec.STRING.optionalFieldOf("armor_gear", "").forGetter(Entry::armorGear),
			Codec.STRING.optionalFieldOf("weapon_gear", "").forGetter(Entry::weaponGear)
		).apply(i, Entry::new));

		public Entry withMode(int m) {
			return new Entry(mob, type, number, m, dim, x, y, z, hp, maxHp, bonus, armor, damage, armorGear, weaponGear);
		}
	}

	/** A player's record: level of the technique (kept fresh while online), counters per entity type, held mobs. */
	public record Rec(int level, Map<String, Integer> counters, List<Entry> mobs) {
		public static final Codec<Rec> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("level", 0).forGetter(Rec::level),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(Rec::counters),
			Entry.CODEC.listOf().optionalFieldOf("mobs", List.of()).forGetter(Rec::mobs)
		).apply(i, Rec::new));

		public static final Rec EMPTY = new Rec(0, Map.of(), List.of());
	}

	private static final Codec<Map<String, Rec>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Rec.CODEC);
	private static final Codec<GraspData> CODEC = MAP_CODEC.xmap(GraspData::new, GraspData::asStrings);

	public static final SavedDataType<GraspData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "grasp"), GraspData::new, CODEC, null);

	private final Map<UUID, Rec> players = new HashMap<>();

	public GraspData() {}

	private GraspData(Map<String, Rec> loaded) {
		for (var e : loaded.entrySet()) {
			try {
				players.put(UUID.fromString(e.getKey()), e.getValue());
			} catch (IllegalArgumentException ignored) {
				// a damaged key: skip it
			}
		}
	}

	private Map<String, Rec> asStrings() {
		Map<String, Rec> out = new HashMap<>();
		for (var e : players.entrySet()) {
			out.put(e.getKey().toString(), e.getValue());
		}
		return out;
	}

	public static GraspData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public Rec get(UUID owner) {
		return players.getOrDefault(owner, Rec.EMPTY);
	}

	public void put(UUID owner, Rec rec) {
		players.put(owner, rec);
		setDirty();
	}

	public Iterable<Map.Entry<UUID, Rec>> all() {
		return new ArrayList<>(players.entrySet());
	}

	public int held(UUID owner) {
		return get(owner).mobs().size();
	}

	public Entry find(UUID owner, UUID mob) {
		for (Entry e : get(owner).mobs()) {
			if (e.mob().equals(mob)) {
				return e;
			}
		}
		return null;
	}

	/** Adds an entry (counter already bumped by the caller through {@link #nextNumber}). */
	public void add(UUID owner, Entry entry) {
		Rec r = get(owner);
		List<Entry> list = new ArrayList<>(r.mobs());
		list.add(entry);
		put(owner, new Rec(r.level(), r.counters(), list));
	}

	/** Replaces the entry of the same mob. */
	public void replace(UUID owner, Entry entry) {
		Rec r = get(owner);
		List<Entry> list = new ArrayList<>(r.mobs());
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).mob().equals(entry.mob())) {
				list.set(i, entry);
				put(owner, new Rec(r.level(), r.counters(), list));
				return;
			}
		}
	}

	/** Removes the entry of a mob. Returns it, or null when it was not held. */
	public Entry remove(UUID owner, UUID mob) {
		Rec r = get(owner);
		Entry found = null;
		List<Entry> list = new ArrayList<>();
		for (Entry e : r.mobs()) {
			if (e.mob().equals(mob)) {
				found = e;
			} else {
				list.add(e);
			}
		}
		if (found != null) {
			put(owner, new Rec(r.level(), r.counters(), list));
		}
		return found;
	}

	public void setLevel(UUID owner, int level) {
		Rec r = get(owner);
		if (r.level() != level) {
			put(owner, new Rec(level, r.counters(), r.mobs()));
		}
	}

	/** The next number of an entity type for this player (never reused): 1, 2, 3 ... */
	public int nextNumber(UUID owner, String type) {
		Rec r = get(owner);
		Map<String, Integer> c = new HashMap<>(r.counters());
		int n = c.getOrDefault(type, 0) + 1;
		c.put(type, n);
		put(owner, new Rec(r.level(), c, r.mobs()));
		return n;
	}
}
