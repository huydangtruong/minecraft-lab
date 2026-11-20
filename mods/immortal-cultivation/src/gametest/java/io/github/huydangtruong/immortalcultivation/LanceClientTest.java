package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.technique.Lance;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * P-061 (D157, D159): Solar Lance in a real client. Key R = technique mode, cell 0 = Solar Lance. Holding the left button
 * runs the beam, letting go stops it. Screenshots 120-... . Run with ./gradlew :immortal-cultivation:runClientGameTest.
 */
public class LanceClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;

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
			server.runCommand("tp @a 0 100 0 -90 0");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.LANCE, 10, 20).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.LANCE)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			// two husks in a row in front of the player (yaw -90 looks toward +x)
			server.runCommand("summon husk 6 100 0 {NoAI:1b}");
			server.runCommand("summon husk 12 100 0 {NoAI:1b}");
			context.waitTicks(10);

			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.LANCE), "Solar Lance is in hand");
			context.getInput().holdMouse(LEFT);
			context.waitTicks(30);
			check(server.computeOnServer(s -> Lance.active(s.getPlayerList().getPlayers().get(0))), "the beam runs while the button is held");
			context.takeScreenshot("120-solar-lance-beam");
			context.waitTicks(40);
			context.takeScreenshot("121-solar-lance-bar");
			context.getInput().releaseMouse(LEFT);
			context.waitTicks(10);
			check(!server.computeOnServer(s -> Lance.active(s.getPlayerList().getPlayers().get(0))), "letting go stops the beam");
			context.takeScreenshot("122-solar-lance-released");
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
