package io.github.huydangtruong.immortalcultivation.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** D125: while a technique slot is selected the vanilla hotbar draws without its selection frame (the skill bar has it). */
@Mixin(Hud.class)
abstract class HotbarSelectionMixin {
	@Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void permahearts$noSelectionFrame(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
		if (ClientSkills.selected() >= 0 && sprite.getPath().equals("hud/hotbar_selection")) {
			return;
		}
		g.blitSprite(pipeline, sprite, x, y, w, h);
	}
}
