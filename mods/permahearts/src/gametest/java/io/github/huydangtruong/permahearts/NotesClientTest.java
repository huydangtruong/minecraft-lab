package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.NotesTab;
import io.github.huydangtruong.permahearts.client.StatsPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-042 (D117): the Notes button opens the panel, a note is sent and listed, a screenshot of the page. */
public class NotesClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			context.waitTicks(30);
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.clickScreenButton("Notes");
			context.waitTicks(5);
			if (!context.computeOnClient(mc -> StatsPanel.isOpen() && StatsPanel.tab() == StatsPanel.Tab.NOTES)) {
				throw new AssertionError("the Notes button did not open the Notes page");
			}
			context.runOnClient(mc -> {
				NotesTab.setText("buy more iron");
				NotesTab.send();
			});
			context.waitTicks(10);
			context.runOnClient(mc -> {
				NotesTab.setText("second private?");
			});
			context.takeScreenshot("notes-shared");
			if (context.computeOnClient(mc -> NotesTab.current().size()) != 1) {
				throw new AssertionError("the sent note did not come back in the shared list");
			}
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}
}
