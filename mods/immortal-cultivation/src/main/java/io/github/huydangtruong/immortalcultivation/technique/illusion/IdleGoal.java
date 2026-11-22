package io.github.huydangtruong.immortalcultivation.technique.illusion;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.PhaseState;
import io.github.huydangtruong.immortalcultivation.technique.SafeTeleport;
import static io.github.huydangtruong.immortalcultivation.technique.Illusion.FOLLOW_FROM;
import static io.github.huydangtruong.immortalcultivation.technique.Illusion.TELEPORT_FROM;

/** Without a target: follows the owner (within about 6 blocks), wanders, looks around, sometimes jumps or sneaks, sometimes looks at a player. */
public final class IdleGoal extends Goal {
	private final Illusion a;
	private int timer;
	private int crouchLeft;
	private int lookLeft;
	private Vec3 lookAt = Vec3.ZERO;

	public IdleGoal(Illusion a) {
		this.a = a;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return a.getTarget() == null && !a.job.works() && OwnedSummon.ownerOf(a) != null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		a.setCrouching(false);
		a.setSprinting(false);
	}

	@Override
	public void tick() {
		Player o = OwnedSummon.ownerOf(a);
		if (o == null || !(a.level() instanceof ServerLevel sl)) {
			return;
		}
		double d2 = a.distanceToSqr(o);
		if (d2 > TELEPORT_FROM * TELEPORT_FROM) {
			if (PhaseState.phasing(o)) { // D169: wait while the owner is in the walls
				return;
			}
			Vec3 p = SafeTeleport.spotOrNull(sl, a, o.position().add(a.getRandom().nextInt(5) - 2, 0, a.getRandom().nextInt(5) - 2)); // D170
			if (p != null) { // D170: not into the air or water while the owner flies or swims
				a.snapTo(p.x, p.y, p.z);
				a.getNavigation().stop();
			}
			return;
		}
		if (lookLeft > 0) {
			lookLeft--;
			a.getLookControl().setLookAt(lookAt.x, lookAt.y, lookAt.z);
		}
		if (crouchLeft > 0 && --crouchLeft == 0) {
			a.setCrouching(false);
		}
		if (d2 > FOLLOW_FROM * FOLLOW_FROM) {
			a.setCrouching(false);
			crouchLeft = 0;
			a.setSprinting(o.isSprinting() || d2 > 12 * 12);
			if (a.tickCount % 10 == 0) {
				a.getNavigation().moveTo(o, 1.0);
			}
			return;
		}
		a.setSprinting(false);
		if (--timer <= 0) {
			timer = 20 + a.getRandom().nextInt(60);
			act(o);
		}
	}

	private void act(Player o) {
		double r = a.getRandom().nextDouble();
		if (r < 0.40) { // wander near the owner
			a.getNavigation().moveTo(o.getX() + (a.getRandom().nextDouble() - 0.5) * 8, o.getY(), o.getZ() + (a.getRandom().nextDouble() - 0.5) * 8, 1.0);
		} else if (r < 0.60) { // look at the nearest player
			Player p = a.level().getNearestPlayer(a, 16.0);
			if (p != null) {
				lookAt = p.getEyePosition();
				lookLeft = 30 + a.getRandom().nextInt(30);
			}
		} else if (r < 0.75) {
			if (a.onGround()) {
				a.getJumpControl().jump();
			}
		} else if (r < 0.88) { // sneak for a moment
			a.setCrouching(true);
			crouchLeft = 20 + a.getRandom().nextInt(30);
		} else { // look around
			double ang = a.getRandom().nextDouble() * Math.PI * 2;
			lookAt = new Vec3(a.getX() + Math.cos(ang) * 6, a.getEyeY() + (a.getRandom().nextDouble() - 0.5) * 3, a.getZ() + Math.sin(ang) * 6);
			lookLeft = 20 + a.getRandom().nextInt(30);
		}
	}
}
