package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryNet;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryNet.RunInfo;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryNet.State;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Body of the Purgatory tab (D234-D238): a Solo box and a Team box (floor, cooldown, Start / Join), the current fight
 * with Give up, and the running floors with Watch. Rects are remembered where they were last drawn; clicks use them.
 */
public final class PurgatoryTab {
	private static final int TEXT = 0xFF404040;
	private static final int DIM = 0xFF707070;
	private static final int GOOD = 0xFF1E8A2E;
	private static final int RED = 0xFFB02020;
	private static final int BOX_FILL = 0xFFB8B8B8;
	private static final int BUTTON_W = 44;
	private static final int BUTTON_H = 16;
	private static final int ROW_H = 18;
	private static final int SCROLL_STEP = 9;

	private static int[] soloButton = new int[4];
	private static int[] teamButton = new int[4];
	private static final List<int[]> watchButtons = new ArrayList<>();
	private static final List<Integer> watchIds = new ArrayList<>();
	private static int scroll;

	private PurgatoryTab() {}

	static void reset() {
		scroll = 0;
	}

	private static int left(int ticks) {
		return Math.max(0, ticks - ClientPurgatory.age());
	}

	public static void draw(GuiGraphicsExtractor g, Font font, int x0, int y0, int w, int h, int mx, int my) {
		State s = ClientPurgatory.state();
		int tx = x0 + 7;
		int right = x0 + w - 7;
		int y = y0 + 19;
		boolean inside = s.mode() == PurgatoryNet.MODE_FIGHTING || s.mode() == PurgatoryNet.MODE_FALLEN;
		watchButtons.clear();
		watchIds.clear();

		// Solo
		WandererScreen.inset(g, tx, y, right - tx, 30, BOX_FILL);
		g.text(font, Component.literal("Solo · Floor " + s.soloFloor()).withStyle(ChatFormatting.BOLD), tx + 4, y + 4, TEXT, false);
		int soloCd = left(s.soloCooldown());
		g.text(font, soloCd > 0 ? "Next floor in " + PurgatoryRules.clock(soloCd) : "Ready", tx + 4, y + 17, soloCd > 0 ? DIM : GOOD, false);
		soloButton = new int[] {right - BUTTON_W - 4, y + 7, BUTTON_W, BUTTON_H};
		button(g, font, soloButton, "Start", soloCd == 0 && !inside && !s.queued() && s.mode() != PurgatoryNet.MODE_WATCHING, mx, my);
		y += 34;

		// Team
		WandererScreen.inset(g, tx, y, right - tx, 40, BOX_FILL);
		g.text(font, Component.literal("Team · Floor " + s.teamFloor()).withStyle(ChatFormatting.BOLD), tx + 4, y + 4, TEXT, false);
		int teamCd = left(s.teamCooldown());
		g.text(font, teamCd > 0 ? "Next floor in " + PurgatoryRules.clock(teamCd) : "Ready", tx + 4, y + 16, teamCd > 0 ? DIM : GOOD, false);
		String queue = s.queueSize() == 0 ? "Nobody waiting"
			: s.queueSize() + " waiting" + (s.countdown() >= 0 ? " · starts in " + Math.max(0, (left(s.countdown()) + 19) / 20) + " s"
				: s.queueSize() < PurgatoryRules.TEAM_MIN_PLAYERS ? " · needs " + PurgatoryRules.TEAM_MIN_PLAYERS : "");
		g.text(font, queue, tx + 4, y + 28, DIM, false);
		teamButton = new int[] {right - BUTTON_W - 4, y + 12, BUTTON_W, BUTTON_H};
		button(g, font, teamButton, s.queued() ? "Leave" : "Join", s.queued() || teamCd == 0 && !inside && s.mode() != PurgatoryNet.MODE_WATCHING, mx, my);
		y += 44;

		// Current fight
		if (inside) {
			g.text(font, Component.literal(s.mode() == PurgatoryNet.MODE_FALLEN ? "You fell: watching your team" : "You are fighting")
				.withStyle(ChatFormatting.BOLD), tx, y + 4, RED, false);
			if (s.mode() == PurgatoryNet.MODE_FIGHTING) {
				g.text(font, "Give up: bottom of the screen", tx, y + 14, DIM, false); // D250: the button lives under the inventory
				y += 10;
			}
			y += BUTTON_H + 4;
		} else if (s.mode() == PurgatoryNet.MODE_WATCHING) {
			g.text(font, "Watching · leave: Esc menu or key", tx, y + 4, DIM, false);
			y += BUTTON_H + 4;
		}

		// Running floors
		g.text(font, Component.literal("Running floors").withStyle(ChatFormatting.BOLD), tx, y, TEXT, false);
		y += 11;
		int listTop = y;
		int listBottom = y0 + h - 7;
		if (s.runs().isEmpty()) {
			g.text(font, "None", tx, y + 2, DIM, false);
			return;
		}
		int max = Math.max(0, s.runs().size() * ROW_H - (listBottom - listTop));
		scroll = Math.clamp(scroll, 0, max);
		g.enableScissor(tx, listTop, right, listBottom);
		int ry = listTop - scroll;
		for (RunInfo r : s.runs()) {
			String head = (r.team() ? "Team" : "Solo") + " F" + r.floor() + " · " + PurgatoryRules.clock(left(r.ticksLeft()));
			g.text(font, head, tx, ry + 1, TEXT, false);
			g.text(font, font.plainSubstrByWidth(r.names(), right - tx - BUTTON_W - 4), tx, ry + 10, DIM, false);
			int[] b = {right - BUTTON_W, ry + 1, BUTTON_W, BUTTON_H};
			boolean can = s.mode() == PurgatoryNet.MODE_NONE;
			if (ry + ROW_H > listTop && ry < listBottom) {
				button(g, font, b, "Watch", can && my >= listTop && my < listBottom, mx, my);
				if (can) {
					watchButtons.add(new int[] {b[0], Math.max(b[1], listTop), b[2], Math.min(b[1] + b[3], listBottom) - Math.max(b[1], listTop)});
					watchIds.add(r.id());
				}
			}
			ry += ROW_H;
		}
		g.disableScissor();
	}

	private static void button(GuiGraphicsExtractor g, Font font, int[] r, String label, boolean enabled, int mx, int my) {
		boolean hover = enabled && in(mx, my, r);
		String sprite = !enabled ? "widget/button_disabled" : hover ? "widget/button_highlighted" : "widget/button";
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(sprite), r[0], r[1], r[2], r[3]);
		g.text(font, label, r[0] + (r[2] - font.width(label)) / 2, r[1] + (r[3] - 8) / 2, enabled ? 0xFFFFFFFF : 0xFFA0A0A0, true);
	}

	private static boolean in(double mx, double my, int[] r) {
		return r[2] > 0 && r[3] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
	}

	/** A click on the panel or its edge tabs. True only when it hit one of this tab's buttons. */
	public static boolean click(int x0, int y0, int w, int h, double mx, double my, int button) {
		if (in(mx, my, soloButton)) {
			ClientPurgatory.send(PurgatoryNet.START_SOLO, 0);
			return true;
		}
		if (in(mx, my, teamButton)) {
			ClientPurgatory.send(PurgatoryNet.TEAM, 0);
			return true;
		}
		for (int i = 0; i < watchButtons.size(); i++) {
			if (in(mx, my, watchButtons.get(i))) {
				ClientPurgatory.send(PurgatoryNet.WATCH, watchIds.get(i));
				return true;
			}
		}
		return false; // not on a button: the edge tabs and the screen still get the click
	}

	public static boolean scroll(int x0, int y0, int w, int h, double mx, double my, double amount) {
		scroll -= (int) Math.signum(amount) * SCROLL_STEP;
		return true;
	}

	/** GUI centres of Start and Join (tests). */
	public static List<int[]> hotspots() {
		List<int[]> out = new ArrayList<>();
		for (int[] r : new int[][] {soloButton, teamButton}) {
			out.add(new int[] {r[0] + r[2] / 2, r[1] + r[3] / 2});
		}
		return out;
	}
}
