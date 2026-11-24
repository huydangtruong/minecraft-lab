package io.github.huydangtruong.immortalcultivation.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

/** One rune: the enchantment it strengthens and its level (D241). */
public record RuneEntry(Holder<Enchantment> enchantment, int level) {
	public static final Codec<RuneEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
		Enchantment.CODEC.fieldOf("enchantment").forGetter(RuneEntry::enchantment),
		Codec.intRange(1, 64).fieldOf("level").forGetter(RuneEntry::level)
	).apply(i, RuneEntry::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, RuneEntry> STREAM_CODEC = StreamCodec.composite(
		Enchantment.STREAM_CODEC, RuneEntry::enchantment,
		ByteBufCodecs.VAR_INT, RuneEntry::level,
		RuneEntry::new);
	public static final Codec<List<RuneEntry>> LIST_CODEC = CODEC.listOf();
	public static final StreamCodec<RegistryFriendlyByteBuf, List<RuneEntry>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

	public boolean sameEnchantment(RuneEntry o) {
		return o != null && o.enchantment.unwrapKey().equals(enchantment.unwrapKey());
	}

	public int bonus() {
		return RuneRules.bonus(level);
	}

	public boolean sameKind(RuneEntry o) {
		return o != null && o.level == level && sameEnchantment(o);
	}
}
