package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/** P-019: Thousand Mile Step in a real client (R, cell 0 = teleport). Screenshots 60-... to 62-... . */
public class TeleportClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;
	private static final int RIGHT = 3;

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
				TechniqueData.DEFAULT.withLevel(Technique.TELEPORT, 1, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.TELEPORT)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEY);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.TELEPORT), "the focus is in hand");
			context.takeScreenshot("60-teleport-focus-in-hand");

			// holding right click: 1.5 s is half of the 3 s full charge
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(30);
			float half = context.computeOnClient(mc -> ClientSkills.chargeFraction());
			check(Math.abs(half - 0.5f) < 0.12f, "1.5 s of 3 s is about half a charge, was " + half);
			context.takeScreenshot("61-teleport-charging");
			context.getInput().releaseMouse(RIGHT);
			context.waitTicks(20);
			double moved = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getX());
			check(moved > 40 && moved < 110, "half charge moves about 75 blocks along +x, x = " + moved);

			// clear the cooldown and use a left click (full charge at once)
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueAttachment.get(s.getPlayerList().getPlayers().get(0)).withCooldown(Technique.TELEPORT, 0)));
			server.runCommand("tp @a 0 100 0 -90 0");
			context.waitTicks(10);
			context.getInput().pressMouse(LEFT);
			context.waitTicks(30);
			double x = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getX());
			check(x > 120, "left click casts at full charge: about 150 blocks, x = " + x);
			context.takeScreenshot("62-teleport-after");
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
