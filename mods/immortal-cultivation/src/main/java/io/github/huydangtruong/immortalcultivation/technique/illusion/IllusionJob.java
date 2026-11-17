package io.github.huydangtruong.immortalcultivation.technique.illusion;

/**
 * What an Illusion clone does (D73), chosen once at the cast from the item in the owner's first hotbar slot (D85; an empty slot gives FIGHT). Pure Java so the
 * item table can be unit-tested on item ids.
 */
public enum IllusionJob {
	/** Anything else: fight and follow the owner, like P-024. */
	FIGHT,
	FARM,
	FISH,
	CHOP,
	SHEAR,
	MILK,
	BREED;

	/** True for the jobs that work in the work area (all but FIGHT). */
	public boolean works() {
		return this != FIGHT;
	}

	/** The job for an item id such as {@code minecraft:iron_hoe}. Items of other namespaces and unknown items give FIGHT. */
	public static IllusionJob fromItemId(String id) {
		if (id == null) {
			return FIGHT;
		}
		String path = id;
		int colon = id.indexOf(':');
		if (colon >= 0) {
			if (!id.startsWith("minecraft:")) {
				return FIGHT;
			}
			path = id.substring(colon + 1);
		}
		if (path.endsWith("_hoe")) {
			return FARM;
		}
		if (path.endsWith("_axe")) { // "_pickaxe" does not end in "_axe"
			return CHOP;
		}
		return switch (path) {
			case "fishing_rod" -> FISH;
			case "shears" -> SHEAR;
			case "bucket" -> MILK;
			case "wheat", "carrot", "potato" -> BREED;
			default -> FIGHT;
		};
	}
}
