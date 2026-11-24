package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.rune.RuneEntry;
import io.github.huydangtruong.immortalcultivation.rune.RuneForgeMenu;
import io.github.huydangtruong.immortalcultivation.rune.RuneRules;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * D242: the Rune Forge. Inscribe tab: the gear on the left, its rune slots on the right (locked ones crossed out).
 * Merge tab: two equal runes, "+", an arrow and the result.
 */
public class RuneForgeScreen extends AbstractContainerScreen<RuneForgeMenu> {
	private static final int TEXT = 0xFF404040;
	private static final int DIM = 0xFF707070;
	private static final int SLOT_FILL = 0xFF8B8B8B;
	private static final int LOCKED = 0xFF5A5A5A;
	private static final int ORANGE = 0xFFC86400;
	private Button inscribeTab;
	private Button mergeTab;

	public RuneForgeScreen(RuneForgeMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, RuneForgeMenu.W, RuneForgeMenu.H);
	}

	@Override
	protected void init() {
		super.init();
		inscribeTab = addRenderableWidget(Button.builder(Component.literal("Inscribe"), b -> pick(RuneForgeMenu.TAB_INSCRIBE))
			.bounds(leftPos + 8, topPos + 16, 60, 16).build());
		mergeTab = addRenderableWidget(Button.builder(Component.literal("Merge"), b -> pick(RuneForgeMenu.TAB_MERGE))
			.bounds(leftPos + 70, topPos + 16, 60, 16).build());
	}

	private void pick(int tab) {
		if (menu.clickMenuButton(minecraft.player, tab)) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tab);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		inscribeTab.active = menu.tab() != RuneForgeMenu.TAB_INSCRIBE;
		mergeTab.active = menu.tab() != RuneForgeMenu.TAB_MERGE;
		WandererScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (Slot slot : menu.slots) {
			if (slot.isActive()) {
				WandererScreen.inset(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, SLOT_FILL);
			}
		}
		int y = topPos + RuneForgeMenu.ROW_Y;
		if (menu.tab() == RuneForgeMenu.TAB_INSCRIBE) {
			for (int i = menu.gearSlots(); i < RuneForgeMenu.MAX_SLOTS; i++) {
				int x = leftPos + RuneForgeMenu.RUNES_X + i * 22;
				WandererScreen.inset(g, x - 1, y - 1, 18, 18, LOCKED);
				g.fill(x + 3, y + 7, x + 13, y + 9, 0xFF3A3A3A); // a bar: locked
			}
		} else {
			int ax = leftPos + (RuneForgeMenu.MERGE_A_X + RuneForgeMenu.MERGE_B_X + 16) / 2;
			g.fill(ax - 3, y + 7, ax + 4, y + 9, DIM);
			g.fill(ax - 1, y + 5, ax + 2, y + 11, DIM);
			int rx = leftPos + RuneForgeMenu.MERGE_B_X + 22;
			int rEnd = leftPos + RuneForgeMenu.RESULT_X - 6;
			g.fill(rx, y + 7, rEnd - 4, y + 9, DIM);
			for (int i = 0; i < 4; i++) {
				g.fill(rEnd - 4 + i, y + 4 + i, rEnd - 3 + i, y + 12 - i, DIM);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, 8, 6, TEXT, false);
		g.text(font, playerInventoryTitle, 8, 73, TEXT, false);
		int y = RuneForgeMenu.ROW_Y + 22;
		if (menu.tab() == RuneForgeMenu.TAB_INSCRIBE) {
			ItemStack gear = menu.gearStack();
			String hint;
			if (gear.isEmpty()) {
				hint = "Put in a weapon, tool or armor";
			} else {
				ItemStack carried = menu.getCarried();
				RuneEntry r = Runes.entryOf(carried);
				hint = r != null ? "Costs " + menu.costFor(r) + " levels · taking out is free"
					: "Rune: " + RuneRules.COST_PER_LEVEL + " levels per rune level";
			}
			g.text(font, hint, 8, y, DIM, false);
		} else {
			RuneEntry a = Runes.entryOf(menu.getSlot(RuneForgeMenu.MERGE_A).getItem());
			g.text(font, a == null ? "2 equal runes: 1 level higher" : "Free", 8, y, a == null ? DIM : ORANGE, false);
		}
	}
}
