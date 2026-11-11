package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record Quest(long id, List<QuestLine> lines, boolean accepted) {
	public static final Codec<Quest> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.fieldOf("id").forGetter(Quest::id),
		QuestLine.CODEC.listOf().fieldOf("lines").forGetter(Quest::lines),
		Codec.BOOL.optionalFieldOf("accepted", false).forGetter(Quest::accepted)
	).apply(i, Quest::new));

	public boolean allDoneAndClaimed() {
		for (QuestLine l : lines) {
			if (!l.done() || !l.claimed()) {
				return false;
			}
		}
		return true;
	}

	Quest withLine(int index, QuestLine line) {
		java.util.ArrayList<QuestLine> copy = new java.util.ArrayList<>(lines);
		copy.set(index, line);
		return new Quest(id, List.copyOf(copy), accepted);
	}
}
