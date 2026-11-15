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
	private static final double REACH = 6.0;

	private List<BlockPos> tree = new ArrayList<>();
	private final Set<BlockPos> notNatural = new HashSet<>();
	/** Where a trunk stood and the sapling item that belongs there. */
	private final List<Stump> stumps = new ArrayList<>();
	private boolean had;

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
		if (!g.goTo(log, at, REACH) || !g.ready()) {
			return true;
		}
		a.getLookControl().setLookAt(at.x, at.y, at.z);
		BlockState st = sl.getBlockState(log);
		for (ItemStack d : Block.getDrops(st, sl, log, null, a, a.lockedItem())) {
			ItemStack left = a.carry().add(d);
			if (!left.isEmpty()) {
				Block.popResource(sl, log, left);
			}
		}
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
