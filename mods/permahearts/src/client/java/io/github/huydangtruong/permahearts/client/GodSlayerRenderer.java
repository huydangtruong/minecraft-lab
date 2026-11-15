package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.PermaHearts;
import io.github.huydangtruong.permahearts.godslayer.GodSlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The God Slayer: a player-sized humanoid model scaled up by the entity's scale attribute, with two horns and black
 * shoulder plates added to the model, a white hair layer, a glowing yellow eyes layer and the two katanas the entity
 * holds. The skin is drawn by tools/make_god_slayer_texture.py (64x64, the player skin layout).
 */
public class GodSlayerRenderer extends HumanoidMobRenderer<GodSlayer, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "god_slayer"), "main");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "textures/entity/god_slayer.png");
	private static final Identifier EYES = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "textures/entity/god_slayer_eyes.png");

	public GodSlayerRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HumanoidModel<>(ctx.bakeLayer(LAYER)), 0.9f);
		addLayer(new Eyes(this));
	}

	/** The humanoid mesh plus two horns on the head and a black plate on each shoulder. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		for (int side = -1; side <= 1; side += 2) {
			// horn: a stub on the forehead corner and a thinner tip leaning outwards; texture (0,32) and (8,32)
			float x = side * 2.5f;
			PartDefinition horn = head.addOrReplaceChild(side < 0 ? "horn_right" : "horn_left",
				CubeListBuilder.create().texOffs(0, 32).addBox(-1f, -2f, -1f, 2, 2, 2),
				PartPose.offsetAndRotation(x, -8f, -2.5f, -0.35f, 0f, side * 0.25f));
			horn.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 32).addBox(-0.5f, -3f, -0.5f, 1, 3, 1),
				PartPose.offsetAndRotation(0f, -2f, 0f, -0.1f, 0f, side * 0.3f));
		}
		// shoulder plates (sode): a 6x2x6 slab on top of each arm, texture (16,32)
		root.getChild("right_arm").addOrReplaceChild("plate", CubeListBuilder.create().texOffs(16, 32)
			.addBox(-4f, -3.5f, -3f, 6, 2, 6), PartPose.ZERO);
		root.getChild("left_arm").addOrReplaceChild("plate", CubeListBuilder.create().texOffs(16, 32)
			.addBox(-2f, -3.5f, -3f, 6, 2, 6), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}

	/** Glowing yellow eyes: the eyes texture is only the two eye pixels on the face, drawn bright in the dark. */
	private static final class Eyes extends EyesLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
		Eyes(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
			super(parent);
		}

		@Override
		public RenderType renderType() {
			return RenderTypes.eyes(EYES);
		}
	}
}
