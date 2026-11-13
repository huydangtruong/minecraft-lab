package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.ElixirItems;
import io.github.huydangtruong.permahearts.ElixirStat;
import io.github.huydangtruong.permahearts.Messages;
import io.github.huydangtruong.permahearts.wanderer.DeathMatchRules;
import io.github.huydangtruong.permahearts.wanderer.QuestSnapshot;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.StayTime;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import io.github.huydangtruong.permahearts.wanderer.WandererMenu;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Quest screen laid out like the villager trade screen: quest panel on the left, vanilla-looking inventory panel on
 * the right (Wanderer preview on top, player inventory below). Drawn with fills in vanilla GUI colours, no textures.
 * Layout constants live in WandererMenu so slots and drawn squares always agree: every slot gets a square at
 * (slot.x - 1, slot.y - 1), 18x18.
 */
public class WandererScreen extends AbstractContainerScreen<WandererMenu> {
	// Vanilla container colours.
	private static final int OUTLINE = 0xFF000000;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADE = 0xFF555555;
	private static final int SLOT_FILL = 0xFF8B8B8B;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int ROW_LINE = 0xFFA0A0A0;
	private static final int TEXT = 0xFF404040;
	private static final int TEXT_DONE = 0xFF1E7A1E;
	private static final int TEXT_DIM = 0xFF7A7A7A;

	private static final int ICON_X = 6;
	/** Text column of a quest row: from after the icon to just before the slot square. */
	private static final int TEXT_X = 26;
	private static final int TEXT_W = WandererMenu.LINE_SLOT_X - 1 - 4 - TEXT_X;

	/** Event rows (blood moon, boss) show a Start / Summon button at the right end of the row instead of a slot. */
	private static final int BUTTON_W = 62;
	private static final int BUTTON_H = 18;
	private static final int BUTTON_X = WandererMenu.QUEST_W - 6 - BUTTON_W;
	private static final int EVENT_TEXT_W = BUTTON_X - 4 - TEXT_X;

	/** Wanderer preview box in the top of the inventory panel. */
	private static final int PREVIEW_X = WandererMenu.INV_PANEL_X + 8;
	private static final int PREVIEW_Y = 17;
	private static final int PREVIEW_W = 50;
	private static final int PREVIEW_H = 56;

	/** Client ticks left in which a second Give up click confirms (0 = not armed). */
	private static final int CONFIRM_TICKS = 60;
	private int confirmTicks;
	private Button giveUpButton;

	private boolean shownAccepted;
	private final Button[] eventButtons = new Button[WandererMenu.LINES];

	public WandererScreen(WandererMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, WandererMenu.PANEL_W, WandererMenu.PANEL_H);
	}

	@Override
	protected void init() {
		super.init();
		shownAccepted = menu.isAccepted();
		giveUpButton = addRenderableWidget(Button.builder(giveUpLabel(), b -> onGiveUp())
			.bounds(leftPos + (WandererMenu.QUEST_W - WandererMenu.GIVE_UP_W) / 2, topPos + WandererMenu.GIVE_UP_Y,
				WandererMenu.GIVE_UP_W, WandererMenu.GIVE_UP_H).build());
		if (!shownAccepted) {
			addRenderableWidget(Button.builder(Messages.accept(),
					b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
				.bounds(leftPos + (WandererMenu.QUEST_W - 100) / 2, topPos + 120, 100, 20).build());
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

	private Component giveUpLabel() {
		return (confirmTicks > 0 ? Messages.giveUpConfirm() : Messages.giveUp()).copy().withStyle(ChatFormatting.RED);
	}

	/** First click arms the button for 3 seconds, the second click sends Give up to the server. */
	private void onGiveUp() {
		if (confirmTicks > 0) {
			confirmTicks = 0;
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WandererMenu.GIVE_UP_BUTTON);
		} else {
			confirmTicks = CONFIRM_TICKS;
			giveUpButton.setMessage(giveUpLabel());
		}
	}

	private static boolean hasButton(QuestType type) {
		return type == QuestType.BLOOD_MOON || type == QuestType.BOSS || type == QuestType.DEATH_MATCH;
	}

	private Component buttonLabel(int i) {
		QuestType type = menu.snapshot().lines().get(i).type();
		if (menu.eventRunning(i)) {
			return type == QuestType.BOSS ? Messages.fighting() : Messages.running();
		}
		return type == QuestType.BOSS ? Messages.summon() : Messages.start();
	}

	/** Why the Death Match cannot start now (shown as the tooltip of the inactive button), or null. */
	private Component deathMatchRefusal() {
		if (menu.playersOnline() < DeathMatchRules.MIN_ONLINE) {
			return Messages.deathMatchNeedPlayers();
		}
		return menu.pvpOn() ? null : Messages.deathMatchPvpOff();
	}

	private void updateButtons() {
		for (int i = 0; i < WandererMenu.LINES; i++) {
			Button b = eventButtons[i];
			if (b != null) {
				b.visible = !menu.isDone(i); // a done row shows the reward slot instead
				QuestType type = menu.snapshot().lines().get(i).type();
				Component refusal = type == QuestType.DEATH_MATCH && !menu.eventRunning(i) ? deathMatchRefusal() : null;
				b.active = !menu.eventRunning(i) && refusal == null;
				b.setTooltip(refusal == null ? null : Tooltip.create(refusal));
				b.setMessage(buttonLabel(i));
			}
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (confirmTicks > 0 && --confirmTicks == 0 && giveUpButton != null) {
			giveUpButton.setMessage(giveUpLabel());
		}
		if (menu.isAccepted() != shownAccepted) {
			rebuildWidgets(); // swaps the Accept button for the quest rows
		}
		updateButtons();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		panel(g, leftPos, topPos, WandererMenu.QUEST_W, imageHeight);
		panel(g, leftPos + WandererMenu.INV_PANEL_X, topPos, WandererMenu.INV_PANEL_W, imageHeight);

		if (menu.isAccepted()) {
			int rows = Math.min(WandererMenu.LINES, menu.snapshot().lines().size());
			for (int i = 1; i < rows; i++) {
				int y = topPos + WandererMenu.LINE_TOP + i * WandererMenu.LINE_STEP - 1;
				g.fill(leftPos + 6, y, leftPos + WandererMenu.QUEST_W - 6, y + 1, ROW_LINE);
			}
		}

		int px = leftPos + PREVIEW_X;
		int py = topPos + PREVIEW_Y;
		inset(g, px, py, PREVIEW_W, PREVIEW_H, 0xFF000000);
		WanderingTrader npc = nearestWanderer();
		if (npc != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 1, py + 1, px + PREVIEW_W - 1, py + PREVIEW_H - 1,
				24, 0.0625F, mouseX, mouseY, npc);
		}

		for (Slot slot : menu.slots) {
			if (slot.isActive()) {
				inset(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, SLOT_FILL);
			}
		}
	}

	/** Vanilla-style raised panel: black outline with cut corners, white top-left bevel, grey bottom-right bevel. */
	private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x + 2, y, x + w - 2, y + 1, OUTLINE);
		g.fill(x + 2, y + h - 1, x + w - 2, y + h, OUTLINE);
		g.fill(x, y + 2, x + 1, y + h - 2, OUTLINE);
		g.fill(x + w - 1, y + 2, x + w, y + h - 2, OUTLINE);
		g.fill(x + 1, y + 1, x + 2, y + 2, OUTLINE);
		g.fill(x + w - 2, y + 1, x + w - 1, y + 2, OUTLINE);
		g.fill(x + 1, y + h - 2, x + 2, y + h - 1, OUTLINE);
		g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, OUTLINE);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
		g.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
		g.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
		g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADE);
		g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADE);
	}

	/** Sunken box like a vanilla slot: dark top-left edge, white bottom-right edge. */
	private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
		g.fill(x, y, x + w, y + h, SLOT_DARK);
		g.fill(x + 1, y + 1, x + w, y + h, LIGHT);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
	}

	private WanderingTrader nearestWanderer() {
		if (minecraft.player == null || minecraft.level == null) {
			return null;
		}
		return minecraft.level.getEntitiesOfClass(WanderingTrader.class, minecraft.player.getBoundingBox().inflate(10)).stream()
			.min(Comparator.comparingDouble(e -> e.distanceToSqr(minecraft.player)))
			.orElse(null);
	}

	/** Called with the pose already translated to the panel's top-left corner, so coordinates here are panel-relative. */
	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, Messages.questsTitle(), 8, 6, TEXT, false);
		g.text(font, title, WandererMenu.INV_PANEL_X + 8, 6, TEXT, false);
		g.text(font, playerInventoryTitle, WandererMenu.INV_X, WandererMenu.INV_Y - 11, TEXT, false);

		int infoX = PREVIEW_X + PREVIEW_W + 6;
		int infoW = WandererMenu.INV_PANEL_X + WandererMenu.INV_PANEL_W - 8 - infoX;
		if (!menu.isAccepted()) {
			wrapped(g, Messages.wandererIntro(), 8, 24, WandererMenu.QUEST_W - 16, TEXT);
			leavesIn(g, wrapped(g, Messages.questOffer(), infoX, PREVIEW_Y + 2, infoW, TEXT), infoX, infoW);
			return;
		}
		QuestSnapshot snap = menu.snapshot();
		int rows = Math.min(WandererMenu.LINES, snap.lines().size());
		int done = 0;
		for (int i = 0; i < rows; i++) {
			extractRow(g, i, snap.lines().get(i));
			if (menu.isDone(i) || menu.isClaimed(i)) {
				done++;
			}
		}
		leavesIn(g, wrapped(g, Messages.questSummary(done, rows), infoX, PREVIEW_Y + 2, infoW, TEXT), infoX, infoW);
	}

	/** The countdown under the info text; y is the first free line. Must end above the "Inventory" label. */
	private void leavesIn(GuiGraphicsExtractor g, int y, int infoX, int infoW) {
		// "Leaves in" / "2 days 0 h" / "(≈ 40 min)": explicit breaks so a unit never wraps alone onto the next line.
		String time = StayTime.format(menu.ticksLeft()).replace(" (", "\n(");
		wrapped(g, Messages.leavesIn("\n" + time), infoX, y + 3, infoW, TEXT_DIM);
	}

	/** Word-wrapped text without drop shadow (the shadowed variant looks smudged on the light grey panel). Returns the y after the last line. */
	private int wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			g.text(font, line, x, y, color, false);
			y += 9;
		}
		return y;
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
			text = Wanderer.lineText(line.type(), line.target(), menu.requiredOf(i), menu.progressOf(i), menu.eventRunning(i));
			icon = switch (line.type()) {
				case COLLECT -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(line.target())));
				case KILL -> new ItemStack(Items.IRON_SWORD);
				case EXPLORE -> new ItemStack(Items.COMPASS);
				case BLOOD_MOON -> new ItemStack(Items.CLOCK);
				case BOSS -> new ItemStack(Items.DIAMOND_HELMET);
				case DEATH_MATCH -> new ItemStack(Items.IRON_SWORD);
			};
		}
		if (!icon.isEmpty()) {
			g.fakeItem(icon, ICON_X, slotY);
		}
		// Up to three lines of text (27 px) centred on the row.
		boolean narrow = hasButton(line.type()) && !menu.isDone(i);
		List<FormattedCharSequence> lines = font.split(text, narrow ? EVENT_TEXT_W : TEXT_W);
		int n = Math.min(3, lines.size());
		int y = slotY + 8 - (n * 9) / 2;
		for (int k = 0; k < n; k++) {
			g.text(font, lines.get(k), TEXT_X, y + k * 9, color, false);
		}
	}
}
