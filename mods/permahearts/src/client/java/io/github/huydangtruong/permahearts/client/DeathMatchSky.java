package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.wanderer.DeathMatch;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector3f;

/** D116: purple sky and fog while a Death Match runs (the server syncs the flag as a player attachment). Dark, a little brighter than the Blood Moon. */
public final class DeathMatchSky {
	/** Sky dome color. */
	public static final Vector3f SKY = new Vector3f(0.24f, 0.09f, 0.38f);
	/** Fog (horizon) color: slightly lighter than the dome. */
	public static final Vector3f FOG = new Vector3f(0.30f, 0.12f, 0.46f);

	private DeathMatchSky() {}

	public static boolean active() {
		var p = Minecraft.getInstance().player;
		return p != null && Boolean.TRUE.equals(p.getAttached(DeathMatch.SKY));
	}

	public static void applySky(SkyRenderState state) {
		if (active()) {
			state.skyColor = SKY;
		}
	}

	public static void applyFog(FogData fog, Camera camera, ClientLevel level) {
		if (active() && level.getBlockState(camera.blockPosition()).getFluidState().isEmpty()) {
			fog.color.set(FOG.x, FOG.y, FOG.z, 1f);
		}
	}
}
