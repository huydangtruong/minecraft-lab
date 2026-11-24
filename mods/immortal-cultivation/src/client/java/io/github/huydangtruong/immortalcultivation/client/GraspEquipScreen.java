package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspEquipMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** D244: gear of a grasped mob on the left (armor column, the mob, hands column), the player inventory on the right. */
public class GraspEquipScreen extends AbstractContainerScreen<GraspEquipMenu> {
	private static final int TEXT = 0xFF404040;
	private static final int SLOT_FILL = 0xFF8B8B8B;

	public GraspEquipScreen(GraspEquipMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, GraspEquipMenu.PANEL_W, GraspEquipMenu.PANEL_H);
	}

	private LivingEntity mob() {
		Entity e = minecraft.level == null ? null : minecraft.level.getEntity(menu.entityId());
		return e instanceof LivingEntity l ? l : null;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		WandererScreen.panel(g, leftPos, topPos, GraspEquipMenu.GEAR_W, imageHeight);
		WandererScreen.panel(g, leftPos + GraspEquipMenu.INV_PANEL_X, topPos, GraspEquipMenu.INV_PANEL_W, imageHeight);
		int px = leftPos + GraspEquipMenu.PREVIEW_X;
		int py = topPos + GraspEquipMenu.PREVIEW_Y;
		WandererScreen.inset(g, px, py, GraspEquipMenu.PREVIEW_W, GraspEquipMenu.PREVIEW_H, 0xFF000000);
		LivingEntity mob = mob();
		if (mob != null) {
			float h = Math.max(0.5f, mob.getBbHeight());
			int scale = (int) Math.max(8, Math.min(30, (GraspEquipMenu.PREVIEW_H - 14) / h));
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 1, py + 1, px + GraspEquipMenu.PREVIEW_W - 1,
				py + GraspEquipMenu.PREVIEW_H - 1, scale, 0.0625F, mouseX, mouseY, mob);
		}
		for (Slot slot : menu.slots) {
			WandererScreen.inset(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, SLOT_FILL);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, font.plainSubstrByWidth(title.getString(), GraspEquipMenu.GEAR_W - 16), 8, 6, TEXT, false);
		g.text(font, playerInventoryTitle, GraspEquipMenu.INV_X, GraspEquipMenu.INV_Y - 11, TEXT, false);
		LivingEntity mob = mob();
		if (mob != null) {
			int y = GraspEquipMenu.PREVIEW_Y + GraspEquipMenu.PREVIEW_H + 6;
			g.text(font, "Health " + Math.round(mob.getHealth()) + " / " + Math.round(mob.getMaxHealth()), 8, y, TEXT, false);
			g.text(font, "Armor " + mob.getArmorValue(), 8, y + 11, TEXT, false);
		}
		g.text(font, "Main", GraspEquipMenu.HANDS_X - 2, GraspEquipMenu.SLOT_Y0 + 18 - 9, 0xFF707070, false);
	}
}
