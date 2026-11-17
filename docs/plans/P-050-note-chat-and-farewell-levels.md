# P-050 — Shared-note chat line, Crimson Master farewell levels

> **Tóm tắt cho Huy (VN):** ai ghi vào ghi chú chung thì cả server thấy một dòng chat ngắn (không kèm nội dung). Khi Crimson Master rời đi (hết giờ, bị bỏ cuộc, hoặc xong hết nhiệm vụ), mọi người chơi đang online được tặng số cấp = số nhiệm vụ đã xong × 8, kèm một dòng chat ngay dưới dòng báo rời đi (D131).

- **Status:** ready · **Branch:** `feat/P-050-note-chat-farewell-levels` (from `main`) · **Decisions:** D131

## 1. Shared-note chat line
- `notes/NotesNet.apply`, `kind == ADD`, **shared only**, after `data.add` succeeded: broadcast to every online player a system chat line, e.g. `"<player> added a note to the shared notes"` (player display name, gray text is fine). **Never include the note text.** Private notes: no message. Deleting: no message.
- New `Messages.sharedNoteAdded(Component player)` with `translatableWithFallback` + the key in `lang/en_us.json`.

## 2. Farewell levels
- **Rule (pure Java, no Minecraft imports, JUnit-tested):** e.g. `QuestLogic.farewellLevels(int completed)` or a small `FarewellGift` class: `levels = completed * 8`; `completed <= 0` → 0.
- **Completed** = number of lines of the visit's current quest with `done()` true (claimed or not) at the moment the Crimson Master leaves. No quest (planned visit, never accepted, etc.) → 0. Read it **before** the data is cleared.
- **When:** every way the visit ends with a departure message:
  1. time up — `WandererScheduler` → `Wanderer.leave(server, Messages.leftAfterStay())`;
  2. give up — `Wanderer.giveUp` and `QuestCommand` → `Wanderer.leave(server, Messages.gaveUp(...))`;
  3. all lines done and claimed — `Wanderer.farewellIfFinished` (`Messages.farewell()`).
  Not on `Wanderer.reset` (admin/tests).
- **Who:** every player online at that moment, `ServerPlayer#giveExperienceLevels(levels)`. Offline players get nothing (no saving for later).
- **Chat:** when levels > 0, broadcast right **after** the departure message (so it shows on the line below it): e.g. `"Everyone receives 24 levels for 3 completed quests"` (singular "quest" for 1). New `Messages.farewellLevels(int levels, int quests)` + lang key. When levels == 0: no gift, no line.
- Keep it in one place, e.g. a private `giftLevels(server, completed)` called from `leave` and `farewellIfFinished` after their broadcast.

## Tests
- JUnit for the rule (0, 1, 3, negative).
- Game tests (`WandererGameTest` / `NotesGameTest` style): a quest with 2 done lines + 1 not done → `leave` → player level +16; give up path same; farewell path with all lines done and claimed → +8 × line count; no quest → level unchanged. Shared note add via `NotesNet.apply` does not throw and a private note add still works (checking the chat line itself is optional).

## Verify
`./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest` green (JDK 25, muted client; known flaky game tests may fail once: rerun).

## Commit
Branch `feat/P-050-dev` from `feat/P-050-note-chat-farewell-levels`, commit, then report (manager fast-forwards). Do not push, do not touch `main`. English commits ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
