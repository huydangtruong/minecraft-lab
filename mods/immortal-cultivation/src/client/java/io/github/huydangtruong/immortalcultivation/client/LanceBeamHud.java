package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.LanceRules;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Solar Lance over the HUD (D163): in first person the 3D beam starts below the screen and the hotbar, hearts and XP bar are
 * drawn over it. This element runs after the vanilla bars, projects the beam to the screen and draws the same gold layers as
 * {@link LightBeam#draw} as a 2D line from the bottom-centre up to the top of the HUD area, fading out there into the 3D beam.
 */
public final class LanceBeamHud implements HudElement {
	/** Gui pixels above the screen bottom where the overlay is fully drawn, and where it has faded out. */
	private static final int FULL_UP_TO = 62;
	private static final int FADE_UP_TO = 100;

	private LanceBeamHud() {}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HELD_ITEM_TOOLTIP,
			Identifier.fromNamespaceAndPath("permahearts", "lance_beam"), new LanceBeamHud());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker t) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 end = ClientLance.localEnd();
		var cam = mc.gameRenderer.mainCamera();
		if (end == null || mc.player == null || cam.isDetached() || mc.gui.hud.isHidden()) {
			return;
		}
		float pt = t.getGameTimeDeltaPartialTick(false);
		float fov = cam.getFov();
		if (!(fov > 1f && fov < 170f)) {
			fov = mc.options.fov().get();
		}
		double tanHalf = Math.tan(Math.toRadians(fov) / 2);
		int gw = g.guiWidth(), gh = g.guiHeight();
		double aspect = gw / (double) gh;
		var f = cam.forwardVector();
		var l = cam.leftVector();
		var u = cam.upVector();
		Vec3 camPos = cam.position();
		// beam start and end in camera space (x left, y up, z forward)
		double[] a = toCamera(LightBeam.chestPos(mc, cam, mc.player, pt).subtract(camPos), f, l, u);
		double[] b = toCamera(end.subtract(camPos), f, l, u);
		// clip to the near plane so a beam end behind the camera still projects
		final double near = 0.05;
		if (a[2] < near && b[2] < near) {
			return;
		}
		if (b[2] < near) {
			b = lerp(a, b, (near - a[2]) / (b[2] - a[2]));
		} else if (a[2] < near) {
			a = lerp(b, a, (near - b[2]) / (a[2] - b[2]));
		}
		float width = LanceRules.widthFactor(ClientLance.localTicksLeft(), ClientLance.localTotal()) * 1.5f;
		float time = mc.player.tickCount + pt;
		float pulse = 1f + 0.12f * (float) Math.sin(time * 0.9f);
		double halfH = gh / 2.0;
		double[] sa = project(a, tanHalf, aspect, gw, gh), sb = project(b, tanHalf, aspect, gw, gh);
		double dx = sb[0] - sa[0], dy = sb[1] - sa[1];
		double len = Math.max(Math.abs(dx), Math.abs(dy));
		if (len < 1) {
			return;
		}
		boolean vertical = Math.abs(dy) >= Math.abs(dx);
		int steps = (int) Math.ceil(Math.min(len, 4000));
		// {half width, red, green, blue, alpha}: same layers as LightBeam.draw
		float[][] layers = {
			{0.12f, 1f, 0.78f, 0.25f, 0.14f},
			{0.065f, 1f, 0.82f, 0.30f, 0.30f},
			{0.03f, 1f, 0.78f, 0.15f, 0.75f},
			{0.012f, 1f, 0.97f, 0.75f, 1f}};
		for (float[] L : layers) {
			int rgb = (Math.round(L[1] * 255) << 16) | (Math.round(L[2] * 255) << 8) | Math.round(L[3] * 255);
			for (int i = 0; i < steps; i++) {
				double k = (i + 0.5) / steps;
				double px = sa[0] + dx * k, py = sa[1] + dy * k;
				if (px < -200 || px > gw + 200 || py < 0 || py > gh) {
					continue;
				}
				float fade = fade(gh - py);
				if (fade <= 0f) {
					continue;
				}
				// perspective-correct depth: 1/z is linear along a projected line
				double z = 1.0 / ((1 - k) / a[2] + k / b[2]);
				double half = Math.max(0.5, L[0] * width * pulse / (z * tanHalf) * halfH);
				int alpha = Math.min(255, Math.round(L[4] * fade * 255));
				if (alpha <= 0) {
					continue;
				}
				int color = (alpha << 24) | rgb;
				if (vertical) {
					g.fill((int) Math.round(px - half), (int) Math.floor(py), (int) Math.round(px + half), (int) Math.floor(py) + 1, color);
				} else {
					g.fill((int) Math.floor(px), (int) Math.round(py - half), (int) Math.floor(px) + 1, (int) Math.round(py + half), color);
				}
			}
		}
	}

	/** 1 near the bottom of the screen, 0 above the HUD area; distance is gui pixels from the bottom edge. */
	private static float fade(double fromBottom) {
		if (fromBottom <= FULL_UP_TO) {
			return 1f;
		}
		if (fromBottom >= FADE_UP_TO) {
			return 0f;
		}
		return 1f - (float) ((fromBottom - FULL_UP_TO) / (FADE_UP_TO - FULL_UP_TO));
	}

	private static double[] toCamera(Vec3 d, org.joml.Vector3fc f, org.joml.Vector3fc l, org.joml.Vector3fc u) {
		return new double[] {d.x * l.x() + d.y * l.y() + d.z * l.z(), d.x * u.x() + d.y * u.y() + d.z * u.z(),
			d.x * f.x() + d.y * f.y() + d.z * f.z()};
	}

	private static double[] lerp(double[] from, double[] to, double k) {
		return new double[] {from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k, from[2] + (to[2] - from[2]) * k};
	}

	private static double[] project(double[] c, double tanHalf, double aspect, int gw, int gh) {
		double nx = -c[0] / (c[2] * tanHalf * aspect), ny = c[1] / (c[2] * tanHalf);
		return new double[] {(nx + 1) / 2 * gw, (1 - ny) / 2 * gh};
	}
}
