package io.github.huydangtruong.permahearts.mixin;

import io.github.huydangtruong.permahearts.technique.illusion.IllusionBreeding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Illusion (D73): breeding that a clone started gives no XP orb. Only the orb spawn is skipped; the baby is made as usual. */
@Mixin(Animal.class)
public abstract class AnimalBreedXpMixin {
	@Redirect(method = "finalizeSpawnChildFromBreeding", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean permahearts$noCloneBreedXp(ServerLevel level, Entity orb) {
		Animal self = (Animal) (Object) this;
		if (IllusionBreeding.isMarked(self)) {
			return false;
		}
		return level.addFreshEntity(orb);
	}
}
