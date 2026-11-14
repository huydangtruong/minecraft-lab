# P-023 — Aqua Marine: faster swim at level 20, clear underwater view

> Tóm tắt (VN): Aqua Marine Lv 20 bơi **1,2 ô/tick khi sprint** và **1,0 ô/tick khi không sprint** (Lv 1 vẫn bằng cá heo, 0,5); trong lúc chiêu chạy, nhìn dưới nước **rõ và sáng** ngay (không mờ).
> Decision: D69. Branch: dev on `feat/P-023-dev`, merges into `feat/P-023-aqua-faster-clear`.

- **Status:** ready
- **Dev:** one Sonnet subagent

## 1. Swim speed (D69)

Today `TechniqueRules.aquaTargetSpeed(level, sprinting)` = plain + `aquaSwimFactor(level)` × (dolphin − plain). Measured steady speeds:

| | Lv 1 | Lv 20 |
|---|---|---|
| Sprinting | 0.50 | 0.80 |
| Not sprinting | 0.50 | 0.90 |

New targets (blocks per tick, steady):

| | Lv 1 | Lv 20 |
|---|---|---|
| Sprinting | 0.50 (= Dolphin's Grace) | **1.20** |
| Not sprinting | 0.50 (= Dolphin's Grace) | **1.00** |

- `aquaTargetSpeed(level, sprinting) = linear(level, swimSpeed(sprinting, true), sprinting ? 1.2 : 1.0)`.
  - Name the two constants `AQUA_SPRINT_MAX` and `AQUA_SWIM_MAX`.
  - Drop `aquaSwimFactor` if nothing else uses it.
- `aquaWaterEfficiency` keeps its formula.
  - Check the result stays inside the `WATER_MOVEMENT_EFFICIENCY` attribute range: read the range with javap.
  - Check both cases: on the ground (efficiency ×1) and in open water (×0.5).
  - If a target cannot be reached inside that range, stop and report the highest reachable speed. Do not invent another mechanism.
- Detail page stat lines (`case AQUA`): replace "Swim speed ×N dolphin" with two lines:
  - "Swim speed" = target × 20, shown as `N blocks/s`;
  - "Sprint swim" = target × 20, shown as `N blocks/s`.
- Tests:
  - Update the JUnit tests in `TechniqueRulesTest`.
  - In `AquaGameTest`, measure the steady speed the way P-022 did. Expect Lv 20 sprint ≈ 1.2 and non-sprint ≈ 1.0, within 2%, in open water and near the floor.

## 2. Clear, bright view underwater (D69)

Huy: "xuống nước là nó mờ tịt". While Aqua Marine is active (client: `AquaMarine.END` is synced to the owner, as `SkillBarHud` reads it) and the camera is in water:

- **Clear:**
  - Full water vision at once, as with Conduit Power. In vanilla the water fog distance ramps up over ~30 s underwater (`LocalPlayer` water vision).
  - Find the 26.3 hook with javap, likely `LocalPlayer#getWaterVision`, and return 1.0 while the buff is active.
- **Bright:** the scene lit like Night Vision, only while the camera is in water.
  - Hook where the client asks for the night vision scale (`GameRenderer`/`LightTexture`, find with javap).
  - Do not add a real Night Vision effect: it would show on land and flicker in its last 10 s.
- Do not use a real Conduit Power effect: it would also speed up mining on land.
- Client-only code goes in `src/client`. Keep it thin.
- Add the names you find to `docs/system/02-findings.md`.
- Test:
  - Extend `AquaClientTest` with screenshots of the same deep-water view (night, a few blocks under) with and without the buff.
  - Look at both images yourself: with the buff it must be clearly brighter and see farther.

## Done when

- `./gradlew :permahearts:build` green, and the Aqua client test is green with the screenshots checked by you.
- `docs/workflow/playtest.md` (Vietnamese) Aqua steps updated: speed at Lv 20, clear view.
- `docs/mods/permahearts/techniques-levels.md` regenerated if a generator exists.
- `feat/P-023-dev` merged into `feat/P-023-aqua-faster-clear`. Do **not** push (the manager does).
- Report in ≤10 lines.
