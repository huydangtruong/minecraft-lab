package io.github.huydangtruong.permahearts.technique;

import java.util.Optional;

/** The techniques every player can learn (D45, D46, D52), in list order. Pure Java: numbers live in TechniqueRules. */
public enum Technique {
	FIRE("fire", "Fireball", 16, true), // names: D56
	ICE("ice", "Frozen Breath", 24, true),
	HEALING("healing", "Healing Light", 60, false),
	PHASE("phase", "Shadow Walk", 90, false),
	REPULSE("repulse", "Heaven Blast", 20, false),
	SACRIFICE("sacrifice", "Undying Jutsu", 45, false), // D47: was 30 s
	FLIGHT("flight", "Cloud Travel", 120, false), // D52
	BLADE("blade", "Life Steal Blade", 12, false),
	TELEPORT("teleport", "Thousand Mile Step", 60, true), // D58: charged throw like Fireball, then the caster teleports // D52: held like a weapon; each landed hit starts the 12 s cooldown
	GOLEM("golem", "Violet Guardian", 180, false), // D60: summons loyal violet iron golems
	ANGEL("angel", "Warrior Angel", 120, false), // D63: summons gold-and-white angels that fly through walls
	AQUA("aqua", "Aqua Marine", 120, false); // D64: breathe, swim fast and dig fast underwater for a while

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
