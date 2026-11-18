package io.github.huydangtruong.immortalcultivation.danger;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** P-054: natural spawn tweaks (mob cap, daylight/rain spawns, new types). Called from mixins; server thread only. */
public final class DangerSpawn {
	/** Hostile cap multiplier of the level being spawned right now (set in createState, once per level per tick). */
	private static double multiplier = 1.0;
	private static long dayMarkPos = Long.MIN_VALUE;
	private static long dayMarkTick = -1;

	private DangerSpawn() {}

	public static double multiplier() {
		return multiplier;
	}

	public static void beginLevel(ServerLevel level) {
		multiplier = level.dimension() == Level.OVERWORLD ? Danger.spawnMultiplier(level.getServer()) : 1.0;
	}

	/** Sky light would block this monster spawn: allow it by chance in daylight, always in rain. Block light still counts. */
	public static boolean allowDaySpawn(ServerLevelAccessor la, BlockPos pos, RandomSource random) {
		ServerLevel sl = la.getLevel();
		if (sl.dimension() != Level.OVERWORLD || sl.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		int lvl = Danger.level(sl.getServer());
		if (lvl < 6) {
			return false;
		}
		int limit = la.dimensionType().monsterSpawnBlockLightLimit();
		if (limit < 15 && la.getBrightness(LightLayer.BLOCK, pos) > limit) {
			return false;
		}
		boolean ok = DangerRules.rainSpawns(lvl) && sl.isRainingAt(pos)
			|| sl.isBrightOutside() && random.nextDouble() < DangerRules.daySpawnFactor(lvl);
		if (ok) {
			dayMarkPos = pos.asLong();
			dayMarkTick = sl.getGameTime();
		}
		return ok;
	}

	/** True when this fresh entity stands where a day spawn was just granted (same tick, same column). */
	public static boolean consumeDayMark(Entity e, long gameTime) {
		if (dayMarkTick != gameTime) {
			return false;
		}
		BlockPos p = BlockPos.of(dayMarkPos);
		return p.getX() == e.getBlockX() && p.getZ() == e.getBlockZ() && Math.abs(p.getY() - e.getBlockY()) <= 1;
	}

	public static EntityType<?> entityType(DangerRules.Type t) {
		return switch (t) {
			case PILLAGER -> EntityTypes.PILLAGER;
			case VINDICATOR -> EntityTypes.VINDICATOR;
			case BLAZE -> EntityTypes.BLAZE;
			case WITHER_SKELETON -> EntityTypes.WITHER_SKELETON;
			case EVOKER -> EntityTypes.EVOKER;
			case PIGLIN_BRUTE -> EntityTypes.PIGLIN_BRUTE;
			case RAVAGER -> EntityTypes.RAVAGER;
		};
	}

	/** The vanilla Overworld monster picks plus the types the level has unlocked. Returns the input when nothing is added. */
	public static WeightedList<MobSpawnSettings.SpawnerData> withExtras(ServerLevel level, BlockPos pos, WeightedList<MobSpawnSettings.SpawnerData> base) {
		if (level.dimension() != Level.OVERWORLD || base.isEmpty()) {
			return base;
		}
		int lvl = Danger.level(level.getServer());
		if (lvl < 4) {
			return base;
		}
		WeightedList.Builder<MobSpawnSettings.SpawnerData> b = WeightedList.builder();
		for (Weighted<MobSpawnSettings.SpawnerData> w : base.unwrap()) {
			b.add(w.value(), w.weight());
		}
		boolean dark = level.getMaxLocalRawBrightness(pos) <= 7;
		for (DangerRules.Type t : DangerRules.unlockedTypes(lvl)) {
			if (!t.darkOnly || dark) {
				b.add(new MobSpawnSettings.SpawnerData(entityType(t), ConstantInt.of(1)), t.weight);
			}
		}
		return b.build();
	}
}
