package io.github.huydangtruong.permahearts.technique.illusion;

import io.github.huydangtruong.permahearts.technique.Illusion;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.sheep.Sheep;

/** SHEAR: walks to an adult sheep that still has its wool and shears it the vanilla way, then waits for the next one. */
final class ShearTask implements WorkTask {
	private Sheep target;
	private boolean had;

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		if (target != null && (!target.isAlive() || !target.readyForShearing() || g.isSkipped(target.getUUID()))) {
			target = null;
		}
		if (target == null) {
			if (!g.scanReady()) {
				return had;
			}
			double r = a.workRadius();
			List<Sheep> sheep = sl.getEntitiesOfClass(Sheep.class, a.workBox(), s -> s.readyForShearing() && g.inArea(s.getX(), s.getZ()) && !g.isSkipped(s.getUUID()));
			target = sheep.stream().min(java.util.Comparator.comparingDouble(s -> s.distanceToSqr(a))).orElse(null);
			had = target != null;
			if (target == null) {
				return false;
			}
		}
		if (!g.goTo(target.getUUID(), target.position(), 3.5) || !g.ready()) {
			return true;
		}
		a.getLookControl().setLookAt(target);
		target.shear(sl, SoundSource.NEUTRAL, a.lockedItem());
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
		g.pickupFor(60);
		target = null;
		return true;
	}
}
