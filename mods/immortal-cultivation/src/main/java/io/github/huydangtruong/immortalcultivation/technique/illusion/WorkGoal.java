package io.github.huydangtruong.immortalcultivation.technique.illusion;

import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The work of an Illusion clone (D73). It sits below the fight goal: a clone with a target fights, then comes back here.
 * This goal holds what every job shares (walking, the pickup of drops, the chest and delivery) and asks the job's
 * {@link WorkTask} to do the actual work each tick.
 */
public final class WorkGoal extends Goal {
	/** A clone starts delivering this many ticks before its time ends (3 s). */
	private static final int DELIVER_BEFORE_END = 60;
	/** Ticks of "no work in the area" before a clone takes what it carries to the chest. */
	private static final int IDLE_BEFORE_DELIVERY = 30;

	private final Illusion a;
	private WorkTask task;
	private int cooldown;
	private boolean delivering;
	private long deliverSince;
	private long pickupUntil;
	private Vec3 pickupAt; // where the last action was: its drops can land out of the clone's reach
	private BlockPos chestPos;
	private long nextChestScan;
	private int idle;
	private int nextScan;
	private Object goKey;
	private int goSince;
	private double goBest;
	private int goBestAt;
	/** Ticks without getting at least half a block closer before a target counts as unreachable. */
	private static final int NO_PROGRESS = 60;
	private final Map<Object, Integer> skipped = new HashMap<>();
	private final List<int[]> areas = new java.util.ArrayList<>();

	public WorkGoal(Illusion a) {
		this.a = a;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return a.job().works() && a.getTarget() == null && a.ownerPlayer() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		a.setSprinting(false);
		if (task == null) {
			task = WorkTask.create(a.job());
		}
	}

	@Override
	public void stop() {
		a.getNavigation().stop();
		goKey = null;
	}

	// ---- helpers for the tasks ----

	public Illusion illusion() {
		return a;
	}

	/** True when the pause between two actions is over. */
	public boolean ready() {
		return cooldown <= 0;
	}

	/** Starts the pause after an action; {@code extra} is the block's own break time on top. */
	public void acted(int extra) {
		cooldown = a.workTicks() + Math.max(0, extra);
	}

	/** A short pause (for example after breaking a leaf in the way). */
	public void pause(int ticks) {
		cooldown = Math.max(cooldown, ticks);
	}

	public void unskip(Object key) {
		skipped.remove(key);
		goKey = null;
	}

	/** Lets the clone pick up item drops within 3 blocks for this many more ticks. */
	public void pickupFor(int ticks) {
		pickupUntil = Math.max(pickupUntil, a.level().getGameTime() + ticks);
	}

	/** Same, and also picks up drops within 3 blocks of {@code at} (the clone acts from up to 3.5 blocks away). */
	public void pickupFor(int ticks, Vec3 at) {
		pickupFor(ticks);
		pickupAt = at;
	}

	/** True about every 10 ticks: tasks search the area only then. */
	public boolean scanReady() {
		if (a.tickCount < nextScan) {
			return false;
		}
		nextScan = a.tickCount + 10 + a.getRandom().nextInt(5);
		return true;
	}

	/** True when the point is inside the work circle. */
	public boolean inArea(double x, double z) {
		BlockPos c = a.workCenter();
		double dx = x - (c.getX() + 0.5), dz = z - (c.getZ() + 0.5);
		double r = a.workRadius();
		return dx * dx + dz * dz <= r * r;
	}

	public void skip(Object key, int ticks) {
		skipped.put(key, a.tickCount + ticks);
		if (skipped.size() > 200) {
			skipped.values().removeIf(t -> t < a.tickCount);
		}
	}

	public boolean isSkipped(Object key) {
		Integer until = skipped.get(key);
		if (until != null && until > a.tickCount) {
			return true;
		}
		if (key instanceof BlockPos p) {
			for (int i = areas.size() - 1; i >= 0; i--) {
				int[] ar = areas.get(i); // x, y, z, radius, until
				if (ar[4] <= a.tickCount) {
					areas.remove(i);
				} else if (Math.abs(p.getX() - ar[0]) <= ar[3] && Math.abs(p.getY() - ar[1]) <= ar[3] && Math.abs(p.getZ() - ar[2]) <= ar[3]) {
					return true;
				}
			}
		}
		return false;
	}

	/** Skips every block position within {@code radius} of {@code p} for a while (a whole unreachable field, not one crop at a time). */
	public void skipAround(BlockPos p, int radius, int ticks) {
		areas.add(new int[] {p.getX(), p.getY(), p.getZ(), radius, a.tickCount + ticks});
	}

	/**
	 * Walks towards the point. Returns true once the clone is within {@code reach} blocks (and stops). A key that cannot be reached
	 * (no path, or no progress for 15 s) is skipped for a while, so the task picks another target.
	 */
	public boolean goTo(Object key, Vec3 p, double reach) {
		return goTo(key, p, reach, false);
	}

	/** With {@code strict}, a clone whose path has ended without getting closer for 3 s gives the key up (for targets on the ground, like crops). */
	public boolean goTo(Object key, Vec3 p, double reach, boolean strict) {
		if (a.position().distanceToSqr(p) <= reach * reach) {
			a.getNavigation().stop();
			return true;
		}
		if (!key.equals(goKey)) {
			goKey = key;
			goSince = a.tickCount;
			goBest = Double.MAX_VALUE;
			goBestAt = a.tickCount;
		} else if (a.tickCount - goSince > 300 || (strict && a.tickCount - goBestAt > NO_PROGRESS && a.getNavigation().isDone())) {
			skip(key, 600);
			goKey = null;
			return false;
		}
		double dist = a.position().distanceTo(p);
		if (dist < goBest - 0.5) { // a partial path ends short of the target and the clone then stands still: no progress for 3 s skips it
			goBest = dist;
			goBestAt = a.tickCount;
		}
		if (!a.onGround() && !a.isInWater()) {
			return false; // no path can start in the air, and onGround is still false on the first ticks after the spawn: wait, never skip the target for it
		}
		if (a.getNavigation().isDone() || a.tickCount % 10 == 0) {
			if (!a.getNavigation().moveTo(p.x, p.y, p.z, 1.0) && !moveBeside(p)) {
				skip(key, 200);
			}
		}
		return false;
	}

	/** A spot that is itself no path target (a crop, a log, a mob in a pen) is approached from one of the four sides. */
	private boolean moveBeside(Vec3 p) {
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			if (a.getNavigation().moveTo(p.x + d.getStepX(), p.y, p.z + d.getStepZ(), 1.0)) {
				return true;
			}
		}
		return false;
	}

	// ---- the chest ----

	/** The container of the nearest chest or barrel near the work area, or null. */
	public Container chest(ServerLevel sl) {
		if (chestPos != null) {
			Container c = containerAt(sl, chestPos);
			if (c != null) {
				return c;
			}
			chestPos = null;
		}
		long now = sl.getGameTime();
		if (now < nextChestScan) {
			return null;
		}
		nextChestScan = now + 100;
		BlockPos c = a.workCenter();
		int r = (int) Math.ceil(a.workRadius() + io.github.huydangtruong.immortalcultivation.technique.TechniqueRules.ILLUSION_CHEST_EXTRA);
		double best = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -8, -r), c.offset(r, 8, r))) {
			double d = p.distSqr(c);
			if (d >= best || !sl.hasChunkAt(p)) {
				continue;
			}
			if (containerAt(sl, p) != null) {
				best = d;
				chestPos = p.immutable();
			}
		}
		return chestPos == null ? null : containerAt(sl, chestPos);
	}

	/** Position of the cached chest (valid after {@link #chest}). */
	public BlockPos chestPos() {
		return chestPos;
	}

	private static Container containerAt(ServerLevel sl, BlockPos p) {
		BlockState st = sl.getBlockState(p);
		if (st.getBlock() instanceof ChestBlock cb) {
			return ChestBlock.getContainer(cb, st, sl, p, true);
		}
		if (st.getBlock() instanceof BarrelBlock && sl.getBlockEntity(p) instanceof BarrelBlockEntity be) {
			return be;
		}
		return null;
	}

	// ---- the loop ----

	@Override
	public void tick() {
		if (!(a.level() instanceof ServerLevel sl)) {
			return;
		}
		long now = sl.getGameTime();
		if (task == null) {
			task = WorkTask.create(a.job());
		}
		pickUp(sl, now);
		if (delivering) {
			deliver(sl, now);
			return;
		}
		if (!a.carry().isEmpty() && (a.carry().isFull() || a.endTick() - now <= DELIVER_BEFORE_END)) {
			beginDelivery(now);
			return;
		}
		cooldown--;
		if (task.tick(this, sl)) {
			idle = 0;
		} else if (++idle == IDLE_BEFORE_DELIVERY && !a.carry().isEmpty() && a.job() != IllusionJob.BREED) {
			beginDelivery(now);
		}
	}

	private void beginDelivery(long now) {
		delivering = true;
		deliverSince = now;
		goKey = null;
	}

	private void deliver(ServerLevel sl, long now) {
		Container ch = chest(sl);
		if (ch == null) { // no chest: drop it, never delete it
			a.dropCarry();
			delivering = false;
			return;
		}
		Vec3 at = Vec3.atCenterOf(chestPos);
		if (a.position().distanceToSqr(at) <= 3.5 * 3.5) {
			a.getNavigation().stop();
			a.carry().storeIn(ch);
			sl.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f, 1.2f);
			if (a.carry().isFull()) { // the chest is full: do not keep a stuck carry
				a.dropCarry();
			}
			delivering = false;
			return;
		}
		if (now - deliverSince > 400) {
			a.dropCarry();
			delivering = false;
			return;
		}
		if (a.getNavigation().isDone() || a.tickCount % 10 == 0) {
			a.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
		}
	}

	/** Picks up item drops near the clone while the pickup window is open; never what the owner dropped. */
	private void pickUp(ServerLevel sl, long now) {
		if (now >= pickupUntil) {
			pickupAt = null;
			return;
		}
		if (now % 3 != 0) {
			return;
		}
		net.minecraft.world.phys.AABB box = a.getBoundingBox().inflate(3.0);
		if (pickupAt != null) {
			box = box.minmax(new net.minecraft.world.phys.AABB(pickupAt, pickupAt).inflate(3.0));
		}
		List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, box,
			e -> e.isAlive() && !e.hasPickUpDelay() && !(e.getOwner() != null && e.getOwner().getUUID().equals(a.owner())));
		for (ItemEntity e : items) {
			ItemStack left = a.carry().add(e.getItem());
			if (left.isEmpty()) {
				e.discard();
			} else {
				e.setItem(left);
			}
			sl.playSound(null, e.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2f, 1.5f);
		}
	}
}
