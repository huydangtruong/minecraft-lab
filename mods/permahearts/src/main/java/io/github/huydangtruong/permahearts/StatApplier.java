package io.github.huydangtruong.permahearts;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;

/** Turns elixir levels into permanent attribute modifiers (all removed while the world switch is off). */
public final class StatApplier {
	private StatApplier() {}

	private static List<Holder<Attribute>> attributes(ElixirStat stat) {
		return switch (stat) {
			case STRENGTH -> List.of(Attributes.ATTACK_DAMAGE);
			case FURY -> List.of(Attributes.ATTACK_SPEED);
			case SWIFTNESS -> List.of(Attributes.MOVEMENT_SPEED);
			case IRON_SKIN -> List.of(Attributes.ARMOR);
			case REACH -> List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE);
			case MINING -> List.of(Attributes.BLOCK_BREAK_SPEED);
			case VITALITY -> List.of(); // handled when eating, see PermaHearts#onAte
		};
	}

	public static void apply(ServerPlayer p) {
		boolean on = PermaHearts.enabled(p.level().getServer());
		CultivationData data = HeartsAttachment.getCultivation(p);
		for (ElixirStat stat : ElixirStat.values()) {
			double value = on ? stat.bonus(data.level(stat)) : 0;
			Identifier id = Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "elixir_" + stat.id());
			AttributeModifier.Operation op = stat.multiplier()
				? AttributeModifier.Operation.ADD_MULTIPLIED_BASE
				: AttributeModifier.Operation.ADD_VALUE;
			for (Holder<Attribute> attr : attributes(stat)) {
				AttributeInstance inst = p.getAttribute(attr);
				if (inst == null) {
					continue;
				}
				if (value == 0) {
					inst.removeModifier(id);
				} else {
					inst.addOrReplacePermanentModifier(new AttributeModifier(id, value, op));
				}
			}
		}
	}
}
