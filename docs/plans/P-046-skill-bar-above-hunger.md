# P-046 — Skill bar above the hunger bar (experiment 2)

> **Tóm tắt cho Huy (VN):** nhánh thử thứ hai: thanh chiêu nằm ngay trên thanh đói, ô nhỏ cỡ hàng tim + giáp, nhãn phím là ô vuông nhỏ nền đen ở góc trên trái (D127).

- **Status:** ready · **Branch:** `feat/P-046-skill-bar-above-hunger` (from `feat/P-045-skill-bar-keys`, keeps all of D125/D126) · **Decisions:** D127

## Change (layout only, `client/SkillBarHud.java`)
Input (Z X C V keys, scroll ring, 1–9, selection rules) stays exactly as P-045. Only where and how the bar is drawn changes.
1. **Position:** right half above the vanilla food row. Vanilla: hotbar is 182 wide centred, food row is drawn right-aligned at `guiWidth/2 + 91`, y = `guiHeight - 39` (9 px icons); air bubbles one row above (`guiHeight - 49`) when shown; mount health replaces food when riding. Check the real numbers in 26.3 `Hud`/`Gui` with javap rather than trusting these. The bar's **right edge = the food row's right edge**, its **bottom sits just above the food row** (2 px gap), or above the air-bubble row when bubbles are visible (and above the mount-health rows when riding). Cells are laid out right to left or left to right — keep slot order left to right (slot 1 leftmost), right-aligned as a group.
2. **Cell size:** about the height of the left side's heart row + armor row together: **20 px** square (scale the technique icon to fit; cooldown overlay and seconds text as today, scaled). Gap 2 px. The gold selection frame (`hud/hotbar_selection`, D49) scaled to the cell.
3. **Key label:** a small square in the **top-left corner** of the cell with a **solid black background** (e.g. `0xE0000000`), the key name in a **smaller font** than P-045 (draw with a pose scale of ~0.5–0.66 so one letter fits a ~7×7 box; longer names like "LSHIFT" may widen the box, never past the cell). Follows rebinding as today.
4. **Other text that used the old bottom-right corner** (Cloud Travel / Aqua Marine time lines, the selected technique's name, anything else positioned from the old `top`): move it so it stays readable and does not overlap the new bar or the charge bar (`guiHeight - 62`, centred). Reasonable choice: put it just above the new bar, right-aligned with it. If the charge bar now overlaps the skill bar, move the charge bar up above the bar's top.
5. Nothing else moves; the HeartXP bar on the left stays.

## Tests
Update `SkillBarClientTest` (and any client test reading HUD positions) and take a screenshot in the client test with 4 techniques slotted, one selected, one on cooldown, so the manager can look at it: save it under the test's usual screenshot path and say where it is.

## Verify
`./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest` green (JDK 25; known flaky game tests may fail once: rerun).

## Commit
Branch `feat/P-046-dev` from `feat/P-046-skill-bar-above-hunger`, commit, then report (manager fast-forwards). Do not push. English commits with the Co-Authored-By line.
