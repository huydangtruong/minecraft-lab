package io.github.huydangtruong.permahearts;

/** Pure rules, no Minecraft imports. */
public final class HeartRules {
	public static final int BASE = 10;
	public static final int MIN = 1;
	public static final int MAX = 30;
	public static final int KILLS_PER_HEART = 100;

	public enum DeathOutcome { LOST, AT_FLOOR }
	public enum KillOutcome { PROGRESS, GAINED, AT_CAP }

	public record DeathResult(HeartsData data, DeathOutcome outcome) {}
	public record KillResult(HeartsData data, KillOutcome outcome) {}

	private HeartRules() {}

	public static int maxHearts(HeartsData d) {
		return BASE + d.bonusHearts();
	}

	public static DeathResult onDeath(HeartsData d) {
		int deaths = d.totalDeaths() + 1;
		if (maxHearts(d) > MIN) {
			return new DeathResult(new HeartsData(d.bonusHearts() - 1, d.killProgress(), d.totalHostileKills(), deaths), DeathOutcome.LOST);
		}
		return new DeathResult(new HeartsData(d.bonusHearts(), d.killProgress(), d.totalHostileKills(), deaths), DeathOutcome.AT_FLOOR);
	}

	public static KillResult onHostileKill(HeartsData d) {
		int kills = d.totalHostileKills() + 1;
		int progress = d.killProgress() + 1;
		if (progress >= KILLS_PER_HEART) {
			if (maxHearts(d) < MAX) {
				return new KillResult(new HeartsData(d.bonusHearts() + 1, 0, kills, d.totalDeaths()), KillOutcome.GAINED);
			}
			return new KillResult(new HeartsData(d.bonusHearts(), 0, kills, d.totalDeaths()), KillOutcome.AT_CAP);
		}
		return new KillResult(new HeartsData(d.bonusHearts(), progress, kills, d.totalDeaths()), KillOutcome.PROGRESS);
	}

	public static HeartsData withHearts(HeartsData d, int n) {
		int c = Math.max(MIN, Math.min(MAX, n));
		return new HeartsData(c - BASE, d.killProgress(), d.totalHostileKills(), d.totalDeaths());
	}

	public static HeartsData withProgress(HeartsData d, int n) {
		int c = Math.max(0, Math.min(KILLS_PER_HEART - 1, n));
		return new HeartsData(d.bonusHearts(), c, d.totalHostileKills(), d.totalDeaths());
	}
}
