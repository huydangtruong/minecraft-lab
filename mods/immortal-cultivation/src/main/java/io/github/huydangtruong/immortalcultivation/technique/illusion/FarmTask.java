package io.github.huydangtruong.immortalcultivation.technique.illusion;

import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * FARM: harvests ripe crops, replants at once, then plants the seeds it carries (or takes from a chest nearby, D120) on farmland that already exists and is empty.
 * It never makes new farmland (D84): no planting of dirt or grass. When all farmland is planted and nothing is ripe it has no work,
 * so the WorkGoal delivers what it carries (chest, or dropped on the ground). The crops are broken like a player would (normal drops); the work counts as the clone's, so no XP and no tool wear.
 */
final class FarmTask implements WorkTask {
	private static final double REACH = 4.0;
	private static final double CHEST_REACH = 3.0;
	private BlockPos target;
	private Mode mode = Mode.HARVEST;
	private boolean had;

	/** What the target is: a ripe crop, empty farmland, or the chest that holds seeds (D120). */
	private enum Mode { HARVEST, PLANT, FETCH }

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		if (target != null && (g.isSkipped(target) || !stillValid(g, sl, a))) {
			if (g.isSkipped(target) && mode != Mode.FETCH) { // an unreachable crop: its neighbours (the same field) are not worth a try each
				g.skipAround(target, 4, 600);
			}
			target = null;
		}
		if (target == null) {
			if (!g.scanReady()) {
				return had;
			}
			search(g, sl, a);
			had = target != null;
			if (target == null) {
				return false;
			}
		}
		Vec3 at = mode == Mode.FETCH ? Vec3.atCenterOf(target) : Vec3.atBottomCenterOf(target);
		if (!g.goTo(target, at, mode == Mode.FETCH ? CHEST_REACH : REACH, true) || !g.ready()) {
			return true;
		}
		a.getLookControl().setLookAt(at.x, at.y, at.z);
		switch (mode) {
			case PLANT -> plant(g, sl, a, target);
			case FETCH -> fetchSeeds(g, sl, a);
			default -> harvest(g, sl, a, target);
		}
		target = null;
		return true;
	}

	private void search(WorkGoal g, ServerLevel sl, Illusion a) {
		BlockPos best = null;
		double bd = Double.MAX_VALUE;
		boolean seeds = hasSeeds(a);
		BlockPos bestPlant = null;
		double bt = Double.MAX_VALUE;
		boolean emptySoil = false;
		for (BlockPos p : WorkTask.cells(g, sl, 3, 6)) {
			double d = p.distSqr(a.blockPosition());
			if (d < bd && ripe(sl.getBlockState(p)) && !g.isSkipped(p)) {
				bd = d;
				best = p.immutable();
			} else if (d < bt && canPlant(sl, p) && !g.isSkipped(p)) {
				emptySoil = true;
				if (seeds) {
					bt = d;
					bestPlant = p.immutable();
				}
			}
		}
		mode = best != null ? Mode.HARVEST : Mode.PLANT;
		target = best != null ? best : bestPlant;
		if (target == null && emptySoil && !seeds) { // D120: empty farmland and no seeds in hand: fetch some from a chest nearby
			Container ch = g.chest(sl);
			BlockPos cp = g.chestPos();
			if (ch != null && cp != null && !g.isSkipped(cp) && IllusionCarry.countIn(ch, FarmTask::isSeed) > 0) {
				mode = Mode.FETCH;
				target = cp.immutable();
			}
		}
	}

	private boolean stillValid(WorkGoal g, ServerLevel sl, Illusion a) {
		return switch (mode) {
			case PLANT -> canPlant(sl, target);
			case FETCH -> {
				Container ch = g.chest(sl);
				yield !hasSeeds(a) && ch != null && target.equals(g.chestPos()) && IllusionCarry.countIn(ch, FarmTask::isSeed) > 0;
			}
			default -> ripe(sl.getBlockState(target));
		};
	}

	/** D120: takes up to a stack of one kind of seed out of the chest. */
	private void fetchSeeds(WorkGoal g, ServerLevel sl, Illusion a) {
		Container ch = g.chest(sl);
		if (ch != null) {
			ItemStack got = IllusionCarry.takeFrom(ch, FarmTask::isSeed, 64);
			if (!got.isEmpty()) {
				ItemStack left = a.carry().add(got);
				if (!left.isEmpty()) {
					IllusionCarry.insert(ch, left);
				}
				sl.playSound(null, target, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f, 1.2f);
			}
		}
		g.acted(0);
	}

	private static boolean isSeed(ItemStack s) {
		return cropOf(s) != null;
	}

	/** A crop of the five plants the clone knows in its last two growth stages (D123); nether wart only when fully grown. */
	static boolean ripe(BlockState st) {
		Block b = st.getBlock();
		if (b == Blocks.NETHER_WART) {
			return st.getValue(NetherWartBlock.AGE) >= 3;
		}
		return (b == Blocks.WHEAT || b == Blocks.CARROTS || b == Blocks.POTATOES || b == Blocks.BEETROOTS)
			&& b instanceof CropBlock c && c.getAge(st) >= c.getMaxAge() - 1;
	}

	private static Item seedOf(Block b) {
		if (b == Blocks.WHEAT) {
			return Items.WHEAT_SEEDS;
		}
		if (b == Blocks.CARROTS) {
			return Items.CARROT;
		}
		if (b == Blocks.POTATOES) {
			return Items.POTATO;
		}
		if (b == Blocks.BEETROOTS) {
			return Items.BEETROOT_SEEDS;
		}
		return b == Blocks.NETHER_WART ? Items.NETHER_WART : Items.AIR;
	}

	private static Block cropOf(ItemStack seed) {
		if (seed.is(Items.WHEAT_SEEDS)) {
			return Blocks.WHEAT;
		}
		if (seed.is(Items.CARROT)) {
			return Blocks.CARROTS;
		}
		if (seed.is(Items.POTATO)) {
			return Blocks.POTATOES;
		}
		if (seed.is(Items.MELON_SEEDS)) {
			return Blocks.MELON_STEM;
		}
		if (seed.is(Items.PUMPKIN_SEEDS)) {
			return Blocks.PUMPKIN_STEM;
		}
		return seed.is(Items.BEETROOT_SEEDS) ? Blocks.BEETROOTS : null;
	}

	private static boolean hasSeeds(Illusion a) {
		return a.carry().count(s -> cropOf(s) != null) > 0;
	}

	private void harvest(WorkGoal g, ServerLevel sl, Illusion a, BlockPos pos) {
		BlockState st = sl.getBlockState(pos);
		Block crop = st.getBlock();
		if (crop instanceof CropBlock cb && !cb.isMaxAge(st)) { // D123: one stage short: let it ripen, so the drops are the ripe ones
			st = cb.getStateForAge(cb.getMaxAge());
			sl.setBlock(pos, st, 2);
		}
		List<ItemStack> drops = Block.getDrops(st, sl, pos, null, a, a.lockedItem());
		Item seed = seedOf(crop);
		boolean replant = crop.defaultBlockState().canSurvive(sl, pos);
		boolean haveSeed = false;
		if (replant) { // one seed from the drops, else one the clone carries
			for (ItemStack d : drops) {
				if (d.is(seed) && !d.isEmpty()) {
					d.shrink(1);
					haveSeed = true;
					break;
				}
			}
			if (!haveSeed) {
				haveSeed = !a.carry().take(s -> s.is(seed), 1).isEmpty();
			}
		}
		sl.destroyBlock(pos, false, a, 512);
		if (haveSeed) {
			sl.setBlock(pos, crop.defaultBlockState(), 3);
		}
		for (ItemStack d : drops) {
			if (!d.isEmpty()) {
				ItemStack left = a.carry().add(d);
				if (!left.isEmpty()) {
					Block.popResource(sl, pos, left);
				}
			}
		}
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
		g.pickupFor(20);
	}

	/** Existing farmland with nothing on it (a harvest that dropped no seed leaves one). Dirt and grass are never turned into farmland. */
	private static boolean canPlant(ServerLevel sl, BlockPos p) {
		return sl.getBlockState(p).getBlock() == Blocks.FARMLAND && sl.getBlockState(p.above()).isAir();
	}

	private void plant(WorkGoal g, ServerLevel sl, Illusion a, BlockPos pos) {
		ItemStack seed = a.carry().take(s -> cropOf(s) != null, 1);
		if (seed.isEmpty()) {
			return;
		}
		sl.setBlock(pos.above(), cropOf(seed).defaultBlockState(), 3);
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
	}
}
