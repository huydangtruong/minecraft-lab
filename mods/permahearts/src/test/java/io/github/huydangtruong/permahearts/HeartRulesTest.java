package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.permahearts.HeartRules.DeathOutcome;
import io.github.huydangtruong.permahearts.HeartRules.KillOutcome;
import org.junit.jupiter.api.Test;

class HeartRulesTest {
	@Test
	void defaultIsTenHearts() {
		assertEquals(10, HeartRules.maxHearts(HeartsData.DEFAULT));
	}

	@Test
	void deathLosesOneHeart() {
		var r = HeartRules.onDeath(HeartsData.DEFAULT);
		assertEquals(9, HeartRules.maxHearts(r.data()));
		assertEquals(DeathOutcome.LOST, r.outcome());
		assertEquals(1, r.data().totalDeaths());
	}

	@Test
	void deathAtFloorKeepsOneHeart() {
		var d = HeartRules.withHearts(HeartsData.DEFAULT, 1);
		var r = HeartRules.onDeath(d);
		assertEquals(1, HeartRules.maxHearts(r.data()));
		assertEquals(DeathOutcome.AT_FLOOR, r.outcome());
		assertEquals(1, r.data().totalDeaths());
	}

	private static HeartsData at(int hearts, int xp) {
		return HeartRules.withXp(HeartRules.withHearts(HeartsData.DEFAULT, hearts), xp);
	}

	@Test
	void xpForMobHealth() {
		assertEquals(20, HeartRules.xpFor(20f));
		assertEquals(1, HeartRules.xpFor(0.5f));
		assertEquals(500, HeartRules.xpFor(499.6f));
	}

	@Test
	void fullBarGainsHeart() {
		var r = HeartRules.onHostileKill(at(10, 480), 20);
		assertEquals(KillOutcome.GAINED, r.outcome());
		assertEquals(11, HeartRules.maxHearts(r.data()));
		assertEquals(0, r.data().heartXp());
		assertEquals(1, r.heartsGained());
		assertEquals(1, r.data().totalHostileKills());
	}

	@Test
	void partialXpIsProgress() {
		var r = HeartRules.onHostileKill(HeartsData.DEFAULT, 20);
		assertEquals(KillOutcome.PROGRESS, r.outcome());
		assertEquals(20, r.data().heartXp());
		assertEquals(20, r.xpGained());
		assertEquals(0, r.heartsGained());
	}

	@Test
	void overflowCarriesOver() {
		var r = HeartRules.onHostileKill(at(10, 300), 300);
		assertEquals(11, HeartRules.maxHearts(r.data()));
		assertEquals(100, r.data().heartXp());
	}

	@Test
	void bigKillGivesTwoHearts() {
		var r = HeartRules.onHostileKill(HeartsData.DEFAULT, 1200);
		assertEquals(12, HeartRules.maxHearts(r.data()));
		assertEquals(200, r.data().heartXp());
		assertEquals(2, r.heartsGained());
	}

	@Test
	void reachingCapDropsExtraXp() {
		var r = HeartRules.onHostileKill(at(49, 0), 1200);
		assertEquals(KillOutcome.GAINED, r.outcome());
		assertEquals(50, HeartRules.maxHearts(r.data()));
		assertEquals(0, r.data().heartXp());
		assertEquals(1, r.heartsGained());
	}

	@Test
	void killAtCap() {
		var r = HeartRules.onHostileKill(at(50, 0), 20);
		assertEquals(KillOutcome.AT_CAP, r.outcome());
		assertEquals(50, HeartRules.maxHearts(r.data()));
		assertEquals(0, r.data().heartXp());
		assertEquals(0, r.heartsGained());
	}

	@Test
	void xpSurvivesDeath() {
		assertEquals(37, HeartRules.onDeath(at(10, 37)).data().heartXp());
	}

	@Test
	void clamping() {
		assertEquals(1, HeartRules.maxHearts(HeartRules.withHearts(HeartsData.DEFAULT, 0)));
		assertEquals(50, HeartRules.maxHearts(HeartRules.withHearts(HeartsData.DEFAULT, 99)));
		assertEquals(499, HeartRules.withXp(HeartsData.DEFAULT, 600).heartXp());
		assertEquals(0, HeartRules.withXp(HeartsData.DEFAULT, -5).heartXp());
		assertEquals(0, HeartRules.withHearts(at(10, 100), 99).heartXp());
	}

	@Test
	void xpFractionForHud() {
		assertEquals(0.5f, HeartRules.xpFraction(at(10, 250)));
		assertEquals(1f, HeartRules.xpFraction(at(50, 0)));
	}
}
