# P-028 — Illusion chopping fix, new technique picking

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** sửa phân thân chặt cây chậm (20 s chỉ 4 khúc) và không thả gỗ khi không có rương.
> Danh sách chiêu: nút "i" mở chi tiết, thanh level xanh lá dưới tên chiêu; bấm chiêu thì chiêu sáng viền vàng cùng các ô có thể đặt, bấm ô để đặt, ô emerald thành nút Cancel nền đỏ nhạt.
> Đã chọn ô trước thì bấm vào tên chiêu cũng đặt được, không cần dấu cộng.

- **Status:** ready
- **Mod / decision:** `permahearts`, D82, D83 · **Branch:** `feat/P-028-chop-picking` · **Depends on:** none

## Goal

1. **Bug (Huy):** holding an axe next to a tree, no chest around, an Illusion clone broke only 4 logs in 20 s, and at the end the logs were not dropped on the ground.
2. **Techniques tab rework (D83):** the new picking flow and level bar below.

## Part 1: Illusion chopping (D82)

Files: `technique/illusion/ChopTask.java`, `technique/illusion/WorkGoal.java`, `technique/Illusion.java` (`dropCarry`, `remove`, `tick`).

- **Reproduce first** in a server game test (extend `IllusionWorkGameTest`): a natural oak (logs + `persistent=false` leaves on dirt) about 6 logs tall, plus a taller one (birch 7 logs, or stacked logs with natural leaves at the top). Spawn a level-1 CHOP clone with an iron axe next to it, no chest in range, and run 20 s of game time.
- **Suspected cause of the slowness (check, do not trust):**
  - `ChopTask` measures reach from the clone's feet (`a.position()`) to the log centre with `REACH = 6`.
  - A top log 6+ blocks up is out of reach. The clone then tries to walk into the tree, has no path, and stalls up to 15 s (`goTo` skips only after 300 ticks).
  - Fix: measure from the eye position, allow reaching logs straight above from the trunk base, and skip a log at once when the path fails instead of waiting 15 s.
  - Also check `workTicks` and the `acted(destroySpeed*5)` pause add up to something sane.
- **Leaves (Huy, after playtest):**
  - The clone does not break leaves, and leaves block its view of and path to the logs.
  - When natural leaves (`PERSISTENT == false`) sit on the ray from its eyes to a wanted log, or block its path or standing spot, it breaks those leaves. Each break gets a short pause, and the drops are picked up.
  - Only the leaves in the way, never the whole canopy, never player-placed leaves.
- **Target speed:** a level-1 clone fells a whole 6-log oak in **≤ 12 s** of game time; the 7-log tree is fully felled too (no log left floating).
- **Drop bug:**
  - When the clone has no chest, everything it carries must end up on the ground when it delivers or its time runs out. Keep D73: dropped at the owner's feet when the owner is in the work area, else where the clone stands; never deleted.
  - Find why it did not happen. Things to check:
    - the discard path in `tick()` → `remove()` → `dropCarry()`;
    - `deliver()` when `chest()` returns null only because of the 5 s scan cooldown;
    - items dropped at the owner's feet being swallowed or hidden.
  - The game test asserts that after the clone is gone, log `ItemEntity`s exist (or the owner's inventory gained them, if picked up) and the count equals the logs broken.
- Keep the rest of D73 unchanged (chest delivery, pickup, replanting).

## Part 2: Techniques tab (D83)

File: `client/.../TechniquesTab.java` (and `TechniqueClientTest.java`). Server side (`TechniqueNet` SET_SLOT, etc.) stays as is.

**List rows:**
- Remove the "Lv N" text.
- Put a small **"i" button** (16×16, drawn like the existing plus box, letter "i" centred) where the Lv text was. Clicking "i" opens the detail page (the only way to open it now). Tooltip "Details".
- The technique name moves up a little (y + 3).
- Under the name, add a **level bar**:
  - 3 slot boxes wide (`2 * SLOT_PITCH + SLOT` = 68 px), 4 px tall.
  - Dark gray background `0xFF373737`, filled from the left in the vanilla XP green `0xFF80FF20` for `level / MAX_LEVEL`.
  - Level 0 is an empty bar. Tooltip on the bar: "Lv N / 20".
- The "+" button stays as it is in slot-first mode.

**Slot-first (exists today):** select a slot (gold outline), then:
- **clicking the row of a learned technique assigns it**, exactly like the "+" button (same checks: `canAssign`);
- rows that can be assigned get a hover highlight.

**Technique-first (new):**
- Clicking a learned technique's row (not on "i", "+", or the bar tooltip) **no longer opens the detail page**. It selects that technique (`selectedTechnique`):
  - its row gets a **yellow outline** (`GOLD`, 2 px like the selected slot);
  - every slot it can go to also gets the yellow outline: open, and either empty or its technique not on cooldown.
- Clicking an outlined slot sends the existing SET_SLOT request for that slot and technique, then clears the selection.
- Clicking a slot that is not outlined does nothing (a locked slot keeps its tooltip).
- Clicking the same row again clears the selection.
- Clicking an unlearned row (level 0) does nothing. Its tooltip says "Learn it first: click i".
- Only one mode at a time:
  - selecting a technique clears `selectedSlot`;
  - selecting a slot clears `selectedTechnique`.
  - Right click on a slot still clears that slot.

**Cancel box:**
- While a slot or a technique is selected, the emerald box becomes **Cancel**: light red face `0xFFE08080` (hover `0xFFF0A0A0`) with the existing cross sprite. No emerald counter text. Tooltip "Cancel".
- Clicking it clears either selection.

**Detail page:**
- The level text "Lv N / 20" is drawn in the XP green `0xFF80FF20` with a dark outline/shadow so it reads on the light panel (vanilla draws its level number with a black outline).
- Draw the same green level bar under it.

**Hotspots:** update `hotspots(...)` so the client test can find the rows, the "i" buttons and the slots.

## Tests

- **JUnit:** if any picking rule ends up in pure Java (for example "which slots can take this technique"), put it in a small pure class and test it. Otherwise none.
- **Server game tests:** Part 1 as described: speed, the tall tree, and the drop count with no chest.
- **Client test (`TechniqueClientTest`),** with real mouse input (`setCursorPos` + `pressMouse`, never a fake event):
  - Technique-first: click a learned row, screenshot `97-technique-picked` (row and free slots outlined, red Cancel), click an outlined slot, assert the slot holds it.
  - Slot-first: click a slot, click a row name (not "+"), assert it is assigned.
  - "i": opens the detail page, screenshot `98-technique-detail-level`.
  - Cancel clears the selection.
- **Look at the screenshots yourself.**

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
./gradlew :permahearts:runClientGameTest
```

## Docs

- `docs/workflow/playtest.md`: update the technique slot steps and the Illusion chop step (VN).
- `docs/system/02-findings.md`: the root cause of both chop bugs, in one or two lines.

## Commit

English messages, one commit per part. Merge `feat/P-028-dev` into `feat/P-028-chop-picking` and push the branch (never main).

## Done when

- [ ] A level-1 clone fells a 6-log oak in ≤ 12 s, and fells the tall tree completely.
- [ ] With no chest, all broken logs end up on the ground (or picked up by the owner), and the test checks the count.
- [ ] The new picking flow, "i" button, level bars and red Cancel all work, shown in the screenshots.
- [ ] `./gradlew :permahearts:build` and `runClientGameTest` are green.

## Risks / ask first

- If the slowness turns out to come from something not described here, fix the real cause and say so in the report.
- The GolemClientTest "husk bleeds" check is known to be flaky once in a while. Rerun it once before investigating.
