package io.github.huydangtruong.immortalcultivation.technique.chrono;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * D268: which entities stand still in a time stop (the TickRateManager mixin asks this on both sides). The server decides
 * from its zones; the client side is filled in by the client mod (it knows the zones from {@link ChronoNet.Zone}).
 */
public final class ChronoFreeze {
	/** Set by the client mod. */
	public static volatile Predicate<Entity> client = e -> false;

	private ChronoFreeze() {}

	public static boolean frozen(Entity e) {
		return e.level() instanceof ServerLevel ? Chrono.frozen(e) : client.test(e);
	}
}
