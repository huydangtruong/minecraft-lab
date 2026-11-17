package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The God Slayer (P-034, D98): the custom {@link GodSlayerModel}, a glowing layer for the eyes and the blade edges.
 * The katanas are part of the model, so the entity holds no items.
 */
public class GodSlayerRenderer extends MobRenderer<GodSlayer, GodSlayerRenderState, GodSlayerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "god_slayer"), "main");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "textures/entity/god_slayer.png");
	private static final Identifier EYES = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "textures/entity/god_slayer_eyes.png");

	public GodSlayerRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new GodSlayerModel(ctx.bakeLayer(LAYER)), 1.3f);
		addLayer(new Eyes(this));
	}

	@Override
	public GodSlayerRenderState createRenderState() {
		return new GodSlayerRenderState();
	}

	@Override
	public void extractRenderState(GodSlayer entity, GodSlayerRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.swing = entity.getSwingAnimation(partialTick);
		state.flying = entity.isFlyingMode();
		state.forwardLean = entity.freezePose() == GodSlayer.FREEZE_FLY_FORWARD;
	}

	@Override
	public Identifier getTextureLocation(GodSlayerRenderState state) {
		return TEXTURE;
	}

	/** Glowing yellow eyes and a faint glowing blade edge: the eyes texture holds only those pixels. */
	private static final class Eyes extends EyesLayer<GodSlayerRenderState, GodSlayerModel> {
		Eyes(RenderLayerParent<GodSlayerRenderState, GodSlayerModel> parent) {
			super(parent);
		}

		@Override
		public RenderType renderType() {
			return RenderTypes.eyes(EYES);
		}
	}
}
