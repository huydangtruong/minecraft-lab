package io.github.huydangtruong.permahearts;

import java.util.List;

/** The numbers behind /testmode full (D92). No Minecraft imports so JUnit can test it. */
public final class TestModeRules {
	public static final int HEARTS = 50;
	public static final int ELIXIRS_EACH = 10;

	/** One enchantment by its vanilla path ("protection", "fire_aspect"...) and level. */
	public record Ench(String id, int level) {}

	/** One piece of gear: item path and its two enchantments. */
	public record Gear(String item, boolean armor, List<Ench> enchants) {}

	public static final List<Gear> GEAR = List.of(
		new Gear("netherite_helmet", true, List.of(new Ench("protection", 4), new Ench("unbreaking", 3))),
		new Gear("netherite_chestplate", true, List.of(new Ench("protection", 4), new Ench("unbreaking", 3))),
		new Gear("netherite_leggings", true, List.of(new Ench("protection", 4), new Ench("unbreaking", 3))),
		new Gear("netherite_boots", true, List.of(new Ench("protection", 4), new Ench("unbreaking", 3))),
		new Gear("netherite_sword", false, List.of(new Ench("sharpness", 5), new Ench("fire_aspect", 2))),
		new Gear("netherite_pickaxe", false, List.of(new Ench("efficiency", 5), new Ench("fortune", 3))),
		new Gear("netherite_axe", false, List.of(new Ench("sharpness", 5), new Ench("efficiency", 5))),
		new Gear("netherite_shovel", false, List.of(new Ench("efficiency", 5), new Ench("unbreaking", 3))),
		new Gear("netherite_hoe", false, List.of(new Ench("efficiency", 5), new Ench("fortune", 3))));

	private TestModeRules() {}
}
