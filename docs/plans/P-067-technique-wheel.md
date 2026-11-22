# P-067 — Technique wheel (hold R)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** giữ R khi đang chọn một ô chiêu để mở bảng chiêu theo nhóm; bấm chiêu nào thì chiêu đó vào ô hiện tại (đổi chỗ nếu nó đang ở ô khác). Ô hiện tại đang hồi thì hiện chữ đỏ, không đổi.

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D188 · **Branch:** `feat/P-067-technique-wheel` (dev works there directly) · **Depends on:** none

## Goal
Exactly D188 in `docs/system/01-decisions.md` (read it first).

## Behaviour
- New key mapping `key.permahearts.wheel`, default **R** (`InputConstants.KEY_R`, SDL scancode style like the Z X C V keys in `client/ClientSkills.java`), same category. Lang entry in `assets/permahearts/lang/en_us.json` (and other lang files that list the slot keys).
- Works only when `ClientSkills.available(mc)` and `ClientSkills.selected() >= 0` (a technique slot is selected; the slot may be empty) and no other screen is open. Otherwise R does nothing.
- While R is held: open a new `TechniqueWheelScreen` (no pause, mouse cursor visible, world still renders behind with a dim panel). Rows by `Technique` group (D114 groups, same order and labels as the techniques screen), each row the learned techniques (level ≥ 1) as icons with name tooltip; the technique now in the selected slot is highlighted. Header: which slot (Z/X/C/V) is being changed.
- Click a technique → send the existing `TechniqueNet.TechniqueAction(ASSIGN, id, slot)` and close. Releasing R (key up, read via the window key state each tick since a screen swallows key events) closes with no change. Esc closes too.
- Swap (D188): if the clicked technique is already in another slot `j`, the technique that was in the selected slot goes to slot `j`. Do it on the server in the ASSIGN handler (`TechniqueNet` ~line 200, `TechniqueData.withSlot` currently just empties the old slot): keep the cooldown check on the selected slot only (the other slot's cooldown does not block the swap; cooldowns follow the technique, not the slot). Clicking the technique already in the slot = close, no packet.
- Cooldown: if the selected slot's technique is on cooldown, clicking shows a red line "<Name> is on cooldown (N s)" in the screen (client-side check from `ClientSkills.data(p)` with the same rule the server uses, `slotOnCooldown`) and does not send or close. The server check stays as the guard.
- Keep the selected slot selected after the change.

## Pure Java
Swap logic in `TechniqueData` (e.g. `withSlotSwapped(slot, t)`) with JUnit cases: move into empty slot, swap with another slot, same technique no-op, null/locked handled by caller.

## Tests (scope: medium, one feature)
- Checklist in docs/workflow/team.md "Test theo phạm vi thay đổi". JUnit for `TechniqueData` + `runGameTest -Pgt='*:technique*'` (add one server game test: ASSIGN swaps two slots; ASSIGN refused while the selected slot is cooling). Client: compile; **one** client game test screenshot of the wheel is allowed since it is a new screen, look at it yourself. Clients stay muted.

## Commit
Small commits in English ending with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Never push `main`, no install.sh, never touch Huy's saves. Do not edit 03-status.

## Done when
- [ ] Hold R opens the wheel only with a technique slot selected; release closes
- [ ] Click assigns / swaps; cooling slot shows red text and changes nothing
- [ ] JUnit + technique area tests green, one wheel screenshot looked at; report ≤ 10 lines
