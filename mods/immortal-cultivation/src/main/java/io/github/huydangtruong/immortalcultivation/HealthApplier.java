package io.github.huydangtruong.immortalcultivation;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class HealthApplier {
	private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "bonus_hearts");

	private HealthApplier() {}

	public static void apply(ServerPlayer p, boolean refill) {
		StatApplier.apply(p);
		AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
		boolean on = ImmortalCultivation.enabled(p.level().getServer());
		double extra = on ? HeartsAttachment.get(p).bonusHearts() * 2.0 : 0;
		if (extra == 0) {
			a.removeModifier(MODIFIER_ID);
		} else {
			a.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER_ID, extra, AttributeModifier.Operation.ADD_VALUE));
		}
		if (refill) {
			p.setHealth(p.getMaxHealth());
		} else {
			p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
		}
	}
}
