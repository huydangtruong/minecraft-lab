package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.passive.Passive;
import io.github.huydangtruong.immortalcultivation.passive.ItemGuardRules;
import io.github.huydangtruong.immortalcultivation.passive.PassiveData;
import io.github.huydangtruong.immortalcultivation.passive.PassiveRules;
import java.util.List;
import org.junit.jupiter.api.Test;

class PassiveRulesTest {
	@Test
	void cooldownGoesFrom3To16Point5Percent() {
		assertEquals(0, PassiveRules.cooldownPercent(0));
		assertEquals(3, PassiveRules.cooldownPercent(1));
		assertEquals(7.5, PassiveRules.cooldownPercent(4));
		assertEquals(16.5, PassiveRules.cooldownPercent(10));
		assertEquals(200, PassiveRules.reducedCooldown(200, 0));
		assertEquals(167, PassiveRules.reducedCooldown(200, 10));
		assertEquals(1, PassiveRules.reducedCooldown(1, 10));
		assertEquals("Reduce cooldown 7.5%", PassiveRules.title(Passive.COOLDOWN, 4));
	}

	@Test
	void visionGoesFrom20To120Percent() {
		assertEquals(0, PassiveRules.vision(0));
		assertEquals(0.2, PassiveRules.vision(1), 1e-9);
		assertEquals(1.2, PassiveRules.vision(10), 1e-9);
		assertEquals("Increase vision 31.1%", PassiveRules.title(Passive.VISION, 2));
		assertEquals("Increase vision 120%", PassiveRules.title(Passive.VISION, 10));
	}

	@Test
	void hungerFloorIsInDrumsticks() {
		assertEquals(5, PassiveRules.hungerFloor(1)); // 2.5 drumsticks
		assertEquals(7, PassiveRules.hungerFloor(3)); // above 6: sprinting always works
		assertEquals(14, PassiveRules.hungerFloor(10)); // 7 drumsticks
		assertEquals("Hunger sustain 2.5", PassiveRules.title(Passive.HUNGER, 1));
		assertEquals("Hunger sustain 7", PassiveRules.title(Passive.HUNGER, 10));
	}

	@Test
	void xpBonusRoundsTheFractionByChance() {
		assertEquals(30, PassiveRules.xpPercent(10));
		assertEquals(130, PassiveRules.boostedXp(100, 10, 0.99));
		assertEquals(3, PassiveRules.boostedXp(3, 1, 0.99)); // 3% of 3 = 0.09: no bonus at roll 0.99
		assertEquals(4, PassiveRules.boostedXp(3, 1, 0.01)); // ... one extra point at roll 0.01
		assertEquals(5, PassiveRules.boostedXp(5, 0, 0.0));
	}

	@Test
	void inventorySlotsAddUpInTheAgreedOrder() {
		assertEquals(List.of(), PassiveRules.protectedSlots(0, 4));
		assertEquals(List.of(4), PassiveRules.protectedSlots(1, 4));
		assertEquals(List.of(4, 40, 39), PassiveRules.protectedSlots(3, 4));
		assertEquals(List.of(4, 40, 39, 36, 38, 37), PassiveRules.protectedSlots(6, 4));
		assertEquals(14, PassiveRules.protectedSlots(7, 4).size()); // hotbar holds the main hand already
		assertEquals(41, PassiveRules.protectedSlots(10, 0).size());
		assertTrue(PassiveRules.protectedSlots(8, 0).contains(17));
		assertFalse(PassiveRules.protectedSlots(8, 0).contains(18));
		assertEquals("+Hotbar", PassiveRules.tableValue(Passive.INVENTORY, 7));
	}

	@Test
	void dataClampsAndToggles() {
		PassiveData d = PassiveData.DEFAULT.withLevel(Passive.XP, 99);
		assertEquals(10, d.level(Passive.XP));
		assertEquals(0, d.activeVision());
		d = d.withLevel(Passive.VISION, 10);
		assertEquals(1.2, d.activeVision(), 1e-9);
		assertEquals(0, d.withVisionOn(false).activeVision());
	}

	@Test
	void growthIsTwentyPercentPerLevel() {
		assertEquals(0, PassiveRules.growthPercent(0));
		assertEquals(20, PassiveRules.growthPercent(1));
		assertEquals(200, PassiveRules.growthPercent(10));
		assertEquals(0, PassiveRules.extraGrowth(0, 0.0));
		assertEquals(1, PassiveRules.extraGrowth(1, 0.19)); // 20% chance of one extra attempt
		assertEquals(0, PassiveRules.extraGrowth(1, 0.2));
		assertEquals(1, PassiveRules.extraGrowth(6, 0.5)); // 120%: one, plus 20% chance of a second
		assertEquals(2, PassiveRules.extraGrowth(6, 0.1));
		assertEquals(2, PassiveRules.extraGrowth(10, 0.99)); // 200%: always two
		assertEquals("+200%", PassiveRules.value(Passive.GROWTH, 10));
	}

	@Test
	void growthSwitch() {
		PassiveData d = PassiveData.DEFAULT.withLevel(Passive.GROWTH, 4);
		assertTrue(d.isOn(Passive.GROWTH));
		assertEquals(4, d.activeLevel(Passive.GROWTH));
		assertEquals(0, d.withOn(Passive.GROWTH, false).activeLevel(Passive.GROWTH));
		assertTrue(d.withOn(Passive.GROWTH, false).isOn(Passive.VISION), "the vision switch is separate");
	}

	@Test
	void itemGuardDebt() {
		assertEquals(100, PassiveRules.debtPercent(1));
		assertEquals(60, PassiveRules.debtPercent(5));
		assertEquals(10, PassiveRules.debtPercent(10));
		assertEquals(1561, PassiveRules.debt(1, 1561)); // diamond pickaxe
		assertEquals(937, PassiveRules.debt(5, 1561)); // 60%, rounded up
		assertEquals(157, PassiveRules.debt(10, 1561)); // 156.1 rounded up
		assertEquals("max debt 70%", PassiveRules.value(Passive.GUARD, 4));
		assertEquals("Item guard max debt 10%", PassiveRules.title(Passive.GUARD, 10));
		assertEquals("item_guard", Passive.GUARD.id());
		assertEquals(true, Passive.GUARD.toggleable());
		assertEquals(false, PassiveData.DEFAULT.withOn(Passive.GUARD, false).isOn(Passive.GUARD));
	}

	@Test
	void itemGuardDebtGrowsWithWearUpToTheCap() { // D179
		assertEquals(0, ItemGuardRules.grow(0, 0, 157)); // just broke: no debt
		assertEquals(5, ItemGuardRules.grow(0, 5, 157));
		assertEquals(157, ItemGuardRules.grow(150, 20, 157)); // capped
		assertEquals(157, ItemGuardRules.grow(157, 1, 157));
		assertEquals(900, ItemGuardRules.grow(900, 10, 157)); // above a lower cap (level went up): stays, never grows
	}

	@Test
	void itemGuardRepairPaysDebtFirst() {
		// a diamond pickaxe broke at Lv 5: damage 1561, debt 937
		var r = ItemGuardRules.repair(937, 1561, 500);
		assertEquals(437, r.debt());
		assertEquals(1561, r.damage()); // nothing gained yet
		assertEquals(true, r.broken(1561));
		r = ItemGuardRules.repair(437, 1561, 1000);
		assertEquals(0, r.debt());
		assertEquals(998, r.damage()); // 563 left over becomes durability
		assertEquals(false, r.broken(1561));
		r = ItemGuardRules.repair(100, 1561, 100); // debt gone but still 0 durability
		assertEquals(0, r.debt());
		assertEquals(true, r.broken(1561));
		r = ItemGuardRules.repairTo(157, 1561, 1000); // vanilla wants damage 1000 = 561 points
		assertEquals(0, r.debt());
		assertEquals(1157, r.damage());
		assertEquals(157, ItemGuardRules.debtPaid(157, 9999));
	}
}
