package io.github.huydangtruong.immortalcultivation.technique;

import java.util.List;

/** Crystal Mayhem numbers and pure helpers (D174). No Minecraft imports. */
public final class MayhemRules {
	public static final int SHARDS = 20;
	public static final int CLUSTER_TICKS = 200; // a cluster left by a shard lives 10 s
	public static final int WINDOW_TICKS = 400; // 20 s after the channel
	public static final int LINK = 5; // blocks: player to budding, budding to budding
	public static final double BASE_DAMAGE = 2;
	public static final int NIGHT_FROM = 16000; // day time [16000, 23000): no sky light bonus
	public static final int NIGHT_TO = 23000;
	public static final int CONVERT_CHECK_TICKS = 5;

	private MayhemRules() {}

	private static int lv(int level) {
		return Math.max(1, Math.min(TechniqueRules.MAX_LEVEL, level));
	}

	/** Sky light a shard counts: the real (darkened) sky light, but 0 from 22h to 5h. */
	public static int skyLevel(int rawSky, int skyDarken, long dayTime) {
		long t = Math.floorMod(dayTime, 24000L);
		if (t >= NIGHT_FROM && t < NIGHT_TO) {
			return 0;
		}
		return Math.max(0, rawSky - skyDarken);
	}

	/** Light bonus: the stronger of sky and block light wins (a tie goes to the sky); sky gives 1 per 3 levels, block 1 per 5. */
	public static int lightExtra(int sky, int block) {
		return sky >= block ? sky / 3 : block / 5;
	}

	/** Damage of one shard: 2 plus the light bonus, which grows 5% per level. */
	public static double shardDamage(int level, int sky, int block) {
		return BASE_DAMAGE + lightExtra(sky, block) * (1 + 0.05 * (lv(level) - 1));
	}

	/** Seconds between two shards. */
	public static double shardDelaySeconds(int level) {
		return Math.max(0.1, 1 - 0.05 * (lv(level) - 1));
	}

	public static int shardDelayTicks(int level) {
		return (int) Math.round(shardDelaySeconds(level) * 20);
	}

	public static int maxBuddings(int level) {
		return 4 + lv(level);
	}

	public static double burnDamage(int level) {
		return 1 + 0.5 * (lv(level) - 1);
	}

	public static int burnRadius(int level) {
		return 3 + (lv(level) - 1) / 2;
	}

	/** A spot on the grid (block centre coordinates are x+0.5 etc., the caller passes block centres). */
	public record P(double x, double y, double z) {
		double dist(P o) {
			double dx = x - o.x, dy = y - o.y, dz = z - o.z;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
	}

	/**
	 * Index of the next cluster to turn into a budding: the nearest one within LINK blocks of the player; if none, the nearest
	 * within LINK blocks of any budding. -1 when nothing qualifies. Ties go to the lower index.
	 */
	public static int pickNext(P player, List<P> buddings, List<P> candidates) {
		int best = -1;
		double bestDist = Double.MAX_VALUE;
		for (int i = 0; i < candidates.size(); i++) {
			double d = candidates.get(i).dist(player);
			if (d <= LINK && d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		if (best >= 0) {
			return best;
		}
		for (int i = 0; i < candidates.size(); i++) {
			double d = Double.MAX_VALUE;
			for (P b : buddings) {
				d = Math.min(d, candidates.get(i).dist(b));
			}
			if (d <= LINK && d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		return best;
	}

	/** Which buddings are reachable from the player through links of at most LINK blocks (player, budding, budding). */
	public static boolean[] active(P player, List<P> buddings) {
		boolean[] on = new boolean[buddings.size()];
		java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
		for (int i = 0; i < on.length; i++) {
			if (buddings.get(i).dist(player) <= LINK) {
				on[i] = true;
				queue.add(i);
			}
		}
		while (!queue.isEmpty()) {
			P from = buddings.get(queue.poll());
			for (int i = 0; i < on.length; i++) {
				if (!on[i] && buddings.get(i).dist(from) <= LINK) {
					on[i] = true;
					queue.add(i);
				}
			}
		}
		return on;
	}
}
