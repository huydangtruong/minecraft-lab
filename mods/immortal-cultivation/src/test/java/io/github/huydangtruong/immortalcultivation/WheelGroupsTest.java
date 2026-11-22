package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.WheelGroups;
import org.junit.jupiter.api.Test;

class WheelGroupsTest {
	@Test
	void everyTechniqueIsInExactlyOneGroup() {
		for (Technique t : Technique.values()) {
			int count = 0;
			for (WheelGroups.Group g : WheelGroups.GROUPS) {
				for (Technique m : g.members()) {
					if (m == t) {
						count++;
					}
				}
			}
			assertEquals(1, count, t + " must be in exactly one wheel group");
		}
	}
}
