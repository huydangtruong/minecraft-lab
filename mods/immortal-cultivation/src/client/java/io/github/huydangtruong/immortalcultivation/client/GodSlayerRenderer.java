package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
		addLayer(new ChargeFlash(this));
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
		state.charging = entity.chargeTicks() > 0;
	}

	@Override
	public Identifier getTextureLocation(GodSlayerRenderState state) {
		return TEXTURE;
	}

	/** D288: while the blast charges, the whole body glows orange, on and off every 3 ticks. */
	private static final class ChargeFlash extends RenderLayer<GodSlayerRenderState, GodSlayerModel> {
		private static final int ORANGE = 0xFFFF8A1E;

		ChargeFlash(RenderLayerParent<GodSlayerRenderState, GodSlayerModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, GodSlayerRenderState state, float yRot, float xRot) {
			if (!state.charging || state.isInvisible || ((int) state.ageInTicks / 3) % 2 == 1) {
				return;
			}
			collector.order(3).submitModel(getParentModel(), state, pose, RenderTypes.eyes(TEXTURE), light,
				OverlayTexture.NO_OVERLAY, ORANGE, null, state.outlineColor);
		}
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
