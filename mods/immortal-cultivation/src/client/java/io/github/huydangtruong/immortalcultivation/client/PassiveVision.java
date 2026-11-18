package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.passive.PassiveAttachment;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.player.Player;

/** D148 Increase vision, client only: Night-Vision-like light without the effect (no icon, nothing on the server). */
public final class PassiveVision {
	private PassiveVision() {}

	/** 0 = off, 0.2..1.2 by level. */
	public static double strength(Player p) {
		return p == null ? 0 : PassiveAttachment.get(p).activeVision();
	}

	/** Above 100%: 0..1 for the part over 1.0 (1 at level 10). */
	static float over(double v) {
		return (float) Math.max(0, Math.min(1, (v - 1) / 0.2));
	}

	public static void apply(Player p, LightmapRenderState state) {
		double v = strength(p);
		if (v <= 0) {
			return;
		}
		state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, (float) Math.min(1, v));
		float o = over(v);
		if (o > 0) {
			state.brightness = Math.max(state.brightness, Math.min(1f, state.brightness + 0.5f * o));
			state.darknessEffectScale *= 1 - 0.6f * o; // Darkness pulses weaker
		}
	}

	/** Water vision (fog distance under water) at least this much: clearer under water above 100%. */
	public static float waterVision(Player p, float vanilla) {
		return Math.max(vanilla, over(strength(p)));
	}
}
