package io.github.huydangtruong.immortalcultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspCalls;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspCalls.Pending;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraspCallsTest {
	private static final UUID OWNER = UUID.randomUUID();

	private static Pending call(UUID mob, String dim, int cx, int cz, long deadline) {
		return new Pending(mob, OWNER, dim, cx, cz, false, deadline);
	}

	@Test
	void oneWaitingCallPerMob() {
		GraspCalls c = new GraspCalls();
		UUID m = UUID.randomUUID();
		assertTrue(c.start(call(m, "minecraft:overworld", 1, 2, 100)));
		assertFalse(c.start(call(m, "minecraft:overworld", 1, 2, 200)));
		assertEquals(1, c.size());
		assertTrue(c.has(m));
		assertNotNull(c.finish(m));
		assertNull(c.finish(m));
		assertFalse(c.has(m));
	}

	@Test
	void expiresAtTheDeadline() {
		GraspCalls c = new GraspCalls();
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		c.start(call(a, "minecraft:overworld", 0, 0, 100));
		c.start(call(b, "minecraft:the_nether", 0, 0, 150));
		assertEquals(0, c.expired(99).size());
		assertEquals(1, c.expired(100).size());
		assertEquals(a, c.expired(100).get(0).mob());
		assertEquals(2, c.expired(150).size());
	}

	@Test
	void aSharedChunkKeepsItsTicketUntilTheLastCallIsDone() {
		GraspCalls c = new GraspCalls();
		Pending a = call(UUID.randomUUID(), "minecraft:overworld", 5, 5, 100);
		Pending b = call(UUID.randomUUID(), "minecraft:overworld", 5, 5, 100);
		Pending other = call(UUID.randomUUID(), "minecraft:the_nether", 5, 5, 100);
		c.start(a);
		c.start(b);
		c.start(other);
		assertTrue(c.chunkStillNeeded(a), "b needs the same chunk");
		assertFalse(c.chunkStillNeeded(other), "same coordinates in another dimension is another chunk");
		assertEquals(2, c.chunkCounts().get("minecraft:overworld@5,5"));
		c.finish(a.mob());
		assertFalse(c.chunkStillNeeded(b), "b is the last one");
	}

	@Test
	void callsOfAPlayerAreListedAndCleared() {
		GraspCalls c = new GraspCalls();
		c.start(call(UUID.randomUUID(), "minecraft:overworld", 0, 0, 100));
		c.start(new Pending(UUID.randomUUID(), UUID.randomUUID(), "minecraft:overworld", 9, 9, true, 100));
		assertEquals(1, c.of(OWNER).size());
		assertEquals(2, c.all().size());
		c.clear();
		assertEquals(0, c.size());
	}
}
