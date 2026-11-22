package io.github.huydangtruong.immortalcultivation.technique.illusion;

import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** What a working clone carries (D73): at most {@link TechniqueRules#ILLUSION_CARRY_STACKS} stacks, and the chest moves in and out. */
public final class IllusionCarry {
	private final List<ItemStack> stacks = new ArrayList<>();

	/** Adds the stack, merging into carried stacks first. Returns what did not fit (empty when all fit). */
	public ItemStack add(ItemStack in) {
		ItemStack left = in.copy();
		for (ItemStack s : stacks) {
			if (left.isEmpty()) {
				return left;
			}
			if (ItemStack.isSameItemSameComponents(s, left)) {
				int move = Math.min(left.getCount(), s.getMaxStackSize() - s.getCount());
				s.grow(move);
				left.shrink(move);
			}
		}
		while (!left.isEmpty() && stacks.size() < TechniqueRules.ILLUSION_CARRY_STACKS) {
			int n = Math.min(left.getCount(), left.getMaxStackSize());
			stacks.add(left.split(n));
		}
		return left;
	}

	/** Count of carried items matching the test. */
	public int count(Predicate<ItemStack> test) {
		int n = 0;
		for (ItemStack s : stacks) {
			if (test.test(s)) {
				n += s.getCount();
			}
		}
		return n;
	}

	/** Takes up to {@code max} items matching the test; returns them as one stack (empty when none). */
	public ItemStack take(Predicate<ItemStack> test, int max) {
		ItemStack out = ItemStack.EMPTY;
		for (var it = stacks.iterator(); it.hasNext() && max > 0;) {
			ItemStack s = it.next();
			if (!test.test(s) || (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, s))) {
				continue;
			}
			int n = Math.min(max, s.getCount());
			if (out.isEmpty()) {
				out = s.copyWithCount(n);
			} else {
				out.grow(n);
			}
			s.shrink(n);
			max -= n;
			if (s.isEmpty()) {
				it.remove();
			}
		}
		return out;
	}

	public boolean isEmpty() {
		return stacks.isEmpty();
	}

	/** True when every slot is used (more items of an already carried kind may still fit, but a new kind would not). */
	public boolean isFull() {
		return stacks.size() >= TechniqueRules.ILLUSION_CARRY_STACKS;
	}

	/** Empties the carry and returns everything it held. */
	public List<ItemStack> drain() {
		List<ItemStack> out = new ArrayList<>(stacks);
		stacks.clear();
		return out;
	}

	/** Moves as much as fits into the container; what does not fit stays carried. */
	public void storeIn(Container chest) {
		for (var it = stacks.iterator(); it.hasNext();) {
			ItemStack left = insert(chest, it.next());
			if (left.isEmpty()) {
				it.remove();
			}
		}
	}

	/** True when the container has room for at least one item of what is carried (same rules as {@link #insert}, D208). */
	public boolean fitsIn(Container c) {
		for (ItemStack s : stacks) {
			for (int i = 0; i < c.getContainerSize(); i++) {
				ItemStack there = c.getItem(i);
				if (!c.canPlaceItem(i, s)) {
					continue;
				}
				if (there.isEmpty() || ItemStack.isSameItemSameComponents(there, s)
					&& there.getCount() < Math.min(c.getMaxStackSize(), there.getMaxStackSize())) {
					return true;
				}
			}
		}
		return false;
	}

	/** Inserts into the container (merging first, then empty slots); returns what did not fit. The input stack is changed. */
	public static ItemStack insert(Container c, ItemStack s) {
		for (int pass = 0; pass < 2 && !s.isEmpty(); pass++) {
			for (int i = 0; i < c.getContainerSize() && !s.isEmpty(); i++) {
				ItemStack there = c.getItem(i);
				if (!c.canPlaceItem(i, s)) {
					continue;
				}
				if (pass == 0 && !there.isEmpty() && ItemStack.isSameItemSameComponents(there, s)) {
					int move = Math.min(s.getCount(), Math.min(c.getMaxStackSize(), there.getMaxStackSize()) - there.getCount());
					if (move > 0) {
						there.grow(move);
						s.shrink(move);
					}
				} else if (pass == 1 && there.isEmpty()) {
					int move = Math.min(s.getCount(), Math.min(c.getMaxStackSize(), s.getMaxStackSize()));
					c.setItem(i, s.split(move));
				}
			}
		}
		c.setChanged();
		return s;
	}

	/** Takes up to {@code max} items matching the test out of the container. */
	public static ItemStack takeFrom(Container c, Predicate<ItemStack> test, int max) {
		ItemStack out = ItemStack.EMPTY;
		for (int i = 0; i < c.getContainerSize() && max > 0; i++) {
			ItemStack s = c.getItem(i);
			if (s.isEmpty() || !test.test(s) || (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, s))) {
				continue;
			}
			int n = Math.min(max, s.getCount());
			if (out.isEmpty()) {
				out = s.copyWithCount(n);
			} else {
				out.grow(n);
			}
			s.shrink(n);
			max -= n;
			if (s.isEmpty()) {
				c.setItem(i, ItemStack.EMPTY);
			}
		}
		c.setChanged();
		return out;
	}

	/** Count of matching items in the container. */
	public static int countIn(Container c, Predicate<ItemStack> test) {
		int n = 0;
		for (int i = 0; i < c.getContainerSize(); i++) {
			ItemStack s = c.getItem(i);
			if (!s.isEmpty() && test.test(s)) {
				n += s.getCount();
			}
		}
		return n;
	}
}
