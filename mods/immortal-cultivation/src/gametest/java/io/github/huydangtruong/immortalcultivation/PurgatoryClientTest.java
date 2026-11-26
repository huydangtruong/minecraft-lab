package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.PurgatoryTab;
import io.github.huydangtruong.immortalcultivation.client.StatsPanel;
import io.github.huydangtruong.immortalcultivation.purgatory.DungeonBuilder;
import io.github.huydangtruong.immortalcultivation.purgatory.Purgatory;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryFloor;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Room;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.RoomKind;
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

	/** P-082: a look into each kind of room (each module once) and the boss room. */
	private static void tour(ClientGameTestContext context, TestServerContext server) {
		int rooms = server.computeOnServer(s -> {
			ServerPlayer p = player(s);
			p.getAbilities().invulnerable = true;
			p.onUpdateAbilities();
			return PurgatoryFloor.dungeon(Purgatory.data(s).runOf(p.getUUID().toString())).rooms().size();
		});
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (int i = 0; i < rooms; i++) {
			int index = i;
			String name = server.computeOnServer(s -> {
				ServerPlayer p = player(s);
				var r = Purgatory.data(s).runOf(p.getUUID().toString());
				if (r == null) {
					return null;
				}
				Room room = PurgatoryFloor.dungeon(r).rooms().get(index);
				if (room.kind == RoomKind.START) {
					return null;
				}
				var c = PurgatoryFloor.feet(r.slot, room.centre);
				double mx = DungeonBuilder.origin(r.slot).getX() + (room.x0() + room.x1()) / 2.0;
				double mz = DungeonBuilder.origin(r.slot).getZ() + (room.z0() + room.z1()) / 2.0;
				// stand back from the middle, looking across the room
				double bx = mx - (room.x1() - room.x0()) * 0.3;
				double bz = mz - (room.z1() - room.z0()) * 0.3;
				var level = Purgatory.level(s);
				var feet = net.minecraft.core.BlockPos.containing(bx, c.y, bz);
				boolean free = level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
				double x = free ? bx : c.x;
				double z = free ? bz : c.z;
				float yaw = (float) Math.toDegrees(Math.atan2(-(mx - x), mz - z));
				p.teleportTo(level, x, c.y, z, java.util.Set.of(), free ? yaw : -45f, 12f, false);
				return room.module;
			});
			if (name == null || !seen.add(name)) {
				continue;
			}
			context.waitTicks(25);
			context.takeScreenshot("purgatory-room-" + name);
		}
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
			server.runOnServer(s -> Purgatory.data(s).solo(player(s).getUUID().toString()).floor = 6); // two layers and stairs
			long seed = 1; // D265: a floor with Seal Pillar rooms, to see them
			while (io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Layout.generate(seed, 6).rooms().stream()
					.filter(x -> x.kind == RoomKind.PILLAR && (x.module.equals("pillar_shrine") || x.module.equals("pillar_spire"))).map(x -> x.module)
					.distinct().count() < 2) {
				seed++;
			}
			Purgatory.nextSeedForTests = seed;
			clickGui(context, context.computeOnClient(mc -> PurgatoryTab.hotspots().get(0))); // Start (solo)
			context.waitTicks(40);
			boolean inBox = server.computeOnServer(s -> player(s).level().dimension() == Purgatory.DIMENSION && Purgatory.fighting(player(s)));
			if (!inBox) {
				throw new AssertionError("Start did not send the player into the purgatory");
			}
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(20);
			context.takeScreenshot("purgatory-dungeon-start");
			tour(context, server);
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
			// D260: the boss dies; the cleared screen opens with the 30 s count and Come back now
			server.runOnServer(s -> Purgatory.data(s).runOf(player(s).getUUID().toString()).boss =
				io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData.Run.BOSS_DEAD);
			context.waitTicks(45);
			context.takeScreenshot("purgatory-cleared");
			boolean pressed = context.computeOnClient(mc -> {
				var sc = mc.gui.screen();
				if (sc == null || !sc.getClass().getSimpleName().equals("PurgatoryClearedScreen")) {
					return false;
				}
				for (var w : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(sc)) {
					if (w.visible && w.getMessage().getString().equals("Come back now")
						&& w instanceof net.minecraft.client.gui.components.Button b) {
						b.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
						return true;
					}
				}
				return false;
			});
			if (!pressed) {
				throw new AssertionError("no cleared screen with Come back now");
			}
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
				p.getInventory().setItem(2, Runes.rune(reg.getOrThrow(Enchantments.PROTECTION), 2)); // D264: one stone colour each
				p.getInventory().setItem(3, Runes.rune(reg.getOrThrow(Enchantments.POWER), 1));
				p.getInventory().setItem(4, Runes.rune(reg.getOrThrow(Enchantments.EFFICIENCY), 2));
				p.getInventory().setItem(5, Runes.rune(reg.getOrThrow(Enchantments.RIPTIDE), 1));
				p.getInventory().setItem(6, Runes.rune(reg.getOrThrow(Enchantments.SWEEPING_EDGE), 1));
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
