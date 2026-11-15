package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.godslayer.GodSlayer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * P-032/P-034: the God Slayer seen from the front and from the side, in a real client (screenshots 90-... and 91-...). The boss stands in front of the
 * player, who looks at it in third person front view. Run with ./gradlew :permahearts:runClientGameTest.
 */
public class GodSlayerClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 stone");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a 0.5 100 0.5 -90 4");
			context.waitTicks(20);
			server.runOnServer(s -> {
				GodSlayer b = GodSlayer.TYPE.create(s.overworld(), EntitySpawnReason.MOB_SUMMONED);
				b.setBossLevel(1);
				// ahead of the player (who looks east) and a little to the side, turned to face the camera behind the player
				b.snapTo(7, 100, 4.5, 109f, 0f);
				turn(b, 109f);
				b.setNoAi(true);
				s.overworld().addFreshEntity(b);
			});
			context.waitTicks(10);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("90-god-slayer-front");
			// the same boss seen from the side (it faces south, the camera is on the west)
			server.runOnServer(s -> s.overworld().getEntities(GodSlayer.TYPE, e -> true).forEach(b -> turn(b, 19f)));
			context.waitTicks(10);
			context.takeScreenshot("91-god-slayer-side");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	private static void turn(GodSlayer b, float yaw) {
		b.setYRot(yaw);
		b.yRotO = yaw;
		b.setYBodyRot(yaw);
		b.yBodyRotO = yaw;
		b.setYHeadRot(yaw);
		b.yHeadRotO = yaw;
	}
}
