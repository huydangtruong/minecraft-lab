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
				beam(mc, t);
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

	private static void beam(Minecraft mc, Entity t) {
		Vec3 from = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(0.6)).add(0, -0.3, 0);
		Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0);
		int n = 10;
		float f = fraction();
		for (int i = 0; i <= n; i++) {
			Vec3 at = from.lerp(to, i / (double) n);
			if (i / (double) n <= f + 0.15) {
				mc.level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0, 0);
			} else if (i % 3 == 0) {
				mc.level.addParticle(new DustParticleOptions(0xFFD24A, 0.5f), at.x, at.y, at.z, 0, 0, 0);
			}
		}
	}
}
