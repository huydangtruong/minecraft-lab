package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.notes.NoteRules;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** P-042 (D117): note limits, order and delete-own-only. */
class NoteRulesTest {
	private static final UUID A = UUID.randomUUID();
	private static final UUID B = UUID.randomUUID();

	@Test
	void textIsTrimmedAndLimited() {
		assertEquals("hi", NoteRules.clean("  hi "));
		assertNull(NoteRules.clean("   "));
		assertNull(NoteRules.clean(null));
		assertEquals("a".repeat(32), NoteRules.clean("a".repeat(32)));
		assertNull(NoteRules.clean("a".repeat(33)));
	}

	@Test
	void newestIsOnTop() {
		var r = NoteRules.add(List.of(), true, A, "A", "one", 1, 10);
		r = NoteRules.add(r.list(), true, B, "B", "two", 2, 11);
		assertEquals("two", r.list().get(0).text());
		assertEquals("one", r.list().get(1).text());
	}

	@Test
	void sharedCapIsPerPlayer() {
		List<NoteRules.Note> l = List.of();
		for (int i = 0; i < 16; i++) {
			var r = NoteRules.add(l, true, A, "A", "n" + i, i, i);
			assertTrue(r.ok());
			l = r.list();
		}
		var full = NoteRules.add(l, true, A, "A", "more", 99, 99);
		assertFalse(full.ok());
		assertEquals("Delete an old note first", full.error());
		assertEquals(16, full.list().size());
		assertTrue(NoteRules.add(l, true, B, "B", "mine", 100, 100).ok(), "another player still has room");
	}

	@Test
	void privateCapIsTwentyFour() {
		List<NoteRules.Note> l = List.of();
		for (int i = 0; i < 24; i++) {
			l = NoteRules.add(l, false, A, "A", "n" + i, i, i).list();
		}
		assertFalse(NoteRules.add(l, false, A, "A", "x", 50, 50).ok());
		assertEquals(24, NoteRules.limit(false));
	}

	@Test
	void onlyTheAuthorDeletes() {
		var l = NoteRules.add(List.of(), true, A, "A", "mine", 1, 1).list();
		assertEquals(1, NoteRules.delete(l, 1, B).size());
		assertTrue(NoteRules.delete(l, 1, A).isEmpty());
		assertEquals(1, NoteRules.delete(l, 7, A).size(), "unknown id");
	}

	@Test
	void emptyOrLongTextRejected() {
		assertFalse(NoteRules.add(List.of(), true, A, "A", "", 1, 1).ok());
		assertFalse(NoteRules.add(List.of(), true, A, "A", "x".repeat(33), 1, 1).ok());
	}
}
