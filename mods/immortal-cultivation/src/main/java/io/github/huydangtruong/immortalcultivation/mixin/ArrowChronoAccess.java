package io.github.huydangtruong.immortalcultivation.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** D268: an arrow rewound to while it was flying is no longer stuck in the ground. */
@Mixin(AbstractArrow.class)
public interface ArrowChronoAccess {
	@Invoker("setInGround")
	void permahearts$setInGround(boolean inGround);
}
