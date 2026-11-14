package io.github.huydangtruong.permahearts.stats;

import java.util.List;
import java.util.Locale;

/**
 * Pure maths for the stats panel (D31): splits an attribute value into base, gear (everything not from this mod:
 * items, effects, other mods) and cultivation (this mod: elixirs, bonus hearts). No Minecraft imports.
 */
public final class StatMath {
	private StatMath() {}

	/** One attribute modifier. op: 0 = add value, 1 = add multiplied base, 2 = add multiplied total (vanilla order). */
	public record Mod(double amount, int op, boolean mine) {}

	/** Same order as vanilla AttributeInstance#calculateValue (without the attribute's clamp). */
	public static double value(double base, List<Mod> mods, boolean withMine) {
		double d = base;
		for (Mod m : mods) {
			if (m.op() == 0 && (withMine || !m.mine())) {
				d += m.amount();
			}
		}
		double e = d;
		for (Mod m : mods) {
			if (m.op() == 1 && (withMine || !m.mine())) {
				e += d * m.amount();
			}
		}
		for (Mod m : mods) {
			if (m.op() == 2 && (withMine || !m.mine())) {
				e *= 1 + m.amount();
			}
		}
		return e;
	}

	/** The value of an attribute from its base and only this mod's modifiers (no gear): what the Life Steal Blade uses. */
	public static double baseWithMine(double base, List<Mod> mods) {
		return value(base, mods.stream().filter(Mod::mine).toList(), true);
	}

	public static PlayerStats.Row split(String id, double base, List<Mod> mods) {
		double total = value(base, mods, true);
		double withoutMine = value(base, mods, false);
		return new PlayerStats.Row(id, total, base, withoutMine - base, total - withoutMine);
	}

	/** How a row is shown: as is, as hearts (health / 2), or as a percent of the base value. */
	public enum Kind { PLAIN, HEARTS, PERCENT }

	public static Kind kind(String id) {
		return switch (id) {
			case "hearts" -> Kind.HEARTS;
			case "speed", "mining" -> Kind.PERCENT;
			default -> Kind.PLAIN;
		};
	}

	/** {total, base, gear, cultivation} as text, e.g. {"10", "1", "+8", "+0.5"} or {"115%", "100", "+10", "+5"}. */
	public static String[] parts(PlayerStats.Row r) {
		double scale = switch (kind(r.id())) {
			case HEARTS -> 0.5;
			case PERCENT -> r.base() == 0 ? 0 : 100 / r.base();
			case PLAIN -> 1;
		};
		String total = num(r.total() * scale) + (kind(r.id()) == Kind.PERCENT ? "%" : "");
		String bonus = signed(r.bonus() * scale);
		if (r.id().equals("attack_speed")) {
			// D61: Fury is +10% of the value after gear, so show it as a percent of that, not as +0.16 next to the sword's -2.4
			double before = r.total() - r.bonus();
			bonus = signed(before == 0 ? 0 : r.bonus() / before * 100) + "%";
		}
		return new String[] {total, num(r.base() * scale), signed(r.gear() * scale), bonus};
	}

	/** Up to 2 decimals, no trailing zeros, "-0" shown as "0". */
	public static String num(double v) {
		double r = Math.round(v * 100) / 100.0;
		if (r == 0) {
			return "0";
		}
		String s = String.format(Locale.ROOT, "%.2f", r);
		s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
		return s;
	}

	public static String signed(double v) {
		String s = num(v);
		return s.startsWith("-") ? s : "+" + s;
	}
}
