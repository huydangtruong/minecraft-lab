package io.github.huydangtruong.permahearts.wanderer;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What the client needs to build the menu: the quest's static shape (progress and claimed state travel as data slots). */
public record QuestSnapshot(long questId, List<LineView> lines, boolean accepted) {
	public record LineView(QuestType type, String target, int required, String reward) {
		public static final StreamCodec<ByteBuf, LineView> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.map(QuestType::valueOf, QuestType::name), LineView::type,
			ByteBufCodecs.STRING_UTF8, LineView::target,
			ByteBufCodecs.VAR_INT, LineView::required,
			ByteBufCodecs.STRING_UTF8, LineView::reward,
			LineView::new);
	}

	public static final StreamCodec<ByteBuf, QuestSnapshot> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_LONG, QuestSnapshot::questId,
		LineView.STREAM_CODEC.apply(ByteBufCodecs.list(4)), QuestSnapshot::lines,
		ByteBufCodecs.BOOL, QuestSnapshot::accepted,
		QuestSnapshot::new);

	public static QuestSnapshot of(Quest q) {
		return new QuestSnapshot(q.id(), q.lines().stream()
			.map(l -> new LineView(l.type(), l.target(), l.required(), l.reward())).toList(), q.accepted());
	}
}
