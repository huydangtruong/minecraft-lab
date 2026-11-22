package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidsTest {
	@Test
	void aValidStringGivesTheUuid() {
		UUID id = UUID.randomUUID();
		assertEquals(id, Uuids.parseOrNull(id.toString()));
	}

	@Test
	void badInputGivesNull() {
		assertNull(Uuids.parseOrNull(""));
		assertNull(Uuids.parseOrNull("not a uuid"));
		assertNull(Uuids.parseOrNull("1234"));
	}
}
