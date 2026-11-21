package io.github.huydangtruong.immortalcultivation.technique;

/**
 * D167: pure rules of a grasped Witch's potions: which potion for which target, and which entities a splash may touch.
 * No Minecraft imports, JUnit-tested. The glue (throwing, the splash filter) lives in {@code technique.grasp.WitchPotions}.
 */
public final class WitchRules {
	/** Allies below this health fraction get a healing potion. */
	public static final float ALLY_HURT_BELOW = 0.75f;
	/** At or below this fraction the potion is Healing, above it Regeneration. */
	public static final float ALLY_HEAL_BELOW = 0.5f;
	/** How far (blocks) the witch looks for a hurt ally. */
	public static final double ALLY_RANGE = 10;
	/** Ticks between two throws at allies (vanilla's ranged attack is 60). */
	public static final int ALLY_COOLDOWN_TICKS = 40;

	public enum Potion { HEALING, REGENERATION, SLOWNESS, WEAKNESS, POISON, HARMING }

	/** Who a thrown splash is meant for. ALLY_LIVING = allies, but not undead (Healing would hurt them). */
	public enum Intent { ENEMY, ALLY, ALLY_LIVING }

	private WitchRules() {}

	/** True when an ally at this health fraction (0 to 1) needs a potion. */
	public static boolean needsHealing(float fraction) {
		return fraction < ALLY_HURT_BELOW;
	}

	/** Potion for a hurt ally: Healing at half health or less (or Regeneration for an undead ally, Healing would hurt it). */
	public static Potion allyPotion(float fraction, boolean undead) {
		return fraction <= ALLY_HEAL_BELOW && !undead ? Potion.HEALING : Potion.REGENERATION;
	}

	/** The intent tag of an ally potion: Healing must skip undead allies. */
	public static Intent allyIntent(Potion p) {
		return p == Potion.HEALING ? Intent.ALLY_LIVING : Intent.ALLY;
	}

	/**
	 * Potion for an enemy. Undead get Healing (it hurts them; poison does nothing). Others: Slowness while not slowed and not
	 * adjacent, sometimes Weakness, Poison while healthy enough, else Harming.
	 *
	 * @param roll a random number in [0, 1)
	 */
	public static Potion enemyPotion(boolean undead, boolean slowed, boolean weak, boolean poisoned, float health, double distance, float roll) {
		if (undead) {
			return Potion.HEALING;
		}
		if (!slowed && distance >= 4) {
			return Potion.SLOWNESS;
		}
		if (!weak && roll < 0.3f) {
			return Potion.WEAKNESS;
		}
		if (health >= 8 && !poisoned) {
			return Potion.POISON;
		}
		return Potion.HARMING;
	}

	/**
	 * Whether a splash with this intent may touch an entity. bystander = a player who is not the owner's Death Match
	 * opponent (neither ally nor enemy, so an enemy potion leaves him alone).
	 */
	public static boolean affects(Intent intent, boolean ally, boolean undead, boolean bystander) {
		return switch (intent) {
			case ENEMY -> !ally && !bystander;
			case ALLY -> ally;
			case ALLY_LIVING -> ally && !undead;
		};
	}
}
