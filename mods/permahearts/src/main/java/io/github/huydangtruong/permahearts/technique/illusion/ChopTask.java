package io.github.huydangtruong.permahearts.technique.illusion;

import io.github.huydangtruong.permahearts.technique.Illusion;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * CHOP: finds a natural tree (logs touching leaves that are not player-placed), breaks its logs from the bottom up
 * (up to 64) and plants a sapling of the same wood where the trunk stood. Logs with no natural leaves are never touched.
 */
final class ChopTask implements WorkTask {
	private static final int MAX_LOGS = 64;
	/** Reach from the clone's eyes to the centre of a log. */
	private static final double REACH = 5.5;
	/** Ticks the clone may walk towards one log before it gives up on it (5 s). */
	private static final int WALK_LIMIT = 100;

	private List<BlockPos> tree = new ArrayList<>();
	private final Set<BlockPos> notNatural = new HashSet<>();
	/** Where a trunk stood and the sapling item that belongs there. */
	private final List<Stump> stumps = new ArrayList<>();
	private boolean had;
	/** The lowest log of the tree being felled: the clone walks to its foot to reach the logs above. */
	private BlockPos foot;
	private int walking;

	private record Stump(BlockPos pos, Item sapling) {}

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		tree.removeIf(p -> !sl.getBlockState(p).is(BlockTags.LOGS) || g.isSkipped(p));
		if (tree.isEmpty()) {
			if (replant(g, sl, a)) {
				return true;
			}
			if (!g.scanReady()) {
				return had;
			}
			findTree(g, sl, a);
			had = !tree.isEmpty();
			return had;
		}
		BlockPos log = tree.get(0);
		Vec3 at = Vec3.atCenterOf(log);
		if (a.getEyePosition().distanceToSqr(at) > REACH * REACH) {
			// Out of reach: stand at the foot of the trunk (a log high in a tree is no place to path to), and give up on a
			// log after a few seconds or as soon as there is no way to it, instead of waiting 15 s.
			BlockPos to = foot != null ? foot : log;
			boolean there = g.goTo(to, Vec3.atCenterOf(to), 2.0);
			boolean stuck = g.isSkipped(to) || ++walking > WALK_LIMIT;
			if (stuck || there) {
				BlockPos leaf = stuck && !there ? leafAround(sl, a, to) : null;
				if (leaf != null) { // natural leaves may be what shuts the trunk in: clear the one in the way, then walk again
					if (g.ready()) {
						breakNatural(g, sl, a, leaf);
						g.unskip(to);
						walking = 0;
					}
					return true;
				}
				g.skip(log, 400);
				walking = 0;
			}
			return true;
		}
		walking = 0;
		a.getNavigation().stop();
		if (!g.ready()) {
			return true;
		}
		BlockPos blocking = leafInTheWay(sl, a, at, log);
		if (blocking != null) { // only the leaves between the clone's eyes and this log, never the whole canopy
			a.getLookControl().setLookAt(blocking.getX() + 0.5, blocking.getY() + 0.5, blocking.getZ() + 0.5);
			breakNatural(g, sl, a, blocking);
			return true;
		}
		a.getLookControl().setLookAt(at.x, at.y, at.z);
		BlockState st = sl.getBlockState(log);
		collectDrops(sl, a, st, log);
		Item sapling = saplingFor(st.getBlock());
		boolean base = sl.getBlockState(log.below()).is(BlockTags.DIRT);
		sl.destroyBlock(log, false, a, 512);
		tree.remove(0);
		if (base && sapling != null) {
			stumps.add(new Stump(log, sapling));
		}
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted((int) (st.getDestroySpeed(sl, log) * 5)); // a block still takes its break time
		g.pickupFor(100);
		return true;
	}

	private static void collectDrops(ServerLevel sl, Illusion a, BlockState st, BlockPos pos) {
		for (ItemStack d : Block.getDrops(st, sl, pos, null, a, a.lockedItem())) {
			ItemStack left = a.carry().add(d);
			if (!left.isEmpty()) {
				Block.popResource(sl, pos, left);
			}
		}
	}

	private static boolean naturalLeaves(BlockState n) {
		return n.is(BlockTags.LEAVES) && n.hasProperty(LeavesBlock.PERSISTENT) && !n.getValue(LeavesBlock.PERSISTENT);
	}

	/** Breaks a natural leaf block: it drops its usual things (saplings, sticks, apples) into the carry, and costs a short pause. */
	private static void breakNatural(WorkGoal g, ServerLevel sl, Illusion a, BlockPos leaf) {
		collectDrops(sl, a, sl.getBlockState(leaf), leaf);
		sl.destroyBlock(leaf, false, a, 512);
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.pause(Math.max(4, a.workTicks() / 2));
		g.pickupFor(60);
	}

	/** The natural leaf block the clone's line of sight to the log runs into first, or null when the way is clear. */
	private static BlockPos leafInTheWay(ServerLevel sl, Illusion a, Vec3 at, BlockPos log) {
		var hit = sl.clip(new ClipContext(a.getEyePosition(), at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, a));
		if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(log)) {
			return null;
		}
		return naturalLeaves(sl.getBlockState(hit.getBlockPos())) ? hit.getBlockPos().immutable() : null;
	}

	/** A natural leaf within 2 blocks around the trunk foot (low enough to stand beside it) that the clone can reach, nearest first. */
	private static BlockPos leafAround(ServerLevel sl, Illusion a, BlockPos foot) {
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		Vec3 eye = a.getEyePosition();
		for (BlockPos p : BlockPos.betweenClosed(foot.offset(-2, 0, -2), foot.offset(2, 2, 2))) {
			if (!naturalLeaves(sl.getBlockState(p))) {
				continue;
			}
			double d = eye.distanceToSqr(Vec3.atCenterOf(p));
			if (d <= REACH * REACH && d < bestD) {
				bestD = d;
				best = p.immutable();
			}
		}
		return best;
	}

	/** Plants a carried sapling on a stump when the clone has one. Returns true while it is busy with that. */
	private boolean replant(WorkGoal g, ServerLevel sl, Illusion a) {
		for (var it = stumps.iterator(); it.hasNext();) {
			Stump s = it.next();
			BlockState here = sl.getBlockState(s.pos());
			if (!here.isAir() || !sl.getBlockState(s.pos().below()).is(BlockTags.DIRT)) {
				it.remove();
				continue;
			}
			if (a.carry().count(i -> i.is(s.sapling())) == 0) {
				continue;
			}
			g.pickupFor(40);
			if (!g.goTo(s.pos(), Vec3.atCenterOf(s.pos()), 2.6)) {
				return true;
			}
			if (g.ready()) {
				BlockState sap = ((BlockItem) s.sapling()).getBlock().defaultBlockState();
				a.carry().take(i -> i.is(s.sapling()), 1);
				if (sap.canSurvive(sl, s.pos())) {
					sl.setBlock(s.pos(), sap, 3);
				}
				a.swingForAttack(InteractionHand.MAIN_HAND);
				g.acted(0);
				it.remove();
			}
			return true;
		}
		if (!stumps.isEmpty()) { // waiting for a leaf to drop a sapling
			g.pickupFor(40);
		}
		return false;
	}

	private void findTree(WorkGoal g, ServerLevel sl, Illusion a) {
		List<BlockPos> logs = new ArrayList<>();
		for (BlockPos p : WorkTask.cells(g, sl, 3, 12)) {
			if (sl.getBlockState(p).is(BlockTags.LOGS) && !notNatural.contains(p) && !g.isSkipped(p)) {
				logs.add(p.immutable());
			}
		}
		BlockPos from = a.blockPosition();
		logs.sort(Comparator.comparingDouble(p -> p.distSqr(from)));
		for (int i = 0; i < logs.size() && i < 8; i++) {
			BlockPos start = logs.get(i);
			if (notNatural.contains(start)) {
				continue;
			}
			List<BlockPos> found = flood(sl, start);
			if (natural(sl, found)) {
				found.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(p -> p.distSqr(from)));
				tree = found;
				foot = found.get(0);
				walking = 0;
				return;
			}
			notNatural.addAll(found);
		}
	}

	/** The logs connected to the start (up to 64), through any neighbour including diagonals. */
	private static List<BlockPos> flood(ServerLevel sl, BlockPos start) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> q = new ArrayDeque<>();
		q.add(start);
		seen.add(start);
		while (!q.isEmpty() && out.size() < MAX_LOGS) {
			BlockPos p = q.poll();
			out.add(p);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = p.offset(dx, dy, dz);
						if (seen.add(n) && sl.hasChunkAt(n) && sl.getBlockState(n).is(BlockTags.LOGS)) {
							q.add(n);
						}
					}
				}
			}
		}
		return out;
	}

	/** A tree is natural when some log touches leaves that decay by themselves (not player-placed). */
	private static boolean natural(ServerLevel sl, List<BlockPos> logs) {
		for (BlockPos p : logs) {
			for (Direction d : Direction.values()) {
				BlockState n = sl.getBlockState(p.relative(d));
				if (n.is(BlockTags.LEAVES) && n.hasProperty(LeavesBlock.PERSISTENT) && !n.getValue(LeavesBlock.PERSISTENT)) {
					return true;
				}
			}
		}
		return false;
	}

	/** The sapling item of a log's wood ("minecraft:oak_log" gives oak_sapling), or null when there is none. */
	static Item saplingFor(Block log) {
		String path = BuiltInRegistries.BLOCK.getKey(log).getPath();
		if (path.startsWith("stripped_")) {
			path = path.substring(9);
		}
		String wood = path.endsWith("_log") ? path.substring(0, path.length() - 4) : path.endsWith("_wood") ? path.substring(0, path.length() - 5) : null;
		if (wood == null) {
			return null;
		}
		String id = wood.equals("mangrove") ? "mangrove_propagule" : wood + "_sapling";
		return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(id)).filter(i -> i instanceof BlockItem).orElse(null);
	}
}
