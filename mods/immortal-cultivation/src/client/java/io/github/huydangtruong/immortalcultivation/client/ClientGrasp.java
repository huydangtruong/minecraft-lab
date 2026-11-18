package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Soul Grasp, client side (P-053): the aim outline (yellow = can be grasped, red = cannot), the tether beam and the charge
 * bar while the server tether runs, the hold-to-tether input and the list screen. The server decides everything; this only
 * asks and shows what it was told.
 */
public final class ClientGrasp {
	private static int held;
	private static int targetId = -1;
	private static int duration;
	private static int elapsed;
	private static int aimId = -1;

	private ClientGrasp() {}

	public static void register() {
		net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(ClientGrasp::submitBeam);
		ClientPlayNetworking.registerGlobalReceiver(Grasp.State.TYPE, (payload, ctx) -> {
			held = payload.held();
			if (payload.targetId() >= 0) {
				targetId = payload.targetId();
				duration = Math.max(1, payload.duration());
				elapsed = 0;
			} else if (payload.targetId() == -1) {
				targetId = -1;
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Grasp.Menu.TYPE, (payload, ctx) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.gui.screen() instanceof GraspScreen open) {
				open.update(payload);
			} else if (mc.gui.screen() == null) {
				mc.gui.setScreen(new GraspScreen(payload));
			}
		});
	}

	public static boolean tethering() {
		return targetId >= 0;
	}

	public static float fraction() {
		return tethering() ? Math.min(1f, elapsed / (float) duration) : 0f;
	}

	/** Left click with the technique selected: asks for the list. */
	public static void openList() {
		ClientPlayNetworking.send(new Grasp.Request(Grasp.LIST, "", 0));
	}

	/** The mob under the crosshair within the select range, or null. */
	private static Mob aimed(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return null;
		}
		Vec3 eye = mc.player.getEyePosition();
		Vec3 end = eye.add(mc.player.getLookAngle().scale(GraspRules.SELECT_RANGE));
		Mob best = null;
		double bestD = Double.MAX_VALUE;
		AABB box = mc.player.getBoundingBox().expandTowards(mc.player.getLookAngle().scale(GraspRules.SELECT_RANGE)).inflate(1);
		for (Mob m : mc.level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && !e.isSpectator())) {
			var hit = m.getBoundingBox().inflate(0.1).clip(eye, end);
			if (hit.isPresent()) {
				double d = eye.distanceToSqr(hit.get());
				if (d < bestD) {
					bestD = d;
					best = m;
				}
			}
		}
		return best;
	}

	/** Right click: hold to tether the mob under the crosshair, let go to cancel. */
	public static void tickUse(Minecraft mc, boolean down, boolean wasDown) {
		if (down && !wasDown) {
			Mob m = aimed(mc);
			if (m == null) {
				mc.player.sendOverlayMessage(Component.literal("Look at a mob within " + (int) GraspRules.SELECT_RANGE + " blocks"));
			} else {
				ClientPlayNetworking.send(new Grasp.Request(Grasp.START, "", m.getId()));
			}
		} else if (!down && wasDown && tethering()) {
			ClientPlayNetworking.send(new Grasp.Request(Grasp.CANCEL, "", 0));
			targetId = -1;
		}
	}

	/** Stops a running tether on the client when the technique is deselected. */
	public static void cancelIfRunning() {
		if (tethering()) {
			ClientPlayNetworking.send(new Grasp.Request(Grasp.CANCEL, "", 0));
			targetId = -1;
		}
	}

	/** Every client tick: beam while tethering, outline of the aimed mob while Soul Grasp is selected. */
	public static void tick(Minecraft mc, boolean selectedGrasp) {
		if (mc.level == null || mc.player == null) {
			targetId = -1;
			return;
		}
		if (tethering()) {
			elapsed++;
			Entity t = mc.level.getEntity(targetId);
			if (t == null || !t.isAlive()) {
				targetId = -1;
			} else {
				// the beam is drawn every frame by submitBeam
			}
		}
		if (!selectedGrasp) {
			aimId = -1;
			return;
		}
		Mob m = aimed(mc);
		aimId = m == null ? -1 : m.getId();
		if (m != null && mc.player.tickCount % 3 == 0 && !tethering()) {
			int level = ClientSkills.data(mc.player).level(Technique.GRASP);
			boolean ok = GraspRules.check(Grasp.facts(m), m.getMaxHealth(), level, held).ok();
			outline(mc, m.getBoundingBox(), ok ? 0xFFD24A : 0xFF3030);
		}
	}

	private static void outline(Minecraft mc, AABB b, int color) {
		var p = new DustParticleOptions(color, 0.6f);
		double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
		for (double x : xs) {
			for (double y : ys) {
				for (double z : zs) {
					mc.level.addParticle(p, x, y, z, 0, 0, 0);
				}
			}
		}
		for (double y = b.minY; y <= b.maxY; y += 0.5) {
			mc.level.addParticle(p, b.minX, y, b.minZ, 0, 0, 0);
			mc.level.addParticle(p, b.maxX, y, b.maxZ, 0, 0, 0);
		}
	}

	/** Where the beam leaves the player: the right hand (first person: near the bottom right of the screen). */
	private static Vec3 handPos(Minecraft mc, net.minecraft.client.Camera cam, float pt) {
		if (!cam.isDetached()) {
			var f = cam.forwardVector();
			var l = cam.leftVector();
			var u = cam.upVector();
			return cam.position().add(f.x() * 0.6 - l.x() * 0.38 - u.x() * 0.32, f.y() * 0.6 - l.y() * 0.38 - u.y() * 0.32,
				f.z() * 0.6 - l.z() * 0.38 - u.z() * 0.32);
		}
		Vec3 base = mc.player.getPosition(pt).add(0, mc.player.getBbHeight() * 0.7, 0);
		double yaw = Math.toRadians(mc.player.getYRot());
		return base.add(-Math.cos(yaw) * 0.4 - Math.sin(yaw) * 0.3, 0, -Math.sin(yaw) * 0.4 + Math.cos(yaw) * 0.3);
	}

	/** Draws the tether as a continuous gold line of light with a soft glow, every frame (a quad ribbon facing the camera). */
	private static void submitBeam(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (!tethering() || mc.level == null || mc.player == null) {
			return;
		}
		Entity t = mc.level.getEntity(targetId);
		if (t == null || !t.isAlive()) {
			return;
		}
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		var cam = mc.gameRenderer.mainCamera();
		Vec3 camPos = cam.position();
		Vec3 from = handPos(mc, cam, pt).subtract(camPos);
		Vec3 to = t.getPosition(pt).add(0, t.getBbHeight() / 2, 0).subtract(camPos);
		Vec3 dir = to.subtract(from);
		Vec3 side = dir.cross(to.add(from).scale(0.5)); // across the view direction
		if (side.lengthSqr() < 1e-8) {
			return;
		}
		side = side.normalize();
		float f = fraction();
		float time = (mc.player.tickCount + pt);
		float pulse = 1f + 0.12f * (float) Math.sin(time * 0.9f);
		float power = 0.55f + 0.45f * f; // brighter with the charge
		Vec3 s = side;
		ctx.submitNodeCollector().submitCustomGeometry(ctx.poseStack(), net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads(), (pose, vc) -> {
			// {half width, red, green, blue, alpha}: outer glow, glow, gold body, white-gold core
			float[][] layers = {
				{0.12f, 1f, 0.78f, 0.25f, 0.14f},
				{0.065f, 1f, 0.82f, 0.30f, 0.30f},
				{0.03f, 1f, 0.78f, 0.15f, 0.75f},
				{0.012f, 1f, 0.97f, 0.75f, 1f}};
			int seg = 24;
			for (float[] L : layers) {
				float w = L[0] * pulse * (0.7f + 0.3f * power);
				int a = Math.min(255, Math.round(L[4] * power * 255));
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
