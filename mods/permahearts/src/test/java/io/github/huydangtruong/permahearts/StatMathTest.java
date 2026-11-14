package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.permahearts.stats.PlayerStats;
import io.github.huydangtruong.permahearts.stats.StatMath;
import io.github.huydangtruong.permahearts.stats.StatMath.Mod;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatMathTest {
	@Test
	void damageWithSwordAndStrength() {
		// base 1, diamond sword +6, one Strength elixir +0.5
		PlayerStats.Row r = StatMath.split("damage", 1, List.of(new Mod(6, 0, false), new Mod(0.5, 0, true)));
		assertEquals(7.5, r.total(), 1e-9);
		assertArrayEquals(new String[] {"7.5", "1", "+6", "+0.5"}, StatMath.parts(r));
	}

	@Test
	void attackSpeedMultiplierElixir() {
		// base 4, sword -2.4, Fury +10%: vanilla multiplies base after flat adds: 1.6 * 1.1 = 1.76
		PlayerStats.Row r = StatMath.split("attack_speed", 4, List.of(new Mod(-2.4, 0, false), new Mod(0.1, 1, true)));
		assertEquals(1.76, r.total(), 1e-9);
		assertArrayEquals(new String[] {"1.76", "4", "-2.4", "+0.16"}, StatMath.parts(r));
	}

	@Test
	void speedAsPercentWithEffect() {
		// base 0.1, Speed I +20% total, Swiftness elixir +5% base
		PlayerStats.Row r = StatMath.split("speed", 0.1, List.of(new Mod(0.2, 2, false), new Mod(0.05, 1, true)));
		assertArrayEquals(new String[] {"126%", "100", "+20", "+6"}, StatMath.parts(r));
	}

	@Test
	void heartsShownAsHearts() {
		PlayerStats.Row r = StatMath.split("hearts", 20, List.of(new Mod(10, 0, true)));
		assertArrayEquals(new String[] {"15", "10", "+0", "+5"}, StatMath.parts(r));
	}

	@Test
	void numbers() {
		assertEquals("0", StatMath.num(-0.0001));
		assertEquals("4.5", StatMath.num(4.5));
		assertEquals("0.25", StatMath.num(0.25));
		assertEquals("+0", StatMath.signed(0));
		assertEquals("-2.4", StatMath.signed(-2.4));
	}

	@Test
	void bladeBaseIgnoresGearButKeepsCultivation() {
		// base 1, diamond sword +6 (gear), Strength elixirs +1.5: the blade sees 2.5
		assertEquals(2.5, StatMath.baseWithMine(1, List.of(new Mod(6, 0, false), new Mod(1.5, 0, true))), 1e-9);
		assertEquals(1, StatMath.baseWithMine(1, List.of(new Mod(6, 0, false))), 1e-9);
	}
}
