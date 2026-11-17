package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.reward.RewardBoxMenu;
import io.github.huydangtruong.immortalcultivation.reward.RewardBoxes;
import io.github.huydangtruong.immortalcultivation.reward.RewardMessages;
import io.github.huydangtruong.immortalcultivation.reward.RewardOption;
import io.github.huydangtruong.immortalcultivation.reward.RewardSelection;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * The choose screen of a reward box (D76): a row of cards (icon, count, name), click one to select it (gold outline),
 * then Claim (the emerald box: pick 2 cards, then Claim). Same vanilla-looking panel as the quest screen. Selection lives on the server (menu data slot).
 */
public class RewardBoxScreen extends AbstractContainerScreen<RewardBoxMenu> {
	private static final int CARD_W = 50;
	private static final int CARD_H = 80;
	private static final int GAP = 4;
	private static final int MARGIN = 8;
	private static final int CARDS_Y = 30;
	private static final int BUTTONS_Y = CARDS_Y + CARD_H + 8;
	private static final int BUTTON_W = 70;
	private static final int PANEL_H = BUTTONS_Y + 20 + 8;

	private static final int SLOT_FILL = 0xFF8B8B8B;
	private static final int SLOT_SELECTED = 0xFFA89A60;
	private static final int GOLD = 0xFFFFD21F;
	private static final int TEXT = 0xFF404040;
	private static final int TEXT_DIM = 0xFF6A6A6A;

	private Button claimButton;

	public RewardBoxScreen(RewardBoxMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, 2 * MARGIN + menu.options().size() * CARD_W + (menu.options().size() - 1) * GAP, PANEL_H);
	}

	@Override
	protected void init() {
		super.init();
		int mid = leftPos + imageWidth / 2;
		claimButton = addRenderableWidget(Button.builder(RewardMessages.claim(),
				b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RewardBoxMenu.CLAIM_BUTTON))
			.bounds(mid - BUTTON_W - 4, topPos + BUTTONS_Y, BUTTON_W, 20).build());
		addRenderableWidget(Button.builder(RewardMessages.close(), b -> onClose())
			.bounds(mid + 4, topPos + BUTTONS_Y, BUTTON_W, 20).build());
		claimButton.active = menu.canClaim();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		claimButton.active = menu.canClaim();
	}

	private int cardX(int i) {
		return leftPos + MARGIN + i * (CARD_W + GAP);
	}

	private int cardAt(double mx, double my) {
		for (int i = 0; i < menu.options().size(); i++) {
			if (mx >= cardX(i) && mx < cardX(i) + CARD_W && my >= topPos + CARDS_Y && my < topPos + CARDS_Y + CARD_H) {
				return i;
			}
		}
		return -1;
	}

	private ItemStack stackOf(RewardOption o) {
		return RewardBoxes.cardStack(o, minecraft.level.registryAccess());
	}

	/** The card name; an enchanted book shows its enchantment ("Sharpness V") instead of "Enchanted Book". */
	private static Component label(ItemStack stack) {
		var stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
		if (stored != null && !stored.isEmpty()) {
			var e = stored.entrySet().iterator().next();
			// Plain literal: the vanilla name is grey, which is hard to read on the card.
			return Component.literal(net.minecraft.world.item.enchantment.Enchantment.getFullname(e.getKey(), e.getIntValue()).getString());
		}
		return stack.getHoverName();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int card = cardAt(event.x(), event.y());
		// 26.3 numbers buttons like SDL: left 1, right 3. Both pick a card (Huy).
		if (card >= 0 && (event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT)) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, card);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		WandererScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
		int mask = menu.selected();
		List<RewardOption> options = menu.options();
		for (int i = 0; i < options.size(); i++) {
			boolean selected = RewardSelection.isSelected(mask, i);
			int x = cardX(i);
			int y = topPos + CARDS_Y;
			WandererScreen.inset(g, x, y, CARD_W, CARD_H, selected ? SLOT_SELECTED : SLOT_FILL);
			if (selected) {
				g.outline(x - 1, y - 1, CARD_W + 2, CARD_H + 2, GOLD);
				g.outline(x - 2, y - 2, CARD_W + 4, CARD_H + 4, GOLD);
			}
			RewardOption o = options.get(i);
			ItemStack stack = stackOf(o);
			g.pose().pushMatrix();
			g.pose().translate(x + (CARD_W - 32) / 2f, y + 5);
			g.pose().scale(2f, 2f);
			g.fakeItem(stack, 0, 0);
			g.pose().popMatrix();
			g.centeredText(font, Component.literal("x" + o.count()), x + CARD_W / 2, y + 38, 0xFFFFFFFF);
			// Names are drawn at 80% so a long word ("Enchanted") fits the card without breaking mid-word; smaller still
			// for a longer word ("Enlightenment").
			Component name = label(stack);
			float scale = 0.8f;
			for (String word : name.getString().split(" ")) {
				scale = Math.min(scale, (CARD_W - 2) / (float) Math.max(1, font.width(word)));
			}
			List<FormattedCharSequence> lines = font.split(name, (int) ((CARD_W - 2) / scale));
			g.pose().pushMatrix();
			g.pose().translate(x + CARD_W / 2f, y + 49);
			g.pose().scale(scale, scale);
			for (int k = 0; k < Math.min(3, lines.size()); k++) {
				FormattedCharSequence line = lines.get(k);
				g.text(font, line, -font.width(line) / 2, k * 10, TEXT, false);
			}
			g.pose().popMatrix();
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, MARGIN, 6, TEXT, false);
		g.text(font, RewardMessages.hint(menu.tier().picks(), Integer.bitCount(menu.selected())), MARGIN, 18, TEXT_DIM, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		int card = cardAt(mouseX, mouseY);
		if (card >= 0) {
			g.setTooltipForNextFrame(font, stackOf(menu.options().get(card)), mouseX, mouseY);
		}
	}
}
