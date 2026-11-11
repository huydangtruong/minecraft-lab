package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.permahearts.HeartRules.DeathOutcome;
import io.github.huydangtruong.permahearts.HeartRules.KillOutcome;
import io.github.huydangtruong.permahearts.HeartRules.KillResult;
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

	private static KillResult kills(HeartsData d, int n) {
		KillResult r = null;
		for (int i = 0; i < n; i++) {
			r = HeartRules.onHostileKill(d);
			d = r.data();
		}
		return r;
	}

	@Test
	void hundredKillsGainHeart() {
		var r99 = kills(HeartsData.DEFAULT, 99);
		assertEquals(KillOutcome.PROGRESS, r99.outcome());
		assertEquals(99, r99.data().killProgress());
		var r = HeartRules.onHostileKill(r99.data());
		assertEquals(KillOutcome.GAINED, r.outcome());
		assertEquals(11, HeartRules.maxHearts(r.data()));
		assertEquals(0, r.data().killProgress());
		assertEquals(100, r.data().totalHostileKills());
	}

	@Test
	void hundredthKillAtCap() {
		var d = HeartRules.withProgress(HeartRules.withHearts(HeartsData.DEFAULT, 30), 99);
		var r = HeartRules.onHostileKill(d);
		assertEquals(KillOutcome.AT_CAP, r.outcome());
		assertEquals(30, HeartRules.maxHearts(r.data()));
		assertEquals(0, r.data().killProgress());
	}

	@Test
	void progressSurvivesDeath() {
		var d = HeartRules.withProgress(HeartsData.DEFAULT, 37);
		assertEquals(37, HeartRules.onDeath(d).data().killProgress());
	}

	@Test
	void clamping() {
		assertEquals(1, HeartRules.maxHearts(HeartRules.withHearts(HeartsData.DEFAULT, 0)));
		assertEquals(30, HeartRules.maxHearts(HeartRules.withHearts(HeartsData.DEFAULT, 99)));
		assertEquals(99, HeartRules.withProgress(HeartsData.DEFAULT, 150).killProgress());
		assertEquals(0, HeartRules.withProgress(HeartsData.DEFAULT, -5).killProgress());
	}
}
