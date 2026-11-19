package io.github.huydangtruong.immortalcultivation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * P-059: the glow of Mega mobs, drawn like spider eyes (RenderTypes.eyes) from textures/entity/mega/&lt;species&gt;.png
 * (made by scripts/mega-glow/GenMegaGlow.java). The species comes from the synced `permahearts:mega` attachment.
 */
public final class MegaGlowLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	public static final RenderStateDataKey<String> SPECIES = RenderStateDataKey.create(() -> "permahearts:mega");

	private static final Set<EntityType<?>> TYPES = Set.of(EntityTypes.PIGLIN_BRUTE, EntityTypes.BLAZE, EntityTypes.WITHER_SKELETON,
		EntityTypes.BREEZE, EntityTypes.VINDICATOR, EntityTypes.EVOKER, EntityTypes.RAVAGER, EntityTypes.HOGLIN, EntityTypes.ZOGLIN,
		EntityTypes.ENDERMAN, EntityTypes.WITCH, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.DROWNED, EntityTypes.GUARDIAN,
		EntityTypes.ELDER_GUARDIAN);

	/** Model the glow is drawn on; for the bogged it is the moss outer layer, which hides the inner face. */
	private final EntityModel<? super S> model;

	public MegaGlowLayer(RenderLayerParent<S, M> parent, EntityModel<? super S> model) {
		super(parent);
		this.model = model;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (TYPES.contains(type)) {
				EntityModel model = type == EntityTypes.BOGGED
					? new SkeletonModel(context.bakeLayer(ModelLayers.BOGGED_OUTER_LAYER)) : renderer.getModel();
				helper.register(new MegaGlowLayer(renderer, model));
			}
		});
	}

	public static Identifier texture(String species) {
		return Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "textures/entity/mega/" + species + ".png");
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		String species = ((FabricRenderState) state).getData(SPECIES);
		if (species == null || state.isInvisible) {
			return;
		}
		collector.order(2).submitModel(model, state, pose, RenderTypes.eyes(texture(species)), light,
			OverlayTexture.NO_OVERLAY, state.outlineColor);
	}
}
