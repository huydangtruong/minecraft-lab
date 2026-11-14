package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.technique.PhaseState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Phase Walk (technique PHASE): blocks at or above the floor level have no collision for the phasing player, both sides. */
@Mixin(EntityCollisionContext.class)
abstract class EntityCollisionContextMixin {
	@Shadow
	@Final
	private Entity entity;

	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
	private void permahearts$phase(BlockState state, CollisionGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
		if (PhaseState.passesThrough(entity, pos)) {
			cir.setReturnValue(Shapes.empty());
		}
	}
}
