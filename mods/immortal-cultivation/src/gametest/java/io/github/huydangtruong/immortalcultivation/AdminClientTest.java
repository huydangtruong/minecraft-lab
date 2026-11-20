package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.admin.AdminNetworking;
import io.github.huydangtruong.immortalcultivation.client.AdminScreen;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-060 (D155): /admin opens the panel, clicks write lines (a switch replaces), Esc keeps the draft, Confirm closes and runs. Screenshots admin-player, admin-techniques, admin-world. */
public class AdminClientTest implements FabricClientGameTest {
	private static void open(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().get(0);
			s.getPlayerList().op(p.nameAndId(), java.util.Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), java.util.Optional.empty());
			AdminNetworking.open(p);
		});
		context.waitTicks(10);
		if (!context.computeOnClient(mc -> mc.gui.screen() instanceof AdminScreen)) {
			throw new AssertionError("/admin did not open the panel");
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			context.waitTicks(20);
			open(context, server);
			AdminScreen.draft().clear();
			context.clickScreenButton("Set hearts");
			context.clickScreenButton("Set hearts"); // same key: replaces, not a second line
			context.clickScreenButton("Testmode full");
			context.waitTicks(3);
			if (AdminScreen.draft().lines().size() != 2 || !AdminScreen.draft().lines().get(0).equals("hearts set @s 10")) {
				throw new AssertionError("expected 2 lines, got " + AdminScreen.draft().lines());
			}
			context.takeScreenshot("admin-player");
			context.clickScreenButton("Techniques");
			context.waitTicks(3);
			context.clickScreenButton("Cast");
			context.takeScreenshot("admin-techniques");
			context.clickScreenButton("Danger");
			context.clickScreenButton("Info");
			context.clickScreenButton("World");
			context.clickScreenButton("Cultivation: ON");
			context.waitTicks(3);
			context.takeScreenshot("admin-world");
			int before = AdminScreen.draft().lines().size();
			context.runOnClient(mc -> mc.gui.screen().onClose()); // Esc: closes without running, keeps the draft
			context.waitTicks(5);
			open(context, server);
			if (AdminScreen.draft().lines().size() != before || before < 5) {
				throw new AssertionError("the draft was not kept: " + AdminScreen.draft().lines());
			}
			context.clickScreenButton("Confirm");
			context.waitTicks(10);
			if (context.computeOnClient(mc -> mc.gui.screen() instanceof AdminScreen)) {
				throw new AssertionError("Confirm did not close the panel");
			}
			AtomicInteger hearts = new AtomicInteger();
			server.runOnServer(s -> hearts.set(HeartRules.maxHearts(HeartsAttachment.get(s.getPlayerList().getPlayers().get(0)))));
			if (hearts.get() != 50) {
				throw new AssertionError("Confirm did not run the lines in order (hearts set 10, then testmode full = 50): hearts " + hearts.get());
			}
			if (!AdminScreen.draft().isEmpty()) {
				throw new AssertionError("the draft was not cleared after Confirm");
			}
		}
	}
}
