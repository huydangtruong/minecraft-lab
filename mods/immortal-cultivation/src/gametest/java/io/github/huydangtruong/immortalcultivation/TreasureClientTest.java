package io.github.huydangtruong.immortalcultivation;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-059: the blue-violet Deadly Treasure title and the glow of every Mega species at night. Screenshots treasure-title, mega-lineup-0..3. */
public class TreasureClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set midnight");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -14 99 -6 14 99 14 grass_block");
			server.runCommand("fill -14 100 -6 14 112 14 air");
			server.runCommand("tp @a 0 100 0 0 10");
			context.waitTicks(20);
			server.runCommand("treasure start");
			context.waitTicks(15);
			context.takeScreenshot("treasure-title");
			server.runCommand("treasure giveup");
			context.waitTicks(130);
			for (int page = 0; page < 4; page++) {
				server.runCommand("kill @e[tag=permahearts.mega]");
				server.runCommand("execute as @p at @p run treasure lineup " + page);
				context.waitTicks(30);
				context.takeScreenshot("mega-lineup-" + page);
			}
		}
	}
}
