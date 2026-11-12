package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.wanderer.WandererNpc;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

/**
 * Wandering trader renderer that gives the quest Wanderer a crimson robe (texture recoloured from the vanilla one),
 * so it stands out from ordinary traders. The marker is the synced WANDERER attachment.
 */
public class WandererRenderer extends WanderingTraderRenderer {
	private static final RenderStateDataKey<Boolean> QUEST_NPC = RenderStateDataKey.create(() -> "permahearts:wanderer");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("permahearts", "textures/entity/wanderer.png");

	public WandererRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public void extractRenderState(WanderingTrader trader, VillagerRenderState state, float partialTick) {
		super.extractRenderState(trader, state, partialTick);
		((FabricRenderState) state).setData(QUEST_NPC, WandererNpc.isWanderer(trader));
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return ((FabricRenderState) state).getDataOrDefault(QUEST_NPC, false) ? TEXTURE : super.getTextureLocation(state);
	}
}
