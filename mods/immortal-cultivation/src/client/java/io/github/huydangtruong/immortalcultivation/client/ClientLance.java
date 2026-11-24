package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.Lance;
import io.github.huydangtruong.immortalcultivation.technique.LanceRules;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Solar Lance, client side (D157, D159): draws the beams the server reports (the local player's and other players'), shows the
 * draining bar, and tells the server when the button that started the beam is let go.
 */
public final class ClientLance {
	private static final class Beam {
		Vec3 end = Vec3.ZERO;
		int ticksLeft;
		int total = 1;
		int age; // client ticks since the last update; a beam that goes quiet is dropped
	}

	private static final Map<Integer, Beam> BEAMS = new HashMap<>();
	/** 0 = none, 1 = the beam was started with the left button, 2 = with the right button. */
	private static int button;

	private ClientLance() {}

	public static void register() {
		net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(ClientLance::submit);
		ClientTickEvents.END_CLIENT_TICK.register(ClientLance::tick);
		ClientPlayNetworking.registerGlobalReceiver(Lance.State.TYPE, (payload, ctx) -> {
			if (!payload.active()) {
				BEAMS.remove(payload.caster());
				return;
			}
			Beam b = BEAMS.computeIfAbsent(payload.caster(), k -> new Beam());
			b.end = new Vec3(payload.x(), payload.y(), payload.z());
			b.ticksLeft = payload.ticksLeft();
			b.total = payload.total();
			b.age = 0;
		});
	}

	/** ClientSkills calls this when it sent a Solar Lance cast: 1 = left button, 2 = right button. */
	static void started(int whichButton) {
		button = whichButton;
	}

	/** Fraction of the local player's bar left (1 = full), or -1 when no beam runs. */
	public static float localFraction() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return -1f;
		}
		Beam b = BEAMS.get(mc.player.getId());
		return b == null ? -1f : LanceRules.barFraction(b.ticksLeft, b.total);
	}

	/** The local player's beam end point, or null when none runs (for the HUD overlay). */
	static Vec3 localEnd() {
		Minecraft mc = Minecraft.getInstance();
		Beam b = mc.player == null ? null : BEAMS.get(mc.player.getId());
		return b == null ? null : b.end;
	}

	static int localTicksLeft() {
		Minecraft mc = Minecraft.getInstance();
		Beam b = mc.player == null ? null : BEAMS.get(mc.player.getId());
		return b == null ? 0 : b.ticksLeft;
	}

	/** Full firing time of the local player's beam (D243: it depends on the level). */
	static int localTotal() {
		Minecraft mc = Minecraft.getInstance();
		Beam b = mc.player == null ? null : BEAMS.get(mc.player.getId());
		return b == null ? 1 : b.total;
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			BEAMS.clear();
			button = 0;
			return;
		}
		for (Iterator<Beam> it = BEAMS.values().iterator(); it.hasNext();) {
			Beam b = it.next();
			if (++b.age > 4) {
				it.remove();
			}
		}
		if (button != 0) {
			boolean down = button == 1 ? mc.options.keyAttack.isDown() : mc.options.keyUse.isDown();
			if (!down) { // D159: let go = the beam stops now (the server ignores it when no beam runs)
				button = 0;
				ClientPlayNetworking.send(new TechniqueNet.SkillAction(TechniqueNet.RELEASE, 0, 0f));
			}
		}
	}

	private static void submit(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || BEAMS.isEmpty()) {
			return;
		}
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		var cam = mc.gameRenderer.mainCamera();
		for (Map.Entry<Integer, Beam> e : BEAMS.entrySet()) {
			Entity who = mc.level.getEntity(e.getKey());
			if (who == null) {
				continue;
			}
			LightBeam.draw(ctx, LightBeam.chestPos(mc, cam, who, pt), e.getValue().end, 1f, 1.5f * LanceRules.widthFactor(e.getValue().ticksLeft, e.getValue().total), mc.player.tickCount + pt);
		}
	}
}
