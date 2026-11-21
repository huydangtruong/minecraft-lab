package io.github.huydangtruong.immortalcultivation.passive;

import java.util.Optional;

/** The passives of the Passives tab (D148). Pure Java. The id is saved: never rename one. */
public enum Passive {
	COOLDOWN("cooldown", "Reduce cooldown"),
	VISION("vision", "Increase vision"),
	HUNGER("hunger", "Hunger sustain"),
	XP("xp", "XP gain bonus"),
	GROWTH("growth", "Crop growth"),
	INVENTORY("inventory", "Inventory protect"),
	GUARD("item_guard", "Item guard");

	private final String id;
	private final String displayName;

	Passive(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	/** Passives with an on/off switch in the tab. */
	public boolean toggleable() {
		return this == VISION || this == GROWTH || this == GUARD;
	}

	public static Optional<Passive> byId(String id) {
		for (Passive p : values()) {
			if (p.id.equals(id)) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}
}
