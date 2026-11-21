package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientMayhem;
import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.technique.Mayhem;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** P-064 (D174): Crystal Mayhem in a real client. Cell 0 = Crystal Mayhem. Screenshots 130-... */
public class MayhemClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set night");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 stone");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a 0 100 0 -90 0");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.MAYHEM, 10, 20).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.MAYHEM)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			server.runCommand("summon husk 6 100 0 {NoAI:1b}");
			context.waitTicks(10);

			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.MAYHEM), "Crystal Mayhem is in hand");
			context.getInput().pressMouse(LEFT); // a real left click fires one shard
			context.waitTicks(25); // level 10: 0.55 s between shards
			check(server.computeOnServer(s -> Mayhem.shardsLeft(s.getPlayerList().getPlayers().get(0)) == 19), "the click fired a shard");
			// a few clusters around the player, then the channel (the 1 s hold is covered by the server game tests)
			String why = server.computeOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				var level = p.level();
				long now = level.getGameTime();
				int[][] at = {{3, -2}, {3, 2}, {5, -4}, {5, 4}, {7, 0}, {2, 0}};
				for (int[] a : at) {
					Mayhem.placeCluster(p, level, new BlockPos(a[0], 100, a[1]), Direction.UP, now);
				}
				return Mayhem.input(p, 10, 1f);
			});
			check(why == null, "the channel starts: " + why);
			context.waitTicks(40);
			check(context.computeOnClient(mc -> ClientMayhem.shards() == 20), "the HUD knows the 20 refilled shards");
			context.takeScreenshot("130-crystal-mayhem-buddings-night");
			context.waitTicks(40);
			context.takeScreenshot("131-crystal-mayhem-hud");
			server.runOnServer(s -> Mayhem.end(s.getPlayerList().getPlayers().get(0)));
			context.waitTicks(10);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("kill @e[type=husk]");
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
