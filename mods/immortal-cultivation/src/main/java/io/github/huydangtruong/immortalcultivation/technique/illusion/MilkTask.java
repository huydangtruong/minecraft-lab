package io.github.huydangtruong.immortalcultivation.technique.illusion;

import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * MILK: every milk bucket needs one empty bucket taken from the chest (the clone starts with none, and no bucket is ever made
 * from nothing). No chest or no empty bucket: it waits near the cows.
 */
final class MilkTask implements WorkTask {
	private Cow target;
	private boolean had;

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		Container chest = g.chest(sl);
		if (chest == null || IllusionCarry.countIn(chest, s -> s.is(Items.BUCKET)) == 0) {
			target = null;
			had = false;
			return false;
		}
		if (target != null && (!target.isAlive() || g.isSkipped(target.getUUID()))) {
			target = null;
		}
		if (target == null) {
			if (!g.scanReady()) {
				return had;
			}
			List<Cow> cows = sl.getEntitiesOfClass(Cow.class, a.workBox(), c -> !c.isBaby() && g.inArea(c.getX(), c.getZ()) && !g.isSkipped(c.getUUID()));
			target = cows.stream().min(java.util.Comparator.comparingDouble(c -> c.distanceToSqr(a))).orElse(null);
			had = target != null;
			if (target == null) {
				return false;
			}
		}
		if (!g.goTo(target.getUUID(), target.position(), 3.5) || !g.ready()) {
			return true;
		}
		a.getLookControl().setLookAt(target);
		ItemStack bucket = IllusionCarry.takeFrom(chest, s -> s.is(Items.BUCKET), 1);
		if (!bucket.isEmpty()) {
			ItemStack left = a.carry().add(new ItemStack(Items.MILK_BUCKET));
			if (!left.isEmpty()) {
				IllusionCarry.insert(chest, bucket); // no room to carry: put the bucket back
			} else {
				sl.playSound(null, target.blockPosition(), SoundEvents.COW_MILK, SoundSource.NEUTRAL, 1f, 1f);
			}
		}
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
		return true;
	}
}
