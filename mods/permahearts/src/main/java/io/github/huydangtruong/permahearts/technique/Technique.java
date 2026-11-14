package io.github.huydangtruong.permahearts.technique;

import java.util.Optional;

/** The techniques every player can learn (D45, D46), in list order. Pure Java: numbers live in TechniqueRules. */
public enum Technique {
	FIRE("fire", "Flame Burst", 16, true),
	ICE("ice", "Frost Orb", 24, true),
	HEALING("healing", "Healing Light", 60, false),
	PHASE("phase", "Phase Walk", 90, false),
	REPULSE("repulse", "Repulse", 20, false),
	SACRIFICE("sacrifice", "Blood Oath", 30, false);

	private final String id;
	private final String displayName;
	private final int cooldownSeconds;
	private final boolean charged;

	Technique(String id, String displayName, int cooldownSeconds, boolean charged) {
		this.id = id;
		this.displayName = displayName;
		this.cooldownSeconds = cooldownSeconds;
		this.charged = charged;
	}

	public String id() {
		return id;
	}

	/** English in-game name (D16). */
	public String displayName() {
		return displayName;
	}

	public int cooldownSeconds() {
		return cooldownSeconds;
	}

	public int cooldownTicks() {
		return cooldownSeconds * 20;
	}

	/** True: thrown with a charge (hold right click like a bow; left click = full charge). False: cast at once. */
	public boolean charged() {
		return charged;
	}

	public static Optional<Technique> byId(String id) {
		for (Technique t : values()) {
			if (t.id.equals(id)) {
				return Optional.of(t);
			}
		}
		return Optional.empty();
	}
}
