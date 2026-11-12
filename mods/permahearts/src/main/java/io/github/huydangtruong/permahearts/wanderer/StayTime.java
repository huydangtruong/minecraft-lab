package io.github.huydangtruong.permahearts.wanderer;

/** Formats how long the Wanderer stays: Minecraft time first, real time in brackets. No Minecraft imports. */
public final class StayTime {
	public static final long DAY_TICKS = 24000L;
	private static final long HOUR_TICKS = 1000L;

	private StayTime() {}

	/** e.g. 25200 -> "1 day 1 h (≈ 21 min)"; under a day -> "7 h (≈ 5 min)"; under a minute of real time -> "0 h (≈ 30 s)". */
	public static String format(long ticksLeft) {
		long t = Math.max(0, ticksLeft);
		long days = t / DAY_TICKS;
		long hours = (t % DAY_TICKS) / HOUR_TICKS;
		long seconds = t / 20;
		String game = days > 0 ? days + (days == 1 ? " day " : " days ") + hours + " h" : hours + " h";
		String real = seconds >= 60 ? "≈ " + (seconds / 60) + " min" : "≈ " + seconds + " s";
		return game + " (" + real + ")";
	}
}
