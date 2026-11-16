# P-043 — Fixes: boss Ghast reflected fireball, Notes tabs, clone seeds from chests, Quantum Leap cooldown

> **Tóm tắt cho Huy (VN):** trùm Ghast không chết ngay vì quả cầu dội lại (D119); panel Ghi chú bỏ 2 tab Cultivating thừa; phân thân làm vườn lấy hạt giống trong rương gần đó (D120); Quantum Leap hồi chiêu 40 s, −1 s mỗi level (D121).

- **Status:** ready · **Branch:** `feat/P-043-fixes` · **Decisions:** D119, D120, D121

## 1. Boss Ghast and reflected fireballs (D119)
Vanilla `Ghast.hurtServer` turns a reflected fireball (`isReflectedFireball`) into 1000 damage. For **our boss/chieftain Ghast only** (find how the mod marks it — `wanderer/` boss spawning, D35), skip that rule: the fireball does its normal damage (impact + explosion), with the boss armor rules as today. Use a mixin or an `ALLOW_DAMAGE`/hurt hook; check names with javap on 26.3. Game test: a reflected fireball (owner = player) hitting the boss Ghast leaves it alive; a plain Ghast still dies.

## 2. Notes panel shows two extra Cultivating tabs (bug)
When the Notes panel is open (P-042, `client/NotesTab.java` + `client/StatsPanel.java`), two extra tab buttons of the Cultivating panel are drawn. Find why (tabs/widgets added twice, or the Cultivating tab widgets not hidden when Notes is open) and fix. Extend `NotesClientTest` screenshot to confirm (muted client).

## 3. Clones fetch seeds from chests (D120)
Slot 1 has a hoe → farming job (D73/D84/D85). When a clone has no seeds and there are empty farmland blocks in its work area, it walks to the nearest chest/barrel within the work radius that holds seeds (wheat seeds, carrot, potato, beetroot seeds, melon seeds, pumpkin seeds), takes up to a stack, and plants them. Still no tilling of new soil. Don't take other items. Server game test: chest with seeds next to empty farmland, clone plants them.

## 4. Quantum Leap cooldown (D121)
`TechniqueRules.cooldownSeconds`: TELEPORT = 40 − (level − 1) (Lv 20 = 21 s). The stats line on the detail page shows the level's value. JUnit.

## 5. Summons attack the Death Match opponent (bug, added)
In a Death Match the owner's Warrior Angels do not attack the opponent player. Warrior Angel, Illusion clones and Violet Guardian golems must all target the owner's Death Match opponent (only that player, only during the match; outside a match they never attack players). Server game test for each.

## 6. Notes panel changes (D122, added)
Wrap long notes; no player name, text in the author's Cosmic Warping portal color; X is a real button; 32 characters max in both tabs; Shared/Private as side tabs like the Cultivating panel.

## Verify
`./gradlew :permahearts:build` green (JDK 25; known flaky game tests may fail once: rerun) and `./gradlew :permahearts:runClientGameTest` green (muted).

## Commit
Branch `feat/P-043-dev` from `feat/P-043-fixes`, commit each part, ff-merge back. Do not push. English commits with the Co-Authored-By line. No `__pycache__`.
