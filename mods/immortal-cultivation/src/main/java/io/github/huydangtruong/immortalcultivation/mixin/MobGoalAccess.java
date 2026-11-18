package io.github.huydangtruong.immortalcultivation.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Soul Grasp needs the target selector, the vanilla goal registration and the persistence flag of a Mob. */
@Mixin(Mob.class)
public interface MobGoalAccess {
	@Accessor("targetSelector")
	GoalSelector permahearts$targetSelector();

	@Invoker("registerGoals")
	void permahearts$registerGoals();

	@Accessor("persistenceRequired")
	void permahearts$setPersistenceRequired(boolean value);
}
