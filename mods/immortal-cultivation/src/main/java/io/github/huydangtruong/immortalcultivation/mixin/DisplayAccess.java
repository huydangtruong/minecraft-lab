package io.github.huydangtruong.immortalcultivation.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Display entity setters are private in 26.3; Seism (boulder and spikes) needs them to size and place block displays. */
@Mixin(Display.class)
public interface DisplayAccess {
	@Invoker("setTransformation")
	void permahearts$setTransformation(Transformation transformation);

	@Invoker("setViewRange")
	void permahearts$setViewRange(float range);
}
