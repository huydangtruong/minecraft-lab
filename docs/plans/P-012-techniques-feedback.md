# P-012 — Techniques: Huy's first playtest feedback

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Sửa theo lần chơi thử đầu: bảng Techniques (dòng cao hơn, cuộn dọc, ô chiêu dính trên cùng, trang chi tiết có Back + biểu tượng + tên + level, bấm trái ô = đổi chiêu với nút +, bấm phải = xoá ô), bấm R thì chiêu chèn vào đầu thanh đồ (bấm lại thì trả chỗ), gồng chiêu có hiệu ứng như cung + thanh lực giữa màn hình, sửa lỗi cầu lửa gồng tối đa không nổ.

- **Status:** in-dev
- **Mod / decision:** `permahearts`, D48 · **Branches:** `feat/P-012a-techniques-tab` (dev 1), `feat/P-012b-skill-hotbar` (dev 2), both from `main` · **Depends on:** P-011 (merged)

## Part 1 — Techniques tab (dev 1, `client/TechniquesTab.java`, `client/StatsPanel.java`, `tools/make_technique_sprites.py`)

1. **List rows taller** so the 16 px icons fit with room (about 20–22 px per row, icon centred vertically). The technique list **scrolls vertically** (mouse wheel over the list, a thin scrollbar on the right when it overflows; clip drawing to the list area). The **slot row (and the emerald box) stays at the top** and never scrolls.
2. **Detail page header**: one line `[Back] [icon] Name` (Back button first, on the left), and under the name the current level line (`Lv 3 / 20`). The slot row is **not** shown on the detail page. Below the header: the details (description, current → next numbers, Upgrade, Reset). Remove the old Back position.
3. **Slot clicks**:
   - **Left click** on an open slot → **edit mode** for that slot (gold outline). In edit mode the **emerald box becomes an ✕ button** (cancel edit mode) and the emerald text next to it is hidden. Left click on the same slot again, or ✕, leaves edit mode.
   - **Right click** on a slot → clears it (existing `CLEAR_SLOT`).
   - While the technique **in that slot is on cooldown**, both left and right click on it do nothing (show the remaining seconds as a tooltip / short text).
4. **Edit mode rows**: instead of the ✓, every technique with level ≥ 1 shows a **button with a + icon** at the row end (not shown for the technique already in the edited slot). Pressing + puts that technique in the edited slot; if it was in another slot, that other slot becomes **empty** (server `ASSIGN` already moves it; check and keep that rule). Example: slot 1 Fire, slot 2 Ice; edit slot 1, press + on Ice → slot 1 Ice, slot 2 empty. Edit mode ends after a successful +.
5. Tests: client test screenshots of the scrolled list, the detail header, edit mode with ✕ and + buttons, after a move (slot 2 empty); game test for the move rule if not already covered.

## Part 2 — skill hotbar mode, charge feedback, fire bug (dev 2, `client/ClientSkills.java`, `client/SkillBarHud.java`, `client/SkillWheelScreen.java`, client mixins, `technique/TechniqueOrb.java`, `technique/OrbBallistics.java`)

1. **Technique key (R) toggles "technique mode"** instead of cycling. The hold-R wheel is **removed** (and its key handling).
   - **Mode on**: the hotbar shows the N open, filled technique slots **first** (cells 1..N, technique icons with cooldown overlay), then the player's items **shifted right**: cell N+1 shows item slot 1, cell N+2 item slot 2, …; the last N item slots are hidden while the mode is on. Selecting a cell (scroll, number keys 1–9) works on the shifted view: cell < N selects that technique (focus item in hand, casting like now); cell ≥ N selects the real item slot `cell − N` (sync it to the server like a normal hotbar change).
   - **Mode off**: the normal hotbar. The selected **cell index stays the same** when toggling (e.g. cell 1 = sword → press R → cell 1 = technique 1 selected → press R → cell 1 = sword again).
   - Never move items in the inventory: it is a view and selection mapping only. Mode resets to off on death, when the world switch is off, or when no slot is filled.
   - Empty slots are skipped (N = filled open slots, in slot order).
2. **Right-side bar becomes display only**: first a flat **label** with the bound key's name (`KeyMapping#getTranslatedKeyMessage`, e.g. `R`), no sunken box (replace the ✕ cell), then one cell per filled slot with its icon and cooldown overlay + seconds (ready vs on cooldown at a glance). No selection frame there (the hotbar shows the selection now).
3. **Charging feedback like a bow**: while charging, the view zooms in like drawing a bow (field of view modifier like vanilla bow: find it with javap, e.g. the player FOV modifier that checks `isUsingItem`); the **power bar** is drawn at the **centre of the screen, just above the health/armour and hunger rows** (not next to the right bar).
4. **Bug — fully charged Flame Burst falls and does nothing**: reproduce (game test: full-charge fire orb on flat ground at max range must create the fire field). Likely causes: a lifetime cap discarding the orb before it lands, the custom hit sweep missing the ground at high speed, or the orb leaving loaded chunks. Fix so that **every end of an orb** (block hit, entity hit, lifetime end) triggers the impact effect at the last position. Same for Frost Orb.
5. Tests: game tests for the orb fix (both orbs, full charge); client screenshots: mode off/on hotbar (with a sword in slot 1 and an axe in slot 2 → after R: techniques first, sword in cell N+1), right label bar with cooldowns, charging zoom + power bar.

## Both

- Read `AGENTS.md`, `docs/workflow/team.md`; numbers only from `technique/TechniqueRules.java`; English in-game text; Vietnamese playtest steps in `docs/workflow/playtest.md`; new API facts in `docs/system/02-findings.md`.
- Commit and push after every working step; never push `main`; no install/package scripts.
- Verify: `./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest`.
