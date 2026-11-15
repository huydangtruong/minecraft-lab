package io.github.huydangtruong.permahearts.technique.illusion;

import io.github.huydangtruong.permahearts.technique.Illusion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * FISH: stands on the shore of open water (at least 2x2), casts (an arm swing, no hook entity), waits 5-30 s (Lure shortens
 * it, as in vanilla) and rolls the vanilla fishing loot table with the rod's Luck of the Sea.
 */
public final class FishTask implements WorkTask {
	/** Game tests set this to a short wait in ticks; negative means the normal random wait. */
	public static int testWaitTicks = -1;

	private BlockPos water;
	private BlockPos stand;
	private int wait = -1;
	private boolean had;

	@Override
	public boolean tick(WorkGoal g, ServerLevel sl) {
		Illusion a = g.illusion();
		if (water != null && (!isWater(sl, water) || g.isSkipped(water))) {
			water = null;
			stand = null;
			wait = -1;
		}
		if (water == null) {
			if (!g.scanReady()) {
				return had;
			}
			find(g, sl, a);
			had = water != null;
			return had;
		}
		Vec3 at = Vec3.atBottomCenterOf(stand);
		if (!g.goTo(water, at, 1.2)) {
			return true;
		}
		Vec3 w = Vec3.atCenterOf(water);
		a.getLookControl().setLookAt(w.x, w.y, w.z);
		if (wait < 0) {
			if (!g.ready()) {
				return true;
			}
			int lure = (int) (EnchantmentHelper.getFishingTimeReduction(sl, a.lockedItem(), a) * 20);
			wait = testWaitTicks >= 0 ? testWaitTicks : Math.max(20, 100 + a.getRandom().nextInt(501) - lure);
			a.swingForAttack(InteractionHand.MAIN_HAND);
			sl.playSound(null, a.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f / (a.getRandom().nextFloat() * 0.4f + 0.8f));
			return true;
		}
		if (--wait > 0) {
			if (wait % 20 == 0) {
				sl.sendParticles(ParticleTypes.BUBBLE, w.x, water.getY() + 0.9, w.z, 2, 0.2, 0, 0.2, 0.0);
			}
			return true;
		}
		wait = -1;
		sl.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5f, 1f);
		sl.sendParticles(ParticleTypes.SPLASH, w.x, water.getY() + 0.9, w.z, 12, 0.3, 0, 0.3, 0.1);
		LootParams params = new LootParams.Builder(sl)
			.withParameter(LootContextParams.ORIGIN, w)
			.withParameter(LootContextParams.TOOL, a.lockedItem())
			.withParameter(LootContextParams.THIS_ENTITY, a)
			.withLuck(EnchantmentHelper.getFishingLuckBonus(sl, a.lockedItem(), a))
			.create(LootContextParamSets.FISHING);
		LootTable table = sl.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
		for (ItemStack s : table.getRandomItems(params)) {
			ItemStack left = a.carry().add(s);
			if (!left.isEmpty()) {
				Block.popResource(sl, a.blockPosition(), left);
			}
		}
		a.swingForAttack(InteractionHand.MAIN_HAND);
		g.acted(0);
		return true;
	}

	private static boolean isWater(ServerLevel sl, BlockPos p) {
		return sl.getFluidState(p).is(FluidTags.WATER) && sl.getFluidState(p).isSource();
	}

	private static boolean open(ServerLevel sl, BlockPos p) {
		return isWater(sl, p) && sl.getBlockState(p.above()).isAir();
	}

	/** Open water of at least 2x2 inside the area, and a dry spot to stand on within 1.5-4 blocks of it. */
	private void find(WorkGoal g, ServerLevel sl, Illusion a) {
		BlockPos best = null;
		double bd = Double.MAX_VALUE;
		for (BlockPos p : WorkTask.cells(g, sl, 3, 3)) {
			double d = p.distSqr(a.blockPosition());
			if (d < bd && !g.isSkipped(p) && open(sl, p) && open(sl, p.east()) && open(sl, p.south()) && open(sl, p.east().south())) {
				BlockPos s = standFor(sl, p, a);
				if (s != null) {
					bd = d;
					best = p.immutable();
					stand = s;
				}
			}
		}
		water = best;
		if (best == null) {
			stand = null;
		}
	}

	private static BlockPos standFor(ServerLevel sl, BlockPos w, Illusion a) {
		BlockPos best = null;
		double bd = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(w.offset(-3, 0, -3), w.offset(3, 2, 3))) {
			double dw = p.distSqr(w.atY(p.getY()));
			if (dw < 2.0 || dw > 16.0 || !sl.getFluidState(p).isEmpty() || !sl.getBlockState(p).getCollisionShape(sl, p).isEmpty()
				|| !sl.getBlockState(p.above()).getCollisionShape(sl, p.above()).isEmpty()
				|| sl.getBlockState(p.below()).getCollisionShape(sl, p.below()).isEmpty() || !sl.getFluidState(p.below()).isEmpty()) {
				continue;
			}
			double d = p.distSqr(a.blockPosition());
			if (d < bd) {
				bd = d;
				best = p.immutable();
			}
		}
		return best;
	}
}
