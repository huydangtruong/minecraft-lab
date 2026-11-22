package io.github.huydangtruong.immortalcultivation;

import java.util.UUID;

/** UUID helpers with no Minecraft imports, so JUnit can test them. */
public final class Uuids {
	private Uuids() {
	}

	/** The UUID written in the string, or null when it is not a valid UUID (a null string still throws, as UUID.fromString does). */
	public static UUID parseOrNull(String s) {
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
