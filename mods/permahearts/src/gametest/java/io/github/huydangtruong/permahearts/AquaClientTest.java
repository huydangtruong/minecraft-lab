package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.AquaMarine;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * P-022: the player under water with Aqua Marine in a real client: the HUD timer and the bubbles around the player.
 * Screenshots 90-... . Run with ./gradlew :permahearts:runClientGameTest.
 */
public class AquaClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -12 99 -12 12 99 12 sand");
			server.runCommand("fill -12 100 -12 12 112 12 water");
			server.runCommand("tp @a 0 104 0 -90 5");
			context.waitTicks(30);
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				p.setAirSupply(60); // low air, so the bubble row would show without the buff
				TechniqueCasts.cast(p, Technique.AQUA, 20, 1f);
			});
			context.waitTicks(40);
			int air = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getAirSupply());
			boolean active = server.computeOnServer(s -> AquaMarine.active(s.getPlayerList().getPlayers().get(0)));
			if (!active || air < 300) {
				throw new AssertionError("Aqua Marine active with full air, was active=" + active + " air=" + air);
			}
			context.takeScreenshot("90-aqua-underwater-first-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("91-aqua-underwater-third-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}
}
