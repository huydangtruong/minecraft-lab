package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ElixirStatTest {
	@Test
	void bonusScalesWithLevel() {
		assertEquals(1.5, ElixirStat.STRENGTH.bonus(3), 1e-9);
		assertEquals(0.2, ElixirStat.FURY.bonus(2), 1e-9);
	}

	@Test
	void vitalityFoodHeal() {
		assertEquals(0f, ElixirStat.foodHeal(8, 0), 1e-6);
		assertEquals(2f, ElixirStat.foodHeal(8, 1), 1e-6); // steak, 1 bottle: +1 heart
		assertEquals(8f, ElixirStat.foodHeal(8, 4), 1e-6);
		assertEquals(1f, ElixirStat.foodHeal(4, 1), 1e-6); // apple
		assertEquals("+25%", ElixirStat.VITALITY.format(ElixirStat.VITALITY.bonus(1)));
		assertEquals(ElixirStat.VITALITY, ElixirStat.byId("vitality").orElseThrow());
	}

	@Test
	void byId() {
		assertEquals(ElixirStat.REACH, ElixirStat.byId("reach").orElseThrow());
		assertTrue(ElixirStat.byId("nope").isEmpty());
	}

	@Test
	void format() {
		assertEquals("+1.5", ElixirStat.STRENGTH.format(1.5));
		assertEquals("+20%", ElixirStat.FURY.format(0.2));
	}

	@Test
	void plusFromEmptyAndTwice() {
		CultivationData d = CultivationData.DEFAULT.plus(ElixirStat.IRON_SKIN, 1);
		assertEquals(1, d.level(ElixirStat.IRON_SKIN));
		assertEquals(2, d.plus(ElixirStat.IRON_SKIN, 1).level(ElixirStat.IRON_SKIN));
		assertEquals(0, d.level(ElixirStat.MINING));
	}

	@Test
	void noCapAndNeverNegative() {
		assertEquals(1000, CultivationData.DEFAULT.plus(ElixirStat.STRENGTH, 1000).level(ElixirStat.STRENGTH));
		assertEquals(0, CultivationData.DEFAULT.plus(ElixirStat.STRENGTH, -5).level(ElixirStat.STRENGTH));
	}

	@Test
	void codecKeepsUnknownKey() {
		CultivationData d = new CultivationData(Map.of("strength", 2, "future_stat", 7));
		var json = CultivationData.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
		CultivationData back = CultivationData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(d, back);
		assertEquals(7, back.levels().get("future_stat"));
		assertEquals(CultivationData.DEFAULT, CultivationData.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow());
		// unknown key survives a later level-up
		assertEquals(7, back.plus(ElixirStat.STRENGTH, 1).levels().get("future_stat"));
	}
}
