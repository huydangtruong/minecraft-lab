package io.github.huydangtruong.immortalcultivation.danger;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.entity.EntityTypeTest;

/** D190: every mob of a raid glows (the outline a rung bell gives), refreshed each second while it is in the raid. */
public final class RaidGlow {
	/** A little longer than the refresh, so the outline never blinks. */
	static final int GLOW_TICKS = 60;

	private RaidGlow() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0 && ImmortalCultivation.enabled(server)) {
				tick(server);
			}
		});
	}

	public static void tick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (Raider r : level.getEntities(EntityTypeTest.forClass(Raider.class), r -> r.isAlive() && r.getCurrentRaid() != null)) {
				r.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
			}
		}
	}
}
