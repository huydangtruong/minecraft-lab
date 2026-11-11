package io.github.huydangtruong.permahearts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class HeartsCodecTest {
	@Test
	void roundTrips() {
		var d = new HeartsData(-3, 42, 500, 7);
		var json = HeartsData.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
		assertEquals(d, HeartsData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
	}

	@Test
	void emptyDecodesToDefault() {
		assertEquals(HeartsData.DEFAULT, HeartsData.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow());
	}
}
