package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One line of a quest. target = item id (COLLECT) or entity type id (KILL); reward = ElixirStat id. */
public record QuestLine(QuestType type, String target, int required, int progress, String reward, boolean claimed) {
	public static final Codec<QuestType> TYPE_CODEC = Codec.STRING.xmap(QuestType::valueOf, QuestType::name);

	public static final Codec<QuestLine> CODEC = RecordCodecBuilder.create(i -> i.group(
		TYPE_CODEC.fieldOf("type").forGetter(QuestLine::type),
		Codec.STRING.fieldOf("target").forGetter(QuestLine::target),
		Codec.INT.fieldOf("required").forGetter(QuestLine::required),
		Codec.INT.optionalFieldOf("progress", 0).forGetter(QuestLine::progress),
		Codec.STRING.fieldOf("reward").forGetter(QuestLine::reward),
		Codec.BOOL.optionalFieldOf("claimed", false).forGetter(QuestLine::claimed)
	).apply(i, QuestLine::new));

	public boolean done() {
		return progress >= required;
	}

	QuestLine withProgress(int p) {
		return new QuestLine(type, target, required, Math.min(required, p), reward, claimed);
	}

	/** Death Match: the number of kills needed is only known when the match starts. Progress restarts at 0. */
	QuestLine withRequired(int r) {
		return new QuestLine(type, target, r, 0, reward, claimed);
	}

	QuestLine withClaimed(boolean c) {
		return new QuestLine(type, target, required, progress, reward, c);
	}
}
