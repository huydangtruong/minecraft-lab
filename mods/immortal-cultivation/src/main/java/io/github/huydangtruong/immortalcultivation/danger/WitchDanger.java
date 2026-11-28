package io.github.huydangtruong.immortalcultivation.danger;

import io.github.huydangtruong.immortalcultivation.technique.WitchRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * D282: every witch's potions follow the danger strength the witch itself got when it spawned (a tamed witch keeps it).
 * Instant Health and Harming: x factor (HealOrHarmEffectMixin). Thrown Regeneration: x factor longer. Thrown Poison: level II
 * from x1.5, III from x3. Slowness and Weakness unchanged; potions of players untouched.
 */
public final class WitchDanger {
	private static final String DONE_TAG = "permahearts.witch_scaled";

	private WitchDanger() {}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof AbstractThrownPotion p && !p.entityTags().contains(DONE_TAG) && p.getOwner() instanceof Witch w) {
				p.addTag(DONE_TAG);
				double k = DangerMobs.strengthFactor(w);
				if (k > 1.0) {
					p.setItem(scaled(p.getItem(), k));
				}
			}
		});
	}

	/** Instant Health / Harming multiplier for a potion whose owner (or drinker) is this entity: 1 unless it is a boosted witch. */
	public static double instantFactor(Entity by) {
		return by instanceof Witch w ? WitchRules.instantFactor(DangerMobs.strengthFactor(w)) : 1.0;
	}

	/** The thrown stack with Regeneration lengthened and Poison raised; the same stack when nothing changes. */
	static ItemStack scaled(ItemStack stack, double k) {
		PotionContents c = stack.get(DataComponents.POTION_CONTENTS);
		if (c == null) {
			return stack;
		}
		List<MobEffectInstance> out = new ArrayList<>();
		boolean changed = false;
		for (MobEffectInstance e : c.getAllEffects()) {
			if (e.is(MobEffects.REGENERATION)) {
				out.add(new MobEffectInstance(e.getEffect(), WitchRules.regenerationTicks(e.getDuration(), k), e.getAmplifier()));
				changed = true;
			} else if (e.is(MobEffects.POISON) && WitchRules.poisonAmplifier(k) > e.getAmplifier()) {
				out.add(new MobEffectInstance(e.getEffect(), e.getDuration(), WitchRules.poisonAmplifier(k)));
				changed = true;
			} else {
				out.add(e);
			}
		}
		if (!changed) {
			return stack;
		}
		ItemStack copy = stack.copy();
		copy.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(c.getColor()), out, Optional.empty()));
		return copy;
	}
}
