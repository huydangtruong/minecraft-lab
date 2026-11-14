package io.github.huydangtruong.permahearts.wanderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import io.github.huydangtruong.permahearts.ElixirStat;

public final class QuestGenerator {
	private record Entry(String id, int count) {}

	// Amounts set by Huy (2026-11-12).
	private static final List<Entry> COLLECT = List.of(
		new Entry("minecraft:iron_ingot", 64), new Entry("minecraft:coal", 64), new Entry("minecraft:redstone", 64),
		new Entry("minecraft:lapis_lazuli", 64), new Entry("minecraft:carrot", 64), new Entry("minecraft:potato", 64),
		new Entry("minecraft:wheat", 64), new Entry("minecraft:oak_log", 64),
		new Entry("minecraft:gold_ingot", 48), new Entry("minecraft:mutton", 48), new Entry("minecraft:beef", 48),
		new Entry("minecraft:bone", 48), new Entry("minecraft:string", 48), new Entry("minecraft:leather", 48),
		new Entry("minecraft:gunpowder", 32), new Entry("minecraft:ender_pearl", 32),
		new Entry("minecraft:diamond", 16), new Entry("minecraft:emerald", 16));

	private static final List<Entry> KILL = List.of(
		new Entry("minecraft:zombie", 16), new Entry("minecraft:skeleton", 16), new Entry("minecraft:spider", 16),
		new Entry("minecraft:creeper", 16), new Entry("minecraft:enderman", 16), new Entry("minecraft:drowned", 16),
		new Entry("minecraft:witch", 8));

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

	private static final List<Weighted> BOSS = List.of(new Weighted("minecraft:zombie", 1), new Weighted("minecraft:skeleton", 1),
		new Weighted("minecraft:evoker", 1), new Weighted("minecraft:blaze", 1), new Weighted("minecraft:vindicator", 1),
		new Weighted("minecraft:ghast", 1));

	/** Blood Moon variants: the mob that comes in waves. */
	public static final List<String> BLOOD_MOON = List.of("minecraft:zombie", "minecraft:piglin_brute", "minecraft:creeper",
		"minecraft:skeleton", "minecraft:spider", QuestGenerator.HORSEMAN);

	/** Blood Moon target for zombie horsemen: a zombie with an iron spear riding a zombie horse (D29). */
	public static final String HORSEMAN = "minecraft:zombie_horse";

	/**
	 * Ids a line takes for the no-duplicate rule. Horsemen are zombies on zombie horses, so they also take the zombie:
	 * no kill-zombie line or zombie boss in the same quest (their riders would count / mix with the minions).
	 */
	public static List<String> claims(QuestType type, String target) {
		return type == QuestType.BLOOD_MOON && HORSEMAN.equals(target) ? List.of(HORSEMAN, "minecraft:zombie") : List.of(target);
	}

	/** The Blood Moon mob of a line target; quests saved before 0.6.0's variants have "" and mean zombies. */
	public static String bloodMoonMob(String target) {
		return target == null || target.isEmpty() ? BLOOD_MOON.get(0) : target;
	}

	private QuestGenerator() {}

	/** Weight of a Death Match line (same as a Blood Moon line); only in the draw when 2+ players are online. */
	private static final int DEATH_MATCH_WEIGHT = 12;

	private static QuestType pickType(RandomGenerator rng, boolean offerDeathMatch) {
		int r = rng.nextInt(30 + 30 + 15 + 12 + 13 + (offerDeathMatch ? DEATH_MATCH_WEIGHT : 0));
		if (r < 30) {
			return QuestType.COLLECT;
		} else if (r < 60) {
			return QuestType.KILL;
		} else if (r < 75) {
			return QuestType.EXPLORE;
		} else if (r < 87) {
			return QuestType.BLOOD_MOON;
		}
		return r < 100 ? QuestType.BOSS : QuestType.DEATH_MATCH;
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

	/** Same as generate(id, rng, 1): no Death Match line. */
	public static Quest generate(long id, RandomGenerator rng) {
		return generate(id, rng, 1);
	}

	/** online = players online now; a Death Match line is only possible with 2 or more. */
	public static Quest generate(long id, RandomGenerator rng, int online) {
		boolean offerDeathMatch = DeathMatchRules.canOffer(online);
		int n = 2 + rng.nextInt(3);
		List<QuestLine> lines = new ArrayList<>();
		Set<String> used = new HashSet<>();
		ElixirStat[] stats = ElixirStat.values();
		boolean bossUsed = false;
		boolean moonUsed = false;
		while (lines.size() < n) {
			QuestType type = pickType(rng, offerDeathMatch);
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
				case BLOOD_MOON -> target = BLOOD_MOON.get(rng.nextInt(BLOOD_MOON.size()));
				case DEATH_MATCH -> target = DeathMatchRules.TARGET;
				default -> throw new IllegalStateException();
			}
			// No duplicate targets (so a zombie boss, a zombie Blood Moon and a kill-zombie line never meet, and
			// event mobs never count towards a kill line); at most one boss and one Blood Moon per quest.
			if ((type == QuestType.BOSS && bossUsed) || (type == QuestType.BLOOD_MOON && moonUsed)) {
				continue;
			}
			List<String> claims = claims(type, target);
			if (claims.stream().anyMatch(used::contains)) {
				continue;
			}
			used.addAll(claims);
			bossUsed |= type == QuestType.BOSS;
			moonUsed |= type == QuestType.BLOOD_MOON;
			lines.add(new QuestLine(type, target, required, 0, stats[rng.nextInt(stats.length)].id(), false));
		}
		return new Quest(id, List.copyOf(lines), false);
	}

	/** Always 4 days (D32); rng kept so callers and saves stay the same. */
	public static int daysUntilNext(RandomGenerator rng) {
		return 4;
	}

	/** 4 days (D32): the wait after a Wanderer left (time up, Give up or all lines claimed). */
	public static int daysUntilReturn(RandomGenerator rng) {
		return 4;
	}

	/** {dx, dz}: random direction, distance 450..550 (D27). */
	public static double[] planOffset(RandomGenerator rng) {
		double angle = rng.nextDouble() * Math.PI * 2;
		double dist = 450 + rng.nextDouble() * 100;
		return new double[] {Math.cos(angle) * dist, Math.sin(angle) * dist};
	}
}
