package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.rune.RuneEntry;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * D278: rune textures in a real client: every rune kind with its glyph and level number (1 to 99) in the inventory, an
 * old rune (kind only) that gets the new look, and one in the hand. Screenshots rune-look-inventory, rune-look-hand.
 */
public class RuneLookClientTest implements FabricClientGameTest {
	private static final int[] LEVELS = {1, 2, 3, 5, 8, 9, 10, 12, 99};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().get(0);
				List<Holder.Reference<Enchantment>> all = new java.util.ArrayList<>();
				s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().filter(Runes::eligible).forEach(all::add);
				all.sort(java.util.Comparator.comparing(h -> io.github.huydangtruong.immortalcultivation.rune.RuneRules.kind(h.key().identifier().getPath())
					+ h.key().identifier().getPath()));
				int slot = 0;
				for (Holder.Reference<Enchantment> h : all) {
					if (slot >= 35) {
						break;
					}
					p.getInventory().setItem(slot, Runes.rune(h, LEVELS[slot % LEVELS.length]));
					slot++;
				}
				// an old rune: only the kind in its model data
				ItemStack old = Runes.rune(all.get(0), 4);
				old.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of("attack"), List.of()));
				p.getInventory().setItem(35, old);
				p.getInventory().setSelectedSlot(8);
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(30);
			boolean updated = server.computeOnServer(s -> {
				ItemStack old = s.getPlayerList().getPlayers().get(0).getInventory().getItem(35);
				CustomModelData cmd = old.get(DataComponents.CUSTOM_MODEL_DATA);
				return cmd != null && cmd.strings().size() == 4 && "c4".equals(cmd.strings().get(2));
			});
			if (!updated) {
				throw new AssertionError("an old rune in the inventory gets the glyph and number look");
			}
			context.takeScreenshot("rune-look-hand");
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(10);
			context.takeScreenshot("rune-look-inventory");
			context.setScreen(() -> null);
			// old runes inside a chest never tick: the client must still draw them with the new look
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().get(0);
				net.minecraft.core.BlockPos pos = p.blockPosition().above(3);
				p.level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
				if (p.level().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
					for (int i = 0; i < 9; i++) {
						ItemStack old = p.getInventory().getItem(i).copy();
						String kind = old.get(DataComponents.CUSTOM_MODEL_DATA).strings().get(0);
						old.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(kind), List.of()));
						chest.setItem(i, old);
					}
					p.openMenu(chest);
				}
			});
			context.waitTicks(10);
			context.takeScreenshot("rune-look-old-chest");
		}
	}
}
