package io.github.huydangtruong.permahearts.technique.illusion;

import io.github.huydangtruong.permahearts.technique.Illusion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * FARM: harvests ripe crops, replants at once, then tills dirt or grass next to farmland (near water) for the seeds it carries.
 * The crops are broken like a player would (normal drops); the work counts as the clone's, so no XP and no tool wear.
 */
final class FarmTask implements WorkTask {
	private static final double REACH = 4.0;
	private BlockPos target;
	private boolean tilling;
	private boolean had;

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		if (target != null && (g.isSkipped(target) || !(tilling ? canTill(sl, target, a) : ripe(sl.getBlockState(target))))) {
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
		if (tilling) {
			till(g, sl, a, target);
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
		BlockPos bestTill = null;
		double bt = Double.MAX_VALUE;
		for (BlockPos p : WorkTask.cells(g, sl, 3, 3)) {
			double d = p.distSqr(a.blockPosition());
			if (d < bd && ripe(sl.getBlockState(p)) && !g.isSkipped(p)) {
				bd = d;
				best = p.immutable();
			} else if (seeds && d < bt && canTill(sl, p, a) && !g.isSkipped(p)) {
				bt = d;
				bestTill = p.immutable();
			}
		}
		tilling = best == null;
		target = best != null ? best : bestTill;
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

	private static boolean canTill(ServerLevel sl, BlockPos p, Illusion a) {
		BlockState st = sl.getBlockState(p);
		if ((st.getBlock() != Blocks.DIRT && st.getBlock() != Blocks.GRASS_BLOCK) || !sl.getBlockState(p.above()).isAir()) {
			return false;
		}
		boolean next = false;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (sl.getBlockState(p.relative(d)).getBlock() == Blocks.FARMLAND) {
				next = true;
				break;
			}
		}
		return next && nearWater(sl, p);
	}

	/** Within 4 blocks of water, like vanilla hydration (same level or one up). */
	private static boolean nearWater(ServerLevel sl, BlockPos p) {
		for (BlockPos q : BlockPos.betweenClosed(p.offset(-4, 0, -4), p.offset(4, 1, 4))) {
			if (sl.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	private void till(WorkGoal g, ServerLevel sl, Illusion a, BlockPos pos) {
		ItemStack seed = a.carry().take(s -> cropOf(s) != null, 1);
		if (seed.isEmpty()) {
			return;
		}
		sl.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
		sl.setBlock(pos.above(), cropOf(seed).defaultBlockState(), 3);
		sl.playSound(null, pos, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 1f, 1f);
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
	}
}
