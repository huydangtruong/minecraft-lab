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
			double[] o = QuestGenerator.planOffset(r);
			double dist = Math.hypot(o[0], o[1]);
			assertTrue(dist >= 899.9 && dist <= 1100.1, "dist " + dist);
		}
	}

	@Test
	void dataRoundTrips() {
		var d = new WandererData(Phase.PRESENT, 12, 5, 70, -9, Optional.of("abc"), Optional.of(quest(true)), 3);
		var json = WandererData.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
		assertEquals(d, WandererData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		assertEquals(WandererData.DEFAULT, WandererData.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow());
	}
}
