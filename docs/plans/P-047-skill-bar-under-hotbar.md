# P-047 — Skill bar peeking under the hotbar (experiment 3)

> **Tóm tắt cho Huy (VN):** nhánh thử thứ ba: thanh đồ nâng 4 px, thanh chiêu nằm giữa dưới thanh đồ, bình thường chỉ đè mép thanh đồ 8 px; chọn chiêu thì trượt lên hiện đủ (thanh đồ mờ 50%), rời chiêu thì trượt xuống (D128).

- **Status:** ready · **Branch:** `feat/P-047-skill-bar-under-hotbar` (from `feat/P-045-skill-bar-keys`: input of D125 and /testmode of D126; NOT the P-046 layout) · **Decisions:** D128

## Change (HUD only; input stays exactly as P-045)
1. **Raise the vanilla bottom HUD by 4 px (GUI pixels).** The hotbar and everything stacked on it move up 4: hotbar, offhand slot, attack indicator, health, armor, food, air, mount health, mount jump bar, experience bar/level (and the locator/contextual info bar), held item name, action-bar/overlay message, and the mod's own bottom HUD that is placed relative to them (layered hearts, HeartXP bar on the left, the charge bar at `guiHeight - 62`, anything else grep finds using `guiHeight() -`). Preferred way: for each relevant `VanillaHudElements` entry, `HudElementRegistry.replaceElement` with a wrapper that pushes a pose translate of −4 in y and calls the vanilla element (check the 26.3 `GuiGraphicsExtractor` pose API with javap; P-045's `HotbarSelectionMixin` shows how the hotbar sprite is reached). Chat, boss bars, titles, scoreboard, subtitles stay where they are.
2. **Skill bar under the hotbar, centred.** Remove the bottom-right corner skill bar of P-045 (keep its text lines — Cloud Travel / Aqua Marine time, selected technique name — at the bottom right corner where they are, or just above where the bar was). The new bar: filled-or-unlocked cells exactly as P-045 shows today, slot 1 leftmost, centred horizontally on the screen, cells **20 px** with 2 px gap (like P-046: icon 16 px, cooldown overlay + seconds, gold `hud/hotbar_selection` frame on the selected cell), drawn **after** (on top of) the hotbar.
3. **Two resting positions** (the hotbar's bottom edge is now at `guiHeight - 4`):
   - **Lowered** (a hotbar slot is selected): the bar overlaps the hotbar by **8 px** (top = `guiHeight - 12`), so 12 px of each cell show and the rest is below the screen edge.
   - **Raised** (a technique slot is selected): the whole cell is visible with its bottom 1 px above the screen edge (top = `guiHeight - 21`), covering part of the hotbar's lower half.
3b. **Hotbar fade (owner change):** while raised the hotbar (sprite, items, offhand) looks ~50% opaque, fading with the slide (100% lowered, 50% raised). 26.3 items cannot take alpha, so a translucent black layer (alpha 0..0.5 following the slide) is drawn over the hotbar and offhand area.
4. **Animation:** slide between the two positions over ~150 ms with ease-out, driven by real frame time (partial ticks / `Util.getMillis()`), both ways; interrupting mid-slide reverses smoothly from where it is. Pure easing maths in `SkillBarLogic` (JUnit), e.g. `slideOffset(progress)`.
5. **Key labels:** same as P-046 — small square in the cell's top-left corner, solid black background (~`0xE0000000`), key name drawn at ~0.6 scale, follows rebinding; visible in both positions (it is in the top part of the cell).
6. No skill bar at all when techniques are not usable (world switch off, creative, spectator, no unlocked slot): then the vanilla HUD is still raised 4 px — keep it simple and always raise (say in the report if you think otherwise).

## Tests
Update `SkillBarClientTest` and any client test that reads HUD positions. Take two screenshots with 4 techniques slotted: one with a hotbar slot selected (lowered) and one with a technique selected and on cooldown (raised, after the slide finished). Read them yourself and fix the layout until it matches the plan.

## Verify
`./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest` green (JDK 25; known flaky game tests may fail once: rerun).

## Commit
Branch `feat/P-047-dev` from `feat/P-047-skill-bar-under-hotbar`, commit, report (manager fast-forwards). Do not push. English commits with the Co-Authored-By line. Add new API names to `docs/system/02-findings.md`.
