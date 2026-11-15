package io.github.huydangtruong.permahearts.technique.illusion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** One job of a working Illusion clone (D73). The {@link WorkGoal} calls {@link #tick} every tick while the clone is not fighting. */
interface WorkTask {
	/** Works one tick. Returns false when the work area has no work for this job (the clone then idles and may go to the chest). */
	boolean tick(WorkGoal g, ServerLevel sl);

	static WorkTask create(IllusionJob job) {
		return switch (job) {
			case FARM -> new FarmTask();
			case FISH -> new FishTask();
			case CHOP -> new ChopTask();
			case SHEAR -> new ShearTask();
			case MILK -> new MilkTask();
			case BREED -> new BreedTask();
			case FIGHT -> (g, sl) -> false;
		};
	}

	/** The cells of the work area from {@code below} blocks under to {@code above} blocks over the cast point (mutable: copy before keeping). */
	static Iterable<BlockPos> cells(WorkGoal g, ServerLevel sl, int below, int above) {
		BlockPos c = g.illusion().workCenter();
		int r = (int) Math.ceil(g.illusion().workRadius());
		return () -> BlockPos.betweenClosedStream(c.offset(-r, -below, -r), c.offset(r, above, r))
			.filter(p -> g.inArea(p.getX() + 0.5, p.getZ() + 0.5) && sl.hasChunkAt(p)).iterator();
	}
}
