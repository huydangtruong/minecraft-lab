package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * D115: timed techniques show an icon with the time left in the top-right effect area. Each technique has a plain
 * MobEffect (no attributes, no particles) that only mirrors the technique's own end tick: the technique logic never reads
 * the effect, so milk or /effect clear only removes the icon (it is put back on the next tick while the technique runs).
 */
public final class TechniqueTimers {
	public static final Holder<MobEffect> UNDYING = register("undying_jutsu", 0xb01010);
	public static final Holder<MobEffect> SHADOW = register("shadow_walk", 0x6a3fb0);
	public static final Holder<MobEffect> CLOUD = register("cloud_travel", 0xe8f4ff);
	public static final Holder<MobEffect> AQUA = register("aqua_marine", 0x2fa8d8);

	private TechniqueTimers() {}

	private static Holder<MobEffect> register(String id, int color) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, id),
			new MobEffect(MobEffectCategory.BENEFICIAL, color) {});
	}

	public static void init() {
		UNDYING.value();
	}

	/** Called every server tick for each player: keeps the four icons in line with the technique end ticks. */
	static void tick(ServerPlayer p) {
		long now = p.level().getGameTime();
		sync(p, UNDYING, remaining(p.getAttached(TechniqueCasts.OATH_UNTIL), now));
		PhaseState phase = p.getAttached(PhaseState.ATTACHMENT);
		sync(p, SHADOW, phase == null ? 0 : remaining(phase.endTick(), now));
		sync(p, CLOUD, remaining(p.getAttached(Flight.END), now));
		sync(p, AQUA, remaining(p.getAttached(AquaMarine.END), now));
	}

	private static long remaining(Long end, long now) {
		return end == null ? 0 : Math.max(0, end - now);
	}

	private static void sync(ServerPlayer p, Holder<MobEffect> effect, long remaining) {
		MobEffectInstance cur = p.getEffect(effect);
		if (remaining <= 0) {
			if (cur != null) {
				p.removeEffect(effect);
			}
			return;
		}
		int want = (int) Math.min(remaining, Integer.MAX_VALUE);
		if (cur != null && Math.abs(cur.getDuration() - want) <= 2) {
			return;
		}
		if (cur != null && cur.getDuration() > want) {
			p.removeEffect(effect); // a shorter new end (a new cast): addEffect only ever lengthens
		}
		p.addEffect(new MobEffectInstance(effect, want, 0, false, false, true));
	}
}
