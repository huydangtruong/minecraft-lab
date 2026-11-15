package io.github.huydangtruong.permahearts.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the God Slayer model needs on top of the living state: how far the slash swing has come (0 = idle, 1 = done). */
public class GodSlayerRenderState extends LivingEntityRenderState {
	public float swing;
}
