package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.warp.PortalRecord;
import io.github.huydangtruong.permahearts.technique.warp.WarpColor;
import io.github.huydangtruong.permahearts.technique.warp.WarpNaming;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** P-026: the pure rules of Quantum Leap's rename and Cosmic Warping (D74, D75). */
class WarpRulesTest {
	@Test
	void teleportIsQuantumLeapButKeepsItsId() {
		assertEquals("Quantum Leap", Technique.TELEPORT.displayName());
		assertEquals("teleport", Technique.TELEPORT.id());
	}

	@Test
	void warpTechniqueExists() {
		assertEquals("warp", Technique.WARP.id());
		assertEquals("Cosmic Warping", Technique.WARP.displayName());
	}

	@Test
	void portalLimitGrowsFromTwoToTwenty() {
		assertEquals(2, TechniqueRules.warpMaxPortals(1));
		assertEquals(20, TechniqueRules.warpMaxPortals(20));
		for (int l = 2; l <= 20; l++) {
			assertTrue(TechniqueRules.warpMaxPortals(l) >= TechniqueRules.warpMaxPortals(l - 1), "never fewer at level " + l);
		}
	}

	@Test
	void cooldownShrinksFrom160To65Seconds() {
		assertEquals(160, TechniqueRules.warpCooldownSeconds(1));
		assertEquals(65, TechniqueRules.warpCooldownSeconds(20));
		assertEquals(160 * 20, TechniqueRules.cooldownTicks(Technique.WARP, 1));
		assertEquals(65 * 20, TechniqueRules.cooldownTicks(Technique.WARP, 20));
	}

	@Test
	void duplicateNamesGetANumberSuffix() {
		assertEquals("Plains", WarpNaming.unique("Plains", List.of()));
		assertEquals("Plains 2", WarpNaming.unique("Plains", List.of("Plains")));
		assertEquals("Plains 3", WarpNaming.unique("Plains", List.of("Plains", "Plains 2")));
		assertEquals("Plains", WarpNaming.unique("Plains", List.of("Plains 2")));
	}

	@Test
	void namesAreReadable() {
		assertEquals("Cherry Grove", WarpNaming.readable("minecraft:cherry_grove"));
		assertEquals("Village", WarpNaming.structureName("minecraft:village_desert"));
		assertEquals("Ancient City", WarpNaming.structureName("minecraft:ancient_city"));
		assertEquals("12, 64, -30 (Nether)", WarpNaming.coordinates(12, 64, -30, "minecraft:the_nether"));
		assertEquals("1, 2, 3", WarpNaming.coordinates(1, 2, 3, "minecraft:overworld"));
	}

	@Test
	void colourIsStableAndNeverPurple() {
		for (int i = 0; i < 2000; i++) {
			UUID u = new UUID(i * 7919L, i * 104729L ^ 0x5DEECE66DL);
			double hue = WarpColor.hue(u);
			assertEquals(hue, WarpColor.hue(u), 0.0);
			assertEquals(WarpColor.rgb(u), WarpColor.rgb(u));
			assertTrue(hue < WarpColor.PURPLE_FROM || hue > WarpColor.PURPLE_TO, "hue " + hue + " is purple");
		}
	}

	@Test
	void portalListRoundTrips() {
		UUID owner = UUID.randomUUID();
		PortalRecord a = new PortalRecord(UUID.randomUUID(), owner, "minecraft:overworld", 1.5, 64, -3.5, 90f, "Plains", 10);
		PortalRecord b = new PortalRecord(UUID.randomUUID(), owner, "minecraft:the_nether", 8, 70, 8, 0f, "Basalt Deltas", 5);
		PortalRecord.All all = new PortalRecord.All(List.of(a, b));
		var json = PortalRecord.All.CODEC.encodeStart(JsonOps.INSTANCE, all).getOrThrow();
		PortalRecord.All back = PortalRecord.All.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(all, back);
		assertEquals(List.of(b, a), back.of(owner)); // oldest first
		assertNotNull(back.find(a.id()));
		assertEquals(1, back.without(a.id()).portals().size());
	}
}
