# P-066 — Life Stealer bats: lift and drain over 2 s

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Life Stealer có hoạt cảnh mới (D180): 4 con dơi bay tới từ 4 phía, cắn và nhấc mục tiêu lên 2 ô, hút máu dần trong 2 giây (10 lượt, mỗi 0,2 giây), rồi bay ra 4 phía biến mất, mục tiêu rơi xuống.

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D180 (read it first; it changes D158/D162, keeps D159; the amount is D181: 36% + 2% per level) · **Branch:** `feat/P-066-stealer-bats` · **Depends on:** none

## Goal
Replace the instant drain of Life Stealer with a 2-second bat drain, exactly as D180 in `docs/system/01-decisions.md`.

## Behaviour
- Aim, range 16, target filter, total amount (`StealerRules.amount`) and the 24 s cooldown (from the cast) stay as they are.
- **Fly-in (~10 ticks):** 4 bats appear ~3 blocks away on the 4 sides (N/E/S/W of the target, around its upper body) and fly in to it.
- **Lift:** when they arrive, the target is raised over ~5 ticks to 2 blocks above the ground it stood on (less if a block above is in the way; never into a block), then held there: no gravity, no movement, position kept every tick (mobs and players, players included in a Death Match). Bats stay on the 4 sides flapping, as if biting.
- **Drain:** 10 pulses, one every 4 ticks (0.2 s), each `amount / 10` of MAGIC damage via `TechniqueDamage`; the caster heals what each pulse really took. Vanilla invulnerability frames (10 ticks) would block pulses 4 ticks apart: reset the target's `invulnerableTime` before each pulse, as other multi-hit techniques do if any; look first. Small blood splash per pulse; the blood stream to the caster (`Stream`) runs for the whole 2 s.
- **End (after the 10th pulse):** bats fly out to the 4 sides (~10 ticks) and vanish; the target gets its gravity back and falls (2 blocks: no fall damage in vanilla); then Bleeding + StealerSlow 2 s as now (D159).
- Target dies mid-drain: bats fly out at once. Caster dies, logs out or changes dimension: stop, release the target. Target leaves the level or is removed: stop.
- Bosses (`EnderDragon`, `WitherBoss`, and the mod's bosses; see how other techniques skip lifting/pulling bosses, e.g. Grasp) are not lifted; bats still come, pulses still drain.
- Bats are visual only: no hitbox, no AI, no drops, no sound spam, never saved (no orphans after a restart or crash). Choose the simplest safe way (client-rendered bat models driven by a packet, or server `Bat` entities flagged not-saved, invulnerable, no AI, no collisions) and say which in the report.
- `/technique cast stealer <lv>` keeps working.

## Pure Java
Add to `StealerRules`: `PULSES = 10`, `PULSE_TICKS = 4`, `pulse(level, maxHealth) = amount / PULSES`, fly-in / fly-out ticks, lift height `2`. JUnit in the existing rules test.

## Tests (scope: medium, one technique area)
- Test scope checklist (docs/workflow/team.md "Test theo phạm vi thay đổi"): areas = Life Stealer only; change type = medium (one feature in one area); commands = JUnit + `./gradlew :immortal-cultivation:runGameTest -Pgt='*:*stealer*'` (check the real area prefix). **No full `build`, no `runClientGameTest`** (release.sh runs every server test; Huy checks the look in game).
- Update existing Life Stealer game tests that expect an instant drain. New server game tests: total drained over 2 s equals the old amount (± rounding) and the caster healed the same; the target is 2 blocks up during the drain and back on the ground after; under a low ceiling it is lifted less and never inside a block; target killed mid-drain ends it cleanly; caster gone ends it and the target falls; no bat entities left afterwards (if server bats are used).

## Docs
`docs/system/02-findings.md` for any new API names; Life Stealer text in the mod spec under `docs/mods/immortal-cultivation/` if it describes the drain. Do not edit 03-status.

## Commit
Small coherent commits in English ending with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Work on `feat/P-066-stealer-bats` (or a dev branch merged into it). Never push `main`, never touch Huy's saves or profile, no install.sh / package.sh.

## Done when
- [ ] Fly-in, lift 2 blocks (ceiling-safe), 10 pulses in 2 s, fly-out, fall, then bleed + slow
- [ ] Death / logout / boss cases as above; no leftover bats
- [ ] JUnit + stealer game tests green; report ≤ 10 lines with the test level run

## Risks / ask first
- If holding a player in the air fights the client (rubber-banding), use the simplest stable way (e.g. teleport each tick with zero velocity) and say so.
