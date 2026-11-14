package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import io.github.huydangtruong.permahearts.client.SkillWheelScreen;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.TechniqueSelection;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * P-011 part B: the skill bar in a real client. Key R (tap = next, hold = wheel), right click charge, left click cast,
 * cooldown overlay, hotbar scroll reset. Screenshots are named 30-... to 39-... .
 */
public class SkillBarClientTest implements FabricClientGameTest {
	// SDL numbering of Minecraft 26.3 (InputConstants.MOUSE_BUTTON_LEFT / RIGHT)
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
			server.runCommand("tp @a 0 100 0 -90 12");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.FIRE, 10, 50).withLevel(Technique.ICE, 3, 10)
					.withLevel(Technique.HEALING, 5, 20).withEmeralds(64 + 256 + 512)
					.withSlot(0, Technique.FIRE).withSlot(1, Technique.ICE).withSlot(2, Technique.HEALING)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.takeScreenshot("30-skill-bar-cross");

			// tap R: next entry (cross -> slot 0, Flame Burst)
			context.getInput().pressKey(ClientSkills.KEY);
			context.waitTicks(10);
			check(selected(context) == 0, "tapping R should select slot 0, was " + selected(context));
			check(serverSelection(server) == 0, "the server should know the selection, was " + serverSelection(server));
			context.takeScreenshot("31-skill-bar-fire-selected");

			// hold R: the wheel; mouse to the bottom entry (slot 1, Frost Orb); release picks it
			context.getInput().holdKey(ClientSkills.KEY);
			context.waitTicks(12);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof SkillWheelScreen), "holding R should open the wheel");
			context.getInput().setCursorPos(windowX(context, 0), windowY(context, 52));
			context.waitTicks(3);
			context.takeScreenshot("32-wheel-hover-frost");
			context.getInput().releaseKey(ClientSkills.KEY);
			context.waitTicks(5);
			check(context.computeOnClient(mc -> mc.gui.screen() == null), "releasing R should close the wheel");
			check(selected(context) == 1, "the wheel should have picked slot 1, was " + selected(context));

			// left click casts Frost Orb at once with full charge
			context.getInput().pressMouse(LEFT);
			context.waitTicks(10);
			check(onCooldown(server, Technique.ICE), "left click should cast Frost Orb and start its cooldown");
			context.takeScreenshot("33-frost-cast-cooldown");
			// a second left click while on cooldown shows the message and casts nothing
			context.getInput().pressMouse(LEFT);
			context.waitTicks(3);
			context.takeScreenshot("34-on-cooldown-message");

			// Flame Burst: hold right click to charge, release to throw
			server.runCommand("summon husk 9 100 0 {NoAI:1b}");
			context.getInput().pressKey(ClientSkills.KEY); // slot 1 -> slot 2
			context.waitTicks(5);
			context.getInput().pressKey(ClientSkills.KEY); // slot 2 -> cross
			context.waitTicks(5);
			context.getInput().pressKey(ClientSkills.KEY); // cross -> slot 0
			context.waitTicks(5);
			check(selected(context) == 0, "three more taps should come back to slot 0, was " + selected(context));
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(10);
			check(context.computeOnClient(mc -> ClientSkills.charging()), "holding right click should charge Flame Burst");
			context.takeScreenshot("35-charging");
			context.waitTicks(15);
			context.getInput().releaseMouse(RIGHT);
			context.waitTicks(25);
			check(onCooldown(server, Technique.FIRE), "releasing right click should cast Flame Burst");
			context.takeScreenshot("36-flame-cast");

			// the real hotbar item is not used: right click with a selected technique does nothing else
			// scrolling the hotbar goes back to the cross
			context.getInput().scroll(1);
			context.waitTicks(5);
			check(selected(context) == -1, "scrolling the hotbar should reset to the cross, was " + selected(context));
			check(serverSelection(server) == -1, "the server should see the cross again");
			context.takeScreenshot("37-after-scroll");
			server.runCommand("kill @e[type=husk]");
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private static int selected(ClientGameTestContext context) {
		return context.computeOnClient(mc -> ClientSkills.selected());
	}

	private static int serverSelection(TestServerContext server) {
		return server.computeOnServer(s -> TechniqueSelection.get(s.getPlayerList().getPlayers().get(0)));
	}

	private static boolean onCooldown(TestServerContext server, Technique t) {
		return server.computeOnServer(s -> TechniqueAttachment.get(s.getPlayerList().getPlayers().get(0))
			.onCooldown(t, s.overworld().getGameTime()));
	}

	/** Window pixel x of the GUI centre plus an offset in GUI pixels. */
	private static double windowX(ClientGameTestContext context, double offset) {
		return context.computeOnClient(mc -> (mc.getWindow().getGuiScaledWidth() / 2.0 + offset) * mc.getWindow().getGuiScale());
	}

	private static double windowY(ClientGameTestContext context, double offset) {
		return context.computeOnClient(mc -> (mc.getWindow().getGuiScaledHeight() / 2.0 + offset) * mc.getWindow().getGuiScale());
	}
}
