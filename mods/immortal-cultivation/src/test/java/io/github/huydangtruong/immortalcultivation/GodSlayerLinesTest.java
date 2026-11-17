package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerLines;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GodSlayerLinesTest {
	@Test
	void eachCaseHasThirtyDistinctLines() {
		for (List<String> lines : List.of(GodSlayerLines.WIN, GodSlayerLines.LOSE, GodSlayerLines.SCORN)) {
			assertEquals(30, lines.size());
			assertEquals(30, new HashSet<>(lines).size());
		}
	}

	@Test
	void picksComeFromTheRightList() {
		Random r = new Random(1);
		for (int i = 0; i < 100; i++) {
			assertTrue(GodSlayerLines.WIN.contains(GodSlayerLines.win(r)));
			assertTrue(GodSlayerLines.LOSE.contains(GodSlayerLines.lose(r)));
			assertTrue(GodSlayerLines.SCORN.contains(GodSlayerLines.scorn(r)));
		}
	}
}
