package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.admin.AdminDraft;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminDraftTest {
	@Test
	void appendKeepsOrderAndStripsSlash() {
		AdminDraft d = new AdminDraft();
		assertTrue(d.append("/hearts set @s 30"));
		assertTrue(d.append("  danger set 5  "));
		assertFalse(d.append("   "));
		assertFalse(d.append("/"));
		assertEquals(List.of("hearts set @s 30", "danger set 5"), d.lines());
		assertEquals("hearts set @s 30\ndanger set 5", d.text());
	}

	@Test
	void putReplacesTheSameKey() {
		AdminDraft d = new AdminDraft();
		d.put("hearts", "hearts set @s 10");
		d.append("quest skip");
		d.put("hearts", "hearts set @s 20");
		assertEquals(List.of("hearts set @s 20", "quest skip"), d.lines());
		d.put("danger", "danger set 3");
		assertEquals(3, d.lines().size());
	}

	@Test
	void putAppendsAgainWhenTheLineWasDeletedByHand() {
		AdminDraft d = new AdminDraft();
		d.put("danger", "danger set 3");
		d.setText("quest skip");
		d.put("danger", "danger set 4");
		assertEquals(List.of("quest skip", "danger set 4"), d.lines());
	}

	@Test
	void putKeepsReplacingAfterAHandEditOfOtherLines() {
		AdminDraft d = new AdminDraft();
		d.put("danger", "danger set 3");
		d.setText("quest skip\n\n/danger set 3\n");
		d.put("danger", "danger set 9");
		assertEquals(List.of("quest skip", "danger set 9"), d.lines());
	}

	@Test
	void clearEmptiesEverything() {
		AdminDraft d = new AdminDraft();
		d.put("a", "x");
		d.clear();
		assertTrue(d.isEmpty());
		d.put("a", "y");
		assertEquals(List.of("y"), d.lines());
	}

	@Test
	void limits() {
		AdminDraft d = new AdminDraft();
		for (int i = 0; i < AdminDraft.MAX_LINES; i++) {
			assertTrue(d.append("say " + i));
		}
		assertFalse(d.append("one too many"));
		assertFalse(d.put("k", "one too many"));
		assertFalse(new AdminDraft().append("x".repeat(AdminDraft.MAX_LENGTH + 1)));
		assertTrue(new AdminDraft().append("x".repeat(AdminDraft.MAX_LENGTH)));
	}

	@Test
	void sanitizeDropsExtraAndLong() {
		List<String> raw = new ArrayList<>();
		raw.add("");
		raw.add("/a");
		raw.add("y".repeat(300));
		for (int i = 0; i < 70; i++) {
			raw.add("c" + i);
		}
		AdminDraft.Result r = AdminDraft.sanitize(raw);
		assertEquals(AdminDraft.MAX_LINES, r.lines().size());
		assertEquals("a", r.lines().get(0));
		assertEquals(1 + 7, r.dropped());
	}

	@Test
	void setTextSkipsBlankLines() {
		AdminDraft d = new AdminDraft();
		d.setText("a\n\n  \n/b\n");
		assertEquals(List.of("a", "b"), d.lines());
	}

	@Test
	void levelAndNoCooldownLinesReplaceByKey() {
		AdminDraft d = new AdminDraft();
		d.put("level", "experience set @s 10 levels");
		d.put("nocooldown", "technique nocooldown @s true");
		d.put("level", "experience set @s 50 levels");
		d.put("nocooldown", "technique nocooldown @s false");
		assertEquals(List.of("experience set @s 50 levels", "technique nocooldown @s false"), d.lines());
	}
}
