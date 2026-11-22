package io.github.huydangtruong.immortalcultivation.technique;

/**
 * "16 blocks" -> "18 blocks" reads better as "16 > 18 blocks": the words two stat values share go once, at the end.
 * Pure rule (moved from the technique detail page).
 */
public final class StatDiff {
	private StatDiff() {}

	/** {@code from} and {@code to} are the values without the shared tail; {@code tail} is "" or " " + the shared words. */
	public record Result(String from, String to, String tail) {}

	public static Result compare(String cur, String nxt) {
		String[] a = cur.split(" ");
		String[] b = nxt.split(" ");
		int common = 0;
		while (common < a.length - 1 && common < b.length - 1 && a[a.length - 1 - common].equals(b[b.length - 1 - common])
			&& !Character.isDigit(a[a.length - 1 - common].charAt(0))) {
			common++;
		}
		String curShort = String.join(" ", java.util.Arrays.copyOfRange(a, 0, a.length - common));
		String nxtShort = String.join(" ", java.util.Arrays.copyOfRange(b, 0, b.length - common));
		String tail = common == 0 ? "" : " " + String.join(" ", java.util.Arrays.copyOfRange(a, a.length - common, a.length));
		return new Result(curShort, nxtShort, tail);
	}
}
