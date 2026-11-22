# P-068 — Wheel slots (hold R from the cross, slot row, scroll)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** giữ R khi đang cầm đồ thường thì tự chọn ô chiêu sẵn sàng nhỏ nhất rồi mở bảng. Trên cùng có hàng các ô chiêu, chiêu đã nằm trong ô thì bị làm tối ở danh sách. Giữ R + lăn chuột chuyển sang ô kế tiếp.

- **Status:** in-dev
- **Mod / decision:** `immortal-cultivation`, D193 (extends D188) · **Branch:** `feat/P-068-wheel-slots` · **Depends on:** P-067

## Goal
Exactly D193 in `docs/system/01-decisions.md`.

## Scope
- In: client wheel only (`ClientSkills`, `TechniqueWheelScreen`), a pure `WheelLogic` class, JUnit, `WheelClientTest`.
- Out: server rules (ASSIGN/swap unchanged), key bindings, other screens.

## Files
| File | Where | Change |
|---|---|---|
| `technique/WheelLogic.java` (new, pure) | main | `defaultSlot(open, usable, cooling)`, `step(current, delta, open)` |
| `client/ClientSkills.java` | `tickKey`, `tick` | open from the cross (select default slot), allow empty selected slot, grace so an empty slot is not deselected while filling |
| `client/TechniqueWheelScreen.java` | whole | mutable slot, top slot row, darkened listed techniques, `mouseScrolled`, footer hint |
| `WheelClientTest.java` | gametest | cross open, scroll, render, screenshot `docs/plans/P-068-wheel.png` |

## Steps
1. Pure `WheelLogic` + JUnit. 2. Glue in `ClientSkills`. 3. Screen. 4. Client test + screenshot. 5. Docs (D193, 03-status).

## Tests
- Test scope checklist: areas ticked = "Thanh chiêu, danh sách chiêu" (client only); change type = new logic in a client screen; commands = `:test --tests '*WheelLogicTest'`, `compileClientJava`, the wheel client test only. Server technique tests untouched.
## Docs
D193 row, 03-status row.
## Commit
Small English commits, `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`; push the branch only.
## Done when
- [ ] R on the cross picks the lowest ready slot (else slot 1) and opens the wheel; no open slot = nothing
- [ ] Top row, darkened listed techniques, scroll between open slots (hotbar not scrolled)
- [ ] JUnit + wheel client test green, screenshot saved
## Risks / ask first
An empty slot chosen through the wheel and left empty falls back to the cross (same rule as before for empty slots).
