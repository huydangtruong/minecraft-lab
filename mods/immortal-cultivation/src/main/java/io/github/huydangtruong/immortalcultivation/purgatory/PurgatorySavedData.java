package io.github.huydangtruong.immortalcultivation.purgatory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Purgatory state (D234-D240), saved with the overworld: floors, cooldowns, running boxes, return points, owed rewards. */
public class PurgatorySavedData extends SavedData {
	/** One player's solo progress. floor = the next floor to fight. */
	public static final class Solo {
		static final Codec<Solo> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("floor", 1).forGetter(s -> s.floor),
			Codec.INT.optionalFieldOf("cooldown", 0).forGetter(s -> s.cooldown)
		).apply(i, Solo::new));

		public int floor;
		/** Ticks until the next floor opens; counts only while the player is online and not fighting. */
		public int cooldown;

		public Solo(int floor, int cooldown) {
			this.floor = Math.max(1, floor);
			this.cooldown = Math.max(0, cooldown);
		}
	}

	/** One running box. */
	public static final class Run {
		static final Codec<Run> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("id").forGetter(r -> r.id),
			Codec.INT.fieldOf("slot").forGetter(r -> r.slot),
			Codec.BOOL.optionalFieldOf("team", false).forGetter(r -> r.team),
			Codec.INT.fieldOf("floor").forGetter(r -> r.floor),
			Codec.INT.fieldOf("ticks_left").forGetter(r -> r.ticksLeft),
			Codec.INT.optionalFieldOf("spawn_timer", 0).forGetter(r -> r.spawnTimer),
			Codec.STRING.listOf().fieldOf("players").forGetter(r -> r.players),
			Codec.STRING.listOf().optionalFieldOf("dead", List.of()).forGetter(r -> r.dead),
			Codec.LONG.optionalFieldOf("seed", 0L).forGetter(r -> r.seed),
			Codec.INT.listOf().optionalFieldOf("rooms", List.of()).forGetter(r -> r.rooms),
			Codec.INT.listOf().optionalFieldOf("room_ticks", List.of()).forGetter(r -> r.roomTicks),
			Codec.INT.optionalFieldOf("boss", 0).forGetter(r -> r.boss),
			Codec.INT.optionalFieldOf("victory_ticks", 0).forGetter(r -> r.victoryTicks)
		).apply(i, Run::new));

		/** Room states (P-082). */
		public static final int IDLE = 0;
		public static final int SEALED = 1;
		public static final int DONE = 2;
		/** Sealed, the second and last wave (D259). */
		public static final int SEALED_LAST = 3;
		/** Boss states. */
		public static final int BOSS_WAITING = 0;
		public static final int BOSS_ALIVE = 1;
		public static final int BOSS_DEAD = 2;

		public final int id;
		public final int slot;
		public final boolean team;
		public final int floor;
		public int ticksLeft;
		/** Ticks until the boss calls its next adds. */
		public int spawnTimer;
		public final List<String> players = new ArrayList<>();
		/** Team players who died: they watch inside the dungeon as spectators. */
		public final List<String> dead = new ArrayList<>();
		/** Dungeon seed; 0 = a run saved before P-082 (it ends as a loss that costs nothing). */
		public final long seed;
		/** State of each room by index ({@link #IDLE}, {@link #SEALED}, {@link #DONE}). */
		public final List<Integer> rooms = new ArrayList<>();
		/** Ticks each room has been sealed. */
		public final List<Integer> roomTicks = new ArrayList<>();
		public int boss;
		/** D260: ticks left before the winners go back; above 0 the floor is already won and paid. */
		public int victoryTicks;

		public Run(int id, int slot, boolean team, int floor, int ticksLeft, int spawnTimer, List<String> players, List<String> dead,
				long seed, List<Integer> rooms, List<Integer> roomTicks, int boss, int victoryTicks) {
			this.id = id;
			this.slot = slot;
			this.team = team;
			this.floor = floor;
			this.ticksLeft = ticksLeft;
			this.spawnTimer = spawnTimer;
			this.players.addAll(players);
			this.dead.addAll(dead);
			this.seed = seed;
			this.rooms.addAll(rooms);
			this.roomTicks.addAll(roomTicks);
			this.boss = boss;
			this.victoryTicks = victoryTicks;
		}

		public int room(int index) {
			return index < rooms.size() ? rooms.get(index) : IDLE;
		}

		public void room(int index, int state) {
			while (rooms.size() <= index) {
				rooms.add(IDLE);
			}
			rooms.set(index, state);
		}

		/** Adds one tick to a sealed room's clock and returns it. */
		public int tickRoom(int index) {
			while (roomTicks.size() <= index) {
				roomTicks.add(0);
			}
			int t = roomTicks.get(index) + 1;
			roomTicks.set(index, t);
			return t;
		}

		/** Restarts a sealed room's clock (a new wave). */
		public void resetRoom(int index) {
			if (index < roomTicks.size()) {
				roomTicks.set(index, 0);
			}
		}

		/** The boss is dead and the winners are looking around (D260). */
		public boolean won() {
			return victoryTicks > 0;
		}

		public boolean alive(String uuid) {
			return players.contains(uuid) && !dead.contains(uuid);
		}
	}

	/** Where a fighter or watcher goes back to, and the game mode they had. */
	public record Back(String dimension, double x, double y, double z, float yRot, float xRot, String gameMode) {
		static final Codec<Back> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Back::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(Back::x),
			Codec.DOUBLE.fieldOf("y").forGetter(Back::y),
			Codec.DOUBLE.fieldOf("z").forGetter(Back::z),
			Codec.FLOAT.optionalFieldOf("y_rot", 0f).forGetter(Back::yRot),
			Codec.FLOAT.optionalFieldOf("x_rot", 0f).forGetter(Back::xRot),
			Codec.STRING.optionalFieldOf("game_mode", "survival").forGetter(Back::gameMode)
		).apply(i, Back::new));
	}

	/** A reward won while the player was offline: given when they join. */
	public record Owed(int levels, int runes) {
		static final Codec<Owed> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("levels").forGetter(Owed::levels),
			Codec.INT.fieldOf("runes").forGetter(Owed::runes)
		).apply(i, Owed::new));
	}

	private static final Codec<PurgatorySavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Solo.CODEC).optionalFieldOf("solo", Map.of()).forGetter(d -> d.solo),
		Codec.INT.optionalFieldOf("team_floor", 1).forGetter(d -> d.teamFloor),
		Codec.INT.optionalFieldOf("team_cooldown", 0).forGetter(d -> d.teamCooldown),
		Run.CODEC.listOf().optionalFieldOf("runs", List.of()).forGetter(d -> d.runs),
		Codec.unboundedMap(Codec.STRING, Back.CODEC).optionalFieldOf("back", Map.of()).forGetter(d -> d.back),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("watching", Map.of()).forGetter(d -> d.watching),
		Codec.unboundedMap(Codec.STRING, Owed.CODEC.listOf()).optionalFieldOf("owed", Map.of()).forGetter(d -> d.owed),
		Codec.INT.optionalFieldOf("next_id", 1).forGetter(d -> d.nextId)
	).apply(i, PurgatorySavedData::new));

	public static final SavedDataType<PurgatorySavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "purgatory"), PurgatorySavedData::new, CODEC, null);

	public final Map<String, Solo> solo = new HashMap<>();
	public int teamFloor = 1;
	public int teamCooldown;
	public final List<Run> runs = new ArrayList<>();
	/** uuid -> where to send them back (fighters, dead team members and watchers). */
	public final Map<String, Back> back = new HashMap<>();
	/** watcher uuid -> run id. */
	public final Map<String, Integer> watching = new HashMap<>();
	public final Map<String, List<Owed>> owed = new HashMap<>();
	public int nextId = 1;

	public PurgatorySavedData() {}

	private PurgatorySavedData(Map<String, Solo> solo, int teamFloor, int teamCooldown, List<Run> runs, Map<String, Back> back,
			Map<String, Integer> watching, Map<String, List<Owed>> owed, int nextId) {
		this.solo.putAll(solo);
		this.teamFloor = Math.max(1, teamFloor);
		this.teamCooldown = Math.max(0, teamCooldown);
		this.runs.addAll(runs);
		this.back.putAll(back);
		this.watching.putAll(watching);
		owed.forEach((k, v) -> this.owed.put(k, new ArrayList<>(v)));
		this.nextId = nextId;
	}

	public Solo solo(String uuid) {
		return solo.computeIfAbsent(uuid, k -> new Solo(1, 0));
	}

	/** The run this player fights in (alive or dead), or null. */
	public Run runOf(String uuid) {
		for (Run r : runs) {
			if (r.players.contains(uuid)) {
				return r;
			}
		}
		return null;
	}

	public Run run(int id) {
		for (Run r : runs) {
			if (r.id == id) {
				return r;
			}
		}
		return null;
	}

	/** Lowest slot no running box uses. */
	public int freeSlot() {
		for (int s = 0;; s++) {
			boolean used = false;
			for (Run r : runs) {
				used |= r.slot == s;
			}
			if (!used) {
				return s;
			}
		}
	}

	public void changed() {
		setDirty();
	}
}
