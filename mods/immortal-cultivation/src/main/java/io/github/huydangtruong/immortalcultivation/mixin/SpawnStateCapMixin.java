package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.DangerSpawn;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** P-054: the global hostile mob cap (per chunk count) grows with the danger level in the Overworld. */
@Mixin(NaturalSpawner.SpawnState.class)
public abstract class SpawnStateCapMixin {
	@Redirect(method = "canSpawnForCategoryGlobal", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
	private int permahearts$cap(MobCategory category) {
		int max = category.getMaxInstancesPerChunk();
		return category == MobCategory.MONSTER ? (int) Math.round(max * DangerSpawn.multiplier()) : max;
	}
}
