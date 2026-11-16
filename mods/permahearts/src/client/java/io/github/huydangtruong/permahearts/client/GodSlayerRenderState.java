package io.github.huydangtruong.permahearts.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What the God Slayer model needs on top of the living state: how far the slash swing has come (0 = idle, 1 = done),
 * and whether it is in the flying mode (wings out; the body leans forward by the horizontal speed).
 */
public class GodSlayerRenderState extends LivingEntityRenderState {
	public float swing;
	public boolean flying;
	/** Frozen in the forward-flight pose: full lean without moving (D104). */
	public boolean forwardLean;
}
