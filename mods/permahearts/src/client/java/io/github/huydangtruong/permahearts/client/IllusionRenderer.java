package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.Illusion;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Draws an Illusion clone as its owner. The clone is not an Avatar, so this renderer only fills an
 * {@link AvatarRenderState} (owner's skin, gear and pose); the render dispatcher then draws every AvatarRenderState with
 * the vanilla player renderer for the skin's arm model (wide or slim), with armour, held items and the name tag.
 */
public class IllusionRenderer extends LivingEntityRenderer<Illusion, AvatarRenderState, PlayerModel> {
	public IllusionRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state.skin.body().texturePath();
	}

	@Override
	public void extractRenderState(Illusion clone, AvatarRenderState state, float partialTick) {
		super.extractRenderState(clone, state, partialTick);
		HumanoidMobRenderer.extractHumanoidRenderState(clone, state, partialTick, itemModelResolver);
		state.skin = skinOf(clone.owner());
		state.leftArmPose = clone.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
		state.rightArmPose = clone.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
		state.isSpectator = false;
		state.showHat = true;
		state.showJacket = true;
		state.showLeftPants = true;
		state.showRightPants = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
		state.showCape = false;
		state.id = clone.getId();
		state.heldOnHead.clear();
	}

	/** The owner's skin from the client's player list; a default skin for that UUID when the owner is not listed. */
	private static PlayerSkin skinOf(UUID id) {
		if (id == null) {
			return DefaultPlayerSkin.getDefaultSkin();
		}
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
		return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
	}
}
