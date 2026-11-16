package io.github.huydangtruong.permahearts.notes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** P-042 (D117): the pure rules of the Notes panel. Lists are newest first. No Minecraft imports. */
public final class NoteRules {
	public static final int MAX_CHARS = 24;
	public static final int MAX_SHARED_PER_PLAYER = 16;
	public static final int MAX_PRIVATE_PER_PLAYER = 24;
	public static final String FULL_MESSAGE = "Delete an old note first";

	private NoteRules() {}

	/** One note. id is a sequence number, unique within its store. */
	public record Note(long id, UUID author, String authorName, String text, long time) {}

	/** Result of an add: the new list, or an error message (the list is then the unchanged input). */
	public record AddResult(List<Note> list, String error) {
		public boolean ok() {
			return error == null;
		}
	}

	public static int limit(boolean shared) {
		return shared ? MAX_SHARED_PER_PLAYER : MAX_PRIVATE_PER_PLAYER;
	}

	/** Trimmed text, or null when empty or longer than MAX_CHARS. */
	public static String clean(String text) {
		if (text == null) {
			return null;
		}
		String t = text.strip();
		return t.isEmpty() || t.length() > MAX_CHARS ? null : t;
	}

	public static int countBy(List<Note> list, UUID author) {
		int n = 0;
		for (Note note : list) {
			if (note.author().equals(author)) {
				n++;
			}
		}
		return n;
	}

	/** Adds a note on top. Rejects empty/too long text and a full quota for this author. */
	public static AddResult add(List<Note> list, boolean shared, UUID author, String name, String text, long id, long time) {
		String t = clean(text);
		if (t == null) {
			return new AddResult(list, "A note is 1 to " + MAX_CHARS + " characters");
		}
		if (countBy(list, author) >= limit(shared)) {
			return new AddResult(list, FULL_MESSAGE);
		}
		List<Note> out = new ArrayList<>(list.size() + 1);
		out.add(new Note(id, author, name, t, time));
		out.addAll(list);
		return new AddResult(out, null);
	}

	public static boolean canDelete(Note note, UUID who) {
		return note.author().equals(who);
	}

	/** Removes the note with this id when who wrote it; otherwise the list is returned unchanged. */
	public static List<Note> delete(List<Note> list, long id, UUID who) {
		List<Note> out = new ArrayList<>(list.size());
		for (Note n : list) {
			if (n.id() == id && canDelete(n, who)) {
				continue;
			}
			out.add(n);
		}
		return out;
	}
}
