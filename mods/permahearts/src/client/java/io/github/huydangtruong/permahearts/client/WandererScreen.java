package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.ElixirItems;
import io.github.huydangtruong.permahearts.ElixirStat;
import io.github.huydangtruong.permahearts.Messages;
import io.github.huydangtruong.permahearts.wanderer.QuestSnapshot;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import io.github.huydangtruong.permahearts.wanderer.WandererMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Quest screen, drawn with plain fills (no textures). Layout constants live in WandererMenu so the slots and the
 * drawn squares always agree: every slot gets a square at (slot.x - 1, slot.y - 1), 18x18.
 */
public class WandererScreen extends AbstractContainerScreen<WandererMenu> {
	private static final int PANEL = 0xFF3B2A1C;
	private static final int PANEL_EDGE = 0xFF9C7B47;
	private static final int SLOT_EDGE = 0xFF1E140C;
	private static final int SLOT_FILL = 0xFF6B5238;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_TITLE = 0xFFFFE9B0;
	private static final int TEXT_DONE = 0xFF8CFF8C;
	private static final int TEXT_DIM = 0xFFA8A8A8;
	/** Text column of a quest row: from the icon to just before the slot square. */
	private static final int TEXT_X = 30;
	private static final int TEXT_W = WandererMenu.LINE_SLOT_X - 3 - TEXT_X;

	/** Event rows (blood moon, boss) keep their Start / Summon button left of the slot square, so the text column is narrower. */
	private static final int BUTTON_W = 78;
	private static final int BUTTON_H = 18;
	private static final int BUTTON_X = WandererMenu.LINE_SLOT_X - 3 - BUTTON_W;
	private static final int EVENT_TEXT_W = BUTTON_X - 4 - TEXT_X;

	private boolean shownAccepted;
	private final Button[] eventButtons = new Button[WandererMenu.LINES];

	public WandererScreen(WandererMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, WandererMenu.PANEL_W, WandererMenu.PANEL_H);
	}

	@Override
	protected void init() {
		super.init();
		shownAccepted = menu.isAccepted();
		if (!shownAccepted) {
			addRenderableWidget(Button.builder(Messages.accept(),
					b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
				.bounds(leftPos + (imageWidth - 120) / 2, topPos + 76, 120, 20).build());
			return;
		}
		QuestSnapshot snap = menu.snapshot();
		for (int i = 0; i < WandererMenu.LINES; i++) {
			eventButtons[i] = null;
			if (i < snap.lines().size() && hasButton(snap.lines().get(i).type())) {
				int line = i;
				// 18 px high, same top as the 18x18 slot square of that row.
				Button b = Button.builder(buttonLabel(i), x -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10 + line))
					.bounds(leftPos + BUTTON_X, topPos + WandererMenu.LINE_SLOT_Y0 + i * WandererMenu.LINE_STEP - 1, BUTTON_W, BUTTON_H)
					.build();
				eventButtons[i] = addRenderableWidget(b);
			}
		}
		updateButtons();
	}

	private static boolean hasButton(QuestType type) {
		return type == QuestType.BLOOD_MOON || type == QuestType.BOSS;
	}

	private Component buttonLabel(int i) {
		QuestType type = menu.snapshot().lines().get(i).type();
		if (menu.eventRunning(i)) {
			return type == QuestType.BOSS ? Messages.fighting() : Messages.running();
		}
		return type == QuestType.BOSS ? Messages.summon() : Messages.start();
	}

	private void updateButtons() {
		for (int i = 0; i < WandererMenu.LINES; i++) {
			Button b = eventButtons[i];
			if (b != null) {
				b.visible = !menu.isDone(i); // a done row shows the reward slot instead
				b.active = !menu.eventRunning(i);
				b.setMessage(buttonLabel(i));
			}
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (menu.isAccepted() != shownAccepted) {
			rebuildWidgets(); // swaps the Accept button for the quest rows
		}
		updateButtons();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_EDGE);
		g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);
		for (Slot slot : menu.slots) {
			if (slot.isActive()) {
				int x = leftPos + slot.x - 1;
				int y = topPos + slot.y - 1;
				g.fill(x, y, x + 18, y + 18, SLOT_EDGE);
				g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
			}
		}
	}

	/** Called with the pose already translated to the panel's top-left corner, so coordinates here are panel-relative. */
	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, 12, 10, TEXT_TITLE, true);
		g.text(font, playerInventoryTitle, WandererMenu.INV_X, WandererMenu.INV_Y - 11, TEXT_DIM, false);
		if (!menu.isAccepted()) {
			g.textWithWordWrap(font, Messages.wandererIntro(), 12, 28, imageWidth - 24, TEXT);
			return;
		}
		QuestSnapshot snap = menu.snapshot();
		for (int i = 0; i < WandererMenu.LINES && i < snap.lines().size(); i++) {
			extractRow(g, i, snap.lines().get(i));
		}
	}

	private void extractRow(GuiGraphicsExtractor g, int i, QuestSnapshot.LineView line) {
		int slotY = WandererMenu.LINE_SLOT_Y0 + i * WandererMenu.LINE_STEP;
		Component text;
		int color = TEXT;
		ItemStack icon;
		if (menu.isClaimed(i)) {
			text = Messages.lineClaimed();
			color = TEXT_DIM;
			icon = ItemStack.EMPTY;
		} else if (menu.isDone(i)) {
			text = Messages.lineDone(Wanderer.rewardName(line.reward()));
			color = TEXT_DONE;
			icon = ElixirStat.byId(line.reward()).map(s -> new ItemStack(ElixirItems.get(s))).orElse(ItemStack.EMPTY);
		} else {
			text = Wanderer.lineText(line.type(), line.target(), line.required(), menu.progressOf(i));
			icon = switch (line.type()) {
				case COLLECT -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(line.target())));
				case KILL -> new ItemStack(Items.IRON_SWORD);
				case EXPLORE -> new ItemStack(Items.COMPASS);
				case BLOOD_MOON -> new ItemStack(Items.CLOCK);
				case BOSS -> new ItemStack(Items.DIAMOND_HELMET);
			};
		}
		if (!icon.isEmpty()) {
			g.fakeItem(icon, 10, slotY);
		}
		// A row with a button (and not yet done) has a narrower text column and may use three lines (3 x 9 = 27 = row step).
		boolean narrow = hasButton(line.type()) && !menu.isDone(i);
		List<FormattedCharSequence> lines = font.split(text, narrow ? EVENT_TEXT_W : TEXT_W);
		int n = Math.min(narrow ? 3 : 2, lines.size());
		int y = slotY + 8 - (n * 9) / 2;
		for (int k = 0; k < n; k++) {
			g.text(font, lines.get(k), TEXT_X, y + k * 9, color, true);
		}
	}
}
