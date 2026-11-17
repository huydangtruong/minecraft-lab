package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Stunned (Heaven Blast, D99): a mob with this effect does not walk, steer, attack or shoot (see MobStunMixin). It can
 * still fall and be pushed by physics. Players never get it. A ring of sparks circles above the head while it lasts.
 */
public final class Stun extends MobEffect {
	public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "stun"), new Stun());

	private Stun() {
		super(MobEffectCategory.HARMFUL, 0xffe14a);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 2 == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		double r = Math.max(0.35, entity.getBbWidth() * 0.5);
		double base = entity.tickCount * 0.45;
		for (int i = 0; i < 3; i++) {
			double a = base + i * Math.PI * 2 / 3;
			level.sendParticles(ParticleTypes.CRIT, entity.getX() + Math.cos(a) * r, entity.getY() + entity.getBbHeight() + 0.3,
				entity.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
		}
		return true;
	}

	public static void init() {
		EFFECT.value(); // touch the class so the effect is registered
	}
}
