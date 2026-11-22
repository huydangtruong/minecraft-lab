package io.github.huydangtruong.immortalcultivation.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * D198: the bookkeeping of Soul Grasp calls that wait for a far mob to load. One pending call per mob; each holds the
 * chunk (dimension id + chunk x/z) that was loaded for it and a deadline. Pure Java (no Minecraft imports), JUnit-tested;
 * the Minecraft glue (tickets, teleport) is in {@code technique.grasp.Grasp}.
 */
public final class GraspCalls {
	/** How long a call waits for the chunk and its entities to load (ticks, 5 s). */
	public static final int TIMEOUT_TICKS = 100;
	/** Radius of the temporary chunk ticket: the mob's own chunk and its neighbours load. */
	public static final int TICKET_RADIUS = 2;

	/** One waiting call. all = it came from Call all (the arrival spot is the mob's ring spot). */
	public record Pending(UUID mob, UUID owner, String dim, int chunkX, int chunkZ, boolean all, long deadline) {
		public boolean sameChunk(Pending o) {
			return dim.equals(o.dim) && chunkX == o.chunkX && chunkZ == o.chunkZ;
		}
	}

	private final Map<UUID, Pending> pending = new LinkedHashMap<>();

	/** Starts a call. False when this mob already has one waiting. */
	public boolean start(Pending p) {
		return pending.putIfAbsent(p.mob(), p) == null;
	}

	public boolean has(UUID mob) {
		return pending.containsKey(mob);
	}

	public int size() {
		return pending.size();
	}

	public List<Pending> all() {
		return new ArrayList<>(pending.values());
	}

	/** Ends the call of a mob (arrived, expired or cancelled). Returns it, or null. */
	public Pending finish(UUID mob) {
		return pending.remove(mob);
	}

	/** True when another waiting call still needs the chunk of this one (so its ticket must stay). */
	public boolean chunkStillNeeded(Pending p) {
		for (Pending o : pending.values()) {
			if (!o.mob().equals(p.mob()) && o.sameChunk(p)) {
				return true;
			}
		}
		return false;
	}

	/** The calls whose deadline has passed at this game time. */
	public List<Pending> expired(long now) {
		List<Pending> out = new ArrayList<>();
		for (Pending p : pending.values()) {
			if (now >= p.deadline()) {
				out.add(p);
			}
		}
		return out;
	}

	/** Every waiting call of a player. */
	public List<Pending> of(UUID owner) {
		List<Pending> out = new ArrayList<>();
		for (Pending p : pending.values()) {
			if (p.owner().equals(owner)) {
				out.add(p);
			}
		}
		return out;
	}

	/** How many chunk tickets each (dimension, chunk) needs now; handy for checks that none is left behind. */
	public Map<String, Integer> chunkCounts() {
		Map<String, Integer> out = new HashMap<>();
		for (Pending p : pending.values()) {
			out.merge(p.dim() + "@" + p.chunkX() + "," + p.chunkZ(), 1, Integer::sum);
		}
		return out;
	}

	public void clear() {
		pending.clear();
	}
}
