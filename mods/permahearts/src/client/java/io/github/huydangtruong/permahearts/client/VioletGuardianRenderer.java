package io.github.huydangtruong.permahearts.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;

/** The vanilla iron golem model, multiplied by a violet colour at render time (no texture is copied or edited). */
public class VioletGuardianRenderer extends IronGolemRenderer {
	/** ARGB multiplier: the grey iron texture becomes violet. */
	static final int VIOLET = 0xFFB070FF;

	public VioletGuardianRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	protected int getModelTint(IronGolemRenderState state) {
		return VIOLET;
	}
}
