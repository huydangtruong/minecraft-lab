package io.github.huydangtruong.immortalcultivation.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** D268: rewinding a mob to before it died clears its dead flag. */
@Mixin(LivingEntity.class)
public interface LivingEntityChronoAccess {
	@Accessor("dead")
	boolean permahearts$dead();

	@Accessor("dead")
	void permahearts$setDead(boolean dead);
}
