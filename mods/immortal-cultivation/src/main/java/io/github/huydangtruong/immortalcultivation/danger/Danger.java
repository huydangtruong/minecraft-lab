package io.github.huydangtruong.immortalcultivation.danger;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gamerules.GameRule;

/** P-054 (D137): the world danger level. State, growth after each Crimson Master visit, and the notice. */
public final class Danger {
	/** Danger on or off (needs the ImmortalCultivation switch too). Off: no growth, every effect behaves as level 0. */
	public static final GameRule<Boolean> ENABLED = GameRuleBuilder.forBoolean(true)
		.category(ImmortalCultivation.RULES)
		.buildAndRegister(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "danger_level"));

	/** Hard ceiling of the hostile mob cap, as a multiple of vanilla. */
	public static final GameRule<Integer> SPAWN_CEILING = GameRuleBuilder.forInteger(4)
		.category(ImmortalCultivation.RULES)
		.range(1, 16)
		.buildAndRegister(Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "danger_spawn_ceiling"));

	private Danger() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				tick(server, server.overworld().getGameTime());
			}
		});
	}

	public static boolean active(MinecraftServer server) {
		return ImmortalCultivation.enabled(server) && server.getGameRules().get(ENABLED);
	}

	/** The level every effect uses: 0 when danger is switched off. */
	public static int level(MinecraftServer server) {
		return active(server) ? DangerSavedData.of(server).level : 0;
	}

	/** The stored level, whatever the switches say. */
	public static int storedLevel(MinecraftServer server) {
		return DangerSavedData.of(server).level;
	}

	/** Hostile mob cap multiplier for the Overworld: 1 + spawnBonus, never above the ceiling game rule. */
	public static double spawnMultiplier(MinecraftServer server) {
		int level = level(server);
		if (level <= 0) {
			return 1.0;
		}
		return Math.min(1.0 + DangerRules.spawnBonus(level), server.getGameRules().get(SPAWN_CEILING));
	}

	/** A Crimson Master left (Wanderer.leave): the next +1 comes 4800 ticks later. */
	public static void onWandererLeft(MinecraftServer server) {
		if (!active(server)) {
			return;
		}
		DangerSavedData d = DangerSavedData.of(server);
		if (d.pendingAtTick == 0) {
			d.pendingAtTick = server.overworld().getGameTime() + DangerRules.GROWTH_DELAY_TICKS;
			d.setDirty();
		}
	}

	/** Raises the level when the pending time has come. Public with an explicit clock for tests. */
	public static void tick(MinecraftServer server, long now) {
		DangerSavedData d = DangerSavedData.of(server);
		if (d.pendingAtTick == 0 || now < d.pendingAtTick) {
			return;
		}
		d.pendingAtTick = 0;
		d.setDirty();
		if (!active(server) || d.level >= DangerRules.MAX_LEVEL) {
			return;
		}
		set(server, d.level + 1, true);
	}

	/** Sets the level (clamped) and optionally tells everyone. */
	public static void set(MinecraftServer server, int level, boolean announce) {
		DangerSavedData d = DangerSavedData.of(server);
		d.level = DangerRules.clamp(level);
		d.setDirty();
		if (announce) {
			announce(server, d.level);
		}
	}

	/** Orange title, lighter subtitle, a chat line and a horn for every online player. */
	public static void announce(MinecraftServer server, int level) {
		DangerRules.Notice n = DangerRules.notice(level);
		Component title = Component.literal(n.title()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		Component sub = Component.literal(n.subtitle()).withStyle(ChatFormatting.YELLOW);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.connection == null) {
				continue;
			}
			p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
			p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
			p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0f, 1.0f);
		}
		server.getPlayerList().broadcastSystemMessage(
			Component.literal(n.title() + ": " + n.subtitle()).withStyle(ChatFormatting.GOLD), false);
	}
}
