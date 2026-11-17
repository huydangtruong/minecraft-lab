package io.github.huydangtruong.immortalcultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * What /testmode full saves so /testmode reset can put it back: hearts, elixirs, every inventory slot (armor, offhand
 * included) by container index, and the XP bar. xpLevel -1 = a snapshot saved before XP was kept (reset leaves XP alone).
 */
public record TestSnapshot(HeartsData hearts, CultivationData cultivation, List<ItemStack> items, int xpLevel, float xpProgress, int xpTotal) {
	public static final Codec<TestSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
		HeartsData.CODEC.fieldOf("hearts").forGetter(TestSnapshot::hearts),
		CultivationData.CODEC.fieldOf("cultivation").forGetter(TestSnapshot::cultivation),
		ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(TestSnapshot::items),
		Codec.INT.optionalFieldOf("xp_level", -1).forGetter(TestSnapshot::xpLevel),
		Codec.FLOAT.optionalFieldOf("xp_progress", 0f).forGetter(TestSnapshot::xpProgress),
		Codec.INT.optionalFieldOf("xp_total", 0).forGetter(TestSnapshot::xpTotal)
	).apply(i, TestSnapshot::new));
}
