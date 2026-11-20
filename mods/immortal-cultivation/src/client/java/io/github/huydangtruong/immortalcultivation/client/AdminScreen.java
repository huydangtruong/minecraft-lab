package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.HeartRules;
import io.github.huydangtruong.immortalcultivation.admin.AdminDraft;
import io.github.huydangtruong.immortalcultivation.admin.AdminNetworking;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.treasure.TreasureRules;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * P-060 (D155): the /admin test panel. Left: tabs of buttons, switches and sliders that only build command lines. Right: the lines
 * (editable text area, word wrap). Confirm sends them to the server, which re-checks permission and runs them in order.
 * Esc closes without running; the draft stays in {@link #DRAFT} for the next /admin.
 */
public class AdminScreen extends Screen {
	/** Survives closing the screen (Esc keeps the draft). */
	private static final AdminDraft DRAFT = new AdminDraft();

	private static final int ROW_H = 22;
	private static final int BTN_H = 20;
	private static final int TAB_W = 80;
	private static final int CONTENT_W = 188;
	private static final int LEFT_W = 4 + TAB_W + 6 + CONTENT_W + 8;
	private static final int FRAME = 0xFFC6C6C6;
	private static final int TEXT = 0xFF404040;

	private enum Tab {
		PLAYER("Player"), TECHNIQUES("Techniques"), CRIMSON("Crimson Master"), GOD_SLAYER("God Slayer"),
		DANGER("Danger"), TREASURE("Treasure"), WORLD("World");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private record Placed(AbstractWidget widget, int row, int relX) {}

	private record Label(int row, String text) {}

	private final AdminNetworking.Open rules;
	private Tab tab = Tab.PLAYER;
	private int scroll; // in rows
	private int totalRows;
	private final List<Placed> placed = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<AbstractWidget> tabButtons = new ArrayList<>();
	private final Map<String, Integer> sliderValues = new HashMap<>();
	private final Map<String, String> fieldText = new HashMap<>();
	private final EnumSet<DangerRules.Affix> affixes = EnumSet.noneOf(DangerRules.Affix.class);
	private boolean cultivationOn;
	private boolean dangerOn;
	private boolean noCooldownOn;
	private int groupIndex = -1; // -1 = random group
	private String status = "";
	private MultiLineEditBox box;

	public AdminScreen(AdminNetworking.Open rules) {
		super(Component.literal("Admin"));
		this.rules = rules;
		this.cultivationOn = rules.cultivation();
		this.dangerOn = rules.danger();
		this.noCooldownOn = rules.noCooldown();
		this.fieldText.put("ceiling", String.valueOf(rules.spawnCeiling()));
	}

	/** Test hook. */
	public static AdminDraft draft() {
		return DRAFT;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- layout ----

	private int panelW() {
		return Math.min(width - 12, 430);
	}

	private int panelH() {
		return Math.min(height - 12, 230);
	}

	private int x0() {
		return (width - panelW()) / 2;
	}

	private int y0() {
		return (height - panelH()) / 2;
	}

	private int rightX() {
		return x0() + LEFT_W + 4;
	}

	private int rightW() {
		return panelW() - LEFT_W - 4;
	}

	private int contentX() {
		return x0() + 4 + TAB_W + 6;
	}

	private int contentTop() {
		return y0() + 6;
	}

	private int viewRows() {
		return Math.max(1, (panelH() - 12) / ROW_H);
	}

	@Override
	protected void init() {
		tabButtons.clear();
		int ty = contentTop();
		for (Tab t : Tab.values()) {
			Component name = Component.literal(t.label);
			if (t == tab) {
				name = name.copy().withStyle(ChatFormatting.YELLOW);
			}
			AbstractWidget b = addRenderableWidget(Button.builder(name, btn -> {
				tab = t;
				scroll = 0;
				status = "";
				rebuildTab();
				rebuildTabButtons();
			}).bounds(x0() + 4, ty, TAB_W, BTN_H).build());
			tabButtons.add(b);
			ty += ROW_H;
		}
		int bx = rightX() + 4;
		int bw = rightW() - 8;
		int boxTop = y0() + 22;
		int boxH = panelH() - 22 - 30;
		box = MultiLineEditBox.builder()
			.setX(bx).setY(boxTop)
			.setPlaceholder(Component.literal("One command per line"))
			.build(font, bw, boxH, Component.literal("Commands"));
		box.setValue(DRAFT.text());
		box.setValueListener(DRAFT::setText);
		addRenderableWidget(box);
		int half = (bw - 4) / 2;
		addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> confirm())
			.bounds(bx, y0() + panelH() - 26, half, BTN_H).build());
		addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
			DRAFT.clear();
			syncBox();
		}).bounds(bx + half + 4, y0() + panelH() - 26, half, BTN_H).build());
		rebuildTab();
	}

	private void rebuildTabButtons() {
		for (int i = 0; i < tabButtons.size(); i++) {
			Tab t = Tab.values()[i];
			Component name = Component.literal(t.label);
			tabButtons.get(i).setMessage(t == tab ? name.copy().withStyle(ChatFormatting.YELLOW) : name);
		}
	}

	private void rebuildTab() {
		for (Placed p : placed) {
			removeWidget(p.widget());
		}
		placed.clear();
		labels.clear();
		int[] row = {0};
		switch (tab) {
			case PLAYER -> buildPlayer(row);
			case TECHNIQUES -> buildTechniques(row);
			case CRIMSON -> buildCrimson(row);
			case GOD_SLAYER -> buildGodSlayer(row);
			case DANGER -> buildDanger(row);
			case TREASURE -> buildTreasure(row);
			case WORLD -> buildWorld(row);
		}
		totalRows = row[0];
		scroll = Math.max(0, Math.min(scroll, totalRows - viewRows()));
		layoutRows();
	}

	private void layoutRows() {
		for (Placed p : placed) {
			boolean vis = p.row() >= scroll && p.row() < scroll + viewRows();
			p.widget().visible = vis;
			p.widget().active = vis;
			p.widget().setX(contentX() + p.relX());
			p.widget().setY(contentTop() + (p.row() - scroll) * ROW_H);
		}
	}

	// ---- helpers ----

	private <T extends AbstractWidget> T put(int row, int relX, T w) {
		placed.add(new Placed(w, row, relX));
		addRenderableWidget(w);
		return w;
	}

	private Button button(int row, int relX, int w, String label, Runnable onPress) {
		return put(row, relX, Button.builder(Component.literal(label), b -> onPress.run()).bounds(0, 0, w, BTN_H).build());
	}

	/** A button that adds its line at the end. */
	private Button addLine(int row, int relX, int w, String label, String line) {
		return button(row, relX, w, label, () -> add(line));
	}

	/** Two buttons side by side that add lines. */
	private void pair(int[] row, String l1, String line1, String l2, String line2) {
		addLine(row[0], 0, 92, l1, line1);
		addLine(row[0], 96, 92, l2, line2);
		row[0]++;
	}

	/** Two buttons side by side of the same setting: the later click replaces the earlier line. */
	private void keyedPair(int[] row, String key, String l1, String line1, String l2, String line2) {
		button(row[0], 0, 92, l1, () -> set(key, line1));
		button(row[0], 96, 92, l2, () -> set(key, line2));
		row[0]++;
	}

	private void label(int[] row, String text) {
		labels.add(new Label(row[0], text));
		row[0]++;
	}

	private void add(String line) {
		status = DRAFT.append(line) ? "" : "Line limit reached";
		syncBox();
	}

	private void set(String key, String line) {
		status = DRAFT.put(key, line) ? "" : "Line limit reached";
		syncBox();
	}

	private void syncBox() {
		if (box != null) {
			box.setValue(DRAFT.text());
		}
	}

	private int number(String key, int min, int max, int def) {
		try {
			int v = Integer.parseInt(fieldText.getOrDefault(key, "").trim());
			return Math.max(min, Math.min(max, v));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/** Number box with a button that puts its line (replacing the earlier one for the same key). */
	private void numberRow(int[] row, String key, String defaultText, String buttonLabel, int min, int max, java.util.function.IntFunction<String> line) {
		EditBox eb = new EditBox(font, 0, 0, 48, BTN_H, Component.literal(key));
		eb.setMaxLength(4);
		fieldText.putIfAbsent(key, defaultText);
		eb.setValue(fieldText.get(key));
		eb.setResponder(v -> fieldText.put(key, v));
		put(row[0], 0, eb);
		button(row[0], 52, 136, buttonLabel, () -> {
			int n = number(key, min, max, min);
			fieldText.put(key, String.valueOf(n));
			eb.setValue(String.valueOf(n));
			set(key, line.apply(n));
		});
		row[0]++;
	}

	private void slider(int[] row, int relX, int w, String key, String label, int min, int max, int def, IntConsumer onChange) {
		put(row[0], relX, new NumSlider(w, label, min, max, sliderValues.getOrDefault(key, def), v -> {
			sliderValues.put(key, v);
			onChange.accept(v);
		}));
	}

	// ---- tabs ----

	private void buildPlayer(int[] row) {
		numberRow(row, "hearts", "10", "Set hearts", 1, HeartRules.MAX, n -> "hearts set @s " + n);
		numberRow(row, "heartxp", "0", "Set HeartXP", 0, 499, n -> "hearts xp @s " + n);
		numberRow(row, "level", "30", "Set level", 0, 1000, n -> "experience set @s " + n + " levels");
		numberRow(row, "emeralds", "64", "Give emeralds", 0, TechniqueRules.MAX_EMERALDS, n -> "technique emeralds @s " + n);
		keyedPair(row, "testmode", "Testmode full", "testmode full", "Testmode reset", "testmode reset");
		addLine(row[0], 0, 188, "Clear cooldowns", "technique cooldowns @s clear");
		row[0]++;
		switchRow(row, "nocooldown", "No cooldown", noCooldownOn, on -> noCooldownOn = on, on -> "technique nocooldown @s " + on);
	}

	private void buildTechniques(int[] row) {
		for (Technique t : Technique.values()) {
			String key = "tech:" + t.id();
			slider(row, 0, 134, key, t.displayName(), 0, TechniqueRules.MAX_LEVEL, 0,
				v -> set(key, "technique set @s " + t.id() + " " + v));
			button(row[0], 138, 50, "Cast", () -> add("technique cast " + t.id() + " " + Math.max(1, sliderValues.getOrDefault(key, 1))));
			row[0]++;
		}
	}

	private void buildCrimson(int[] row) {
		pair(row, "Summon", "quest summon", "Skip", "quest skip");
		pair(row, "Complete 1", "quest complete 1", "Complete 2", "quest complete 2");
		pair(row, "Complete 3", "quest complete 3", "Complete 4", "quest complete 4");
		pair(row, "Give up", "quest giveup", "Reset", "quest reset");
	}

	private void buildGodSlayer(int[] row) {
		addLine(row[0], 0, 188, "Summon", "boss summon @s");
		row[0]++;
		slider(row, 0, 188, "boss:level", "Level", 1, 5, 1, v -> set("boss:level", "boss level " + v));
		row[0]++;
		keyedPair(row, "boss:freeze", "Freeze: stand", "boss freeze stand", "Freeze: fly", "boss freeze fly");
		keyedPair(row, "boss:freeze", "Freeze: fly fwd", "boss freeze fly_forward", "Freeze: off", "boss freeze off");
		addLine(row[0], 0, 188, "Give up", "boss giveup");
		row[0]++;
	}

	private void buildDanger(int[] row) {
		slider(row, 0, 188, "danger", "Danger level", 0, DangerRules.MAX_LEVEL, 0, v -> set("danger", "danger set " + v));
		row[0]++;
		addLine(row[0], 0, 188, "Info", "danger info");
		row[0]++;
		label(row, "Elite affixes");
		DangerRules.Affix[] all = DangerRules.Affix.values();
		for (int i = 0; i < all.length; i += 2) {
			for (int j = i; j < Math.min(all.length, i + 2); j++) {
				DangerRules.Affix a = all[j];
				Button[] self = new Button[1];
				self[0] = put(row[0], (j - i) * 96, Button.builder(Component.literal(chipText(a)), x -> {
					if (!affixes.remove(a)) {
						affixes.add(a);
					}
					self[0].setMessage(Component.literal(chipText(a)));
				}).bounds(0, 0, 92, BTN_H).build());
			}
			row[0]++;
		}
		button(row[0], 0, 188, "Summon elite", () -> {
			if (affixes.isEmpty()) {
				status = "Pick an affix first";
				return;
			}
			List<String> names = new ArrayList<>();
			for (DangerRules.Affix a : affixes) {
				names.add(a.name().toLowerCase(java.util.Locale.ROOT));
			}
			add("danger elite " + String.join(" ", names));
		});
		row[0]++;
	}

	private String chipText(DangerRules.Affix a) {
		return (affixes.contains(a) ? "[x] " : "[ ] ") + a.label;
	}

	private void buildTreasure(int[] row) {
		pair(row, "Status", "treasure status", "Start", "treasure start");
		Button[] picker = new Button[1];
		picker[0] = put(row[0], 0, Button.builder(Component.literal(groupText()), b -> {
			groupIndex = groupIndex + 1 >= TreasureRules.GROUPS.size() ? -1 : groupIndex + 1;
			picker[0].setMessage(Component.literal(groupText()));
		}).bounds(0, 0, 118, BTN_H).build());
		button(row[0], 122, 66, "Here", () -> add(groupIndex < 0 ? "treasure here" : "treasure here " + TreasureRules.GROUPS.get(groupIndex).id()));
		row[0]++;
		label(row, "Lineup (Mega species page)");
		for (int i = 0; i < 4; i++) {
			int page = i;
			button(row[0], i * 48, 44, "Page " + i, () -> add("treasure lineup " + page));
		}
		row[0]++;
		addLine(row[0], 0, 188, "Give up", "treasure giveup");
		row[0]++;
	}

	private String groupText() {
		return "Group: " + (groupIndex < 0 ? "random" : TreasureRules.GROUPS.get(groupIndex).name());
	}

	private void buildWorld(int[] row) {
		label(row, "Game rules");
		switchRow(row, "rule:ic", "Cultivation", cultivationOn, on -> cultivationOn = on, on -> "gamerule permahearts:immortal_cultivation " + on);
		switchRow(row, "rule:danger", "Danger level", dangerOn, on -> dangerOn = on, on -> "gamerule permahearts:danger_level " + on);
		numberRow(row, "ceiling", String.valueOf(rules.spawnCeiling()), "Spawn ceiling", 1, 16,
			n -> "gamerule permahearts:danger_spawn_ceiling " + n);
		label(row, "Vanilla");
		keyedPair(row, "time", "Day", "time set day", "Night", "time set night");
		keyedPair(row, "weather", "Clear", "weather clear", "Rain", "weather rain");
		keyedPair(row, "gamemode", "Creative", "gamemode creative", "Survival", "gamemode survival");
		addLine(row[0], 0, 188, "Kill hostile within 32", "kill @e[type=#permahearts:hostile,distance=..32]");
		row[0]++;
	}

	private void switchRow(int[] row, String key, String name, boolean initial, java.util.function.Consumer<Boolean> remember, java.util.function.Function<Boolean, String> line) {
		boolean[] on = {initial};
		Button[] b = new Button[1];
		b[0] = put(row[0], 0, Button.builder(Component.literal(name + ": " + (on[0] ? "ON" : "OFF")), x -> {
			on[0] = !on[0];
			remember.accept(on[0]);
			b[0].setMessage(Component.literal(name + ": " + (on[0] ? "ON" : "OFF")));
			set(key, line.apply(on[0]));
		}).bounds(0, 0, 188, BTN_H).build());
		row[0]++;
	}

	// ---- Confirm / close ----

	private void confirm() {
		AdminDraft.Result r = AdminDraft.sanitize(DRAFT.lines());
		if (r.dropped() > 0 && minecraft.player != null) {
			minecraft.player.sendSystemMessage(Component.literal("Dropped " + r.dropped() + " line(s): max "
				+ AdminDraft.MAX_LINES + " lines of " + AdminDraft.MAX_LENGTH + " characters."));
		}
		if (!r.lines().isEmpty()) {
			ClientPlayNetworking.send(new AdminNetworking.Run(r.lines()));
		}
		DRAFT.clear();
		onClose();
	}

	// ---- drawing and input ----

	private static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		g.fill(x0 + 1, y0, x1 - 1, y1, 0xFF000000);
		g.fill(x0, y0 + 1, x1, y1 - 1, 0xFF000000);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFFFFFFF);
		g.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
		g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, FRAME);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x88000000);
		frame(g, x0(), y0(), x0() + LEFT_W, y0() + panelH());
		frame(g, rightX(), y0(), x0() + panelW(), y0() + panelH());
		g.text(font, "Commands", rightX() + 8, y0() + 8, TEXT, false);
		if (!status.isEmpty()) {
			String s = font.plainSubstrByWidth(status, rightW() - 80);
			g.text(font, s, x0() + panelW() - 8 - font.width(s), y0() + 8, 0xFFC02020, false);
		}
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		for (Label l : labels) {
			if (l.row() >= scroll && l.row() < scroll + viewRows()) {
				g.text(font, l.text(), contentX() + 2, contentTop() + (l.row() - scroll) * ROW_H + 6, TEXT, false);
			}
		}
		if (totalRows > viewRows()) {
			int trackTop = contentTop();
			int trackH = viewRows() * ROW_H - 2;
			int barH = Math.max(12, trackH * viewRows() / totalRows);
			int barY = trackTop + (trackH - barH) * scroll / Math.max(1, totalRows - viewRows());
			int bx = x0() + LEFT_W - 7;
			g.fill(bx, trackTop, bx + 2, trackTop + trackH, 0xFF8B8B8B);
			g.fill(bx, barY, bx + 2, barY + barH, 0xFF404040);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX < rightX() && totalRows > viewRows()) {
			scroll = Math.max(0, Math.min(totalRows - viewRows(), scroll - (int) Math.signum(scrollY)));
			layoutRows();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** A slider over an integer range that shows "label: value". */
	private static final class NumSlider extends AbstractSliderButton {
		private final String label;
		private final int min;
		private final int max;
		private final IntConsumer onChange;

		NumSlider(int w, String label, int min, int max, int init, IntConsumer onChange) {
			super(0, 0, w, BTN_H, Component.empty(), max == min ? 0 : (init - min) / (double) (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.onChange = onChange;
			updateMessage();
		}

		private int current() {
			return min + (int) Math.round(value * (max - min));
		}

		@Override
		protected void updateMessage() {
			if (label != null) {
				setMessage(Component.literal(label + ": " + current()));
			}
		}

		@Override
		protected void applyValue() {
			onChange.accept(current());
		}
	}
}
