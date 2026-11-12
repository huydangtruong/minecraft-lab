package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import io.github.huydangtruong.permahearts.wanderer.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuestLogicTest {
	private static Quest quest(boolean accepted) {
		return new Quest(1, List.of(
			new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 64, 0, "strength", false),
			new QuestLine(QuestType.KILL, "minecraft:zombie", 2, 0, "fury", false)), accepted);
	}

	@Test
	void depositWrongItemOrBeforeAccept() {
		assertEquals(0, QuestLogic.deposit(quest(true), 0, "minecraft:gold_ingot", 5).taken());
		assertEquals(0, QuestLogic.deposit(quest(false), 0, "minecraft:iron_ingot", 5).taken());
		assertEquals(0, QuestLogic.deposit(quest(true), 1, "minecraft:zombie", 5).taken());
	}

	@Test
	void depositTakesOnlyWhatIsNeeded() {
		var r = QuestLogic.deposit(quest(true), 0, "minecraft:iron_ingot", 70);
		assertEquals(64, r.taken());
		assertTrue(r.quest().lines().get(0).done());
		assertEquals(0, QuestLogic.deposit(r.quest(), 0, "minecraft:iron_ingot", 1).taken());
	}

	@Test
	void killCounts() {
		assertTrue(QuestLogic.kill(quest(true), "minecraft:cow").isEmpty());
		assertTrue(QuestLogic.kill(quest(false), "minecraft:zombie").isEmpty());
		var r = QuestLogic.kill(quest(true), "minecraft:zombie").orElseThrow();
		assertEquals(1, r.line());
		assertEquals(1, r.quest().lines().get(1).progress());
	}

	@Test
	void claimRules() {
		Quest q = quest(true);
		assertTrue(QuestLogic.claim(q, 1).isEmpty());
		Quest done = QuestLogic.complete(q, 1);
		Quest claimed = QuestLogic.claim(done, 1).orElseThrow();
		assertTrue(QuestLogic.claim(claimed, 1).isEmpty());
		assertFalse(claimed.allDoneAndClaimed());
		Quest all = QuestLogic.claim(QuestLogic.complete(claimed, 0), 0).orElseThrow();
		assertTrue(all.allDoneAndClaimed());
	}

	@Test
	void generatorIsValid() {
		for (int seed = 0; seed < 100; seed++) {
			Quest q = QuestGenerator.generate(seed, new Random(seed));
			assertTrue(q.lines().size() >= 2 && q.lines().size() <= 4);
			var targets = new HashSet<String>();
			for (QuestLine l : q.lines()) {
				assertTrue(targets.add(l.target()));
				assertTrue(ElixirStat.byId(l.reward()).isPresent());
				assertTrue(l.required() > 0);
			}
		}
	}

	@Test
	void daysAndOffset() {
		Random r = new Random(7);
		for (int i = 0; i < 200; i++) {
			int d = QuestGenerator.daysUntilNext(r);
			assertTrue(d >= 3 && d <= 5);
			int back = QuestGenerator.daysUntilReturn(r);
			assertTrue(back >= 5 && back <= 7, "return " + back);
			double[] o = QuestGenerator.planOffset(r);
			double dist = Math.hypot(o[0], o[1]);
			assertTrue(dist >= 899.9 && dist <= 1100.1, "dist " + dist);
		}
	}

	@Test
	void dataRoundTrips() {
		var d = new WandererData(Phase.PRESENT, 12, 5, 70, -9, Optional.of("abc"), Optional.of(quest(true)), 3,
			Optional.of(new BloodMoonState(1, "uuid-1", 5000L)), Optional.of(new BossState(2, "uuid-2")), 99000L, true);
		var json = WandererData.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
		assertEquals(d, WandererData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		assertEquals(WandererData.DEFAULT, WandererData.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow());
	}

	private static Quest eventQuest() {
		return new Quest(2, List.of(
			new QuestLine(QuestType.EXPLORE, "#minecraft:village", 1, 0, "strength", false),
			new QuestLine(QuestType.EXPLORE, "minecraft:fortress", 1, 0, "fury", false),
			new QuestLine(QuestType.BLOOD_MOON, "", 1, 0, "strength", false),
			new QuestLine(QuestType.BOSS, "minecraft:zombie", 1, 0, "fury", false)), true);
	}

	@Test
	void bloodMoonMobDefaultsToZombie() {
		assertEquals("minecraft:zombie", QuestGenerator.bloodMoonMob(""));
		assertEquals("minecraft:creeper", QuestGenerator.bloodMoonMob("minecraft:creeper"));
	}

	@Test
	void generatorAmounts() {
		for (int seed = 0; seed < 500; seed++) {
			for (QuestLine l : QuestGenerator.generate(seed, new Random(seed)).lines()) {
				if (l.type() == QuestType.KILL) {
					assertEquals(l.target().equals("minecraft:witch") ? 8 : 16, l.required(), l.target());
				}
				if (l.type() == QuestType.COLLECT) {
					assertTrue(List.of(16, 32, 48, 64).contains(l.required()), l.target());
				}
			}
		}
	}

	@Test
	void generatorWithAllTypes() {
		var seen = new HashSet<QuestType>();
		for (int seed = 0; seed < 1000; seed++) {
			Quest q = QuestGenerator.generate(seed, new Random(seed));
			int moons = 0;
			int bosses = 0;
			var targets = new HashSet<String>();
			for (QuestLine l : q.lines()) {
				seen.add(l.type());
				assertTrue(targets.add(l.target()), "duplicate target, seed " + seed);
				if (l.type() == QuestType.BLOOD_MOON) {
					moons++;
					assertTrue(QuestGenerator.BLOOD_MOON.contains(l.target()), l.target());
				}
				if (l.type() == QuestType.BOSS) {
					bosses++;
					assertTrue(List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:evoker", "minecraft:blaze",
						"minecraft:vindicator").contains(l.target()), l.target());
				}
				if (l.type().isEvent()) {
					assertEquals(1, l.required());
				}
				if (l.type() == QuestType.EXPLORE) {
					assertTrue(l.target().contains(":"));
				}
			}
			assertTrue(moons <= 1 && bosses <= 1, "seed " + seed);
		}
		assertEquals(5, seen.size());
	}

	@Test
	void exploreMatchesFirstNotDoneLine() {
		Quest q = eventQuest();
		assertTrue(QuestLogic.explore(q, t -> t.equals("minecraft:igloo")).isEmpty());
		var r = QuestLogic.explore(q, t -> t.equals("#minecraft:village")).orElseThrow();
		assertEquals(0, r.line());
		assertTrue(r.quest().lines().get(0).done());
		assertFalse(r.quest().lines().get(1).done());
		assertTrue(QuestLogic.explore(r.quest(), t -> t.equals("#minecraft:village")).isEmpty());
		Quest notAccepted = new Quest(2, q.lines(), false);
		assertTrue(QuestLogic.explore(notAccepted, t -> true).isEmpty());
	}

	@Test
	void eventDoneMarksLine() {
		Quest q = eventQuest();
		Quest after = QuestLogic.eventDone(q, 2);
		assertTrue(after.lines().get(2).done());
		assertFalse(after.lines().get(3).done());
		assertEquals(after, QuestLogic.eventDone(after, 2));
		assertEquals(q, QuestLogic.eventDone(q, 9));
	}

	@Test
	void oldDataWithoutEventStateStillLoads() {
		var json = new com.google.gson.JsonObject();
		json.addProperty("phase", "PRESENT");
		json.addProperty("quest_counter", 4);
		var d = WandererData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(Phase.PRESENT, d.phase());
		assertTrue(d.bloodMoon().isEmpty() && d.boss().isEmpty());
	}
}
