package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.huydangtruong.permahearts.wanderer.*;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DeathMatchTest {
	private static Quest quest() {
		return new Quest(1, List.of(
			new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 64, 0, "strength", false),
			new QuestLine(QuestType.DEATH_MATCH, DeathMatchRules.TARGET, 1, 0, "fury", false)), true);
	}

	@Test
	void neededIsHalfRoundedUp() {
		assertEquals(0, DeathMatchRules.needed(0));
		assertEquals(0, DeathMatchRules.needed(1));
		assertEquals(1, DeathMatchRules.needed(2));
		assertEquals(2, DeathMatchRules.needed(3));
		assertEquals(2, DeathMatchRules.needed(4));
		assertEquals(3, DeathMatchRules.needed(5));
	}

	@Test
	void offeredFromTwoPlayers() {
		assertFalse(DeathMatchRules.canOffer(1));
		assertTrue(DeathMatchRules.canOffer(2));
	}

	@Test
	void neverGeneratedWithOnePlayer() {
		for (long seed = 0; seed < 2000; seed++) {
			Quest q = QuestGenerator.generate(seed, new Random(seed), 1);
			assertTrue(q.lines().stream().noneMatch(l -> l.type() == QuestType.DEATH_MATCH), "seed " + seed);
			// the two-argument overload keeps working and means "one player"
			assertEquals(q, QuestGenerator.generate(seed, new Random(seed)));
		}
	}

	@Test
	void generatedAtMostOnceWithTwoPlayers() {
		int withMatch = 0;
		for (long seed = 0; seed < 2000; seed++) {
			Quest q = QuestGenerator.generate(seed, new Random(seed), 2);
			long n = q.lines().stream().filter(l -> l.type() == QuestType.DEATH_MATCH).count();
			assertTrue(n <= 1, "seed " + seed);
			withMatch += (int) n;
			for (QuestLine l : q.lines()) {
				if (l.type() == QuestType.DEATH_MATCH) {
					assertEquals(DeathMatchRules.TARGET, l.target());
					assertEquals(1, l.required());
				}
			}
		}
		assertTrue(withMatch > 100, "some quests contain a Death Match, got " + withMatch);
	}

	@Test
	void startedSetsRequiredAndResetsProgress() {
		Quest q = QuestLogic.deathMatchStarted(quest(), 1, 2);
		assertEquals(2, q.lines().get(1).required());
		assertEquals(0, q.lines().get(1).progress());
		assertFalse(q.lines().get(1).done());
		assertEquals(quest().lines().get(0), q.lines().get(0));
	}

	@Test
	void progressIsCappedAtRequired() {
		Quest q = QuestLogic.deathMatchStarted(quest(), 1, 2);
		q = QuestLogic.deathMatchProgress(q, 1, 1);
		assertEquals(1, q.lines().get(1).progress());
		assertFalse(q.lines().get(1).done());
		q = QuestLogic.deathMatchProgress(q, 1, 5);
		assertEquals(2, q.lines().get(1).progress());
		assertTrue(q.lines().get(1).done());
		assertSame(q, QuestLogic.deathMatchProgress(q, 9, 1)); // no such line
	}

	@Test
	void stateCountsEachVictimOnce() {
		DeathMatchState s = new DeathMatchState(1, List.of());
		DeathMatchState a = s.with("a");
		assertEquals(List.of("a"), a.fallen());
		assertSame(a, a.with("a"));
		assertEquals(List.of("a", "b"), a.with("b").fallen());
	}

	@Test
	void dataRoundTripsWithAndWithoutDeathMatch() {
		WandererData with = WandererData.DEFAULT.withQuest(Optional.of(quest()))
			.withDeathMatch(Optional.of(new DeathMatchState(1, List.of("a", "b"))));
		var json = WandererData.CODEC.encodeStart(JsonOps.INSTANCE, with).getOrThrow();
		assertEquals(with, WandererData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		WandererData without = WandererData.DEFAULT.withQuest(Optional.of(quest()));
		var json2 = WandererData.CODEC.encodeStart(JsonOps.INSTANCE, without).getOrThrow();
		assertFalse(json2.getAsJsonObject().has("death_match"));
		assertEquals(without, WandererData.CODEC.parse(JsonOps.INSTANCE, json2).getOrThrow());
		// an old save has no death_match field at all
		WandererData old = WandererData.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
		assertTrue(old.deathMatch().isEmpty());
	}
}
