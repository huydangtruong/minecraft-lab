package io.github.huydangtruong.permahearts.stats;

import java.util.List;

/**
 * What the stats panel shows (D31), computed on the server once a second and synced to that player only.
 * vitality: Elixir of Vitality level. spawn: the respawn point (bed / anchor) or the world spawn.
 */
public record PlayerStats(List<Row> rows, int vitality, Spawn spawn) {
	public static final PlayerStats EMPTY = new PlayerStats(List.of(), 0, new Spawn(0, 0, 0, "minecraft:overworld", false));

	/** total = base + gear + bonus (bonus = this mod: elixirs, bonus hearts). */
	public record Row(String id, double total, double base, double gear, double bonus) {}

	/** personal: a bed or respawn anchor; false: the world spawn. */
	public record Spawn(int x, int y, int z, String dimension, boolean personal) {}
}
