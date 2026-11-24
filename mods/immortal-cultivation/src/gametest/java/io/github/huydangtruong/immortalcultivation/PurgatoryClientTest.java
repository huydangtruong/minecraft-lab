package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.PurgatoryTab;
import io.github.huydangtruong.immortalcultivation.client.StatsPanel;
import io.github.huydangtruong.immortalcultivation.purgatory.Purgatory;
import io.github.huydangtruong.immortalcultivation.rune.RuneEntry;
import io.github.huydangtruong.immortalcultivation.rune.RuneForge;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/** D234-D242: the Purgatory tab, a real Start click into the purgatory dimension, the win, the Rune Forge and rune tooltips. */
public class PurgatoryClientTest implements FabricClientGameTest {
	private static final int LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;

	private static void clickGui(ClientGameTestContext context, int[] gui) {
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(2);
		context.getInput().pressMouse(LEFT);
		context.waitTicks(6);
	}

	private static void hover(ClientGameTestContext context, int slotIndex) {
		int[] at = context.computeOnClient(mc -> {
			var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.gui.screen();
			var slot = screen.getMenu().getSlot(slotIndex);
			var acc = (io.github.huydangtruong.immortalcultivation.client.mixin.ContainerScreenAccess) screen;
			return new int[] {acc.permahearts$leftPos() + slot.x + 8, acc.permahearts$topPos() + slot.y + 8};
		});
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(at[0] * scale, at[1] * scale);
		context.waitTicks(10);
	}

	private static void openTab(ClientGameTestContext context) {
		context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
			net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(5);
		if (!context.computeOnClient(mc -> StatsPanel.isOpen())) {
			context.clickScreenButton("Cultivation");
			context.waitTicks(3);
		}
		context.clickScreenButton("Purgatory");
		context.waitTicks(25); // one state sync
		if (!context.computeOnClient(mc -> StatsPanel.isOpen() && StatsPanel.tab() == StatsPanel.Tab.PURGATORY)) {
			throw new AssertionError("the Purgatory tab did not open");
		}
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer s) {
		return s.getPlayerList().getPlayers().get(0);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("gamemode survival @a");
			boolean hasDim = server.computeOnServer(s -> s.getLevel(Purgatory.DIMENSION) != null);
			if (!hasDim) {
				throw new AssertionError("the purgatory dimension is missing");
			}
			context.waitTicks(30);
			openTab(context);
			context.takeScreenshot("purgatory-tab-ready");
			clickGui(context, context.computeOnClient(mc -> { // a real mouse click on the Stats edge tab (the bug ate it)
				for (var w : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(mc.gui.screen())) {
					if (w.visible && w.getMessage().getString().equals("Stats")) {
						return new int[] {w.getX() + w.getWidth() / 2, w.getY() + w.getHeight() / 2};
					}
				}
				throw new AssertionError("no Stats tab");
			}));
			if (!context.computeOnClient(mc -> StatsPanel.tab() == StatsPanel.Tab.STATS)) {
				throw new AssertionError("the Stats tab could not be clicked from the Purgatory tab");
			}
			context.clickScreenButton("Purgatory");
			context.waitTicks(3);
			clickGui(context, context.computeOnClient(mc -> PurgatoryTab.hotspots().get(0))); // Start (solo)
			context.waitTicks(40);
			boolean inBox = server.computeOnServer(s -> player(s).level().dimension() == Purgatory.DIMENSION && Purgatory.fighting(player(s)));
			if (!inBox) {
				throw new AssertionError("Start did not send the player into the purgatory");
			}
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(120); // a few mobs come
			context.takeScreenshot("purgatory-box-floor-1");
			openTab(context);
			context.takeScreenshot("purgatory-tab-fighting");
			boolean giveUpAtBottom = context.computeOnClient(mc -> { // D250: Give up at the bottom centre of the inventory
				var sc = mc.gui.screen();
				for (var w : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(sc)) {
					if (w.visible && w.getMessage().getString().equals("Give up")) {
						return Math.abs(w.getX() + w.getWidth() / 2 - sc.width / 2) <= 1 && w.getY() + w.getHeight() >= sc.height - 4;
					}
				}
				return false;
			});
			if (!giveUpAtBottom) {
				throw new AssertionError("no Give up button at the bottom centre of the inventory");
			}
			context.runOnClient(mc -> mc.gui.setScreen(null));
			server.runOnServer(s -> Purgatory.finish(player(s), true));
			context.waitTicks(40);
			boolean home = server.computeOnServer(s -> player(s).level() == s.overworld() && player(s).experienceLevel >= 31);
			if (!home) {
				throw new AssertionError("the win did not send the player home with 31 levels");
			}
			openTab(context);
			context.takeScreenshot("purgatory-tab-cooldown");
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(5);

			// Rune Forge: a sword with a Sharpness rune and a Smite rune (no conflict)
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				var reg = s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
				ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
				sword.enchant(reg.getOrThrow(Enchantments.SHARPNESS), 5);
				Runes.setInscribed(sword, List.of(new RuneEntry(reg.getOrThrow(Enchantments.SHARPNESS), 4),
					new RuneEntry(reg.getOrThrow(Enchantments.SMITE), 2)));
				p.getInventory().setItem(0, sword);
				p.getInventory().setItem(1, Runes.rune(reg.getOrThrow(Enchantments.LOOTING), 3));
				p.giveExperienceLevels(40);
				BlockPos at = p.blockPosition().offset(2, 0, 0);
				s.overworld().setBlockAndUpdate(at, RuneForge.BLOCK.defaultBlockState());
				RuneForge.open(p, s.overworld(), at);
			});
			context.waitTicks(10);
			hover(context, 8 + 27); // hotbar slot 0 of the forge menu (inventory starts at 8)
			context.takeScreenshot("rune-tooltip-sword");
			hover(context, 8 + 28);
			context.takeScreenshot("rune-tooltip-rune");
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				ItemStack sword = p.getInventory().removeItemNoUpdate(0);
				p.containerMenu.getSlot(0).set(sword); // the sword goes into the forge
				p.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			context.takeScreenshot("rune-forge-inscribe");
			context.clickScreenButton("Merge");
			context.waitTicks(5);
			context.takeScreenshot("rune-forge-merge");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}
}
