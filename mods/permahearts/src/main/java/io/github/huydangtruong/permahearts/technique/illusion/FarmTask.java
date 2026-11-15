package io.github.huydangtruong.permahearts.technique.illusion;

import io.github.huydangtruong.permahearts.technique.Illusion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * FARM: harvests ripe crops, replants at once, then plants the seeds it carries on farmland that already exists and is empty.
 * It never makes new farmland (D84): no planting of dirt or grass. When all farmland is planted and nothing is ripe it has no work,
 * so the WorkGoal delivers what it carries (chest, or dropped on the ground). The crops are broken like a player would (normal drops); the work counts as the clone's, so no XP and no tool wear.
 */
final class FarmTask implements WorkTask {
	private static final double REACH = 4.0;
	private BlockPos target;
	private boolean planting;
	private boolean had;

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		if (target != null && (g.isSkipped(target) || !(planting ? canPlant(sl, target) : ripe(sl.getBlockState(target))))) {
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
		Vec3 at = Vec3.atBottomCenterOf(target);
		if (!g.goTo(target, at, REACH) || !g.ready()) {
			return true;
		}
		a.getLookControl().setLookAt(at.x, at.y, at.z);
		if (planting) {
			plant(g, sl, a, target);
		} else {
			harvest(g, sl, a, target);
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
		for (BlockPos p : WorkTask.cells(g, sl, 3, 3)) {
			double d = p.distSqr(a.blockPosition());
			if (d < bd && ripe(sl.getBlockState(p)) && !g.isSkipped(p)) {
				bd = d;
				best = p.immutable();
			} else if (seeds && d < bt && canPlant(sl, p) && !g.isSkipped(p)) {
				bt = d;
				bestPlant = p.immutable();
			}
		}
		planting = best == null;
		target = best != null ? best : bestPlant;
	}

	/** A fully grown crop of the five plants the clone knows. */
	static boolean ripe(BlockState st) {
		Block b = st.getBlock();
		if (b == Blocks.NETHER_WART) {
			return st.getValue(NetherWartBlock.AGE) >= 3;
		}
		return (b == Blocks.WHEAT || b == Blocks.CARROTS || b == Blocks.POTATOES || b == Blocks.BEETROOTS)
			&& b instanceof CropBlock c && c.isMaxAge(st);
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
		return seed.is(Items.BEETROOT_SEEDS) ? Blocks.BEETROOTS : null;
	}

	private static boolean hasSeeds(Illusion a) {
		return a.carry().count(s -> cropOf(s) != null) > 0;
	}

	private void harvest(WorkGoal g, ServerLevel sl, Illusion a, BlockPos pos) {
		BlockState st = sl.getBlockState(pos);
		Block crop = st.getBlock();
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
