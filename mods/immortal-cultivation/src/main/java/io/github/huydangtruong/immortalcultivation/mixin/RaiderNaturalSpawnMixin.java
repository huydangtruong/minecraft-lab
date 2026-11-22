package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * D190: vanilla lets every naturally spawned illager walk into a raid nearby (only witches are kept out). With the danger
 * level spawning pillagers and vindicators everywhere, the raid never ran out of raiders. Natural spawns never join a raid.
 */
@Mixin(Raider.class)
public abstract class RaiderNaturalSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("RETURN"))
	private void permahearts$naturalStaysOut(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
		if (reason == EntitySpawnReason.NATURAL && ImmortalCultivation.enabled(level.getLevel().getServer())) {
			((Raider) (Object) this).setCanJoinRaid(false);
		}
	}
}
