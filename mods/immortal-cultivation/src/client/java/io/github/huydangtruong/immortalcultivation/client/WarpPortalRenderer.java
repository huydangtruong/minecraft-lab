package io.github.huydangtruong.immortalcultivation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.huydangtruong.immortalcultivation.technique.warp.WarpPortal;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a Cosmic Warping portal (D75) with two plain textured shapes, no shaders: a static purple ring with a halo, and
 * a swirl disc that slowly turns inside it, tinted with the owner's colour. Both are lit full-bright.
 */
public class WarpPortalRenderer extends EntityRenderer<WarpPortal, WarpPortalRenderer.State> {
	private static final Identifier RING = Identifier.fromNamespaceAndPath("permahearts", "textures/entity/warp_portal_ring.png");
	private static final Identifier SWIRL = Identifier.fromNamespaceAndPath("permahearts", "textures/entity/warp_portal_swirl.png");
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final int SEGMENTS = 24;
	/** Radians per tick the swirl turns. */
	private static final float SPIN = 0.04f;

	public static class State extends EntityRenderState {
		float yaw;
		int color;
	}

	public WarpPortalRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(WarpPortal portal, State state, float partialTick) {
		super.extractRenderState(portal, state, partialTick);
		state.yaw = portal.getYRot();
		state.color = portal.color();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		double yaw = Math.toRadians(state.yaw);
		float tx = (float) Math.cos(yaw), tz = (float) Math.sin(yaw); // the portal plane runs along this
		float nx = -tz, nz = tx; // its normal
		float cy = 0.75f; // centre height (entity is 1.5 tall)
		// ring and halo: one quad, 1.3 wide and 1.95 tall (the texture's ring fills 0.92 of it)
		collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(RING), (p, vc) -> {
			float hw = 0.65f, hh = 0.975f;
			vertex(vc, p, tx * -hw, cy - hh, tz * -hw, 0, 1, 0xFFFFFFFF, nx, nz);
			vertex(vc, p, tx * hw, cy - hh, tz * hw, 1, 1, 0xFFFFFFFF, nx, nz);
			vertex(vc, p, tx * hw, cy + hh, tz * hw, 1, 0, 0xFFFFFFFF, nx, nz);
			vertex(vc, p, tx * -hw, cy + hh, tz * -hw, 0, 0, 0xFFFFFFFF, nx, nz);
		});
		// swirl: an ellipse made of triangles; the texture is turned by changing the UVs, so it always stays inside
		float angle = (state.ageInTicks) * SPIN;
		float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
		float pulse = 0.85f + 0.15f * (float) Math.sin(state.ageInTicks * 0.1f);
		int tint = ((int) (255 * pulse) << 24) | (state.color & 0xFFFFFF);
		collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(SWIRL), (p, vc) -> {
			float hw = 0.6f, hh = 0.9f;
			for (int i = 0; i < SEGMENTS; i++) {
				double a0 = Math.PI * 2 * i / SEGMENTS, a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
				float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
				// centre
				vertex(vc, p, 0, cy, 0, 0.5f, 0.5f, tint, nx, nz);
				vertex(vc, p, tx * c0 * hw, cy + s0 * hh, tz * c0 * hw, u(c0, s0, cos, sin), v(c0, s0, cos, sin), tint, nx, nz);
				vertex(vc, p, tx * c1 * hw, cy + s1 * hh, tz * c1 * hw, u(c1, s1, cos, sin), v(c1, s1, cos, sin), tint, nx, nz);
				vertex(vc, p, tx * c1 * hw, cy + s1 * hh, tz * c1 * hw, u(c1, s1, cos, sin), v(c1, s1, cos, sin), tint, nx, nz);
			}
		});
	}

	/** UV of a point on the unit circle after turning by the angle (cos, sin); the circle spans 0..1 of the texture. */
	private static float u(float c, float s, float cos, float sin) {
		return 0.5f + 0.5f * (c * cos - s * sin);
	}

	private static float v(float c, float s, float cos, float sin) {
		return 0.5f + 0.5f * (c * sin + s * cos);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, float nx, float nz) {
		vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, 0f, nz);
	}
}
