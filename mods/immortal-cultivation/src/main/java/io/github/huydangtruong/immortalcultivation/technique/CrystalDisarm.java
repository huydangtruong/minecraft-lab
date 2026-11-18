package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Disarmed (Crystal Prison, D143): a caged hostile mob, or the Death Match opponent of the caster, cannot attack or pick a
 * target for a while. It can still walk. The work is done by MobDisarmMixin (mobs) and the hooks in Crystal (players).
 */
public final class CrystalDisarm extends MobEffect {
	public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "crystal_disarm"), new CrystalDisarm());

	private CrystalDisarm() {
		super(MobEffectCategory.HARMFUL, 0x40e0d0);
	}

	public static boolean disarmed(LivingEntity e) {
		return e.hasEffect(EFFECT);
	}

	public static void init() {
		EFFECT.value(); // touch the class so the effect is registered
	}
}
