package io.github.huydangtruong.immortalcultivation.godslayer;

import java.util.ArrayList;
import java.util.List;

/** The skill list of the God Slayer. Lv 1 has only the slash; P-033 adds the rest here. */
public final class GodSlayerSkills {
	private GodSlayerSkills() {}

	/** Fresh skill instances for a boss of this level (those whose unlock level is reached). */
	public static List<GodSlayerSkill> forLevel(int level) {
		List<GodSlayerSkill> all = new ArrayList<>();
		all.add(new SlashSkill());
		// P-033: more skills are added to the list above.
		List<GodSlayerSkill> out = new ArrayList<>();
		for (GodSlayerSkill s : all) {
			if (s.unlockLevel() <= level) {
				out.add(s);
			}
		}
		return out;
	}
}
