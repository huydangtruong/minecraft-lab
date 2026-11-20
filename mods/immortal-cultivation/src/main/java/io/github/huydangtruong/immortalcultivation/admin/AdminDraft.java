package io.github.huydangtruong.immortalcultivation.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P-060 (D155): the command lines of the /admin panel. Pure Java. One command per line, no leading slash, blank lines skipped.
 * A keyed line (a switch, slider or pair of buttons of the same setting) replaces its earlier line instead of adding a new one.
 */
public final class AdminDraft {
	public static final int MAX_LINES = 64;
	public static final int MAX_LENGTH = 256;

	/** Cleaned lines plus how many were dropped (too many lines or too long). */
	public record Result(List<String> lines, int dropped) {}

	private final List<String> lines = new ArrayList<>();
	private final Map<String, String> keyed = new HashMap<>();

	/** Trims and strips leading slashes. A blank result means "skip". */
	public static String clean(String raw) {
		String s = raw == null ? "" : raw.strip();
		while (s.startsWith("/")) {
			s = s.substring(1).stripLeading();
		}
		return s;
	}

	/** Cleans a list of lines and applies the limits (used by the client before sending and by the server before running). */
	public static Result sanitize(List<String> raw) {
		List<String> out = new ArrayList<>();
		int dropped = 0;
		for (String r : raw) {
			String s = clean(r);
			if (s.isEmpty()) {
				continue;
			}
			if (s.length() > MAX_LENGTH || out.size() >= MAX_LINES) {
				dropped++;
				continue;
			}
			out.add(s);
		}
		return new Result(out, dropped);
	}

	public List<String> lines() {
		return List.copyOf(lines);
	}

	public String text() {
		return String.join("\n", lines);
	}

	public boolean isEmpty() {
		return lines.isEmpty();
	}

	/** Adds a line at the end. False when blank, too long or the list is full. */
	public boolean append(String line) {
		String s = clean(line);
		if (s.isEmpty() || s.length() > MAX_LENGTH || lines.size() >= MAX_LINES) {
			return false;
		}
		lines.add(s);
		return true;
	}

	/** Puts the line for a key: replaces the line this key put earlier (if it is still there), else appends. */
	public boolean put(String key, String line) {
		String s = clean(line);
		if (s.isEmpty() || s.length() > MAX_LENGTH) {
			return false;
		}
		String old = keyed.get(key);
		int at = old == null ? -1 : lines.indexOf(old);
		if (at >= 0) {
			lines.set(at, s);
			keyed.put(key, s);
			return true;
		}
		if (lines.size() >= MAX_LINES) {
			return false;
		}
		lines.add(s);
		keyed.put(key, s);
		return true;
	}

	public void clear() {
		lines.clear();
		keyed.clear();
	}

	/** Replaces the lines with the text of the box (after a manual edit). Keys stay: they only match while their line is still in the text. */
	public void setText(String text) {
		lines.clear();
		for (String part : text.split("\n", -1)) {
			String s = clean(part);
			if (!s.isEmpty() && lines.size() < MAX_LINES) {
				lines.add(s);
			}
		}
	}
}
