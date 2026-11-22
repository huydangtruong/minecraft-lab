# P-071 — Lance push, sheep icon, one-heart health bar

> **Tóm tắt cho Huy:** Solar Lance làm chậm và đẩy lùi nhẹ (D201); icon Soul Grasp là mặt cừu mắt xoáy (D202); thanh máu thành một trái tim + thanh tiến độ rộng bằng thanh giáp (D203). Ảnh chụp: `docs/plans/P-071-*.png`.

- **Status:** review
- **Mod / decision:** `immortal-cultivation`, D201-D203 · **Branch:** `feat/P-070-leap-distance` (same branch as P-070)

## Scope
- D201: every beam hit (each 10 ticks) adds Slowness II for 30 ticks and a horizontal push of 0.35 along the look direction plus 0.1 lift, both scaled by `1 - knockbackResistance` (push) so bosses with resistance 1 are slowed but not moved. Numbers in `LanceRules`.
- D202: new 16x16 `GRASP` art in `tools/make_technique_sprites.py`; writes `technique/grasp.png` and `item/focus_grasp.png`.
- D203: `HealthBarRules` (pure: 81 px width, layer pick, layer colours, fill, absorption strip, armour shift) and `LayeredHeartsHud` always draws heart (9) + gap (2) + bar (70, 7 high) at the vanilla health position; the armour bar is shifted to sit above the single row (also with absorption).
- Out: no health numbers, no regeneration hop, no damage blink.

## Tests
- Areas ticked: Solar Lance; Soul Grasp icon (art only); health HUD (shared client). Change type: new logic (lance, HUD pure rules) + visuals.
- `./gradlew :immortal-cultivation:test --tests '*LanceStealerRulesTest' --tests '*HealthBarRulesTest' --tests '*HeartLayersTest'`
- `./gradlew :immortal-cultivation:runGameTest -Pgt='*:lance*'` (new `theBeamSlowsWhatItCrossesAndPushesItAwayFromTheCaster` fails without D201).
- Client: `HealthBarClientTest` (screenshots 140-147), `LanceClientTest` (120-122).
