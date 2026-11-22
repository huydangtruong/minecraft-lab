package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.PassivesTab;
import io.github.huydangtruong.immortalcultivation.client.StatsPanel;
import io.github.huydangtruong.immortalcultivation.passive.Passive;
import io.github.huydangtruong.immortalcultivation.passive.PassiveAttachment;
import io.github.huydangtruong.immortalcultivation.passive.PassiveData;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** P-057 (D148): the Passives tab, a real "+" click, the Vision toggle, the detail page, and screenshots. */
public class PassiveClientTest implements FabricClientGameTest {
	private static final int LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;

	private static void clickGui(ClientGameTestContext context, int[] gui) {
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(2);
		context.getInput().pressMouse(LEFT);
		context.waitTicks(6);
	}

	private static void clickGuiHover(ClientGameTestContext context, int[] gui) {
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(10);
	}

	private static int[] spot(ClientGameTestContext context, int i) {
		List<int[]> s = context.computeOnClient(mc -> StatsPanel.passiveHotspots());
		return s.get(i);
	}

	private static PassiveData data(ClientGameTestContext context) {
		return context.computeOnClient(mc -> PassiveAttachment.get(mc.player));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("xp add @a 20 levels");
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				PassiveAttachment.set(p, PassiveData.DEFAULT.withLevel(Passive.COOLDOWN, 4).withLevel(Passive.VISION, 10)
					.withLevel(Passive.HUNGER, 2).withLevel(Passive.INVENTORY, 7).withLevel(Passive.GUARD, 4));
				var broken = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
				io.github.huydangtruong.immortalcultivation.passive.ItemGuard.breakGuarded(broken, 4, broken.getMaxDamage() * 2); // worn to the Lv 4 cap (D179)
				p.getInventory().setItem(0, broken);
			});
			context.waitTicks(30);
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.clickScreenButton("Cultivation");
			context.waitTicks(3);
			if (!context.computeOnClient(mc -> StatsPanel.isOpen())) { // the panel state is static: an earlier test may have left it open
				context.clickScreenButton("Cultivation");
				context.waitTicks(3);
			}
			context.clickScreenButton("Passives");
			context.waitTicks(5);
			if (!context.computeOnClient(mc -> StatsPanel.isOpen() && StatsPanel.tab() == StatsPanel.Tab.PASSIVES)) {
				throw new AssertionError("the Passives tab did not open");
			}
			int n = Passive.values().length;
			clickGui(context, spot(context, n + Passive.XP.ordinal())); // "+" of XP gain bonus
			context.waitTicks(5);
			if (data(context).level(Passive.XP) != 1) {
				throw new AssertionError("the + button did not upgrade XP gain bonus");
			}
			if (context.computeOnClient(mc -> mc.player.experienceLevel) != 15) {
				throw new AssertionError("the upgrade should cost 5 levels");
			}
			clickGui(context, spot(context, 2 * n + Passive.VISION.ordinal())); // the Vision switch: off
			context.waitTicks(5);
			if (data(context).visionOn()) {
				throw new AssertionError("the Vision switch did not turn it off");
			}
			context.takeScreenshot("passives-list");
			clickGui(context, spot(context, Passive.INVENTORY.ordinal()));
			if (PassivesTab.detail() != Passive.INVENTORY) {
				throw new AssertionError("clicking a row did not open its detail page");
			}
			context.takeScreenshot("passives-detail-inventory");
			clickGui(context, spot(context, 3 * n)); // Back
			clickGui(context, spot(context, Passive.VISION.ordinal()));
			context.takeScreenshot("passives-detail-vision");
			clickGui(context, spot(context, 3 * n)); // Back from Vision
			context.runOnClient(mc -> PassivesTab.scrollTo(true)); // Item guard is below the view: scroll down
			context.waitTicks(2);
			context.takeScreenshot("passives-list-scrolled");
			clickGui(context, spot(context, Passive.GUARD.ordinal()));
			if (PassivesTab.detail() != Passive.GUARD) {
				throw new AssertionError("clicking Item guard did not open its detail page");
			}
			context.takeScreenshot("passives-detail-item-guard");
			clickGui(context, spot(context, 3 * n)); // Back
			// a broken item in the hotbar: hover it for the red tooltip
			int[] at = context.computeOnClient(mc -> {
				var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.gui.screen();
				var slot = screen.getMenu().getSlot(36);
				var acc = (io.github.huydangtruong.immortalcultivation.client.mixin.ContainerScreenAccess) screen;
				return new int[] {acc.permahearts$leftPos() + slot.x + 8, acc.permahearts$topPos() + slot.y + 8};
			});
			clickGuiHover(context, at);
			context.takeScreenshot("passives-broken-item-tooltip");
			if (PassivesTab.detail() != null) {
				throw new AssertionError("Back did not return to the list");
			}
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}
}
