package io.github.huydangtruong.immortalcultivation.reward;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The data component of a rolled reward box: its cards. Saved with the stack, so dropping, chests and relogging keep it. */
public record RewardBoxData(List<RewardOption> options) {
	public static final Codec<RewardBoxData> CODEC = RewardOption.CODEC.listOf().xmap(RewardBoxData::new, RewardBoxData::options);

	public static final StreamCodec<ByteBuf, RewardOption> OPTION_STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, RewardOption::itemId,
		ByteBufCodecs.VAR_INT, RewardOption::count,
		ByteBufCodecs.STRING_UTF8, RewardOption::enchant,
		ByteBufCodecs.VAR_INT, RewardOption::enchantLevel,
		RewardOption::new);

	public static final StreamCodec<ByteBuf, List<RewardOption>> OPTIONS_STREAM_CODEC =
		OPTION_STREAM_CODEC.apply(ByteBufCodecs.list(8));

	public static final StreamCodec<ByteBuf, RewardBoxData> STREAM_CODEC =
		OPTIONS_STREAM_CODEC.map(RewardBoxData::new, RewardBoxData::options);

	public RewardBoxData {
		options = List.copyOf(options);
	}
}
