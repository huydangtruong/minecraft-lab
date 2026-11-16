package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.GodSlayerWarningHud;
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
			// the edge: warning ends 5 s from now on the client clock; the number is the real server message
			context.runOnClient(mc -> GodSlayerWarningHud.setTicksLeft(mc.level.getGameTime() + 100));
			server.runOnServer(s -> s.getPlayerList().getPlayers()
				.forEach(p -> p.sendOverlayMessage(Messages.godSlayerCountdown(5))));
			context.waitTicks(5);
			context.takeScreenshot("98-god-slayer-countdown");
			context.runOnClient(mc -> GodSlayerWarningHud.setTicksLeft(-1));
		}
	}
}
