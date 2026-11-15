package io.github.huydangtruong.permahearts.godslayer;

import net.minecraft.world.entity.LivingEntity;

/**
 * One attack of the God Slayer. P-033 adds more of these (one per level up to 10). A skill is a small state machine:
 * the boss asks each unlocked skill {@link #canStart} when it is free, then calls {@link #tick} every tick until
 * {@link #isDone}. Skills are created per boss (see {@link GodSlayerSkills#forLevel}), so they may keep state.
 */
public interface GodSlayerSkill {
	/** Boss level from which this skill is used. */
	int unlockLevel();

	boolean canStart(GodSlayer boss, LivingEntity target);

	/** Runs every tick while the skill is active; the first call right after it was chosen. */
	void tick(GodSlayer boss, LivingEntity target);

	boolean isDone();
}
