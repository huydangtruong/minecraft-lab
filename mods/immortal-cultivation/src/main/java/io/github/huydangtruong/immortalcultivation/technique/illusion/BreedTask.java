package io.github.huydangtruong.immortalcultivation.technique.illusion;

import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;

/**
 * BREED: feeds adult cows, sheep or pigs that can breed, two of the same kind at a time, so the vanilla love mode (and its
 * 5-minute cooldown) does the rest. Food is never free: it comes from the chest first, then from the clone's own share.
 * The birth XP goes straight to the owner, no orb (D140, see {@link IllusionBreeding}).
 */
final class BreedTask implements WorkTask {
	private static final double REACH = 3.5;
	private Animal target;
	private boolean had;

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		Predicate<ItemStack> food = s -> s.is(a.lockedItem().getItem());
		Container chest = g.chest(sl);
		boolean haveFood = a.carry().count(food) > 0 || (chest != null && IllusionCarry.countIn(chest, food) > 0);
		if (!haveFood) {
			target = null;
			had = false;
			return false;
		}
		if (target != null && (!target.isAlive() || !target.canFallInLove() || g.isSkipped(target.getUUID()))) {
			target = null;
		}
		if (target == null) {
			if (!g.scanReady()) {
				return had;
			}
			target = pick(g, sl, a, food);
			had = target != null;
			if (target == null) {
				return false;
			}
		}
		if (!g.goTo(target.getUUID(), target.position(), REACH) || !g.ready()) {
			return true;
		}
		a.getLookControl().setLookAt(target);
		ItemStack eaten = ItemStack.EMPTY;
		if (chest != null) {
			eaten = IllusionCarry.takeFrom(chest, food, 1);
		}
		if (eaten.isEmpty()) {
			eaten = a.carry().take(food, 1);
		}
		if (!eaten.isEmpty()) {
			target.setInLove(null);
			IllusionBreeding.mark(target, a.owner());
			sl.broadcastEntityEvent(target, (byte) 18); // love hearts, as when a player feeds it
		}
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
		target = null;
		return true;
	}

	/** The nearest animal of a kind that has at least two who can breed, and that eats the clone's food. */
	private Animal pick(WorkGoal g, ServerLevel sl, Illusion a, Predicate<ItemStack> food) {
		ItemStack sample = a.lockedItem();
		// the pool holds animals already in love too: they are the partner of the next one to feed
		List<Animal> all = sl.getEntitiesOfClass(Animal.class, a.workBox(), m -> (m instanceof Cow || m instanceof Sheep || m instanceof Pig)
			&& !m.isBaby() && (m.canFallInLove() || m.isInLove()) && m.isFood(sample) && g.inArea(m.getX(), m.getZ()) && !g.isSkipped(m.getUUID()));
		return all.stream()
			.filter(Animal::canFallInLove)
			.filter(m -> all.stream().anyMatch(o -> o != m && o.getType() == m.getType()))
			.min(Comparator.comparingDouble(m -> m.distanceToSqr(a))).orElse(null);
	}
}
