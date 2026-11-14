package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.PhaseState;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Phase Walk sight limit (D51): dark rock fog while a phasing player's camera is inside a solid block. */
public final class PhaseFog {
	/** End distance of the last fog applied by Phase Walk, or -1 when the last frame had none. Read by game tests. */
	public static volatile float lastEnd = -1;

	private PhaseFog() {
	}

	public static void apply(FogData fog, Camera camera, ClientLevel level) {
		int lvl = PhaseState.phaseLevel(camera.entity());
		BlockPos pos = camera.blockPosition();
		BlockState state = level.getBlockState(pos);
		if (lvl <= 0 || !state.isSuffocating(level, pos)) {
			lastEnd = -1;
			return;
		}
		float start = TechniqueRules.phaseFogStart(lvl);
		float end = TechniqueRules.phaseSightRange(lvl);
		fog.environmentalStart = start;
		fog.environmentalEnd = end;
		fog.renderDistanceStart = Math.min(fog.renderDistanceStart, start);
		fog.renderDistanceEnd = Math.min(fog.renderDistanceEnd, end);
		fog.skyEnd = end;
		fog.cloudEnd = end;
		fog.color.set(0.12f, 0.12f, 0.13f, 1f);
		lastEnd = end;
	}
}
