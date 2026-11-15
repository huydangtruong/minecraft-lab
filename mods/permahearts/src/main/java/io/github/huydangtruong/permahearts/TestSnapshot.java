package io.github.huydangtruong.permahearts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** What /testmode full saves so /testmode reset can put it back: hearts, elixirs and every inventory slot (armor, offhand included), by container index. */
public record TestSnapshot(HeartsData hearts, CultivationData cultivation, List<ItemStack> items) {
	public static final Codec<TestSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
		HeartsData.CODEC.fieldOf("hearts").forGetter(TestSnapshot::hearts),
		CultivationData.CODEC.fieldOf("cultivation").forGetter(TestSnapshot::cultivation),
		ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(TestSnapshot::items)
	).apply(i, TestSnapshot::new));
}
