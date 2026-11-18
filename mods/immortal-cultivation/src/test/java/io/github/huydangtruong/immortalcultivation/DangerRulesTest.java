package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.danger.DangerRules;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules.Affix;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules.GearPlan;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules.Tier;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** P-054 (D137, D138): the danger curves, gear, elites and notices. */
class DangerRulesTest {
	private static final double EPS = 1e-9;

	@Test
	void spawnBonusCurve() {
		assertEquals(0, DangerRules.spawnBonus(0), EPS);
		assertEquals(0.1, DangerRules.spawnBonus(1), EPS);
		assertEquals(0.8, DangerRules.spawnBonus(8), EPS);
		assertEquals(1.0, DangerRules.spawnBonus(10), EPS);
		assertEquals(2.0, DangerRules.spawnBonus(20), EPS);
		assertEquals(2.2, DangerRules.spawnBonus(21), EPS);
		assertEquals(3.0, DangerRules.spawnBonus(25), EPS);
		assertEquals(3.0, DangerRules.spawnBonus(26), EPS);
		assertEquals(3.0, DangerRules.spawnBonus(30), EPS);
	}

	@Test
	void strengthBonusCurve() {
		assertEquals(0, DangerRules.strengthBonus(0), EPS);
		assertEquals(0.075, DangerRules.strengthBonus(1), EPS);
		assertEquals(0.6, DangerRules.strengthBonus(8), EPS);
		assertEquals(0.75, DangerRules.strengthBonus(10), EPS);
		assertEquals(1.5, DangerRules.strengthBonus(20), EPS);
		assertEquals(1.65, DangerRules.strengthBonus(21), EPS);
		assertEquals(2.25, DangerRules.strengthBonus(25), EPS);
		assertEquals(3.0, DangerRules.strengthBonus(30), EPS);
	}

	@Test
	void daySpawnsAndRain() {
		assertEquals(0, DangerRules.daySpawnFactor(0), EPS);
		assertEquals(0, DangerRules.daySpawnFactor(7), EPS);
		assertEquals(0.15, DangerRules.daySpawnFactor(8), EPS);
		assertEquals(1.0, DangerRules.daySpawnFactor(20), EPS);
		assertEquals(1.0, DangerRules.daySpawnFactor(30), EPS);
		assertTrue(DangerRules.daySpawnFactor(14) > 0.15 && DangerRules.daySpawnFactor(14) < 1.0);
		assertFalse(DangerRules.rainSpawns(5));
		assertTrue(DangerRules.rainSpawns(6));
	}

	@Test
	void unlockedTypes() {
		assertTrue(DangerRules.unlockedTypes(3).isEmpty());
		assertEquals(List.of(DangerRules.Type.PILLAGER), DangerRules.unlockedTypes(4));
		assertEquals(2, DangerRules.unlockedTypes(6).size());
		assertEquals(4, DangerRules.unlockedTypes(10).size());
		assertTrue(DangerRules.Type.BLAZE.darkOnly && DangerRules.Type.WITHER_SKELETON.darkOnly);
		assertEquals(7, DangerRules.unlockedTypes(16).size());
	}

	@Test
	void noExtraGearUpToTwenty() {
		Random rng = new Random(1);
		for (int level = 0; level <= 20; level++) {
			for (int i = 0; i < 50; i++) {
				GearPlan p = DangerRules.gearRoll(level, rng);
				for (Tier t : p.armor()) {
					assertEquals(Tier.NONE, t);
				}
			}
		}
		assertEquals(0, DangerRules.gearChance(20), EPS);
		assertEquals(0.2, DangerRules.gearChance(21), EPS);
		assertEquals(0.7, DangerRules.gearChance(30), EPS);
	}

	@Test
	void levelThirtyAlwaysHasANetheritePiece() {
		Random rng = new Random(42);
		for (int i = 0; i < 5000; i++) {
			assertTrue(DangerRules.gearRoll(30, rng).hasNetherite(), "roll " + i);
		}
	}

	@Test
	void gearImprovesWithLevel() {
		Random rng = new Random(7);
		int low = 0;
		int high = 0;
		int highNetherite = 0;
		for (int i = 0; i < 4000; i++) {
			for (Tier t : DangerRules.gearRoll(21, rng).armor()) {
				low += t == Tier.NONE ? 0 : 1;
			}
			for (Tier t : DangerRules.gearRoll(29, rng).armor()) {
				high += t == Tier.NONE ? 0 : 1;
				highNetherite += t == Tier.NETHERITE ? 1 : 0;
			}
		}
		assertTrue(high > low * 2, "more pieces at 29 than at 21");
		assertTrue(highNetherite > 0);
	}

	@Test
	void eliteChanceCurve() {
		assertEquals(0, DangerRules.eliteChance(9), EPS);
		assertEquals(0.02, DangerRules.eliteChance(10), EPS);
		assertEquals(0.10, DangerRules.eliteChance(20), EPS);
		assertEquals(0.15, DangerRules.eliteChance(30), EPS);
		assertTrue(DangerRules.eliteChance(15) > 0.02 && DangerRules.eliteChance(15) < 0.10);
	}

	@Test
	void affixCount() {
		assertEquals(0, DangerRules.maxAffixes(9));
		assertEquals(1, DangerRules.maxAffixes(10));
		assertEquals(1, DangerRules.maxAffixes(19));
		assertEquals(2, DangerRules.maxAffixes(20));
		assertEquals(2, DangerRules.maxAffixes(29));
		assertEquals(3, DangerRules.maxAffixes(30));
		Random rng = new Random(3);
		int most = 0;
		for (int i = 0; i < 500; i++) {
			List<Affix> a = DangerRules.rollAffixes(30, rng);
			assertTrue(a.size() >= 1 && a.size() <= 3);
			assertEquals(a.size(), new HashSet<>(a).size(), "distinct");
			most = Math.max(most, a.size());
			assertEquals(1, DangerRules.rollAffixes(10, rng).size());
		}
		assertEquals(3, most);
		assertTrue(DangerRules.rollAffixes(5, rng).isEmpty());
		assertEquals(Affix.SWIFT, Affix.byName("swift"));
	}

	@Test
	void noticeStrings() {
		assertEquals("Danger level 1", DangerRules.notice(1).title());
		assertEquals("+10% mobs, +8% strength", DangerRules.notice(1).subtitle());
		assertEquals("+80% mobs, +60% strength, day spawns", DangerRules.notice(8).subtitle());
		assertEquals("+100% mobs, +75% strength, elites appear, Blazes and Wither Skeletons appear", DangerRules.notice(10).subtitle());
		assertEquals("+220% mobs, +165% strength, more armor", DangerRules.notice(21).subtitle());
		assertEquals("+300% mobs, +300% strength, netherite armor", DangerRules.notice(30).subtitle());
	}
}
