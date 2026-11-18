package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.danger.Danger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-054: the orange "Danger level X" title with its subtitle, and an elite zombie with its orange name. Screenshots danger-title, danger-elite. */
public class DangerClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set midnight");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -12 99 -12 12 99 12 grass_block");
			server.runCommand("fill -12 100 -12 12 110 12 air");
			server.runCommand("tp @a 0 100 0 0 0");
			context.waitTicks(20);
			server.runOnServer(s -> Danger.announce(s, 8));
			context.waitTicks(15);
			context.takeScreenshot("danger-title");
			context.waitTicks(100);
			server.runCommand("execute as @p at @p run danger elite swift ironhide");
			server.runCommand("data merge entity @e[type=zombie,limit=1] {NoAI:1b}");
			context.waitTicks(30);
			context.takeScreenshot("danger-elite");
		}
	}
}
