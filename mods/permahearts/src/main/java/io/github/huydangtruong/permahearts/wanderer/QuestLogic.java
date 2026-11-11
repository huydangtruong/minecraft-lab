package io.github.huydangtruong.permahearts.wanderer;

import java.util.Optional;
import java.util.function.Predicate;

/** Pure quest rules. Every method returns new immutable values; the caller stores them. */
public final class QuestLogic {
	private QuestLogic() {}

	public record DepositResult(Quest quest, int taken) {}

	public record KillResult(Quest quest, int line) {}

	public static Quest accept(Quest q) {
		return new Quest(q.id(), q.lines(), true);
	}

	public static DepositResult deposit(Quest q, int line, String itemId, int count) {
		if (!q.accepted() || line < 0 || line >= q.lines().size() || count <= 0) {
			return new DepositResult(q, 0);
		}
		QuestLine l = q.lines().get(line);
		if (l.type() != QuestType.COLLECT || l.done() || !l.target().equals(itemId)) {
			return new DepositResult(q, 0);
		}
		int taken = Math.min(count, l.required() - l.progress());
		return new DepositResult(q.withLine(line, l.withProgress(l.progress() + taken)), taken);
	}

	/** First not-done KILL line with this target gets +1. */
	public static Optional<KillResult> kill(Quest q, String entityId) {
		if (!q.accepted()) {
			return Optional.empty();
		}
		for (int i = 0; i < q.lines().size(); i++) {
			QuestLine l = q.lines().get(i);
			if (l.type() == QuestType.KILL && !l.done() && l.target().equals(entityId)) {
				return Optional.of(new KillResult(q.withLine(i, l.withProgress(l.progress() + 1)), i));
			}
		}
		return Optional.empty();
	}

	/** Marks the line claimed; empty if it is not done or already claimed. */
	public static Optional<Quest> claim(Quest q, int line) {
		if (line < 0 || line >= q.lines().size()) {
			return Optional.empty();
		}
		QuestLine l = q.lines().get(line);
		if (!l.done() || l.claimed()) {
			return Optional.empty();
		}
		return Optional.of(q.withLine(line, l.withClaimed(true)));
	}

	/** Admin: finish a line. */
	public static Quest complete(Quest q, int line) {
		if (line < 0 || line >= q.lines().size()) {
			return q;
		}
		QuestLine l = q.lines().get(line);
		return q.withLine(line, l.withProgress(l.required()));
	}

	/** First not-done EXPLORE line whose target satisfies isInside gets done. */
	public static Optional<KillResult> explore(Quest q, Predicate<String> isInside) {
		if (!q.accepted()) {
			return Optional.empty();
		}
		for (int i = 0; i < q.lines().size(); i++) {
			QuestLine l = q.lines().get(i);
			if (l.type() == QuestType.EXPLORE && !l.done() && isInside.test(l.target())) {
				return Optional.of(new KillResult(eventDone(q, i), i));
			}
		}
		return Optional.empty();
	}

	/** Marks an event line (blood moon, explore, boss) done. Unchanged if the quest is not accepted or the line is done. */
	public static Quest eventDone(Quest q, int line) {
		if (!q.accepted() || line < 0 || line >= q.lines().size() || q.lines().get(line).done()) {
			return q;
		}
		return complete(q, line);
	}
}
