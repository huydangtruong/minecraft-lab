package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueNet;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** P-019: Thousand Mile Step on long strips of stone (the test world has no ground; padding keeps the tests apart). */
public class ThousandMileStepGameTest {
	private static final int START_X = 12; // outside the barrier walls of the test structure
	private static final int REACH = 150; // teleportRange(1)

	private static void enable(GameTestHelper c) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, true, s);
	}

	/** A strip 5 wide (z 0..4) along +x at y 0. */
	private static void strip(GameTestHelper c, int from, int to, Block b) {
		for (int x = from; x <= to; x++) {
			for (int z = 0; z <= 4; z++) {
				c.setBlock(x, 0, z, b);
			}
		}
	}

	private static ServerPlayer start(GameTestHelper c) {
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		Vec3 v = c.absoluteVec(new Vec3(START_X + 0.5, 1, 2.5));
		p.teleportTo(c.getLevel(), v.x, v.y, v.z, Set.of(), -90f, 0f, false); // yaw -90 looks along +x
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.TELEPORT, 1, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.TELEPORT));
		return p;
	}

	private static double relX(GameTestHelper c, ServerPlayer p) {
		return c.relativeVec(p.position()).x;
	}

	private static void clean(GameTestHelper c, int from, int to) {
		strip(c, from, to, Blocks.AIR);
	}

	@GameTest(maxTicks = 60, padding = 128)
	public void fullChargeLandsAboutTheRangeAwayAndStartsTheCooldown(GameTestHelper c) {
		enable(c);
		strip(c, START_X - 2, START_X + REACH + 10, Blocks.STONE);
		ServerPlayer p = start(c);
		p.fallDistance = 40;
		String error = TechniqueNet.cast(p, 0, 1f);
		c.assertTrue(error == null, "cast should work, got: " + error);
		double moved = relX(c, p) - (START_X + 0.5);
		c.assertTrue(Math.abs(moved - REACH) <= 3, "moved about " + REACH + " blocks, was " + moved);
		c.assertTrue(Math.abs(c.relativeVec(p.position()).y - 1) < 0.01, "stands on the floor, y " + c.relativeVec(p.position()).y);
		c.assertTrue(p.fallDistance == 0, "no fall damage");
		long now = c.getLevel().getServer().overworld().getGameTime();
		c.assertTrue(TechniqueAttachment.get(p).onCooldown(Technique.TELEPORT, now), "cooldown started");
		clean(c, START_X - 2, START_X + REACH + 10);
		c.succeed();
	}

	@GameTest(maxTicks = 60, padding = 128)
	public void lowChargeKeepsTheTwentyPercentFloor(GameTestHelper c) {
		enable(c);
		strip(c, START_X - 2, START_X + 50, Blocks.STONE);
		ServerPlayer p = start(c);
		c.assertTrue(TechniqueCasts.cast(p, Technique.TELEPORT, 1, 0f), "cast works at charge 0");
		double moved = relX(c, p) - (START_X + 0.5);
		c.assertTrue(Math.abs(moved - 0.2 * REACH) <= 3, "moved about 30 blocks, was " + moved);
		clean(c, START_X - 2, START_X + 50);
		c.succeed();
	}

	@GameTest(maxTicks = 60, padding = 128)
	public void aWallStopsTheStep(GameTestHelper c) {
		enable(c);
		strip(c, START_X - 2, START_X + 80, Blocks.STONE);
		for (int y = 1; y <= 6; y++) {
			for (int z = 0; z <= 4; z++) {
				c.setBlock(START_X + 40, y, z, Blocks.STONE);
			}
		}
		ServerPlayer p = start(c);
		c.assertTrue(TechniqueCasts.cast(p, Technique.TELEPORT, 1, 1f), "cast works");
		double x = relX(c, p);
		c.assertTrue(x < START_X + 40 && x > START_X + 37, "stops just before the wall at " + (START_X + 40) + ", was " + x);
		for (int y = 1; y <= 6; y++) {
			for (int z = 0; z <= 4; z++) {
				c.setBlock(START_X + 40, y, z, Blocks.AIR);
			}
		}
		clean(c, START_X - 2, START_X + 80);
		c.succeed();
	}

	/** The test world has real ground far from the structures: turns the ground around the landing spot into a lava pool. */
	private static void lavaPool(GameTestHelper c) {
		ServerLevel sl = c.getLevel();
		Vec3 centre = c.absoluteVec(new Vec3(START_X + 0.5 + REACH, 0, 2.5));
		int cx = Mth.floor(centre.x);
		int cz = Mth.floor(centre.z);
		sl.getChunk(cx >> 4, cz >> 4);
		sl.getChunk((cx - 9) >> 4, (cz - 9) >> 4);
		sl.getChunk((cx + 9) >> 4, (cz + 9) >> 4);
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz);
				sl.setBlock(new BlockPos(cx + dx, top - 1, cz + dz), Blocks.LAVA.defaultBlockState(), 3);
			}
		}
	}

	@GameTest(maxTicks = 60, padding = 128)
	public void lavaMeansNoCastAndNoCooldown(GameTestHelper c) {
		enable(c);
		strip(c, START_X - 2, START_X + 5, Blocks.STONE);
		lavaPool(c);
		ServerPlayer p = start(c);
		Vec3 before = p.position();
		String error = TechniqueNet.cast(p, 0, 1f);
		c.assertTrue("No safe place to land".equals(error), "refused with the message, got: " + error + " at " + c.relativeVec(p.position()));
		c.assertTrue(p.position().distanceTo(before) < 0.01, "did not move");
		long now = c.getLevel().getServer().overworld().getGameTime();
		c.assertTrue(!TechniqueAttachment.get(p).onCooldown(Technique.TELEPORT, now), "no cooldown");
		clean(c, START_X - 2, START_X + 5);
		c.succeed();
	}
}
