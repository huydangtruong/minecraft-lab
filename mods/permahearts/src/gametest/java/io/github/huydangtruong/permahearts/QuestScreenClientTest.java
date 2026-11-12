package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.wanderer.Quest;
import io.github.huydangtruong.permahearts.wanderer.QuestLine;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import io.github.huydangtruong.permahearts.wanderer.WandererMenu;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;

/**
 * Not a check: opens a real client in a throwaway world and takes screenshots of the Wanderer quest screen so a
 * human can look at them. Run with ./gradlew :permahearts:runClientGameTest. Fails only on exceptions.
 */
public class QuestScreenClientTest implements FabricClientGameTest {
	private static Quest quest() {
		return new Quest(1, List.of(
			new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 10, 0, "strength", false),
			new QuestLine(QuestType.KILL, "minecraft:zombie", 2, 0, "fury", false),
			new QuestLine(QuestType.BLOOD_MOON, "", 1, 0, "swiftness", false),
			new QuestLine(QuestType.BOSS, "minecraft:zombie", 1, 0, "iron_skin", false)), false);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			context.waitTicks(40);
			shots(context, server, ""); // the mod's text is English only (D16)
		}
	}

	private static void shots(ClientGameTestContext context, TestServerContext server, String suffix) {
		context.runOnClient(mc -> mc.player.closeContainer());
		float[] yawAtSpawn = new float[1];
		server.runOnServer(s -> {
			Wanderer.reset(s);
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			yawAtSpawn[0] = p.getYRot();
			Wanderer.startQuestForTest(p.level(), p.blockPosition().relative(p.getDirection(), 3), quest());
		});
		// The Wanderer stands 3 blocks ahead of the player's facing at spawn time; aim straight at it.
		context.runOnClient(mc -> {
			mc.player.setYRot(yawAtSpawn[0]);
			mc.player.setXRot(5f);
		});
		context.waitTicks(20);
		context.takeScreenshot("00-world-view" + suffix);
		context.getInput().pressKey(o -> o.keyUse); // right-click
		context.waitForScreen(AbstractContainerScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("01-quest-offer" + suffix);

		context.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, 0));
		context.waitTicks(20);
		context.takeScreenshot("02-quest-lines" + suffix);

		context.runOnClient(mc -> mc.player.closeContainer());
		server.runCommand("quest complete 1");
		context.waitTicks(5);
		context.getInput().pressKey(o -> o.keyUse);
		context.waitForScreen(AbstractContainerScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("03-line-completed" + suffix);

		// Hover the reward slot (the first slot of the menu) so its tooltip shows.
		double[] cursor = context.computeOnClient(mc -> {
			net.minecraft.world.inventory.Slot slot = mc.player.containerMenu.slots.get(0);
			double scale = mc.getWindow().getGuiScale();
			double left = (mc.getWindow().getGuiScaledWidth() - WandererMenu.PANEL_W) / 2.0;
			double top = (mc.getWindow().getGuiScaledHeight() - WandererMenu.PANEL_H) / 2.0;
			return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(10);
		context.takeScreenshot("04-reward-tooltip" + suffix);
		context.runOnClient(mc -> mc.player.closeContainer());
	}
}
