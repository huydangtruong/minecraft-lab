package io.github.huydangtruong.immortalcultivation.passive;

/**
 * Item guard (D176, D179), pure Java. A "broken" stack sits at full damage (0 durability) and carries a debt in durability
 * points: 0 when it breaks, then every further point of wear adds to it up to the cap of the passive level. Repair pays
 * the debt first. The stack works again when the debt is 0 and it has at least 1 durability.
 */
public final class ItemGuardRules {
	private ItemGuardRules() {}

	/** Result of a repair: the debt left and the damage value after it. */
	public record Repaired(int debt, int damage) {
		public boolean broken(int maxDamage) {
			return stillBroken(debt, damage, maxDamage);
		}
	}

	/** D179: debt after {@code wear} more points on a broken stack, never above {@code cap} (a debt already above it stays). */
	public static int grow(int debt, int wear, int cap) {
		if (wear <= 0 || debt >= cap) {
			return debt;
		}
		return (int) Math.min(cap, (long) debt + wear);
	}

	/** A broken item is back in use only when nothing is owed and it has durability again. */
	public static boolean stillBroken(int debt, int damage, int maxDamage) {
		return debt > 0 || damage >= maxDamage;
	}

	/** Durability points a repair of {@code amount} pays on the debt. */
	public static int debtPaid(int debt, int amount) {
		return Math.max(0, Math.min(debt, amount));
	}

	/** Repair split: {@code amount} points of repair first pay the debt, the rest lowers the damage. */
	public static Repaired repair(int debt, int damage, int amount) {
		int paid = debtPaid(debt, amount);
		int gained = Math.max(0, amount - paid);
		return new Repaired(debt - paid, Math.max(0, damage - gained));
	}

	/** Same, from the damage value a vanilla repair wants to set (newDamage below the current damage). */
	public static Repaired repairTo(int debt, int damage, int newDamage) {
		return repair(debt, damage, Math.max(0, damage - Math.max(0, newDamage)));
	}
}
