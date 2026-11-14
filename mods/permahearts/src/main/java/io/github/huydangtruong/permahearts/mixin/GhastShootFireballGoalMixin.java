package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.wanderer.GhastLeash;
import io.github.huydangtruong.permahearts.wanderer.QuestBoss;
import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The Ghast boss shoots 20% faster (D35): a shorter wait after each fireball. Other ghasts are unchanged. */
@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
abstract class GhastShootFireballGoalMixin {
	@Shadow
	@Final
	private Ghast ghast;

	@ModifyConstant(method = "tick", constant = @Constant(intValue = -GhastLeash.VANILLA_COOLDOWN))
	private int permahearts$bossCooldown(int cooldown) {
		return ghast.entityTags().contains(QuestBoss.TAG) ? -GhastLeash.BOSS_COOLDOWN : cooldown;
	}
}
