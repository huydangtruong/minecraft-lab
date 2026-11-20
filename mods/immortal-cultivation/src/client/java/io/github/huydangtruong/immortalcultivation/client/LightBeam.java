package io.github.huydangtruong.immortalcultivation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The gold laser look first made for the Soul Grasp tether (P-053), shared by Solar Lance (D157) and the Soul Grasp sky
 * pillar (D156): a continuous line of light with a soft glow, drawn every frame as camera-facing quad ribbons.
 */
final class LightBeam {
	private LightBeam() {}

	/** Where a beam leaves a player: the right hand (first person: near the bottom right of the screen). */
	/** Front of the chest, centred: where Solar Lance fires from. */
	static Vec3 chestPos(Minecraft mc, net.minecraft.client.Camera cam, Entity who, float pt) {
		if (who == mc.player && !cam.isDetached()) {
			var f = cam.forwardVector();
			var u = cam.upVector();
			// D161: bottom-centre of the screen at the real vertical FOV: down = distance * tan(fov / 2), a little more to sit just below the edge
			float fov = cam.getFov();
			if (!(fov > 1f && fov < 170f)) {
				fov = mc.options.fov().get();
			}
			double down = 0.6 * Math.tan(Math.toRadians(fov) / 2) * 1.05;
			return cam.position().add(f.x() * 0.6 - u.x() * down, f.y() * 0.6 - u.y() * down, f.z() * 0.6 - u.z() * down);
		}
		Vec3 base = who.getPosition(pt).add(0, who.getBbHeight() * 0.72, 0);
		double yaw = Math.toRadians(who.getYRot());
		return base.add(-Math.sin(yaw) * 0.35, 0, Math.cos(yaw) * 0.35);
	}

	static Vec3 handPos(Minecraft mc, net.minecraft.client.Camera cam, Entity who, float pt) {
		if (who == mc.player && !cam.isDetached()) {
			var f = cam.forwardVector();
			var l = cam.leftVector();
			var u = cam.upVector();
			return cam.position().add(f.x() * 0.6 - l.x() * 0.38 - u.x() * 0.32, f.y() * 0.6 - l.y() * 0.38 - u.y() * 0.32,
				f.z() * 0.6 - l.z() * 0.38 - u.z() * 0.32);
		}
		Vec3 base = who.getPosition(pt).add(0, who.getBbHeight() * 0.7, 0);
		double yaw = Math.toRadians(who.getYRot());
		return base.add(-Math.cos(yaw) * 0.4 - Math.sin(yaw) * 0.3, 0, -Math.sin(yaw) * 0.4 + Math.cos(yaw) * 0.3);
	}

	/**
	 * Draws a beam between two world points. power 0..1 brightens it, widthScale widens it (1 = the tether).
	 * Call from LevelRenderEvents.COLLECT_SUBMITS.
	 */
	static void draw(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx, Vec3 fromWorld, Vec3 toWorld,
			float power, float widthScale, float time) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.mainCamera().position();
		Vec3 from = fromWorld.subtract(camPos);
		Vec3 to = toWorld.subtract(camPos);
		Vec3 dir = to.subtract(from);
		Vec3 side = dir.cross(to.add(from).scale(0.5)); // across the view direction
		if (side.lengthSqr() < 1e-8) {
			return;
		}
		Vec3 s = side.normalize();
		float pulse = 1f + 0.12f * (float) Math.sin(time * 0.9f);
		float pw = Math.max(0f, Math.min(1f, power));
		int seg = Math.max(24, Math.min(96, (int) (dir.length() / 3)));
		ctx.submitNodeCollector().submitCustomGeometry(ctx.poseStack(), net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads(), (pose, vc) -> {
			// {half width, red, green, blue, alpha}: outer glow, glow, gold body, white-gold core
			float[][] layers = {
				{0.12f, 1f, 0.78f, 0.25f, 0.14f},
				{0.065f, 1f, 0.82f, 0.30f, 0.30f},
				{0.03f, 1f, 0.78f, 0.15f, 0.75f},
				{0.012f, 1f, 0.97f, 0.75f, 1f}};
			for (float[] L : layers) {
				float w = L[0] * widthScale * pulse * (0.7f + 0.3f * pw);
				int a = Math.min(255, Math.round(L[4] * pw * 255));
				for (int i = 0; i < seg; i++) {
					Vec3 p0 = from.lerp(to, i / (double) seg), p1 = from.lerp(to, (i + 1) / (double) seg);
					quad(pose, vc, p0, p1, s, w, L, a);
				}
			}
		});
	}

	private static void quad(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc, Vec3 p0, Vec3 p1,
			Vec3 side, float w, float[] c, int a) {
		int r = Math.round(c[1] * 255), g = Math.round(c[2] * 255), b = Math.round(c[3] * 255);
		Vec3 o = side.scale(w);
		vertex(pose, vc, p0.add(o), r, g, b, a);
		vertex(pose, vc, p1.add(o), r, g, b, a);
		vertex(pose, vc, p1.subtract(o), r, g, b, a);
		vertex(pose, vc, p0.subtract(o), r, g, b, a);
	}

	private static void vertex(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc, Vec3 p, int r, int g, int b, int a) {
		vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
	}
}
