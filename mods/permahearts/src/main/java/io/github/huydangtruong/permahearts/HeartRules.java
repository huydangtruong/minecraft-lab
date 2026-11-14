package io.github.huydangtruong.permahearts;

/** Pure rules, no Minecraft imports. */
public final class HeartRules {
	public static final int BASE = 10;
	public static final int MIN = 1;
	public static final int MAX = 100; // D36
	public static final int XP_PER_HEART = 500;

	public enum DeathOutcome { LOST, AT_FLOOR }
	public enum KillOutcome { PROGRESS, GAINED, AT_CAP }

	public record DeathResult(HeartsData data, DeathOutcome outcome) {}
	public record KillResult(HeartsData data, KillOutcome outcome, int xpGained, int heartsGained) {}

	private HeartRules() {}

	public static int maxHearts(HeartsData d) {
		return BASE + d.bonusHearts();
	}

	/** HeartXP a hostile mob is worth: its max health, rounded, at least 1. */
	public static int xpFor(float mobMaxHealth) {
		return Math.max(1, Math.round(mobMaxHealth));
	}

	public static DeathResult onDeath(HeartsData d) {
		int deaths = d.totalDeaths() + 1;
		if (maxHearts(d) > MIN) {
			return new DeathResult(new HeartsData(d.bonusHearts() - 1, d.heartXp(), d.totalHostileKills(), deaths), DeathOutcome.LOST);
		}
		return new DeathResult(new HeartsData(d.bonusHearts(), d.heartXp(), d.totalHostileKills(), deaths), DeathOutcome.AT_FLOOR);
	}

	public static KillResult onHostileKill(HeartsData d, int xp) {
		int kills = d.totalHostileKills() + 1;
		int hearts = maxHearts(d);
		if (hearts >= MAX) {
			return new KillResult(new HeartsData(d.bonusHearts(), 0, kills, d.totalDeaths()), KillOutcome.AT_CAP, xp, 0);
		}
		int total = d.heartXp() + xp;
		int gained = 0;
		while (total >= XP_PER_HEART && hearts < MAX) {
			hearts++;
			gained++;
			total -= XP_PER_HEART;
		}
		if (hearts >= MAX) {
			total = 0;
		}
		return new KillResult(new HeartsData(hearts - BASE, total, kills, d.totalDeaths()),
			gained > 0 ? KillOutcome.GAINED : KillOutcome.PROGRESS, xp, gained);
	}

	public static HeartsData withHearts(HeartsData d, int n) {
		int c = Math.max(MIN, Math.min(MAX, n));
		return new HeartsData(c - BASE, c >= MAX ? 0 : d.heartXp(), d.totalHostileKills(), d.totalDeaths());
	}

	public static HeartsData withXp(HeartsData d, int n) {
		int c = maxHearts(d) >= MAX ? 0 : Math.max(0, Math.min(XP_PER_HEART - 1, n));
		return new HeartsData(d.bonusHearts(), c, d.totalHostileKills(), d.totalDeaths());
	}

	/** Fill of the HUD bar, 0..1 (1 at the cap). */
	public static float xpFraction(HeartsData d) {
		return maxHearts(d) >= MAX ? 1f : d.heartXp() / (float) XP_PER_HEART;
	}
}
