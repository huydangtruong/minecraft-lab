package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.WitchRules;
import io.github.huydangtruong.immortalcultivation.technique.WitchRules.Intent;
import io.github.huydangtruong.immortalcultivation.technique.WitchRules.Potion;
import org.junit.jupiter.api.Test;

class WitchRulesTest {
	@Test
	void allyNeedsHealingBelowThreeQuarters() {
		assertFalse(WitchRules.needsHealing(1f));
		assertFalse(WitchRules.needsHealing(0.75f));
		assertTrue(WitchRules.needsHealing(0.74f));
	}

	@Test
	void d282PotionsScaleWithTheWitchStrength() {
		assertEquals(0, WitchRules.poisonAmplifier(1.0));
		assertEquals(0, WitchRules.poisonAmplifier(1.49));
		assertEquals(1, WitchRules.poisonAmplifier(1.5));
		assertEquals(1, WitchRules.poisonAmplifier(2.99));
		assertEquals(2, WitchRules.poisonAmplifier(3.0));
		assertEquals(2, WitchRules.poisonAmplifier(4.0));
		assertEquals(900, WitchRules.regenerationTicks(900, 1.0));
		assertEquals(3600, WitchRules.regenerationTicks(900, 4.0));
		assertEquals(1.0, WitchRules.instantFactor(0.5));
		assertEquals(2.5, WitchRules.instantFactor(2.5));
	}

	@Test
	void allyPotionByHealth() {
		assertEquals(Potion.HEALING, WitchRules.allyPotion(0.5f, false));
		assertEquals(Potion.REGENERATION, WitchRules.allyPotion(0.6f, false));
		assertEquals(Potion.REGENERATION, WitchRules.allyPotion(0.2f, true)); // Healing would hurt an undead ally
	}

	@Test
	void healingSplashSkipsUndeadAllies() {
		assertEquals(Intent.ALLY_LIVING, WitchRules.allyIntent(Potion.HEALING));
		assertEquals(Intent.ALLY, WitchRules.allyIntent(Potion.REGENERATION));
	}

	@Test
	void undeadEnemyGetsHealingNeverPoisonOrHarming() {
		for (float roll = 0; roll < 1; roll += 0.1f) {
			assertEquals(Potion.HEALING, WitchRules.enemyPotion(true, false, false, false, 20, 6, roll));
			assertEquals(Potion.HEALING, WitchRules.enemyPotion(true, true, true, true, 3, 1, roll));
		}
	}

	@Test
	void livingEnemyPotionOrder() {
		assertEquals(Potion.SLOWNESS, WitchRules.enemyPotion(false, false, false, false, 20, 6, 0.9f));
		assertEquals(Potion.WEAKNESS, WitchRules.enemyPotion(false, true, false, false, 20, 6, 0.1f));
		assertEquals(Potion.POISON, WitchRules.enemyPotion(false, true, false, false, 20, 6, 0.9f));
		assertEquals(Potion.HARMING, WitchRules.enemyPotion(false, true, true, true, 20, 6, 0.9f));
		assertEquals(Potion.HARMING, WitchRules.enemyPotion(false, true, true, false, 5, 6, 0.9f)); // too weak to poison
		assertEquals(Potion.POISON, WitchRules.enemyPotion(false, false, false, false, 20, 2, 0.9f)); // adjacent: no slowness
	}

	@Test
	void splashSides() {
		assertTrue(WitchRules.affects(Intent.ENEMY, false, false, false));
		assertFalse(WitchRules.affects(Intent.ENEMY, true, false, false)); // owner or ally
		assertFalse(WitchRules.affects(Intent.ENEMY, false, false, true)); // bystander player
		assertTrue(WitchRules.affects(Intent.ALLY, true, true, false));
		assertFalse(WitchRules.affects(Intent.ALLY, false, false, false)); // enemy gets no good effect
		assertTrue(WitchRules.affects(Intent.ALLY_LIVING, true, false, false));
		assertFalse(WitchRules.affects(Intent.ALLY_LIVING, true, true, false));
		assertFalse(WitchRules.affects(Intent.ALLY_LIVING, false, false, false));
	}
}
