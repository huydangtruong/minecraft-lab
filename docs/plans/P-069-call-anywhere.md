# P-069 — Call grasped mobs from anywhere

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Gọi và Gọi tất cả đưa binh lính về dù đang ở chiều không gian khác hay ở rất xa (D198). Mob chưa tải thì tạm tải khu vực nó đứng lần cuối rồi gọi về; không thấy thì báo và không tính hồi chiêu.

- **Status:** review
- **Mod / decision:** `immortal-cultivation`, D198 (extends D168) · **Branch:** `feat/P-068-wheel-slots` · **Depends on:** P-053

## Goal
Exactly D198 in `docs/system/01-decisions.md`.

## Scope
- In: `Grasp.call` / `allCall` (cross-dimension teleport, temporary chunk ticket, wait, timeout), pure `GraspCalls` bookkeeping, JUnit, game tests, overlay texts.
- Out: menu layout, cooldown values, arrival spots, release/death rules.

## Files
| File | Where | Change |
|---|---|---|
| `technique/GraspCalls.java` (new, pure) | main | pending calls, deadline, shared-chunk ticket bookkeeping |
| `technique/grasp/Grasp.java` | `call`, `allCall`, `tick` | `arrive` (teleport across dimension), `startLoad` / `tickCalls` / `endCall`, cancel on disconnect and server stop |
| `GraspCallsTest.java` | test | JUnit of the bookkeeping |
| `GraspGameTest.java` | gametest | Nether call, Nether Call all, unloaded chunk, not found + no cooldown |

## Steps
1. Pure bookkeeping + JUnit. 2. Glue in `Grasp`. 3. Game tests. 4. Docs (D198, 02-findings, 03-status).

## Tests
- Test scope checklist (docs/workflow/team.md): areas ticked = Soul Grasp; change type = new logic in one area; commands = `:test --tests '*GraspCallsTest' --tests '*GraspRulesTest'`, `runGameTest -Pgt='*:grasp*'` (3 runs).

## Verify
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :immortal-cultivation:test --tests '*GraspCallsTest' --tests '*GraspRulesTest'
./gradlew :immortal-cultivation:runGameTest -Pgt='*:grasp*'
```

## Docs
D198, 02-findings (teleport and ticket APIs), 03-status.

## Commit
`Call and Call all reach grasped mobs in other dimensions and in unloaded chunks (D198)`

## Done when
- [x] A mob in another dimension is brought over and stays grasped (same slot, number, bonus).
- [x] A mob in an unloaded chunk is found through a temporary ticket and brought over; the ticket is released.
- [x] Not found: message, nothing changed, no cooldown spent.

## Risks / ask first
- Pending calls time out after 100 ticks; a very slow disk could miss it (message says so, calling again works).
- A mob that was moved while unloaded (never done by this mod) is not found.
