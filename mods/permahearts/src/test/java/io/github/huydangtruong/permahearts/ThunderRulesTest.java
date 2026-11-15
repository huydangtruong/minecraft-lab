package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import java.util.List;
import org.junit.jupiter.api.Test;

/** P-029: the numbers of Thunder Strike (D86). */
class ThunderRulesTest {
	@Test
	void damageGrowsTwoPerLevel() {
		assertEquals(14, TechniqueRules.thunderDamage(1));
		assertEquals(32, TechniqueRules.thunderDamage(10));
		assertEquals(52, TechniqueRules.thunderDamage(20));
	}

	@Test
	void blastGrowsFivePercentOfACreeperPerLevel() {
		assertEquals(3.0, TechniqueRules.thunderPower(1), 1e-9);
		assertEquals(5.85, TechniqueRules.thunderPower(20), 1e-9);
	}

	@Test
	void damageFallsToHalfAtTheEdgeAndStopsOutside() {
		double r = TechniqueRules.thunderRadius(1);
		assertEquals(14, TechniqueRules.thunderDamageAt(1, 0), 1e-9);
		assertEquals(7, TechniqueRules.thunderDamageAt(1, r), 1e-9);
		assertEquals(0, TechniqueRules.thunderDamageAt(1, r + 0.01), 1e-9);
	}

	@Test
	void rangeWarningAndCooldown() {
		assertEquals(128, TechniqueRules.THUNDER_RANGE);
		assertEquals(20, TechniqueRules.THUNDER_WARN_TICKS);
		assertEquals(26, Technique.THUNDER.cooldownSeconds());
		assertEquals(26, TechniqueRules.cooldownSeconds(Technique.THUNDER, 20));
		assertTrue(!Technique.THUNDER.charged());
	}

	@Test
	void statLines() {
		List<TechniqueRules.Stat> s1 = TechniqueRules.stats(Technique.THUNDER, 1);
		assertEquals("Damage", s1.get(0).label());
		assertEquals("14", s1.get(0).value());
		assertEquals("100% of a creeper", s1.get(1).value());
		assertEquals("128 blocks", s1.get(2).value());
		assertEquals("24 s", s1.get(3).value());
		List<TechniqueRules.Stat> s20 = TechniqueRules.stats(Technique.THUNDER, 20);
		assertEquals("52", s20.get(0).value());
		assertEquals("195% of a creeper", s20.get(1).value());
	}
}
