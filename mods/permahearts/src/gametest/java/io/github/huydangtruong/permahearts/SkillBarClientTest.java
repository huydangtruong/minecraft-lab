package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.ClientSkills;
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
 * P-011 part B: the skill bar in a real client. Key R toggles technique mode (techniques first in the hotbar), scroll and number
 * keys on the shifted view, right click charge with zoom, left click cast, cooldown overlay. Screenshots are named 30-... to 39-... .
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
			server.runCommand("item replace entity @a hotbar.0 with iron_sword");
			server.runCommand("item replace entity @a hotbar.1 with iron_axe");
			server.runCommand("item replace entity @a hotbar.8 with apple");
			context.waitTicks(10);
			check(!mode(context), "technique mode starts off");
			check(realSlot(context) == 0, "the sword slot is selected");
			context.takeScreenshot("30-hotbar-mode-off");

			// R toggles technique mode: cell 0 stays selected, now showing technique 1 (Flame Burst)
			context.getInput().pressKey(ClientSkills.KEY);
			context.waitTicks(10);
			check(mode(context), "R should turn technique mode on");
			check(selected(context) == 0, "cell 0 should now select slot 0, was " + selected(context));
			check(serverSelection(server) == 0, "the server should know the selection, was " + serverSelection(server));
			context.takeScreenshot("31-hotbar-mode-on-technique-selected");

			// scroll to cell 3 = item slot 0 (the sword): vanilla frame, item selection synced
			scrollToCell(context, 3);
			context.waitTicks(5);
			check(selected(context) == -1, "an item cell selects no technique");
			check(realSlot(context) == 0, "cell 3 shows item slot 0, real slot was " + realSlot(context));
			context.takeScreenshot("32-hotbar-mode-on-sword-cell");
			scrollToCell(context, 4);
			context.waitTicks(5);
			check(realSlot(context) == 1, "cell 4 shows item slot 1 (axe), real slot was " + realSlot(context));
			check(serverHeld(server) == 1, "the server should hold slot 1, was " + serverHeld(server));
			context.takeScreenshot("33-hotbar-mode-on-axe-cell");
			check(context.computeOnClient(mc -> mc.player.getInventory().getItem(0).is(net.minecraft.world.item.Items.IRON_SWORD)),
				"the inventory must not be moved");

			// number key 2 picks cell 1 = Frost Orb; left click casts it at once
			context.getInput().pressKey(o -> o.keyHotbarSlots[1]);
			context.waitTicks(5);
			check(selected(context) == 1, "number key 2 should select slot 1, was " + selected(context));
			context.getInput().pressMouse(LEFT);
			context.waitTicks(10);
			check(onCooldown(server, Technique.ICE), "left click should cast Frost Orb and start its cooldown");
			context.takeScreenshot("34-frost-cast-cooldown");
			context.getInput().pressMouse(LEFT);
			context.waitTicks(3);
			context.takeScreenshot("35-on-cooldown-message");

			// Flame Burst: number key 1, hold right click to charge (zoom + power bar), release to throw
			server.runCommand("summon husk 9 100 0 {NoAI:1b}");
			context.getInput().pressKey(o -> o.keyHotbarSlots[0]);
			context.waitTicks(5);
			check(selected(context) == 0, "number key 1 should select slot 0, was " + selected(context));
			float fov0 = context.computeOnClient(mc -> mc.player.getFieldOfViewModifier(true, 1f));
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(10);
			check(context.computeOnClient(mc -> ClientSkills.charging()), "holding right click should charge Flame Burst");
			context.waitTicks(10);
			float fov1 = context.computeOnClient(mc -> mc.player.getFieldOfViewModifier(true, 1f));
			check(fov1 < fov0 - 0.05f, "charging should zoom like a bow: " + fov0 + " -> " + fov1);
			context.takeScreenshot("36-charging-zoom-power-bar");
			context.getInput().releaseMouse(RIGHT);
			context.waitTicks(25);
			check(onCooldown(server, Technique.FIRE), "releasing right click should cast Flame Burst");
			context.takeScreenshot("37-flame-cast-right-bar-cooldowns");
			server.runCommand("kill @e[type=husk]");

			// R again: mode off, the cell index stays (cell 0 = item slot 0 = the sword)
			context.getInput().pressKey(ClientSkills.KEY);
			context.waitTicks(5);
			check(!mode(context), "R should turn technique mode off");
			check(selected(context) == -1 && serverSelection(server) == -1, "no technique selected with the mode off");
			check(realSlot(context) == 0, "cell 0 shows the sword again, real slot was " + realSlot(context));
			context.takeScreenshot("38-hotbar-mode-off-again");

			// focus item and hand particles for every technique, first person; the last one also in third person
			Technique[] all = Technique.values();
			for (int i = 0; i < all.length; i++) {
				Technique t = all[i];
				server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
					TechniqueData.DEFAULT.withLevel(t, 5, 10).withEmeralds(64).withSlot(0, t)));
				context.getInput().pressKey(ClientSkills.KEY); // mode on: cell 0 = the technique
				context.waitTicks(20);
				check(selected(context) == 0, "slot 0 should stay selected for " + t.id() + ", was " + selected(context));
				check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == t), "the focus item should be " + t.id());
				context.takeScreenshot("4" + i + "-focus-" + t.id());
				context.getInput().pressKey(ClientSkills.KEY); // mode off
				context.waitTicks(3);
			}
			context.getInput().pressKey(ClientSkills.KEY);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(15);
			context.takeScreenshot("46-focus-third-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private static boolean mode(ClientGameTestContext context) {
		return context.computeOnClient(mc -> ClientSkills.mode());
	}

	private static int realSlot(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
	}

	private static int serverHeld(TestServerContext server) {
		return server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getInventory().getSelectedSlot());
	}

	/** Scrolls the mouse wheel until the technique-mode cell is reached (at most 9 steps). */
	private static void scrollToCell(ClientGameTestContext context, int cell) {
		for (int i = 0; i < 9 && context.computeOnClient(mc -> ClientSkills.cell()) != cell; i++) {
			context.getInput().scroll(-1);
			context.waitTicks(2);
		}
		check(context.computeOnClient(mc -> ClientSkills.cell()) == cell, "scrolling should reach cell " + cell);
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
