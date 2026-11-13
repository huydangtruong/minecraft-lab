package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.Messages;
import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Drives the Wanderer's life cycle: WAITING -> PLANNED -> PRESENT. Work runs once per second. */
public final class WandererScheduler {
	private static final RandomGenerator RNG = new java.util.Random();
	private static final double NEAR = 96.0;

	private WandererScheduler() {}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> WandererBar.forget());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int t = server.getTickCount();
			if (t % 20 == 0 && !PermaHearts.enabled(server)) {
				WandererBar.hide(); // switch off: no Crimson Master bar
			}
			if (t % 20 == 0 && PermaHearts.enabled(server)) {
				work(server, t % 100 == 0);
				WandererBar.tick(server);
				BloodMoon.tick(server);
				QuestBoss.tick(server);
				if (t % 40 == 0) {
					Wanderer.exploreCheck(server);
				}
			}
		});
	}

	/** One scheduler pass. checkNpc: also look for a missing NPC and respawn it (every 5 seconds). */
	public static void work(MinecraftServer server, boolean checkNpc) {
		ServerLevel overworld = server.overworld();
		WandererData d = Wanderer.data(server);
		long day = Wanderer.currentDay(server);
		if ((d.phase() == Phase.PLANNED || d.phase() == Phase.PRESENT) && checkStay(server, d)) {
			return;
		}
		d = Wanderer.data(server);
		switch (d.phase()) {
			case WAITING -> {
				if (d.nextDay() < 0) {
					Wanderer.save(server, d.withNextDay(day + QuestGenerator.daysUntilNext(RNG)));
				} else if (day >= d.nextDay()) {
					List<ServerPlayer> players = overworld.players();
					if (!players.isEmpty()) {
						Wanderer.plan(server, players.get(RNG.nextInt(players.size())));
					}
				}
			}
			case PLANNED -> {
				for (ServerPlayer p : overworld.players()) {
					double dx = p.getX() - d.x();
					double dz = p.getZ() - d.z();
					if (dx * dx + dz * dz <= NEAR * NEAR && overworld.isPositionEntityTicking(new BlockPos(d.x(), overworld.getMinY(), d.z()))) {
						arrive(overworld, d);
						break;
					}
				}
			}
			case PRESENT -> {
				if (checkNpc && d.quest().isPresent()) {
					BlockPos pos = new BlockPos(d.x(), d.y(), d.z());
					Entity npc = Wanderer.npc(server);
					if ((npc == null || npc.isRemoved()) && overworld.isPositionEntityTicking(pos)) {
						Wanderer.spawnNpc(overworld, pos, d.quest().get(), 0f);
					}
				}
			}
		}
	}

	/** The 2-day stay (D20). Returns true when the Wanderer left in this pass. */
	private static boolean checkStay(MinecraftServer server, WandererData d) {
		long clock = Wanderer.clockTime(server);
		if (d.leaveAt() < 0) {
			// A 0.7.0 save: the stay starts now.
			d = d.withLeaveAt(clock + Wanderer.STAY_TICKS);
			Wanderer.save(server, d);
		}
		if (!d.warned() && clock >= d.leaveAt() - Wanderer.WARN_TICKS) {
			Wanderer.save(server, d.withWarned(true));
			server.getPlayerList().broadcastSystemMessage(Messages.leavingSoon(), false);
			WandererBar.announceLeavingSoon(server, d.x(), d.z());
		}
		if (clock >= d.leaveAt() && !BloodMoon.running(server) && !QuestBoss.bossAlive(server)) {
			Wanderer.leave(server, Messages.leftAfterStay());
			return true;
		}
		return false;
	}

	private static void arrive(ServerLevel level, WandererData d) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, d.x(), d.z());
		BlockPos feet = new BlockPos(d.x(), y, d.z());
		if (level.getFluidState(feet.below()).is(FluidTags.WATER)) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					level.setBlock(feet.below().offset(dx, 0, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);
				}
			}
		}
		Wanderer.spawnNpc(level, feet, Wanderer.newQuest(level.getServer()), 0f);
	}
}
