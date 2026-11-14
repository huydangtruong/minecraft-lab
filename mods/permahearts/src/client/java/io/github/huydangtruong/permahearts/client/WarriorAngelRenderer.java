package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.resources.Identifier;

/** The vanilla vex model and animations with an original gold and white texture (drawn by tools/make_angel_texture.py). */
public class WarriorAngelRenderer extends VexRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "textures/entity/warrior_angel.png");
	private static final Identifier TEXTURE_CHARGING = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "textures/entity/warrior_angel_charging.png");

	public WarriorAngelRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTextureLocation(VexRenderState state) {
		return state.isCharging ? TEXTURE_CHARGING : TEXTURE;
	}
}
