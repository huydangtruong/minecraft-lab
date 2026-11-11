package io.github.huydangtruong.permahearts;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class HealthApplier {
	private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "bonus_hearts");

	private HealthApplier() {}

	public static void apply(ServerPlayer p, boolean refill) {
		AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
		double extra = HeartsAttachment.get(p).bonusHearts() * 2.0;
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
