package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.huydangtruong.immortalcultivation.technique.StatDiff;
import org.junit.jupiter.api.Test;

class StatDiffTest {
	@Test
	void sharedWordsGoOnceAtTheEnd() {
		StatDiff.Result r = StatDiff.compare("16 blocks", "18 blocks");
		assertEquals("16", r.from());
		assertEquals("18", r.to());
		assertEquals(" blocks", r.tail());
	}

	@Test
	void severalSharedWords() {
		StatDiff.Result r = StatDiff.compare("2 hearts per second", "3 hearts per second");
		assertEquals("2", r.from());
		assertEquals("3", r.to());
		assertEquals(" hearts per second", r.tail());
	}

	@Test
	void nothingSharedKeepsBothWhole() {
		StatDiff.Result r = StatDiff.compare("4 blocks", "5 meters");
		assertEquals("4 blocks", r.from());
		assertEquals("5 meters", r.to());
		assertEquals("", r.tail());
	}

	@Test
	void sharedNumberIsNotMovedToTheTail() {
		StatDiff.Result r = StatDiff.compare("2 for 10", "3 for 10");
		assertEquals("2 for 10", r.from());
		assertEquals("3 for 10", r.to());
		assertEquals("", r.tail());
	}

	@Test
	void singleWordValues() {
		StatDiff.Result r = StatDiff.compare("10", "12");
		assertEquals("10", r.from());
		assertEquals("12", r.to());
		assertEquals("", r.tail());
	}
}
