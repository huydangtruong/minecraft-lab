package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.wanderer.QuestBoss;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * D119: vanilla turns a reflected fireball into 1000 damage. The Ghast boss skips that rule in hurtServer and takes the
 * fireball's normal damage (impact and explosion); isInvulnerableTo keeps the vanilla exception so the fire-immune boss
 * can still be hurt by it. Other ghasts are unchanged.
 */
@Mixin(Ghast.class)
abstract class GhastMixin {
	@Redirect(method = "hurtServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/monster/Ghast;isReflectedFireball(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
	private boolean permahearts$noInstantKillOnBoss(DamageSource source) {
		boolean reflected = source.getDirectEntity() instanceof LargeFireball && source.getEntity() instanceof Player;
		return reflected && !((Ghast) (Object) this).entityTags().contains(QuestBoss.TAG);
	}
}
