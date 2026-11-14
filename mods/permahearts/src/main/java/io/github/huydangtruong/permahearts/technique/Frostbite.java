package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frostbite (Frost Orb area): slows movement and attack speed. The amplifier is the slow in whole percent minus 1,
 * so amplifier + 1 = percent (40% at level 1). Movement and attack speed are slowed the same (D54), matching
 * TechniqueRules.iceSlow / iceAttackSlow rounded to whole percent.
 */
public final class Frostbite extends MobEffect {
	private static final Identifier MOVE_ID = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "frostbite_move");
	private static final Identifier ATTACK_ID = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "frostbite_attack");
	/** Attack speed is this many percentage points slowed less than movement (0 since D54: the same slow). */
	static final int ATTACK_OFFSET = 0;

	public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "frostbite"), new Frostbite());

	private Frostbite() {
		super(MobEffectCategory.HARMFUL, 0x8fd8ff);
		// Templates register the modifier ids (removal uses them); the real amounts are set in addAttributeModifiers.
		addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVE_ID, -0.01, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_ID, -0.01, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	/** Amplifier of the effect for a level's movement slow (a fraction such as 0.20). */
	public static int amplifierFor(double slowFraction) {
		return Math.max(0, (int) Math.round(slowFraction * 100) - 1);
	}

	/** Movement slow as a fraction at an amplifier. */
	public static double moveSlow(int amplifier) {
		return (amplifier + 1) / 100.0;
	}

	/** Attack speed slow as a fraction at an amplifier. */
	public static double attackSlow(int amplifier) {
		return Math.max(0, amplifier + 1 - ATTACK_OFFSET) / 100.0;
	}

	@Override
	public void addAttributeModifiers(AttributeMap map, int amplifier) {
		var move = map.getInstance(Attributes.MOVEMENT_SPEED);
		if (move != null) {
			move.addOrUpdateTransientModifier(new AttributeModifier(MOVE_ID, -moveSlow(amplifier), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		var attack = map.getInstance(Attributes.ATTACK_SPEED);
		if (attack != null) {
			attack.addOrUpdateTransientModifier(new AttributeModifier(ATTACK_ID, -attackSlow(amplifier), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	public static void init() {
		EFFECT.value(); // touch the class so the effect is registered
	}
}
