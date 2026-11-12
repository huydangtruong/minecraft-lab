package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.permahearts.wanderer.StayTime;
import org.junit.jupiter.api.Test;

class StayTimeTest {
	@Test
	void threeDays() {
		assertEquals("3 days 0 h (≈ 60 min)", StayTime.format(72000));
		assertEquals("2 days 0 h (≈ 40 min)", StayTime.format(48000));
	}

	@Test
	void oneDayOneHour() {
		assertEquals("1 day 1 h (≈ 21 min)", StayTime.format(25200));
	}

	@Test
	void underOneMinute() {
		assertEquals("0 h (≈ 30 s)", StayTime.format(600));
	}

	@Test
	void underADay() {
		assertEquals("7 h (≈ 5 min)", StayTime.format(7000));
	}

	@Test
	void neverNegative() {
		assertEquals("0 h (≈ 0 s)", StayTime.format(-5));
		assertEquals("0 h (≈ 0 s)", StayTime.format(0));
	}
}
