package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.client.TechniqueWheelScreen;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-067 / D188: the technique wheel screen in a real client (one screenshot, named 40-technique-wheel). */
public class WheelClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 stone");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a 0 100 0 -90 12");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.FIRE, 10, 50).withLevel(Technique.THUNDER, 3, 10).withLevel(Technique.LANCE, 2, 10)
					.withLevel(Technique.ICE, 3, 10).withLevel(Technique.HEALING, 5, 20).withLevel(Technique.PHASE, 1, 5)
					.withLevel(Technique.FLIGHT, 2, 5).withLevel(Technique.GOLEM, 4, 10).withEmeralds(64 + 128 + 256)
					.withSlot(0, Technique.FIRE).withSlot(1, Technique.ICE).withSlot(2, Technique.HEALING)));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(5);
			check(ClientSkills.selected() == 0, "Z selects slot 1");
			ClientSkills.testWheelHeld = true;
			context.runOnClient(mc -> mc.gui.setScreen(new TechniqueWheelScreen(ClientSkills.selected())));
			context.waitTicks(5);
			check(ClientSkills.selected() == 0, "the wheel keeps the slot selected");
			context.takeScreenshot("40-technique-wheel");
			ClientSkills.testWheelHeld = false;
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() == null), "letting go of the key closes the wheel");
			check(ClientSkills.selected() == 0, "slot still selected after the wheel closes");
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
