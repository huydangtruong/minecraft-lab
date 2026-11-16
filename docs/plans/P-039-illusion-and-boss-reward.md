# P-039 — Illusion feed and work range, God Slayer level reward split

> **Tóm tắt cho Huy (VN):** Phân thân nhận hết thức ăn cho thú ở ô 1; tầm làm việc +4 mỗi 5 level; level thưởng khi hạ boss = 50 + 50 × số người gây sát thương, chia theo sát thương (D109, D110).

- **Status:** ready · **Branch:** `feat/P-039-illusion-reward` · **Decisions:** D109, D110 · **Backlog:** BL-021, BL-022
- Files: `technique/TechniqueRules.java` (+ test), `technique/Illusion.java` (only where the feed item is split), `godslayer/GodSlayerHunt.java` / `GodSlayer.java` / `GodSlayerRules.java` (reward), `Messages.java`.

## 1. Illusion (D109)
- Slot 1 holds animal feed (wheat, carrot, potato and whatever the feed job already accepts): **all** of that stack is split evenly between the clones (today half). Remainder: give the extra items to the first clones. The player's slot is emptied as the items go to the clones (keep however the current code moves them; just the amount changes).
- `illusionWorkRadius(level)`: 8 at Lv 1, **+4 every 5 levels**: Lv 1–4 = 8, 5–9 = 12, 10–14 = 16, 15–19 = 20, 20 = 24. Update the JUnit test and the stats line.

## 2. God Slayer level reward (D110)
- Track damage per player on the boss: in `GodSlayer.hurtServer`, after the clamp, add the amount actually taken to the owning player (a player, or the owner of an `OwnedSummon`). Keep it on the entity (save it with the entity, or at least for the fight).
- On death: total levels = **50 + 50 × N**, N = players with damage > 0. Each of them gets `round(total × theirDamage / allDamage)` levels (pure function in `GodSlayerRules`, JUnit: 3 players where one did 0 → N = 2, total 150, split by damage; the one with 0 gets nothing). Players offline at the moment get nothing.
- This replaces the "+100 levels to the killer". The 5 boxes stay as now (to the killer). Message each player their share.

## Verify
`./gradlew :permahearts:build` green (JDK 25). The known flaky game tests (thunder pig, golem guardian) may fail once: rerun.

## Commit
Branch `feat/P-039-dev` from `feat/P-039-illusion-reward`, ff-merge back. Do not push. English commits with the Co-Authored-By line. No `__pycache__`.
