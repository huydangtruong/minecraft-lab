package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.godslayer.GodSlayer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * P-032: the God Slayer seen from the front, in a real client (screenshot 90-...). The boss stands in front of the
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
			server.runCommand("tp @a 0 100 0 -90 10");
			context.waitTicks(20);
			server.runOnServer(s -> {
				GodSlayer b = GodSlayer.TYPE.create(s.overworld(), EntitySpawnReason.MOB_SUMMONED);
				b.setBossLevel(1);
				b.setYRot(110f);
				b.yRotO = 110f;
				b.setYBodyRot(110f);
				b.yBodyRotO = 110f;
				b.setYHeadRot(110f);
				b.yHeadRotO = 110f;
				// ahead and to the right of the player (who looks east), turned towards the camera behind the player
				b.snapTo(2.5, 100, 2.3, 110f, 0f);
				b.setNoAi(true);
				s.overworld().addFreshEntity(b);
			});
			context.waitTicks(10);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("90-god-slayer-front");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}
}
