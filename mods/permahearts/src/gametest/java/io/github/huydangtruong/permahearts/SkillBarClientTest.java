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
 * P-011 part B / P-045: the skill bar in a real client. Keys Z X C V select a technique slot, the wheel runs over the hotbar and
 * the filled slots, number keys go back to the hotbar, right click charge with zoom, left click cast, cooldown overlay. Screenshots are named 30-... to 39-... .
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
					.withLevel(Technique.HEALING, 5, 20).withEmeralds(64 + 128 + 256)
					.withSlot(0, Technique.FIRE).withSlot(1, Technique.ICE).withSlot(2, Technique.HEALING)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			server.runCommand("item replace entity @a hotbar.0 with iron_sword");
			server.runCommand("item replace entity @a hotbar.1 with iron_axe");
			server.runCommand("item replace entity @a hotbar.8 with apple");
			context.waitTicks(10);
			check(selected(context) == -1, "the hotbar starts selected");
			check(realSlot(context) == 0, "the sword slot is selected");
			context.takeScreenshot("30-hotbar-vanilla");

			// Z selects slot 1 (Flame Burst): the real slot stays, the server knows the selection
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(10);
			check(selected(context) == 0, "Z should select slot 0, was " + selected(context));
			check(realSlot(context) == 0, "the real slot must not move, was " + realSlot(context));
			check(serverSelection(server) == 0, "the server should know the selection, was " + serverSelection(server));
			context.takeScreenshot("31-technique-selected-no-hotbar-frame");

			// V (slot 4) is empty: nothing happens
			context.getInput().pressKey(ClientSkills.KEYS[3]);
			context.waitTicks(3);
			check(selected(context) == 0, "an empty slot key must do nothing, selected " + selected(context));
			// pressing Z again keeps it selected
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(3);
			check(selected(context) == 0, "Z again keeps slot 0 selected");

			// scroll ring: technique 1 -> 2 -> 3 -> hotbar 1 (3 filled techniques: positions 9, 10, 11)
			scroll(context, 1);
			check(selected(context) == 1, "scroll down from technique 1 -> technique 2, was " + selected(context));
			scroll(context, 1);
			check(selected(context) == 2, "scroll down -> technique 3, was " + selected(context));
			scroll(context, 1);
			check(selected(context) == -1 && realSlot(context) == 0, "scroll down from the last technique -> hotbar 1, real slot " + realSlot(context));
			scroll(context, -1);
			check(selected(context) == 2, "scroll up from hotbar 1 -> last technique, was " + selected(context));
			scroll(context, -1);
			scroll(context, -1);
			scroll(context, -1);
			check(selected(context) == -1 && realSlot(context) == 8, "scroll up from the first technique -> hotbar 9, real slot " + realSlot(context));
			check(serverHeld(server) == 8, "the server should hold hotbar 9, was " + serverHeld(server));
			scroll(context, 1);
			check(selected(context) == 0, "scroll down from hotbar 9 -> first technique, was " + selected(context));
			check(realSlot(context) == 8, "the real slot stays on hotbar 9 while a technique is selected");
			context.takeScreenshot("32-ring-first-technique");

			// number key 2 goes back to hotbar slot 2 (the axe) and clears the technique
			context.getInput().pressKey(o -> o.keyHotbarSlots[1]);
			context.waitTicks(5);
			check(selected(context) == -1, "number key 2 should clear the technique, was " + selected(context));
			check(realSlot(context) == 1, "number key 2 selects hotbar slot 2, was " + realSlot(context));
			check(serverHeld(server) == 1, "the server should hold slot 1, was " + serverHeld(server));
			check(context.computeOnClient(mc -> mc.player.getInventory().getItem(0).is(net.minecraft.world.item.Items.IRON_SWORD)),
				"the inventory must not be moved");
			context.takeScreenshot("33-back-to-hotbar-axe");

			// X selects Frost Orb; left click casts it at once
			context.getInput().pressKey(ClientSkills.KEYS[1]);
			context.waitTicks(5);
			check(selected(context) == 1, "X should select slot 1, was " + selected(context));
			context.getInput().pressMouse(LEFT);
			context.waitTicks(10);
			check(onCooldown(server, Technique.ICE), "left click should cast Frost Orb and start its cooldown");
			context.takeScreenshot("34-frost-cast-cooldown");
			context.getInput().pressMouse(LEFT);
			context.waitTicks(3);
			context.takeScreenshot("35-on-cooldown-message");

			// Flame Burst: Z, hold right click to charge (zoom + power bar), release to throw
			server.runCommand("summon husk 9 100 0 {NoAI:1b}");
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(5);
			check(selected(context) == 0, "Z should select slot 0, was " + selected(context));
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

			// number key 1 goes back to the sword
			context.getInput().pressKey(o -> o.keyHotbarSlots[0]);
			context.waitTicks(5);
			check(selected(context) == -1 && serverSelection(server) == -1, "no technique selected after a number key");
			check(realSlot(context) == 0, "the sword is selected again, real slot was " + realSlot(context));
			context.takeScreenshot("38-hotbar-again");

			// a slot that is cleared while selected sends the player back to the hotbar
			context.getInput().pressKey(ClientSkills.KEYS[2]);
			context.waitTicks(3);
			check(selected(context) == 2, "C should select slot 2, was " + selected(context));
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueAttachment.get(s.getPlayerList().getPlayers().get(0)).withSlot(2, null)));
			context.waitTicks(10);
			check(selected(context) == -1, "clearing the selected slot goes back to the hotbar, was " + selected(context));

			// focus item and hand particles for every technique, first person; the last one also in third person
			Technique[] all = Technique.values();
			for (int i = 0; i < all.length; i++) {
				Technique t = all[i];
				server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
					TechniqueData.DEFAULT.withLevel(t, 5, 10).withEmeralds(64).withSlot(0, t)));
				context.getInput().pressKey(ClientSkills.KEYS[0]); // Z: slot 0 = the technique
				context.waitTicks(20);
				check(selected(context) == 0, "slot 0 should stay selected for " + t.id() + ", was " + selected(context));
				check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == t), "the focus item should be " + t.id());
				context.takeScreenshot("4" + i + "-focus-" + t.id());
				context.getInput().pressKey(o -> o.keyHotbarSlots[0]); // back to the hotbar
				context.waitTicks(3);
			}
			context.getInput().pressKey(ClientSkills.KEYS[0]);
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

	private static int realSlot(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
	}

	private static int serverHeld(TestServerContext server) {
		return server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getInventory().getSelectedSlot());
	}

	/** One wheel notch: +1 = down (next on the ring), -1 = up. */
	private static void scroll(ClientGameTestContext context, int direction) {
		context.getInput().scroll(-direction);
		context.waitTicks(3);
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
