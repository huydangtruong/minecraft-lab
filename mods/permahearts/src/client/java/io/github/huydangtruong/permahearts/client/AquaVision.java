package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.AquaMarine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;

/** P-023 (D69): while Aqua Marine runs and the camera is in water the view is clear and bright (client only, no real effects). */
public final class AquaVision {
	private AquaVision() {}

	/** True while the synced Aqua Marine end tick is still ahead and the player's eyes are in water. */
	public static boolean active(LocalPlayer p) {
		Long end = p.getAttached(AquaMarine.END);
		return end != null && p.level().getGameTime() < end && p.isEyeInFluid(FluidTags.WATER);
	}
}
