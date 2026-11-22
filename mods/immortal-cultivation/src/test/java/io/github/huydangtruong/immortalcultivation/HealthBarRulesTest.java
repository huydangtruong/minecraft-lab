package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class HealthBarRulesTest {
	@Test
	void heartAndBarAreAsWideAsTheArmourBar() { // D203
		assertEquals(81, HealthBarRules.WIDTH);
		assertEquals(HealthBarRules.WIDTH, HealthBarRules.ICON + HealthBarRules.GAP + HealthBarRules.BAR_WIDTH);
		assertEquals(70, HealthBarRules.BAR_WIDTH);
	}

	@Test
	void theHeartShowsTheLayerTheHealthReaches() {
		assertEquals(0, HealthBarRules.layer(0));
		assertEquals(0, HealthBarRules.layer(1));
		assertEquals(0, HealthBarRules.layer(40));
		assertEquals(1, HealthBarRules.layer(41));
		assertEquals(2, HealthBarRules.layer(81));
		assertEquals(14, HealthBarRules.layer(600));
		assertEquals(14, HealthBarRules.layer(5000)); // never past the last colour
	}

	@Test
	void theRowKeepsTheColourOfFullHealth() { // D216
		assertEquals(0, HealthBarRules.rowLayer(20f));
		assertEquals(1, HealthBarRules.rowLayer(60f));
		assertEquals(4, HealthBarRules.rowLayer(200f));
		assertEquals(14, HealthBarRules.rowLayer(5000f));
	}

	@Test
	void everyLayerHasItsOwnBarColour() {
		for (int i = 0; i < 15; i++) {
			for (int j = i + 1; j < 15; j++) {
				assertNotEquals(HealthBarRules.layerColor(i), HealthBarRules.layerColor(j), i + " vs " + j);
			}
		}
		assertEquals(HealthBarRules.layerColor(14), HealthBarRules.layerColor(99));
	}

	@Test
	void fillIsHealthOverMaxHealth() {
		assertEquals(1f, HealthBarRules.fill(20, 20), 1e-6);
		assertEquals(0.5f, HealthBarRules.fill(10, 20), 1e-6);
		assertEquals(0f, HealthBarRules.fill(0, 20), 1e-6);
		assertEquals(1f, HealthBarRules.fill(30, 20), 1e-6); // over max (just after a max-health drop)
		assertEquals(0f, HealthBarRules.fill(5, 0), 1e-6);
		assertEquals(34, HealthBarRules.fillPixels(10, 20, 68));
		assertEquals(1, HealthBarRules.fillPixels(0.1f, 300, 68)); // alive shows at least a pixel
		assertEquals(0, HealthBarRules.fillPixels(0, 300, 68));
		assertEquals(68, HealthBarRules.fillPixels(300, 300, 68));
	}

	@Test
	void absorptionIsAShareOfMaxHealthCappedAtTheBar() {
		assertEquals(17, HealthBarRules.absorptionPixels(4, 16, 68));
		assertEquals(68, HealthBarRules.absorptionPixels(40, 20, 68));
	}

	@Test
	void armourSitsRightAboveTheOneRowWhateverTheHealth() {
		assertEquals(0, HealthBarRules.armorShift(20, 20, 0)); // vanilla already has it right above one row
		assertEquals(0, HealthBarRules.armorShift(20, 10, 0));
		assertEquals(10, HealthBarRules.armorShift(20, 20, 4)); // vanilla added a row for 2 golden hearts: move it back down
		assertEquals(10, HealthBarRules.armorShift(40, 40, 0)); // 2 rows
		assertEquals(18, HealthBarRules.armorShift(60, 60, 0)); // 3 rows at spacing 9
		assertEquals(18, HealthBarRules.armorShift(40, 60, 0)); // health above max still counts, as in vanilla
	}
}
