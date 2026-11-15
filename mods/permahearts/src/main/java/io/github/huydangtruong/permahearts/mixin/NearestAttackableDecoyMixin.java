package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.technique.Illusion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Illusion (D73): hostile mobs used to pick only the real player. After the goal has found a player, the target becomes a
 * random one among that player and the player's clones that the mob could also pick (same conditions: range, sight, and a
 * selector such as an Enderman's). Only {@link Enemy} mobs are touched, so wolves and golems stay vanilla. The goal runs only
 * while the mob has no target, so a choice is kept while it stays valid, and a vanished clone makes the mob pick again.
 */
@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableDecoyMixin extends TargetGoal {
	@Shadow protected LivingEntity target;
	@Shadow protected Class<?> targetType;

	@Shadow
	private TargetingConditions getTargetConditions() {
		throw new AssertionError();
	}

	protected NearestAttackableDecoyMixin(Mob mob, boolean mustSee) {
		super(mob, mustSee);
	}

	@Inject(method = "findTarget", at = @At("TAIL"))
	private void permahearts$decoys(CallbackInfo ci) {
		if (!(target instanceof ServerPlayer p) || !(mob instanceof Enemy) || !(mob.level() instanceof ServerLevel sl)) {
			return;
		}
		List<LivingEntity> choices = null;
		TargetingConditions cond = getTargetConditions();
		for (Illusion c : Illusion.clonesOf(sl, p.getUUID())) {
			if (cond.test(sl, mob, c)) {
				if (choices == null) {
					choices = new ArrayList<>();
					choices.add(p);
				}
				choices.add(c);
			}
		}
		if (choices != null) {
			target = choices.get(mob.getRandom().nextInt(choices.size()));
		}
	}
}
