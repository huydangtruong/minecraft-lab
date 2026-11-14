package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Bleeding (D60), from the Life Steal Blade: 1 damage per second with red drops. Undead bleed too. */
public final class Bleeding extends MobEffect {
	public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "bleeding"), new Bleeding());

	private Bleeding() {
		super(MobEffectCategory.HARMFUL, 0x8a0b0b);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		entity.hurtServer(level, level.damageSources().magic(), 1f);
		level.sendParticles(new DustParticleOptions(0xb01010, 1.0f), entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
			6, entity.getBbWidth() * 0.3, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.3, 0.0);
		return true;
	}

	public static void init() {
		EFFECT.value();
	}
}
