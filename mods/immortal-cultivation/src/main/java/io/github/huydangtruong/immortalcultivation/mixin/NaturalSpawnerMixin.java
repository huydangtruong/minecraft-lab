package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.danger.DangerSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** P-054: danger level. createState runs once per level per tick before the spawn pass: remember the cap multiplier; mobsAt adds unlocked types. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@Inject(method = "createState", at = @At("HEAD"))
	private static void permahearts$beginLevel(int spawnableChunkCount, ServerLevel level, NaturalSpawner.ChunkGetter chunkGetter,
											   LocalMobCapCalculator calculator, CallbackInfoReturnable<NaturalSpawner.SpawnState> cir) {
		DangerSpawn.beginLevel(level);
	}

	@Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
	private static void permahearts$extraTypes(ServerLevel level, StructureManager structures, ChunkGenerator generator,
											   MobCategory category, BlockPos pos, CallbackInfoReturnable<WeightedList<MobSpawnSettings.SpawnerData>> cir) {
		if (category == MobCategory.MONSTER) {
			cir.setReturnValue(DangerSpawn.withExtras(level, pos, cir.getReturnValue()));
		}
	}
}
