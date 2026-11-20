package io.github.huydangtruong.immortalcultivation.technique;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * D159: what Life Stealer leaves on its target for 2 s: exactly -80% movement speed. Vanilla Slowness only moves in steps
 * of 15% (amplifier 4 = -75%, 5 = -90%), so this effect carries its own modifier (the amount is multiplied by amplifier + 1).
 */
public final class StealerSlow extends MobEffect {
	public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "drained"), new StealerSlow());

	private StealerSlow() {
		super(MobEffectCategory.HARMFUL, 0x8a0b0b);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "stealer_slow"),
			-StealerRules.SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	public static void init() {
		EFFECT.value();
	}
}
