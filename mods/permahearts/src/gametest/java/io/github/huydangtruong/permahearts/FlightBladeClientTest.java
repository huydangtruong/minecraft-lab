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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.monster.zombie.Husk;

/**
 * P-014: Life Steal Blade and Flight in a real client. R = technique mode, cell 0 = blade, cell 1 = flight. Screenshots
 * 50-... to 54-... . Run with ./gradlew :permahearts:runClientGameTest.
 */
public class FlightBladeClientTest implements FabricClientGameTest {
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
				TechniqueData.DEFAULT.withLevel(Technique.BLADE, 5, 20).withLevel(Technique.FLIGHT, 10, 40)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.BLADE).withSlot(1, Technique.FLIGHT)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			server.runCommand("item replace entity @a hotbar.0 with diamond_sword");
			server.runCommand("summon husk 2.2 100 0 {NoAI:1b}");
			server.runOnServer(s -> s.getPlayerList().getPlayers().get(0).setHealth(8f));
			context.waitTicks(10);

			// blade: R, cell 0 = the blade; the crosshair is on the husk; left click hits
			context.getInput().pressKey(ClientSkills.KEY);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.BLADE), "the blade is in hand");
			context.takeScreenshot("50-blade-in-hand");
			float husk0 = server.computeOnServer(FlightBladeClientTest::huskHealth);
			context.getInput().pressMouse(LEFT);
			context.waitTicks(4);
			context.takeScreenshot("51-blade-hit");
			context.waitTicks(10);
			float husk1 = server.computeOnServer(FlightBladeClientTest::huskHealth);
			check(husk1 < husk0 - 3, "the blade should have hurt the husk: " + husk0 + " -> " + husk1);
			float hp = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getHealth());
			check(hp > 8f, "the player should have healed, health " + hp);
			check(server.computeOnServer(s -> TechniqueAttachment.get(s.getPlayerList().getPlayers().get(0))
				.onCooldown(Technique.BLADE, s.overworld().getGameTime())), "the hit starts the cooldown");
			context.takeScreenshot("52-blade-cooldown");
			context.getInput().pressMouse(LEFT);
			context.waitTicks(4);
			check(server.computeOnServer(FlightBladeClientTest::huskHealth) == husk1, "no hit while on cooldown");
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(5);
			context.getInput().releaseMouse(RIGHT);
			check(server.computeOnServer(FlightBladeClientTest::huskHealth) == husk1, "right click does nothing with the blade");
			server.runCommand("kill @e[type=husk]");

			// flight: cell 1, right click casts at once
			context.getInput().pressKey(o -> o.keyHotbarSlots[1]);
			context.waitTicks(5);
			check(context.computeOnClient(mc -> ClientSkills.selected()) == 1, "flight cell selected: mode " + context.computeOnClient(mc -> ClientSkills.mode())
				+ " cell " + context.computeOnClient(mc -> ClientSkills.cell()) + " sel " + context.computeOnClient(mc -> ClientSkills.selected()));
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(5);
			context.getInput().releaseMouse(RIGHT);
			context.waitTicks(20);
			check(context.computeOnClient(mc -> mc.player.getAbilities().mayfly && mc.player.getAbilities().flying), "the client should be flying: sel " + context.computeOnClient(mc -> ClientSkills.selected()) + ", server end "
				+ server.computeOnServer(sv -> sv.getPlayerList().getPlayers().get(0).getAttached(io.github.huydangtruong.permahearts.technique.Flight.END))
				+ ", server may/flying " + server.computeOnServer(sv -> sv.getPlayerList().getPlayers().get(0).getAbilities().mayfly + "/" + sv.getPlayerList().getPlayers().get(0).getAbilities().flying)
				+ ", client " + context.computeOnClient(mc -> mc.player.getAbilities().mayfly + "/" + mc.player.getAbilities().flying + " ground " + mc.player.onGround()));
			check(context.computeOnClient(mc -> Math.abs(mc.player.getAbilities().getFlyingSpeed() - 0.075f) < 1e-5f), "flying speed x1.5 on the client");
			server.runCommand("tp @a 0 104 0 -90 10");
			context.waitTicks(10);
			context.takeScreenshot("53-flight-hud-timer");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot("54-flight-third-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	private static float huskHealth(MinecraftServer s) {
		for (var e : s.overworld().getAllEntities()) {
			if (e instanceof Husk h) {
				return h.getHealth();
			}
		}
		return -1f;
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
