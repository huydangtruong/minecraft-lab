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

	private QuestGenerator() {}

	public static Quest generate(long id, RandomGenerator rng) {
		int n = 2 + rng.nextInt(3);
		List<QuestLine> lines = new ArrayList<>();
		Set<String> used = new HashSet<>();
		ElixirStat[] stats = ElixirStat.values();
		while (lines.size() < n) {
			QuestType type = rng.nextBoolean() ? QuestType.COLLECT : QuestType.KILL;
			List<Entry> pool = switch (type) {
				case COLLECT -> COLLECT;
				case KILL -> KILL;
			};
			Entry e = pool.get(rng.nextInt(pool.size()));
			if (!used.add(e.id())) {
				continue;
			}
			lines.add(new QuestLine(type, e.id(), e.count(), 0, stats[rng.nextInt(stats.length)].id(), false));
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
