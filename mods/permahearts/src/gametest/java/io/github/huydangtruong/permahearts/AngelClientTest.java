package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.WarriorAngel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * P-021: the Warrior Angels next to the player in a real client (third person), calm and then fighting, to check the
 * gold and white colours. Screenshots 80-... and 81-... . Run with ./gradlew :permahearts:runClientGameTest.
 */
public class AngelClientTest implements FabricClientGameTest {
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
			server.runCommand("tp @a 0 100 0 -90 10");
			context.waitTicks(30);
			server.runOnServer(s -> TechniqueCasts.cast(s.getPlayerList().getPlayers().get(0), Technique.ANGEL, 15, 1f));
			context.waitTicks(5);
			int n = server.computeOnServer(s -> WarriorAngel.angelsOf(s.overworld(), s.getPlayerList().getPlayers().get(0).getUUID()).size());
			if (n != 5) {
				throw new AssertionError("level 15 summons 5 angels, was " + n);
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("80-angels-third-person");

			// they fly into a monster next to the owner: charging pose and texture
			server.runCommand("summon husk 4 100 0 {NoAI:1b}");
			context.waitTicks(25);
			context.takeScreenshot("81-angels-fight");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}
}
