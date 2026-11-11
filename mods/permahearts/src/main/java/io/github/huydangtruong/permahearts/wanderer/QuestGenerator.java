package io.github.huydangtruong.permahearts.wanderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import io.github.huydangtruong.permahearts.ElixirStat;

public final class QuestGenerator {
	private record Entry(String id, int count) {}

	private static final List<Entry> COLLECT = List.of(
		new Entry("minecraft:iron_ingot", 64), new Entry("minecraft:gold_ingot", 32), new Entry("minecraft:diamond", 8),
		new Entry("minecraft:emerald", 8), new Entry("minecraft:coal", 64), new Entry("minecraft:redstone", 64),
		new Entry("minecraft:lapis_lazuli", 64), new Entry("minecraft:carrot", 64), new Entry("minecraft:potato", 64),
		new Entry("minecraft:wheat", 64), new Entry("minecraft:mutton", 32), new Entry("minecraft:beef", 32),
		new Entry("minecraft:oak_log", 64), new Entry("minecraft:bone", 32), new Entry("minecraft:string", 32),
		new Entry("minecraft:gunpowder", 16), new Entry("minecraft:ender_pearl", 8), new Entry("minecraft:leather", 32));

	private static final List<Entry> KILL = List.of(
		new Entry("minecraft:zombie", 16), new Entry("minecraft:skeleton", 16), new Entry("minecraft:spider", 16),
		new Entry("minecraft:creeper", 8), new Entry("minecraft:enderman", 8), new Entry("minecraft:witch", 4),
		new Entry("minecraft:drowned", 8));

	private record Weighted(String id, int weight) {}

	private static final List<Weighted> EXPLORE = List.of(
		new Weighted("#minecraft:village", 4), new Weighted("#minecraft:ruined_portal", 3),
		new Weighted("minecraft:pillager_outpost", 2), new Weighted("#minecraft:mineshaft", 2),
		new Weighted("minecraft:shipwreck", 2), new Weighted("minecraft:desert_pyramid", 1),
		new Weighted("minecraft:jungle_temple", 1), new Weighted("minecraft:swamp_hut", 1),
		new Weighted("minecraft:igloo", 1), new Weighted("minecraft:trail_ruins", 1),
		new Weighted("minecraft:trial_chambers", 1), new Weighted("minecraft:ocean_monument", 1),
		new Weighted("minecraft:ancient_city", 1), new Weighted("minecraft:fortress", 1),
		new Weighted("minecraft:bastion_remnant", 1));

	private static final List<Weighted> BOSS = List.of(new Weighted("minecraft:zombie", 1), new Weighted("minecraft:skeleton", 1));

	private QuestGenerator() {}

	private static QuestType pickType(RandomGenerator rng) {
		int r = rng.nextInt(30 + 30 + 15 + 12 + 13);
		if (r < 30) {
			return QuestType.COLLECT;
		} else if (r < 60) {
			return QuestType.KILL;
		} else if (r < 75) {
			return QuestType.EXPLORE;
		} else if (r < 87) {
			return QuestType.BLOOD_MOON;
		}
		return QuestType.BOSS;
	}

	private static String pickWeighted(List<Weighted> pool, RandomGenerator rng) {
		int total = 0;
		for (Weighted w : pool) {
			total += w.weight();
		}
		int r = rng.nextInt(total);
		for (Weighted w : pool) {
			r -= w.weight();
			if (r < 0) {
				return w.id();
			}
		}
		return pool.get(0).id();
	}

	public static Quest generate(long id, RandomGenerator rng) {
		int n = 2 + rng.nextInt(3);
		List<QuestLine> lines = new ArrayList<>();
		Set<String> used = new HashSet<>();
		ElixirStat[] stats = ElixirStat.values();
		boolean bossUsed = false;
		while (lines.size() < n) {
			QuestType type = pickType(rng);
			String target;
			int required = 1;
			switch (type) {
				case COLLECT -> {
					Entry e = COLLECT.get(rng.nextInt(COLLECT.size()));
					target = e.id();
					required = e.count();
				}
				case KILL -> {
					Entry e = KILL.get(rng.nextInt(KILL.size()));
					target = e.id();
					required = e.count();
				}
				case EXPLORE -> target = pickWeighted(EXPLORE, rng);
				case BOSS -> target = pickWeighted(BOSS, rng);
				default -> target = ""; // BLOOD_MOON
			}
			// No duplicate targets (so a boss zombie and a kill-zombie line never meet); the blood moon's "" and a
			// second boss are covered by the same set plus the flag.
			if (type == QuestType.BOSS && bossUsed) {
				continue;
			}
			if (!used.add(target)) {
				continue;
			}
			bossUsed |= type == QuestType.BOSS;
			lines.add(new QuestLine(type, target, required, 0, stats[rng.nextInt(stats.length)].id(), false));
		}
		return new Quest(id, List.copyOf(lines), false);
	}

	/** 3..5 days inclusive. */
	public static int daysUntilNext(RandomGenerator rng) {
		return 3 + rng.nextInt(3);
	}

	/** {dx, dz}: random direction, distance 900..1100. */
	public static double[] planOffset(RandomGenerator rng) {
		double angle = rng.nextDouble() * Math.PI * 2;
		double dist = 900 + rng.nextDouble() * 200;
		return new double[] {Math.cos(angle) * dist, Math.sin(angle) * dist};
	}
}
