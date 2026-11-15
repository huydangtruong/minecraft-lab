package io.github.huydangtruong.permahearts.technique;

import java.util.Optional;

/** The techniques every player can learn (D45, D46, D52), in list order. Pure Java: numbers live in TechniqueRules. */
public enum Technique {
	FIRE("fire", "Fireball", 16, true), // names: D56
	ICE("ice", "Frozen Breath", 18, true), // D80: was 24 s
	HEALING("healing", "Healing Domain", 40, false), // D80: was Healing Light, 60 s
	PHASE("phase", "Shadow Walk", 60, false), // D80: was 90 s
	REPULSE("repulse", "Heaven Blast", 20, false),
	SACRIFICE("sacrifice", "Undying Jutsu", 45, false), // D47: was 30 s
	FLIGHT("flight", "Cloud Travel", 80, false), // D52; D80: was 120 s
	BLADE("blade", "Life Steal Blade", 12, false),
	TELEPORT("teleport", "Quantum Leap", 40, true), // D80: was 60 s; // D58, D74 (was Thousand Mile Step): charged throw like Fireball, then the caster teleports // D52: held like a weapon; each landed hit starts the 12 s cooldown
	GOLEM("golem", "Violet Guardian", 120, false), // D80: was 180 s; // D60: summons loyal violet iron golems
	ANGEL("angel", "Warrior Angel", 120, false), // D63: summons gold-and-white angels that fly through walls
	AQUA("aqua", "Aqua Marine", 100, false), // D80: was 120 s; // D64: breathe, swim fast and dig fast underwater for a while
	ILLUSION("illusion", "Illusion", 110, false), // D71: clones of the caster that fight for them; D73: they also work (110 s, was 100)
	WARP("warp", "Cosmic Warping", 90, true), // D75: portal network; the real cooldown falls with the level (TechniqueRules.cooldownSeconds)
	THUNDER("thunder", "Thunder Strike", 24, false); // D86: aim, 1 s warning, then lightning and a creeper-sized blast

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
