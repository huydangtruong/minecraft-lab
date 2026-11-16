package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

/**
 * P-029: Thunder Strike in a real client. A real left click casts it at the crosshair; screenshot 99-thunder-warning
 * during the 1 s warning and 100-thunder-strike when the lightning lands.
 */
public class ThunderClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 grass_block");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("fill -2 100 7 2 102 9 stone"); // a small wall to blast
			server.runCommand("tp @a 0 100 0 0 8");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.THUNDER, 10, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.THUNDER)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.THUNDER), "the focus is in hand");

			context.getInput().pressMouse(LEFT);
			context.waitTicks(8);
			context.takeScreenshot("99-thunder-warning");
			boolean standing = server.computeOnServer(s -> !s.overworld().getBlockState(new BlockPos(0, 100, 7)).isAir());
			check(standing, "the wall is still there during the warning");
			context.waitTicks(11);
			context.takeScreenshot("100-thunder-strike");
			context.waitTicks(10);
			boolean broken = server.computeOnServer(s -> s.overworld().getBlockState(new BlockPos(0, 100, 7)).isAir()
				|| s.overworld().getBlockState(new BlockPos(0, 101, 7)).isAir());
			check(broken, "the lightning blew a hole in the wall");
			boolean cooling = server.computeOnServer(s -> TechniqueAttachment.get(s.getPlayerList().getPlayers().get(0))
				.onCooldown(Technique.THUNDER, s.overworld().getGameTime()));
			check(cooling, "the cooldown started at the cast");

			// D86: aimed at the open sky the strike lands on the ground below the far point (128 blocks away)
			server.runCommand("tp @a 0 100 -20 0 -3");
			context.waitTicks(10);
			String sky = server.computeOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				var eye = p.getEyePosition();
				var end = eye.add(p.getViewVector(1f).scale(TechniqueRules.THUNDER_RANGE));
				var hit = io.github.huydangtruong.permahearts.technique.Thunder.aim(p);
				var below = s.overworld().getBlockState(net.minecraft.core.BlockPos.containing(hit.x, hit.y - 0.05, hit.z));
				boolean farAway = Math.abs(hit.x - end.x) < 0.5 && Math.abs(hit.z - end.z) < 0.5;
				boolean dropped = hit.y < end.y - 1;
				boolean ground = !below.isAir() || hit.y <= s.overworld().getMinY() + 1;
				return farAway && dropped && ground ? "ok" : "hit " + hit + " end " + end + " below " + below;
			});
			check("ok".equals(sky), "the sky strike drops to the ground below the far point: " + sky);
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
