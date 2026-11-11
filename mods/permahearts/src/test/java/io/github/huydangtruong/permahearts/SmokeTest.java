package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmokeTest {
	@Test
	void modIdIsStable() {
		assertEquals("permahearts", PermaHearts.MOD_ID);
	}
}
