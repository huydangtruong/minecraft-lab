package io.github.huydangtruong.immortalcultivation.technique;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * D169/D170: where a follower may teleport to near its owner. Never into a block, never into water or lava
 * (unless it is a water mob: undead "breathe" underwater but a zombie turns into a drowned), and a walker only onto ground at most 3 blocks below the spot (no fall damage).
 * Null means "nowhere safe": the follower waits (the owner phases through walls, flies or swims).
 */
public final class SafeTeleport {
	/** Highest drop a walker may land after a teleport (fall damage starts above 3 blocks). */
	static final double MAX_DROP = 3.0;
	private static final double STEP = 0.25;

	private SafeTeleport() {
	}

	public static Vec3 spotOrNull(ServerLevel sl, Mob mob, Vec3 want) {
		boolean flier = flies(mob);
		for (double dy : new double[] {0, 1, 2, -1}) {
			Vec3 at = want.add(0, dy, 0);
			AABB box = mob.getDimensions(mob.getPose()).makeBoundingBox(at);
			if (!sl.noCollision(mob, box)) {
				continue;
			}
			if (!flier) {
				double drop = dropToGround(sl, mob, box);
				if (drop < 0) {
					continue; // no ground close below
				}
				at = at.add(0, -drop, 0);
				box = box.move(0, -drop, 0);
			}
			if (!swims(mob) && sl.containsAnyLiquid(box)) {
				continue;
			}
			return at;
		}
		return null;
	}

	/** Mobs that fly (no gravity, flying navigation, ghast, phantom, vex, blaze). */
	public static boolean flies(Mob mob) {
		return mob.isNoGravity() || mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
			|| mob instanceof net.minecraft.world.entity.monster.Ghast || mob instanceof net.minecraft.world.entity.monster.Phantom
			|| mob instanceof net.minecraft.world.entity.monster.Vex || mob instanceof net.minecraft.world.entity.monster.Blaze;
	}

	/** Water mobs (fish, squid, dolphin, guardian, axolotl, turtle, drowned) may land in water. */
	static boolean swims(Mob mob) {
		return mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation
			|| mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation
			|| mob instanceof net.minecraft.world.entity.monster.zombie.Drowned;
	}

	/** How far the box falls before it stands on something (to a quarter block), or -1 when that is more than 3 blocks. */
	private static double dropToGround(ServerLevel sl, Mob mob, AABB box) {
		for (double d = 0; d <= MAX_DROP; d += STEP) {
			if (!sl.noCollision(mob, box.move(0, -(d + STEP), 0))) {
				return d;
			}
		}
		return -1;
	}
}
