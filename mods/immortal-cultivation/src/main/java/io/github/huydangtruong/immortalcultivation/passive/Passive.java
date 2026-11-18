package io.github.huydangtruong.immortalcultivation.passive;

import java.util.Optional;

/** The five passives of the Passives tab (D148). Pure Java. The id is saved: never rename one. */
public enum Passive {
	COOLDOWN("cooldown", "Reduce cooldown"),
	VISION("vision", "Increase vision"),
	HUNGER("hunger", "Hunger sustain"),
	XP("xp", "XP gain bonus"),
	INVENTORY("inventory", "Inventory protect");

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

	public static Optional<Passive> byId(String id) {
		for (Passive p : values()) {
			if (p.id.equals(id)) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}
}
