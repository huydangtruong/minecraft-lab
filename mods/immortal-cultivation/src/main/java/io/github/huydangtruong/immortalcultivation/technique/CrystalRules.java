package io.github.huydangtruong.immortalcultivation.technique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure numbers and geometry of Crystal Prison (D143, P-056). No Minecraft imports, JUnit-tested.
 * The cage is a 3x3x3 hollow with a one-block shell on each of the six faces (3x3 each, no edges, no corners).
 */
public final class CrystalRules {
	public static final int MIN_TIER = 1;
	public static final int MAX_TIER = 20;
	/** Aim range of the left click, in blocks. */
	public static final double AIM_RANGE = 64;
	/** Most living things one cage keeps. */
	public static final int MAX_CAGED = 2;
	/** Entities wider or taller than this are never caged. */
	public static final double MAX_SIZE = 3;
	/** Charge value the client sends with a right click (cage around the caster); the left click sends 0. */
	public static final float SELF_CHARGE = 1f;
	public static final int LIGHT = 7;

	private CrystalRules() {}

	public record Cell(int x, int y, int z) {}

	/** Interior cells (27) and shell cells (54) of a cage. */
	public record Cage(int centerX, int floorY, int centerZ, List<Cell> interior, List<Cell> shell) {}

	private static int tier(int t) {
		return Math.max(MIN_TIER, Math.min(MAX_TIER, t));
	}

	/** Block hardness: 0.3 (like glass) at tier 1 up to 3 (like deepslate) at tier 20, a straight line. */
	public static double crystalHardness(int tier) {
		return 0.3 + (3.0 - 0.3) * (tier(tier) - 1) / (MAX_TIER - 1);
	}

	/** Blast resistance: 0.3 at tier 1 up to 10 at tier 20, a straight line. */
	public static double crystalBlast(int tier) {
		return 0.3 + (10.0 - 0.3) * (tier(tier) - 1) / (MAX_TIER - 1);
	}

	/** Seconds a caged hostile mob cannot attack: 3 at Lv 1, +0.5 per level (12.5 at Lv 20). */
	public static double disarmSeconds(int level) {
		return 3 + 0.5 * (tier(level) - 1);
	}

	/** Seconds the cage stands: 8 at Lv 1, +1 per level (27 at Lv 20). */
	public static double cageSeconds(int level) {
		return 8 + (tier(level) - 1);
	}

	/** True when an entity of this size is too big for the cage. */
	public static boolean tooBig(double width, double height) {
		return width > MAX_SIZE || height > MAX_SIZE;
	}

	/**
	 * The cage around the column (x, z) with its floor layer at floorY: the interior is x-1..x+1, z-1..z+1, floorY+1..floorY+3;
	 * the shell is the floor (y = floorY), the roof (y = floorY+4) and four walls (x-2, x+2, z-2, z+2 for y = floorY+1..floorY+3),
	 * each face 3x3, so 54 cells and no edge or corner.
	 */
	public static Cage cage(int x, int floorY, int z) {
		List<Cell> interior = new ArrayList<>();
		List<Cell> shell = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				shell.add(new Cell(x + dx, floorY, z + dz));
				shell.add(new Cell(x + dx, floorY + 4, z + dz));
				for (int dy = 1; dy <= 3; dy++) {
					interior.add(new Cell(x + dx, floorY + dy, z + dz));
				}
			}
		}
		for (int d = -1; d <= 1; d++) {
			for (int dy = 1; dy <= 3; dy++) {
				shell.add(new Cell(x - 2, floorY + dy, z + d));
				shell.add(new Cell(x + 2, floorY + dy, z + d));
				shell.add(new Cell(x + d, floorY + dy, z - 2));
				shell.add(new Cell(x + d, floorY + dy, z + 2));
			}
		}
		return new Cage(x, floorY, z, interior, shell);
	}

	/** Which of {@code count} candidates stay in the cage: all when at most two, else two picked at random (indices, ascending). */
	public static List<Integer> pickKept(int count, Random rng) {
		List<Integer> all = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			all.add(i);
		}
		if (count <= MAX_CAGED) {
			return all;
		}
		Collections.shuffle(all, rng);
		List<Integer> kept = new ArrayList<>(all.subList(0, MAX_CAGED));
		Collections.sort(kept);
		return kept;
	}

	/**
	 * Where a pushed-out entity of this width goes: just outside the shell on the side it is nearest to. Offsets are from the
	 * cage centre (the middle of the centre block), returned as {dx, dz}.
	 */
	public static double[] pushOutOffset(double fromCentreX, double fromCentreZ, double width) {
		double reach = 2.5 + width / 2 + 0.15;
		if (Math.abs(fromCentreX) >= Math.abs(fromCentreZ)) {
			return new double[] {fromCentreX >= 0 ? reach : -reach, Math.max(-2, Math.min(2, fromCentreZ))};
		}
		return new double[] {Math.max(-2, Math.min(2, fromCentreX)), fromCentreZ >= 0 ? reach : -reach};
	}
}
