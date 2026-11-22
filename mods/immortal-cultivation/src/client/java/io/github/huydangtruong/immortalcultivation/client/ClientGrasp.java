package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspNet;
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
		ClientPlayNetworking.registerGlobalReceiver(GraspNet.State.TYPE, (payload, ctx) -> {
			held = payload.held();
			if (payload.targetId() >= 0) {
				targetId = payload.targetId();
				duration = Math.max(1, payload.duration());
				elapsed = 0;
			} else if (payload.targetId() == -1) {
				targetId = -1;
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(GraspNet.Menu.TYPE, (payload, ctx) -> {
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
		ClientPlayNetworking.send(new GraspNet.Request(Grasp.LIST, "", 0));
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
				ClientPlayNetworking.send(new GraspNet.Request(Grasp.START, "", m.getId()));
			}
		} else if (!down && wasDown && tethering()) {
			ClientPlayNetworking.send(new GraspNet.Request(Grasp.CANCEL, "", 0));
			targetId = -1;
		}
	}

	/** Forgets everything of the old world (B5): called when the client disconnects. */
	static void reset() {
		held = 0;
		targetId = -1;
		duration = 0;
		elapsed = 0;
		aimId = -1;
	}

	/** Stops a running tether on the client when the technique is deselected. */
	public static void cancelIfRunning() {
		if (tethering()) {
			ClientPlayNetworking.send(new GraspNet.Request(Grasp.CANCEL, "", 0));
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
				// the pillar is drawn every frame by submitBeam; a few soft particles rise from its foot
				if (mc.player.tickCount % 2 == 0) {
					double r = t.getBbWidth() * 0.5;
					mc.level.addParticle(ParticleTypes.END_ROD, t.getX() + (mc.level.getRandom().nextDouble() - 0.5) * r * 2, t.getY() + 0.1,
						t.getZ() + (mc.level.getRandom().nextDouble() - 0.5) * r * 2, 0, 0.04, 0);
				}
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

	/** D156: while the hold runs, a pillar of light falls from high in the sky onto the mob (no tether from the hand). */
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
		Vec3 foot = t.getPosition(pt);
		Vec3 top = foot.add(0, SKY_HEIGHT, 0);
		float power = 0.55f + 0.45f * fraction(); // brighter with the charge
		LightBeam.draw(ctx, top, foot, power, 2.4f, mc.player.tickCount + pt);
	}

	/** How high above the mob the pillar starts. */
	private static final double SKY_HEIGHT = 96;
}
