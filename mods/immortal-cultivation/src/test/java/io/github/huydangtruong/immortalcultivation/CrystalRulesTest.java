package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.CrystalRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** P-056 / D143: Crystal Prison numbers and cage geometry. */
class CrystalRulesTest {
	@Test
	void hardnessAndBlastGrowWithTheTier() {
		assertEquals(0.3, CrystalRules.crystalHardness(1), 1e-9);
		assertEquals(0.3 + 2.7 * 9 / 19, CrystalRules.crystalHardness(10), 1e-9);
		assertEquals(3.0, CrystalRules.crystalHardness(20), 1e-9);
		assertEquals(0.3, CrystalRules.crystalBlast(1), 1e-9);
		assertEquals(0.3 + 9.7 * 9 / 19, CrystalRules.crystalBlast(10), 1e-9);
		assertEquals(10.0, CrystalRules.crystalBlast(20), 1e-9);
		assertEquals(3.0, CrystalRules.crystalHardness(99), 1e-9, "tiers are clamped");
		assertEquals(0.3, CrystalRules.crystalHardness(0), 1e-9);
	}

	@Test
	void disarmAndCageTimes() {
		assertEquals(3.0, CrystalRules.disarmSeconds(1));
		assertEquals(12.5, CrystalRules.disarmSeconds(20));
		assertEquals(8.0, CrystalRules.cageSeconds(1));
		assertEquals(27.0, CrystalRules.cageSeconds(20));
	}

	@Test
	void tooBigMeansWiderOrTallerThanThree() {
		assertFalse(CrystalRules.tooBig(0.6, 1.95));
		assertFalse(CrystalRules.tooBig(3, 3));
		assertTrue(CrystalRules.tooBig(4, 4), "ghast");
		assertTrue(CrystalRules.tooBig(1, 3.5));
	}

	@Test
	void cageHas27InteriorAndFiftyFourShellCellsWithoutEdgesOrCorners() {
		CrystalRules.Cage cage = CrystalRules.cage(10, 64, -5);
		assertEquals(27, cage.interior().size());
		assertEquals(54, cage.shell().size());
		Set<CrystalRules.Cell> shell = new HashSet<>(cage.shell());
		assertEquals(54, shell.size(), "no duplicate cell");
		for (CrystalRules.Cell in : cage.interior()) {
			assertFalse(shell.contains(in), "the interior is hollow");
			assertTrue(in.y() >= 65 && in.y() <= 67 && Math.abs(in.x() - 10) <= 1 && Math.abs(in.z() + 5) <= 1);
		}
		assertTrue(shell.contains(new CrystalRules.Cell(10, 64, -5)), "floor");
		assertTrue(shell.contains(new CrystalRules.Cell(10, 68, -5)), "roof");
		assertTrue(shell.contains(new CrystalRules.Cell(12, 66, -5)), "wall");
		// the 12 edge cells and 8 corner cells of the 5x5x5 box are empty
		for (int x : new int[] {8, 12}) {
			for (int y : new int[] {64, 68}) {
				for (int z : new int[] {-7, -3}) {
					assertFalse(shell.contains(new CrystalRules.Cell(x, y, z)), "corner");
				}
			}
		}
		assertFalse(shell.contains(new CrystalRules.Cell(12, 64, -5)), "floor edge");
		assertFalse(shell.contains(new CrystalRules.Cell(12, 66, -7)), "vertical edge");
	}

	@Test
	void atMostTwoStayAndTheRestAreChosenAtRandom() {
		assertEquals(List.of(), CrystalRules.pickKept(0, new Random(1)));
		assertEquals(List.of(0), CrystalRules.pickKept(1, new Random(1)));
		assertEquals(List.of(0, 1), CrystalRules.pickKept(2, new Random(1)));
		Set<Integer> seen = new HashSet<>();
		for (int seed = 0; seed < 50; seed++) {
			List<Integer> kept = CrystalRules.pickKept(3, new Random(seed));
			assertEquals(2, kept.size());
			seen.addAll(kept);
		}
		assertEquals(Set.of(0, 1, 2), seen, "every candidate can be kept");
	}

	@Test
	void pushOutGoesToTheNearestSide() {
		double[] east = CrystalRules.pushOutOffset(1.2, 0.3, 0.6);
		assertTrue(east[0] > 2.5 && east[1] == 0.3);
		double[] north = CrystalRules.pushOutOffset(-0.2, -1.4, 0.6);
		assertTrue(north[1] < -2.5 && north[0] == -0.2);
	}

	@Test
	void registration() {
		assertEquals(48, Technique.CRYSTAL.cooldownSeconds());
		assertEquals("Crystal Prison", Technique.CRYSTAL.displayName());
		assertFalse(Technique.CRYSTAL.charged());
		assertEquals(Technique.SACRIFICE.ordinal() + 1, Technique.CRYSTAL.ordinal(), "Crystal Prison follows Undying Jutsu");
		assertTrue(TechniqueRules.stats(Technique.CRYSTAL, 20).size() >= 3);
	}
}
