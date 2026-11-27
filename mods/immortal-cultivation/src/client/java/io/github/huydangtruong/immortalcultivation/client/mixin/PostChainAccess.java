package io.github.huydangtruong.immortalcultivation.client.mixin;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** D268: the Time Rewind grey effect writes its camera and boxes into its first pass every frame. */
@Mixin(PostChain.class)
public interface PostChainAccess {
	@Accessor("passes")
	List<PostPass> permahearts$passes();
}
