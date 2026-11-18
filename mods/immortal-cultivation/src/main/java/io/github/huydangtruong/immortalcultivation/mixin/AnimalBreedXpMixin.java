package io.github.huydangtruong.immortalcultivation.mixin;

import io.github.huydangtruong.immortalcultivation.technique.illusion.IllusionBreeding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Illusion (D73): breeding that a clone started drops no XP orb; the XP goes to the owner (D140). Only the orb spawn is skipped; the baby is made as usual. */
@Mixin(Animal.class)
public abstract class AnimalBreedXpMixin {
	@Redirect(method = "finalizeSpawnChildFromBreeding", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean permahearts$noCloneBreedXp(ServerLevel level, Entity orb) {
		Animal self = (Animal) (Object) this;
		if (IllusionBreeding.isMarked(self)) {
			if (orb instanceof ExperienceOrb xo) {
				IllusionBreeding.giveXpToOwner(self, xo.getValue());
			}
			return false;
		}
		return level.addFreshEntity(orb);
	}
}
