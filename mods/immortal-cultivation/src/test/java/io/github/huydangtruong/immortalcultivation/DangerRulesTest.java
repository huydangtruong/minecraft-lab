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

	/** The pre-D206 curves, for comparing: new level L has the strength of old level L x 3/5. */
	private static double oldSpawn(double o) {
		return o <= 20 ? 0.10 * o : Math.min(3.0, 2.0 + 0.20 * (o - 20));
	}

	private static double oldStrength(double o) {
		return o <= 20 ? 0.075 * o : 1.5 + 0.15 * (o - 20);
	}

	@Test
	void maxLevelIsFiftyAndEqualsOldThirty() {
		assertEquals(50, DangerRules.MAX_LEVEL);
		assertEquals(50, DangerRules.clamp(99));
		assertEquals(oldSpawn(30), DangerRules.spawnBonus(50), EPS);
		assertEquals(oldStrength(30), DangerRules.strengthBonus(50), EPS);
		assertEquals(3.0, DangerRules.strengthBonus(50), EPS);
		assertEquals(0.7, DangerRules.gearChance(50), EPS);
		assertEquals(0.15, DangerRules.eliteChance(50), EPS);
		assertEquals(3, DangerRules.maxAffixes(50));
		assertEquals("netherite armor", DangerRules.unlocks(50));
	}

	@Test
	void spawnBonusCurve() {
		assertEquals(0, DangerRules.spawnBonus(0), EPS);
		assertEquals(0.06, DangerRules.spawnBonus(1), EPS);
		for (int l = 0; l <= 50; l++) {
			assertEquals(oldSpawn(l * 0.6), DangerRules.spawnBonus(l), EPS, "level " + l);
		}
		assertEquals(3.0, DangerRules.spawnBonus(42), EPS);
		assertEquals(3.0, DangerRules.spawnBonus(50), EPS);
	}

	@Test
	void strengthBonusCurve() {
		assertEquals(0, DangerRules.strengthBonus(0), EPS);
		assertEquals(0.045, DangerRules.strengthBonus(1), EPS);
		for (int l = 0; l <= 50; l++) {
			assertEquals(oldStrength(l * 0.6), DangerRules.strengthBonus(l), EPS, "level " + l);
		}
	}

	@Test
	void curvesNeverDropAndStepsStaySmall() {
		for (int l = 1; l <= 50; l++) {
			assertTrue(DangerRules.spawnBonus(l) >= DangerRules.spawnBonus(l - 1), "spawn " + l);
			assertTrue(DangerRules.strengthBonus(l) > DangerRules.strengthBonus(l - 1), "strength " + l);
			assertTrue(DangerRules.strengthBonus(l) - DangerRules.strengthBonus(l - 1) <= 0.0901, "no stair step at " + l);
			assertTrue(DangerRules.daySpawnFactor(l) >= DangerRules.daySpawnFactor(l - 1), "day " + l);
			assertTrue(DangerRules.gearChance(l) >= DangerRules.gearChance(l - 1), "gear " + l);
			assertTrue(DangerRules.eliteChance(l) >= DangerRules.eliteChance(l - 1), "elite " + l);
			assertTrue(DangerRules.maxAffixes(l) >= DangerRules.maxAffixes(l - 1), "affixes " + l);
		}
	}

	@Test
	void oldLevelsConvertByFiveThirdsMinusFive() {
		assertEquals(0, DangerRules.fromOldScale(0));
		assertEquals(0, DangerRules.fromOldScale(1));
		assertEquals(0, DangerRules.fromOldScale(3));
		assertEquals(2, DangerRules.fromOldScale(4));
		assertEquals(28, DangerRules.fromOldScale(20));
		assertEquals(45, DangerRules.fromOldScale(30));
		assertEquals(45, DangerRules.fromOldScale(99));
		for (int o = 4; o <= 30; o++) {
			assertTrue(DangerRules.fromOldScale(o) > DangerRules.fromOldScale(o - 1), "keeps order at " + o);
		}
	}

	@Test
	void xpFollowsHealth() {
		assertEquals(1.0, DangerRules.xpFactor(0, false), EPS);
		assertEquals(1.8, DangerRules.xpFactor(0.8, false), EPS);
		assertEquals(4.0, DangerRules.xpFactor(1.0, true), EPS);
		assertEquals(5, DangerRules.scaledXp(5, 1.0));
		assertEquals(9, DangerRules.scaledXp(5, 1.8));
		assertEquals(20, DangerRules.scaledXp(5, 4.0));
		assertEquals(0, DangerRules.scaledXp(0, 4.0));
		assertEquals(5, DangerRules.scaledXp(5, 0.5), "never below vanilla");
		assertEquals(20, DangerRules.scaledXp(5, DangerRules.xpFactor(DangerRules.strengthBonus(50), false)));
	}

	@Test
	void daySpawnsAndRain() {
		assertEquals(0, DangerRules.daySpawnFactor(0), EPS);
		assertEquals(0, DangerRules.daySpawnFactor(12), EPS);
		assertEquals(0.15, DangerRules.daySpawnFactor(13), EPS);
		assertEquals(1.0, DangerRules.daySpawnFactor(33), EPS);
		assertEquals(1.0, DangerRules.daySpawnFactor(50), EPS);
		assertTrue(DangerRules.daySpawnFactor(23) > 0.15 && DangerRules.daySpawnFactor(23) < 1.0);
		assertFalse(DangerRules.rainSpawns(9));
		assertTrue(DangerRules.rainSpawns(10));
		assertTrue(DangerRules.RAIN_LEVEL < DangerRules.DAY_START_LEVEL);
	}

	@Test
	void unlockedTypes() {
		assertTrue(DangerRules.unlockedTypes(6).isEmpty());
		assertEquals(DangerRules.FIRST_TYPE_LEVEL, DangerRules.Type.PILLAGER.unlockLevel);
		assertEquals(List.of(DangerRules.Type.PILLAGER), DangerRules.unlockedTypes(7));
		assertEquals(2, DangerRules.unlockedTypes(10).size());
		assertEquals(4, DangerRules.unlockedTypes(17).size());
		assertTrue(DangerRules.Type.BLAZE.darkOnly && DangerRules.Type.WITHER_SKELETON.darkOnly);
		assertEquals(7, DangerRules.unlockedTypes(27).size());
	}

	@Test
	void unlockTextsFireAtTheNewLevels() {
		int[] levels = {7, 10, 13, 17, 20, 23, 27, 33, 35, 50};
		for (int l : levels) {
			assertFalse(DangerRules.unlocks(l).isEmpty(), "level " + l);
		}
		for (int l = 0; l <= 50; l++) {
			final int lv = l;
			boolean listed = java.util.Arrays.stream(levels).anyMatch(x -> x == lv);
			assertEquals(listed, !DangerRules.unlocks(l).isEmpty(), "level " + l);
		}
	}

	@Test
	void noExtraGearBelowThirtyFive() {
		Random rng = new Random(1);
		for (int level = 0; level < DangerRules.GEAR_START_LEVEL; level++) {
			for (int i = 0; i < 50; i++) {
				GearPlan p = DangerRules.gearRoll(level, rng);
				for (Tier t : p.armor()) {
					assertEquals(Tier.NONE, t);
				}
			}
		}
		assertEquals(0, DangerRules.gearChance(34), EPS);
		assertEquals(0.2, DangerRules.gearChance(35), EPS);
		assertEquals(0.7, DangerRules.gearChance(50), EPS);
	}

	@Test
	void levelFiftyAlwaysHasANetheritePiece() {
		Random rng = new Random(42);
		for (int i = 0; i < 5000; i++) {
			assertTrue(DangerRules.gearRoll(50, rng).hasNetherite(), "roll " + i);
		}
	}

	@Test
	void gearImprovesWithLevel() {
		Random rng = new Random(7);
		int low = 0;
		int high = 0;
		int highNetherite = 0;
		for (int i = 0; i < 4000; i++) {
			for (Tier t : DangerRules.gearRoll(35, rng).armor()) {
				low += t == Tier.NONE ? 0 : 1;
			}
			for (Tier t : DangerRules.gearRoll(48, rng).armor()) {
				high += t == Tier.NONE ? 0 : 1;
				highNetherite += t == Tier.NETHERITE ? 1 : 0;
			}
		}
		assertTrue(high > low * 2, "more pieces at 48 than at 35");
		assertTrue(highNetherite > 0);
	}

	@Test
	void eliteChanceCurve() {
		assertEquals(0, DangerRules.eliteChance(17), EPS);
		assertEquals(0, DangerRules.eliteChance(32), EPS);
		assertEquals(0.05, DangerRules.eliteChance(33), EPS);
		assertEquals(0.05 + 0.10 * 9 / 17, DangerRules.eliteChance(42), EPS);
		assertEquals(0.15, DangerRules.eliteChance(50), EPS);
	}

	@Test
	void affixCount() {
		assertEquals(0, DangerRules.maxAffixes(32));
		assertEquals(1, DangerRules.maxAffixes(33));
		assertEquals(1, DangerRules.maxAffixes(41));
		assertEquals(2, DangerRules.maxAffixes(42));
		assertEquals(2, DangerRules.maxAffixes(49));
		assertEquals(3, DangerRules.maxAffixes(50));
		Random rng = new Random(3);
		int most = 0;
		for (int i = 0; i < 500; i++) {
			List<Affix> a = DangerRules.rollAffixes(50, rng);
			assertTrue(a.size() >= 1 && a.size() <= 3);
			assertEquals(a.size(), new HashSet<>(a).size(), "distinct");
			most = Math.max(most, a.size());
			assertEquals(1, DangerRules.rollAffixes(33, rng).size());
		}
		assertEquals(3, most);
		assertTrue(DangerRules.rollAffixes(32, rng).isEmpty());
		assertEquals(Affix.SWIFT, Affix.byName("swift"));
	}

	@Test
	void noticeStrings() {
		assertEquals("Danger level 1", DangerRules.notice(1).title());
		assertEquals("+6% mobs, +5% strength", DangerRules.notice(1).subtitle());
		assertTrue(DangerRules.notice(13).subtitle().startsWith("+78% mobs, ") && DangerRules.notice(13).subtitle().endsWith(", day spawns"));
		assertEquals("+300% mobs, +300% strength, netherite armor", DangerRules.notice(50).subtitle());
		assertTrue(DangerRules.notice(7).subtitle().endsWith("Pillagers appear"));
		assertTrue(DangerRules.notice(33).subtitle().endsWith("elites appear, day as busy as night"));
		assertTrue(DangerRules.notice(35).subtitle().endsWith("more armor"));
	}
}
