package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.StatsPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** D217: the trash slot is drawn where the Notes button was; clicking it with a carried stack destroys the stack. */
public class TrashClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			context.waitTicks(30);
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("trash-slot");
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				p.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 12));
				p.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
			int[] t = context.computeOnClient(mc -> StatsPanel.trashCenter());
			context.getInput().setCursorPos(t[0] * scale, t[1] * scale);
			context.waitTicks(10);
			context.takeScreenshot("trash-hover-carrying");
			context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			boolean carrying = context.computeOnClient(mc -> !mc.player.containerMenu.getCarried().isEmpty());
			if (carrying) {
				throw new AssertionError("the carried stack survived a click on the trash slot");
			}
			boolean serverCarrying = server.computeOnServer(s -> !s.getPlayerList().getPlayers().get(0).containerMenu.getCarried().isEmpty());
			if (serverCarrying) {
				throw new AssertionError("the server still has a carried stack");
			}
			context.takeScreenshot("trash-after");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}
}
