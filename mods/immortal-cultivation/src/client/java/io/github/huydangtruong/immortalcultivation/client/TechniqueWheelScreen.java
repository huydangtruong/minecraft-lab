package io.github.huydangtruong.immortalcultivation.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.SkillBarLogic;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.WheelGroups;
import io.github.huydangtruong.immortalcultivation.technique.WheelGroups.Group;
import io.github.huydangtruong.immortalcultivation.technique.WheelLogic;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * D188/D193 technique wheel: opened by holding R (from the cross it first selects the lowest ready slot). A top row shows the
 * technique slots; the mouse wheel moves the target slot. The technique already in the edited slot is drawn darkened.
 * D188: opened by holding R while a technique slot is selected. Shows the learned techniques by group; a click
 * puts that technique in the selected slot (the server swaps two slots when it already sits in another one). D196: the wheel
 * stays open after a pick, so one can scroll on and pick again; only letting go of R (or Esc) closes it. The world keeps running.
 */
public class TechniqueWheelScreen extends Screen {
	private static final int CELL = 22;
	private static final int PITCH = 26;
	private static final int ROW_H = 30;
	private static final int LABEL_W = 96;
	private static final int PAD = 10;
	private static final int HEADER = 30;
	private static final int SLOTS_H = 38;
	private static final int SLOT_PITCH = 34;
	private static final int FOOTER = 18;
	private static final int GOLD = 0xFFFFC800;
	private static final int PANEL = 0xE0101018;
	private static final int RED = 0xFFFF5555;

	private int slot;
	/** True after a click on a cooling slot: the red line shows (its seconds stay live). */
	private boolean warn;
	/** Ticks since opening; the key release is only read after the first tick. */
	private int age;

	public TechniqueWheelScreen(int slot) {
		super(Component.literal("Technique wheel"));
		this.slot = slot;
	}

	public int slot() {
		return slot;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** D193: the mouse wheel moves the target to the next (down) or previous (up) open slot, wrapping; the real selection follows. */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			int next = WheelLogic.step(slot, scrollY < 0 ? 1 : -1, data().slotsOpen());
			if (next >= 0 && next != slot) {
				slot = next;
				warn = false;
				ClientSkills.select(slot);
			}
		}
		return true; // never reaches the hotbar scroll
	}

	@Override
	public void tick() {
		super.tick();
		if (age++ > 0 && !ClientSkills.wheelKeyDown()) {
			onClose(); // R let go: no change
		}
	}

	private TechniqueData data() {
		return ClientSkills.data(minecraft.player);
	}

	private List<Technique> learned(Group g, TechniqueData d) {
		List<Technique> out = new ArrayList<>();
		for (Technique t : g.members()) {
			if (d.level(t) >= 1) {
				out.add(t);
			}
		}
		return out;
	}

	private int rows(TechniqueData d) {
		int n = 0;
		for (Group g : WheelGroups.GROUPS) {
			if (!learned(g, d).isEmpty()) {
				n++;
			}
		}
		return n;
	}

	private int panelW() {
		return LABEL_W + 6 * PITCH + PAD * 2;
	}

	private int panelH(TechniqueData d) {
		return HEADER + SLOTS_H + Math.max(1, rows(d)) * ROW_H + FOOTER;
	}

	private int panelX() {
		return (width - panelW()) / 2;
	}

	private int panelY(TechniqueData d) {
		return (height - panelH(d)) / 2;
	}

	private static String slotKeyName(int slot) {
		return ClientSkills.KEYS[slot].getTranslatedKeyMessage().getString();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		TechniqueData d = data();
		long now = minecraft.level.getGameTime();
		int x0 = panelX(), y0 = panelY(d), w = panelW(), h = panelH(d);
		g.fill(x0, y0, x0 + w, y0 + h, PANEL);
		g.outline(x0, y0, w, h, GOLD);
		g.text(font, Component.literal("Change slot " + slotKeyName(slot)), x0 + PAD, y0 + 7, 0xFFFFFFFF, true);
		Technique current = d.inSlot(slot);
		String now1 = current == null ? "empty" : current.displayName();
		g.text(font, now1, x0 + PAD, y0 + 18, 0xFFE8D090, false);

		Technique hover = null;
		int sy = y0 + HEADER;
		for (int i = 0; i < d.slotsOpen(); i++) {
			int cx = x0 + PAD + i * SLOT_PITCH;
			Technique st = d.inSlot(i);
			String key = slotKeyName(i);
			g.text(font, key, cx + (CELL - font.width(key)) / 2, sy, i == slot ? GOLD : 0xFFC8C8D8, true);
			int cy = sy + 10;
			boolean over = mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL;
			g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF22222F);
			g.outline(cx, cy, CELL, CELL, i == slot ? GOLD : 0xFF5A5A70);
			if (i == slot) {
				g.outline(cx + 1, cy + 1, CELL - 2, CELL - 2, GOLD);
			}
			if (st != null) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(st), cx + 3, cy + 3, 16, 16);
				if (d.onCooldown(st, now)) {
					g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0xB0101018);
					String sec = String.valueOf(SkillBarLogic.secondsLeft(d.cooldownLeft(st, now)));
					g.text(font, sec, cx + (CELL - font.width(sec)) / 2, cy + 7, 0xFFFFFFFF, true);
				}
				if (over) {
					hover = st;
				}
			}
		}
		g.fill(x0 + PAD, y0 + HEADER + SLOTS_H - 3, x0 + w - PAD, y0 + HEADER + SLOTS_H - 2, 0xFF3A3A50);

		int row = 0;
		for (Group grp : WheelGroups.GROUPS) {
			List<Technique> list = learned(grp, d);
			if (list.isEmpty()) {
				continue;
			}
			int ry = y0 + HEADER + SLOTS_H + row * ROW_H;
			g.text(font, grp.label(), x0 + PAD, ry + 7, 0xFFC8C8D8, false);
			for (int i = 0; i < list.size(); i++) {
				Technique t = list.get(i);
				int cx = x0 + PAD + LABEL_W + i * PITCH;
				boolean over = mouseX >= cx && mouseX < cx + CELL && mouseY >= ry && mouseY < ry + CELL;
				g.fill(cx, ry, cx + CELL, ry + CELL, over ? 0xFF3A3A58 : 0xFF22222F);
				g.outline(cx, ry, CELL, CELL, t == current ? GOLD : 0xFF5A5A70);
				if (t == current) {
					g.outline(cx + 1, ry + 1, CELL - 2, CELL - 2, GOLD);
				}
				g.blitSprite(RenderPipelines.GUI_TEXTURED, TechniquesTab.icon(t), cx + 3, ry + 3, 16, 16);
				int at = d.slotOf(t);
				if (at >= 0 && at < d.slotsOpen()) {
					g.text(font, slotKeyName(at), cx + CELL - 1 - font.width(slotKeyName(at)), ry + CELL - 9, 0xFFFFD24A, true);
				}
				if (t == current) {
					g.fill(cx + 1, ry + 1, cx + CELL - 1, ry + CELL - 1, 0xA0000000); // D193: already in the edited slot (others stay bright: a click swaps)
				}
				if (d.onCooldown(t, now)) {
					g.fill(cx + 1, ry + 1, cx + CELL - 1, ry + CELL - 1, 0x90202020);
				}
				if (over) {
					hover = t;
				}
			}
			row++;
		}
		if (row == 0) {
			g.text(font, "No technique learned yet.", x0 + PAD, y0 + HEADER + 8, 0xFFA0A0B0, false);
		}
		if (warn && current != null && d.slotOnCooldown(slot, now)) {
			String s = current.displayName() + " is on cooldown (" + (d.cooldownLeft(current, now) + 19) / 20 + " s)";
			g.centeredText(font, s, x0 + w / 2, y0 + h - 13, RED);
		} else {
			warn = false;
			g.centeredText(font, "Click: set. Scroll: next slot. Release " + ClientSkills.WHEEL.getTranslatedKeyMessage().getString() + ": close.", x0 + w / 2, y0 + h - 13, 0xFF8888A0);
		}
		if (hover != null) {
			g.setTooltipForNextFrame(font, Component.literal(hover.displayName() + (d.onCooldown(hover, now) ? " (cooling down)" : "")), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(event, doubleClick);
		}
		TechniqueData d = data();
		int x0 = panelX(), y0 = panelY(d);
		int row = 0;
		for (Group grp : WheelGroups.GROUPS) {
			List<Technique> list = learned(grp, d);
			if (list.isEmpty()) {
				continue;
			}
			int ry = y0 + HEADER + SLOTS_H + row * ROW_H;
			for (int i = 0; i < list.size(); i++) {
				int cx = x0 + PAD + LABEL_W + i * PITCH;
				if (event.x() >= cx && event.x() < cx + CELL && event.y() >= ry && event.y() < ry + CELL) {
					choose(d, list.get(i));
					return true;
				}
			}
			row++;
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Same technique: nothing. Selected slot cooling: red line (the server checks too). Else ask. The wheel stays open (D196). */
	public void choose(TechniqueData d, Technique t) {
		if (d.inSlot(slot) == t) {
			return;
		}
		if (d.slotOnCooldown(slot, minecraft.level.getGameTime())) {
			warn = true;
			return;
		}
		ClientPlayNetworking.send(new TechniqueNet.TechniqueAction(TechniqueNet.ASSIGN, t.id(), slot));
		ClientPlayNetworking.send(new TechniqueNet.SkillAction(TechniqueNet.SELECT, slot, 0f)); // D193: the server selection follows an empty slot that was just filled
		ClientSkills.keepSlotForFill();
		warn = false;
	}
}
