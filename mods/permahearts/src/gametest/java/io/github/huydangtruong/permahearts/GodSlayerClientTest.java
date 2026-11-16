package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.godslayer.GodSlayer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * P-032/P-034/P-035: the God Slayer in a real client. The camera is behind the player (third person) 4 blocks west of
 * it and looks east. The boss yaw picks the view: 90 = faces the camera (front), 0 = faces south (a real side view),
 * -90 = faces away (back). Screenshots 90-front, 91-side, 92-back, 93-face closeup, 94/95/97 flying hover, 96 flying forward (frozen pose).
 * Run with ./gradlew :permahearts:runClientGameTest.
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
			server.runCommand("effect give @a minecraft:invisibility 999 0 true");
			server.runCommand("tp @a 0.5 100 0.5 -90 -12");
			context.waitTicks(20);
			GodSlayer[] box = new GodSlayer[1];
			server.runOnServer(s -> {
				GodSlayer b = GodSlayer.TYPE.create(s.overworld(), EntitySpawnReason.MOB_SUMMONED);
				b.setBossLevel(1);
				b.snapTo(3.0, 100, 0.5, 90f, 0f);
				turn(b, 90f);
				b.setNoAi(true);
				s.overworld().addFreshEntity(b);
				box[0] = b;
			});
			context.waitTicks(10);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("90-god-slayer-front");
			server.runOnServer(s -> turn(box[0], 0f));
			context.waitTicks(30);
			context.takeScreenshot("91-god-slayer-side");
			// katana close-up: first person from the west, the boss turned to face south (a true side view of its right hand)
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("tp @a -3.2 103.6 1.7 -90 -28");
			context.waitTicks(10);
			context.takeScreenshot("98-god-slayer-katana");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("tp @a 0.5 100 0.5 -90 -12");
			server.runOnServer(s -> turn(box[0], -90f));
			context.waitTicks(25);
			context.takeScreenshot("92-god-slayer-back");
			// head turned to its max (the body stays): from behind, then from the side, to see that the head never cuts the mane
			server.runOnServer(s -> box[0].setYHeadRot(-90f + 50f));
			context.waitTicks(15);
			context.takeScreenshot("back-turned-a");
			server.runOnServer(s -> box[0].setYHeadRot(-90f - 50f));
			context.waitTicks(15);
			context.takeScreenshot("back-turned-b");
			server.runOnServer(s -> {
				turn(box[0], 0f);
				box[0].setYHeadRot(50f);
				box[0].setXRot(-30f);
			});
			context.waitTicks(15);
			context.takeScreenshot("back-turned-side");
			server.runOnServer(s -> {
				turn(box[0], -90f);
				box[0].setXRot(0f);
			});

			// face close-up: first person, the boss 3 blocks ahead facing the player, the player looks at its head
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("tp @a 0.5 100 0.5 -90 -38");
			server.runOnServer(s -> {
				box[0].snapTo(4.0, 100, 0.5, 90f, 0f);
				turn(box[0], 90f);
			});
			context.waitTicks(15);
			context.takeScreenshot("93-god-slayer-face");

			// flying, hovering: wings out, body upright
			server.runCommand("tp @a 0.5 100 0.5 -90 -22");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runOnServer(s -> {
				box[0].snapTo(3.0, 100.5, 0.5, 90f, 0f);
				turn(box[0], 90f);
				box[0].setFlyingMode(true);
			});
			context.waitTicks(15);
			context.takeScreenshot("94-god-slayer-flying-front");
			server.runOnServer(s -> turn(box[0], 0f));
			context.waitTicks(10);
			context.takeScreenshot("95-god-slayer-flying-side");

			server.runOnServer(s -> turn(box[0], -90f));
			context.waitTicks(25);
			context.takeScreenshot("97-god-slayer-flying-back");

			// flying forward, frozen with /boss freeze fly_forward: the 45 degree lean without moving (D104)
			server.runCommand("tp @a 0.5 100 0.5 -90 -8");
			server.runOnServer(s -> {
				box[0].snapTo(3.0, 100.0, -1.0, 0f, 0f);
				turn(box[0], 0f);
				box[0].setFreezePose(GodSlayer.FREEZE_FLY_FORWARD);
			});
			context.waitTicks(15);
			context.takeScreenshot("96-god-slayer-flying-forward");
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
