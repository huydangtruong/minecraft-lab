package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.MayhemRules;
import io.github.huydangtruong.immortalcultivation.technique.MayhemRules.P;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import java.util.List;
import org.junit.jupiter.api.Test;

/** D174 Crystal Mayhem numbers. */
class MayhemRulesTest {
	@Test
	void shardDamageByLight() {
		assertEquals(7, MayhemRules.shardDamage(1, 15, 0), 1e-9);
		assertEquals(9.25, MayhemRules.shardDamage(10, 15, 0), 1e-9);
		assertEquals(11.75, MayhemRules.shardDamage(20, 15, 0), 1e-9);
		assertEquals(5, MayhemRules.shardDamage(1, 0, 15), 1e-9);
		assertEquals(6.35, MayhemRules.shardDamage(10, 0, 15), 1e-9);
		assertEquals(7.85, MayhemRules.shardDamage(20, 0, 15), 1e-9);
		assertEquals(2, MayhemRules.shardDamage(1, 0, 0), 1e-9);
		assertEquals(2, MayhemRules.shardDamage(20, 2, 4), 1e-9); // under the first step of both
	}

	@Test
	void theStrongerLightWinsAndATieGoesToTheSky() {
		assertEquals(3, MayhemRules.lightExtra(9, 15)); // block 15 is stronger than sky 9: 15 / 5
		assertEquals(5, MayhemRules.lightExtra(15, 15)); // tie: sky 15 / 3
		assertEquals(3, MayhemRules.lightExtra(5, 15));
		assertEquals(1, MayhemRules.lightExtra(3, 2));
	}

	@Test
	void skyLightIsDarkenedAndZeroAtNight() {
		assertEquals(15, MayhemRules.skyLevel(15, 0, 6000));
		assertEquals(12, MayhemRules.skyLevel(15, 3, 6000));
		assertEquals(0, MayhemRules.skyLevel(2, 5, 6000));
		assertEquals(0, MayhemRules.skyLevel(15, 0, 16000));
		assertEquals(0, MayhemRules.skyLevel(15, 0, 22999));
		assertEquals(15, MayhemRules.skyLevel(15, 0, 23000));
		assertEquals(0, MayhemRules.skyLevel(15, 0, 24000 * 3 + 18000)); // later days wrap
		assertEquals(15, MayhemRules.skyLevel(15, 0, 15999));
	}

	@Test
	void delayBuddingsBurnRadius() {
		assertArrayEquals(new double[] {1, 0.55, 0.1}, at(MayhemRules::shardDelaySeconds), 1e-9);
		assertEquals(20, MayhemRules.shardDelayTicks(1));
		assertEquals(2, MayhemRules.shardDelayTicks(20));
		assertArrayEquals(new double[] {5, 14, 24}, at(l -> MayhemRules.maxBuddings(l)), 1e-9);
		assertArrayEquals(new double[] {1, 5.5, 10.5}, at(MayhemRules::burnDamage), 1e-9);
		assertArrayEquals(new double[] {3, 7, 12}, at(l -> MayhemRules.burnRadius(l)), 1e-9);
	}

	private interface F {
		double of(int level);
	}

	private static double[] at(F f) {
		return new double[] {f.of(1), f.of(10), f.of(20)};
	}

	@Test
	void chainPrefersTheNearestToThePlayerThenTheNearestToABudding() {
		P player = new P(0, 0, 0);
		List<P> candidates = List.of(new P(4, 0, 0), new P(2, 0, 0), new P(8, 0, 0), new P(12, 0, 0));
		assertEquals(1, MayhemRules.pickNext(player, List.of(), candidates)); // 2 blocks away wins
		// nothing within 5 of the player: a cluster within 5 of a budding qualifies
		List<P> far = List.of(new P(20, 0, 0), new P(9, 0, 0), new P(10, 0, 0));
		assertEquals(-1, MayhemRules.pickNext(player, List.of(), far));
		assertEquals(1, MayhemRules.pickNext(player, List.of(new P(5, 0, 0)), far)); // 9 is 4 from the budding, 10 is 5
		assertEquals(-1, MayhemRules.pickNext(player, List.of(new P(5, 0, 0)), List.of(new P(20, 0, 0))));
	}

	@Test
	void aPlayerNearCandidateBeatsOneNearABudding() {
		P player = new P(0, 0, 0);
		List<P> candidates = List.of(new P(9, 0, 0), new P(4.5, 0, 0)); // 9 is 1 from the budding, but 4.5 is near the player
		assertEquals(1, MayhemRules.pickNext(player, List.of(new P(8, 0, 0)), candidates));
	}

	@Test
	void activeIsEverythingReachableThroughLinksOfFive() {
		P player = new P(0, 0, 0);
		List<P> b = List.of(new P(4, 0, 0), new P(9, 0, 0), new P(14, 0, 0), new P(30, 0, 0), new P(0, 5, 0));
		boolean[] on = MayhemRules.active(player, b);
		assertTrue(on[0] && on[1] && on[2], "a chain of links of 5 reaches 14 blocks out");
		assertFalse(on[3], "a budding 30 blocks away is off");
		assertTrue(on[4], "exactly 5 blocks counts");
		assertFalse(MayhemRules.active(new P(100, 0, 0), b)[0], "a player far from everything lights nothing");
	}

	@Test
	void technique() {
		assertEquals(30, Technique.MAYHEM.cooldownSeconds());
		assertEquals("Crystal Mayhem", Technique.MAYHEM.displayName());
		assertEquals("mayhem", Technique.MAYHEM.id());
		assertEquals(Technique.BLADE.ordinal() + 1, Technique.MAYHEM.ordinal(), "Crystal Mayhem follows Life Stealer in the attack group");
		assertTrue(TechniqueRules.stats(Technique.MAYHEM, 20).size() >= 4);
		assertEquals(30, TechniqueRules.cooldownSeconds(Technique.MAYHEM, 20));
	}
}
