# P-045 — Separate skill bar with Z X C V keys (experiment)

> **Tóm tắt cho Huy (VN):** chiêu không chèn vào thanh đồ nữa; thanh chiêu riêng góc phải chọn bằng Z X C V, cuộn chuột đi liền ô đồ 1–9 rồi tới các ô chiêu, bấm 1–9 về ô đồ (D125).

- **Status:** ready · **Branch:** `feat/P-045-skill-bar-keys` · **Decisions:** D125 (replaces the "R inserts techniques into the hotbar" part of D44/D48/D49)

## Today
`ClientSkills` has one key (R, `key.permahearts.technique`) that toggles technique mode. In the mode `ModeHotbarHud` replaces the vanilla hotbar: techniques take cells 0..N-1, items shift right. `MouseScrollMixin` and `MinecraftInputMixin.permahearts$numberKeyCell` route the wheel / number keys to `ClientSkills.setCell`. `SkillBarHud` draws a view-only bar at the bottom right (key label "R" + techniques + cooldowns). Pure maths in `technique/SkillBarLogic.java` (JUnit `SkillBarLogicTest`), client test `SkillBarClientTest`.

## New behaviour
1. **No more technique mode in the hotbar.** Remove the R key and the hotbar replacement (`ModeHotbarHud` goes; vanilla always draws the hotbar). Delete dead code, don't leave it behind a flag.
2. **Four key mappings** in the mod's key category (yellow, D78): "Technique slot 1..4", defaults **Z, X, C, V** (26.3 uses SDL scancodes, see the KEY_R comment in `ClientSkills`: check `InputConstants` with javap, add the names to 02-findings.md). Lang keys `key.permahearts.slot_1..4`. Pressing a key selects that technique slot if it holds a technique; an empty or locked slot does nothing. Pressing it again while already selected keeps it selected.
3. **Selection state** (client): either a hotbar slot (vanilla) or a technique slot. While a technique slot is selected: the held item is hidden and clicks are intercepted exactly as in today's mode (right hold = charge, left = throw at full power, no item use / block breaking); the vanilla hotbar draws **without** its selection frame; the right bar draws the **gold** selection frame (D49) on that slot. The real inventory selected slot stays where it was (don't send slot changes for technique selection).
4. **Scroll** is one ring: hotbar 0..8, then the **filled** technique slots in slot order, then back to hotbar 0. Scrolling down from hotbar 8 → first filled technique; up from hotbar 0 → last filled technique; up from the first technique → hotbar 8. No filled techniques → plain vanilla scroll. Put the ring maths in `SkillBarLogic` (pure, JUnit), e.g. `scrollRing(position, delta, filledCount)`.
5. **Number keys 1–9** always go back to that hotbar slot (vanilla behaviour, plus clearing the technique selection). Opening a screen, dying or the selected technique slot becoming empty (cleared in the Techniques tab) → back to the hotbar.
6. **Right bar labels**: each technique cell shows its own key's current name (from the key mapping, so rebinding updates it; flat label as today) instead of one "R" label. Only unlocked slots are drawn, as today.
7. Anything that read `ClientSkills.mode()` / cells (casting, charge bar, D85 Illusion slot-1 job, Techniques tab hints, help text, lang strings mentioning R or "technique mode") must keep working with the new selection. Grep for `mode()`, `cell(`, `technique mode`, `"R"`.

## Tests
- JUnit: ring scroll (down/up, wrap both ends, 0..4 filled techniques), key → slot (empty slot ignored).
- Update `SkillBarClientTest` (and other client tests that pressed R / used cells: `GolemClientTest`, `FlightBladeClientTest`, …) to select with the slot key or scroll, cast, check the hotbar is vanilla, and that 1–9 returns to items. Client tests must stay muted.

## Verify
`./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest` green (JDK 25; known flaky game tests may fail once: rerun).

## Commit
Branch `feat/P-045-dev` from `feat/P-045-skill-bar-keys`, commit each part, ff-merge back. Do not push. English commits with the Co-Authored-By line. Add new API names to `docs/system/02-findings.md`.
