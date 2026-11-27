package io.github.huydangtruong.immortalcultivation.technique;

/** D114 technique groups of the wheel, in list order (moved from the wheel screen so a test can check them). */
public final class WheelGroups {
	private WheelGroups() {}

	public record Group(String label, Technique[] members) {}

	public static final Group[] GROUPS = {
		new Group("Attack", new Technique[] {Technique.FIRE, Technique.THUNDER, Technique.SEISM, Technique.LANCE, Technique.BLADE, Technique.MAYHEM}),
		new Group("Defense", new Technique[] {Technique.ICE, Technique.REPULSE, Technique.HEALING, Technique.SACRIFICE, Technique.CRYSTAL, Technique.CHRONO}),
		new Group("Movement", new Technique[] {Technique.PHASE, Technique.FLIGHT, Technique.TELEPORT, Technique.WARP, Technique.AQUA}),
		new Group("Allies", new Technique[] {Technique.GOLEM, Technique.ANGEL, Technique.ILLUSION, Technique.GRASP})
	};
}
