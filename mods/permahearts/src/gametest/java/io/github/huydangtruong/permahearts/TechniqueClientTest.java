package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.PhaseState;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;

/**
 * P-011 part C: screenshots of every technique effect in a real client (fire field, ice area, healing circle, repulse
 * ring, Blood Oath, Phase Walk) and a check that the client does not push a phasing player out of a wall. Run with
 * ./gradlew :permahearts:runClientGameTest. Screenshots are named 20-... to 29-... .
 */
public class TechniqueClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode creative @a");
			server.runCommand("fill -24 99 -24 24 99 24 stone");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			context.waitTicks(40);
			fireAndIce(context, server);
			healing(context, server);
			repulse(context, server);
			bloodOath(context, server);
			phaseWalk(context, server);
		}
	}

	private static void stand(ClientGameTestContext context, TestServerContext server, boolean thirdPerson) {
		server.runCommand("kill @e[type=husk]");
		server.runCommand("tp @a 0 100 0 -90 12");
		context.runOnClient(mc -> mc.options.setCameraType(thirdPerson ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON));
		context.waitTicks(10);
	}

	private static void cast(TestServerContext server, Technique t, int level) {
		server.runOnServer(s -> TechniqueCasts.cast(s.getPlayerList().getPlayers().get(0), t, level, 1f));
	}

	private static void fireAndIce(ClientGameTestContext context, TestServerContext server) {
		stand(context, server, false);
		server.runCommand("summon husk 8 100 0 {NoAI:1b}");
		context.waitTicks(5);
		cast(server, Technique.FIRE, 10);
		context.waitTicks(25);
		context.takeScreenshot("20-fire-field");
		stand(context, server, false);
		server.runCommand("summon husk 8 100 0 {NoAI:1b}");
		context.waitTicks(5);
		cast(server, Technique.ICE, 10);
		context.waitTicks(25);
		context.takeScreenshot("21-ice-area");
	}

	private static void healing(ClientGameTestContext context, TestServerContext server) {
		stand(context, server, true);
		cast(server, Technique.HEALING, 10);
		context.waitTicks(30);
		context.takeScreenshot("22-healing-circle");
	}

	private static void repulse(ClientGameTestContext context, TestServerContext server) {
		stand(context, server, true);
		server.runCommand("summon husk 2 100 2 {NoAI:1b}");
		server.runCommand("summon husk -2 100 2 {NoAI:1b}");
		server.runCommand("summon husk 3 100 -2");
		context.waitTicks(5);
		cast(server, Technique.REPULSE, 10);
		context.waitTicks(3);
		context.takeScreenshot("23-repulse-ring");
		server.runCommand("kill @e[type=husk]");
	}

	private static void bloodOath(ClientGameTestContext context, TestServerContext server) {
		stand(context, server, true);
		server.runOnServer(s -> s.getPlayerList().getPlayers().get(0).setHealth(20f));
		cast(server, Technique.SACRIFICE, 10);
		context.waitTicks(12);
		context.takeScreenshot("24-blood-oath");
		boolean active = server.computeOnServer(s -> TechniqueCasts.oathActive(s.getPlayerList().getPlayers().get(0)));
		if (!active) {
			throw new AssertionError("Blood Oath should be active");
		}
	}

	/** Casts Phase Walk, puts the player inside a wall and checks that the client keeps it there (no push-out). */
	private static void phaseWalk(ClientGameTestContext context, TestServerContext server) {
		stand(context, server, false);
		server.runCommand("fill 2 100 -4 4 103 4 stone");
		context.waitTicks(5);
		cast(server, Technique.PHASE, 1);
		context.waitTicks(5);
		boolean synced = context.computeOnClient(mc -> PhaseState.phasing(mc.player));
		if (!synced) {
			throw new AssertionError("the client should know the player is phasing (synced attachment)");
		}
		server.runCommand("tp @a 3.5 100 0 -90 12");
		context.waitTicks(30);
		double x = context.computeOnClient(mc -> mc.player.getX());
		if (x < 2.5 || x > 4.5) {
			throw new AssertionError("the client pushed the phasing player out of the wall, x = " + x);
		}
		context.takeScreenshot("25-phase-inside-wall");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot("26-phase-inside-wall-third-person");
		// When it ends inside the wall the player is lifted to free space.
		server.runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			TechniqueCasts.endPhase(p);
		});
		context.waitTicks(10);
		double y = context.computeOnClient(mc -> mc.player.getY());
		if (y < 104) {
			throw new AssertionError("the player should be lifted above the wall when Phase Walk ends, y = " + y);
		}
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
	}
}
