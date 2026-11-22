package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.client.TechniqueWheelScreen;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-067 / D188: the technique wheel screen in a real client (one screenshot, named 40-technique-wheel). */
public class WheelClientTest implements FabricClientGameTest {
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
			server.runCommand("tp @a 0 100 0 -90 12");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.FIRE, 10, 50).withLevel(Technique.THUNDER, 3, 10).withLevel(Technique.LANCE, 2, 10)
					.withLevel(Technique.ICE, 3, 10).withLevel(Technique.HEALING, 5, 20).withLevel(Technique.PHASE, 1, 5)
					.withLevel(Technique.FLIGHT, 2, 5).withLevel(Technique.GOLEM, 4, 10).withEmeralds(64 + 128 + 256)
					.withSlot(0, Technique.FIRE).withSlot(1, Technique.ICE).withSlot(2, Technique.HEALING)));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(5);
			check(ClientSkills.selected() == 0, "Z selects slot 1");
			ClientSkills.testWheelHeld = true;
			context.runOnClient(mc -> mc.gui.setScreen(new TechniqueWheelScreen(ClientSkills.selected())));
			context.waitTicks(5);
			check(ClientSkills.selected() == 0, "the wheel keeps the slot selected");
			context.takeScreenshot("40-technique-wheel");
			ClientSkills.testWheelHeld = false;
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() == null), "letting go of the key closes the wheel");
			check(ClientSkills.selected() == 0, "slot still selected after the wheel closes");

			// D193: from the cross. Slot 1 (Fire) is cooling, so the lowest ready slot is slot 2 (index 1).
			context.runOnClient(mc -> ClientSkills.backToHotbar());
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				long now = s.overworld().getGameTime();
				TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.FIRE, now + 20 * 600));
			});
			context.waitTicks(10);
			check(ClientSkills.selected() == -1, "on the cross");
			int hotbar = context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
			ClientSkills.testWheelHeld = true;
			context.getInput().pressKey(ClientSkills.WHEEL);
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof TechniqueWheelScreen), "R on the cross opens the wheel");
			check(ClientSkills.selected() == 1, "the lowest ready slot is selected (slot 1 is cooling)");
			check(context.computeOnClient(mc -> ((TechniqueWheelScreen) mc.gui.screen()).slot()) == 1, "the wheel edits slot 2");
			context.takeScreenshot("41-technique-wheel-cross");

			// scroll: down = next open slot, wraps; the hotbar does not move
			context.getInput().scroll(-1);
			context.waitTicks(3);
			check(ClientSkills.selected() == 2, "scroll down moves to the next slot");
			context.getInput().scroll(-1);
			context.waitTicks(3);
			check(ClientSkills.selected() == 0, "scroll down wraps to the first slot");
			context.getInput().scroll(1);
			context.waitTicks(3);
			check(ClientSkills.selected() == 2, "scroll up wraps to the last open slot");
			check(context.computeOnClient(mc -> ((TechniqueWheelScreen) mc.gui.screen()).slot()) == 2, "the wheel follows the scroll");
			check(context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == hotbar, "the hotbar did not scroll");

			// D196: a pick keeps the wheel open; scroll on and pick again; only letting go of R closes it
			context.runOnClient(mc -> ((TechniqueWheelScreen) mc.gui.screen()).choose(ClientSkills.data(mc.player), Technique.FLIGHT));
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof TechniqueWheelScreen), "the wheel stays open after a pick");
			check(context.computeOnClient(mc -> ClientSkills.data(mc.player).inSlot(2)) == Technique.FLIGHT, "slot 3 now holds Flight");
			context.getInput().scroll(1);
			context.waitTicks(3);
			check(ClientSkills.selected() == 1, "scroll still works after a pick");
			context.runOnClient(mc -> ((TechniqueWheelScreen) mc.gui.screen()).choose(ClientSkills.data(mc.player), Technique.GOLEM));
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof TechniqueWheelScreen), "still open after a second pick");
			check(context.computeOnClient(mc -> ClientSkills.data(mc.player).inSlot(1)) == Technique.GOLEM, "slot 2 now holds Golem");
			context.getInput().scroll(-1);
			context.waitTicks(3);
			ClientSkills.testWheelHeld = false;
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() == null), "the wheel closes on release");
			check(ClientSkills.selected() == 2, "the scrolled slot stays selected");

			// all ready: R from the cross picks slot 1; nothing open: R does nothing
			context.runOnClient(mc -> ClientSkills.backToHotbar());
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				TechniqueAttachment.set(p, TechniqueAttachment.get(p).withoutCooldowns());
			});
			context.waitTicks(10);
			ClientSkills.testWheelHeld = true;
			context.getInput().pressKey(ClientSkills.WHEEL);
			context.waitTicks(5);
			check(ClientSkills.selected() == 0, "with every slot ready the first slot is chosen");
			ClientSkills.testWheelHeld = false;
			context.waitTicks(5);
			context.runOnClient(mc -> ClientSkills.backToHotbar());
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				TechniqueAttachment.set(p, TechniqueAttachment.get(p).withEmeralds(0));
			});
			context.waitTicks(10);
			ClientSkills.testWheelHeld = true;
			context.getInput().pressKey(ClientSkills.WHEEL);
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() == null), "no open slot: the wheel stays shut");
			ClientSkills.testWheelHeld = false;
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
