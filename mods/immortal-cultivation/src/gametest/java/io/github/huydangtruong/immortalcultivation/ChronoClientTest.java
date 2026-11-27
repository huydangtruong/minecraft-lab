package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientChrono;
import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.chrono.Chrono;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * P-083 / D268: Time Rewind in a real client. Screenshots 130-chrono-inside (first person in the stopped box: all grey, the
 * timeline above the bars), 131-chrono-rewound (after two wheel notches down), 132-chrono-outside (the grey box seen from
 * outside) and 133-chrono-techniques-tab.
 */
public class ChronoClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -40 99 -40 40 99 40 grass_block");
			server.runCommand("fill -40 100 -40 40 118 40 air");
			server.runCommand("fill 3 100 6 5 102 6 red_wool");
			server.runCommand("fill -5 100 6 -3 102 6 lime_wool");
			server.runCommand("fill -2 100 20 2 104 20 blue_wool");
			server.runCommand("tp @a 0.5 100 0.5 0 8");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.CHRONO, 10, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.CHRONO)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("summon pig 1 100 4 {NoAI:1b}");
			server.runCommand("summon pig -6 100 4 {NoAI:1b,Tags:[\"chrono_t\"]}");
			context.waitTicks(20);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(60);
			server.runCommand("tp @e[tag=chrono_t] -2 100 4");
			context.waitTicks(10);
			context.runOnClient(mc -> ClientSkills.cast(mc, 0, 1f));
			context.waitTicks(5);
			check(context.computeOnClient(mc -> ClientChrono.own(mc) != null), "the client knows the stop");
			context.takeScreenshot("130-chrono-inside");
			context.getInput().scroll(-1);
			context.getInput().scroll(-1);
			context.waitTicks(15);
			int shown = server.computeOnServer(s -> Chrono.shown(s.getPlayerList().getPlayers().get(0)));
			check(shown == 2 * TechniqueRules.CHRONO_NOTCH_TICKS, "two notches went back 1 s, shown " + shown);
			context.takeScreenshot("131-chrono-rewound");
			server.runCommand("tp @a 0.5 100 -22 0 15");
			context.waitTicks(10);
			context.takeScreenshot("132-chrono-outside");
			server.runCommand("tp @a 0.5 100 0.5 0 8");
			context.waitTicks(5);
			context.runOnClient(mc -> ClientSkills.cast(mc, 0, 1f));
			context.waitTicks(5);
			check(context.computeOnClient(mc -> ClientChrono.own(mc) == null), "the recast let time run");

			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.clickScreenButton("Cultivation");
			context.waitTicks(3);
			context.clickScreenButton("Techniques");
			context.waitTicks(5);
			context.takeScreenshot("133-chrono-techniques-tab");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
