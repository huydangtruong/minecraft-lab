package io.github.huydangtruong.immortalcultivation.client.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** D268: the uniform buffers of a post pass (the JSON ones cannot be written to; Time Rewind swaps in its own). */
@Mixin(PostPass.class)
public interface PostPassAccess {
	@Accessor("customUniforms")
	Map<String, GpuBuffer> permahearts$customUniforms();
}
