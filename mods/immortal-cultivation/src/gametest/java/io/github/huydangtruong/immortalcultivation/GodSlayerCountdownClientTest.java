package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.GodSlayerWarningHud;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-035: the God Slayer warning near its end: red countdown number and the red screen edge. Screenshot 98. */
public class GodSlayerCountdownClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set day");
			server.runCommand("weather clear");
			context.waitTicks(20);
			// start (60 s left): the edge is barely there; end (about 2 s left): the strongest it gets
			context.runOnClient(mc -> GodSlayerWarningHud.setTicksLeft(mc.level.getGameTime() + 1200));
			server.runOnServer(s -> s.getPlayerList().getPlayers()
				.forEach(p -> p.sendOverlayMessage(GodSlayerMessages.godSlayerCountdown(60))));
			context.waitTicks(5);
			context.takeScreenshot("countdown-start");
			context.runOnClient(mc -> GodSlayerWarningHud.setTicksLeft(mc.level.getGameTime() + 40));
			server.runOnServer(s -> s.getPlayerList().getPlayers()
				.forEach(p -> p.sendOverlayMessage(GodSlayerMessages.godSlayerCountdown(2))));
			context.waitTicks(2);
			context.takeScreenshot("countdown-end");
			context.runOnClient(mc -> GodSlayerWarningHud.setTicksLeft(-1));
		}
	}
}
