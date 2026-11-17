package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmokeTest {
	@Test
	void modIdAndNamespaceAreStable() {
		// The data namespace must never change (saves, options); the mod id is the rename (D132).
		assertEquals("permahearts", ImmortalCultivation.NAMESPACE);
		assertEquals("immortal-cultivation", ImmortalCultivation.MOD_ID);
	}
}
