# P-061 — Solar Lance, Life Stealer, Soul Grasp sky beam

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Soul Grasp đổi hình: tia sáng từ trời chiếu xuống mục tiêu. Chiêu mới Solar Lance: tia laser từ tay, xa 256 ô, quét theo tâm ngắm trong 12 giây, gây sát thương tăng dần. Life Steal Blade được thay bằng Life Stealer: hút máu tức thì trong 16 ô, giữ level cũ.

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D156, D157, D158 · **Branch:** `feat/P-061-lance-stealer` · **Depends on:** P-060 (on main)

## Goal
Three technique changes in one batch. Read D86 (Thunder Strike targeting), D134 (Soul Grasp), D52/D60 (old Life Steal Blade, Bleeding) and the new D156–D158 in `docs/system/01-decisions.md`.

## 1. Soul Grasp — sky beam (D156)
- Only the visual while the player holds on a target changes: no tether from the hand. Instead a **vertical pillar of light from high in the sky straight down onto the target**, same gold colour family, like heavenly light (a few soft particles at the foot are fine).
- Everything else (hold time, rules, list screen, success/fail) stays exactly as is. Code: `client/ClientGrasp.java` and wherever the tether is drawn.
- Keep the old hand→target beam renderer code: Solar Lance reuses it.

## 2. Solar Lance — new technique (D157)
- Enum entry after `THUNDER`: id `lance`, name "Solar Lance", cooldown **45 s at every level**, Attack group (D114 order: Fireball, Thunder Strike, Solar Lance, Life Stealer). Slot/emerald costs like any other technique. New icon: a gold-orange spear of light (16×16, same style as the other icons).
- **Cast** like Thunder Strike: focus selected, **left or right click** starts it. No hold needed.
- **Beam**: instantly from the caster's hand to where the crosshair hits (first block or entity hit; max **256** blocks; nothing hit → end at 256). Look = the current Soul Grasp hand tether (the laser look Huy likes). The end point follows the crosshair every tick for the whole duration. Beam does not pass through blocks.
- **Duration 12 s**, then it stops and the 45 s cooldown starts. While it fires: a **progress bar that drains from full to empty** over 12 s (thin bar above the hotbar / near the skill bar, orange-gold; pick a spot that does not cover hearts or the skill bar). It cannot be cancelled early; switching focus or slots does not stop it. Death, dimension change or logout stops it (cooldown starts).
- **Damage**: every living entity on the beam line (caster excluded; the caster's own clones, guardians, angels and grasped mobs excluded, like other techniques) takes damage over time, applied every 10 ticks (half the per-second value each time):
  - per second = `(1 + 0.5 × (level − 1)) × 2^floor(seconds firing / 3)`
  - Lv 1: 1 → 2 → 4 → 8 per second over the 4 three-second stages. Lv 20: 10.5 → 21 → 42 → 84.
  - Use the existing technique damage source (`TechniqueDamage`) so Death Match and kill credit work like other techniques. Should not reset invulnerability frames in a way that blocks other damage — check how `TechniqueDamage` handles hurt time.
- **Hit look**: an entity on the beam is **tinted red** while hit, and throws a few **orange spark particles** each damage tick (small amount).
- Pure Java: `LanceRules` (damage per level and second, stage, duration, range) with JUnit.

## 3. Life Stealer — replaces Life Steal Blade (D158)
- Reuse enum entry `BLADE` and keep id **`blade`** so learned levels, skill slots and saves carry over. Rename to "Life Stealer". Cooldown **24 s at every level**.
- Remove the old blade behaviour (held blade, hits, Bleeding on hit). Keep the `Bleeding` effect *registered* (old saves may have entities with it) but nothing applies it any more.
- **Cast** like Thunder Strike: focus selected, **left or right click**. Target = the living entity the crosshair points at within **16** blocks (line of sight). No target → nothing happens, no cooldown.
- **Drain**: `amount = min(pct × target max health, cap)`
  - `pct = 20% + 2% × (level − 1)` → Lv 20 = 58%
  - `cap = 7 + 3 × (level − 1)` health points → Lv 1 = 7 (3.5 hearts), Lv 20 = 64
  - Damage the target by `amount` (technique damage source, ignore armor so the number holds), then heal the caster by the damage actually dealt (up to their max health).
- **Look**: red blood particles fly from the target to the caster (a stream over ~0.5 s), plus the same blood splash on the target that the old blade's hit made.
- New icon: vampire upper jaw with two fangs, same style as the other icons.
- Update the technique description text (short, ≤ 3 lines like D93).
- Pure Java: `StealerRules` (pct, cap, amount) with JUnit.

## Also
- `/admin` Techniques tab (P-060) reads the technique list, so Solar Lance must appear there with no extra work — check it.
- `/technique cast lance|blade <lv>` must work (server cast path).

## Tests
- JUnit: `LanceRulesTest`, `StealerRulesTest`.
- Server game tests: Lance hurts two zombies on one line and not one off the line, damage doubles after 3 s, stops after 12 s and starts a 45 s cooldown; Life Stealer drains a zombie by the formula, heals the caster, respects the cap on a high-health mob, no target = no cooldown; an old save value for `blade` level stays.
- Client game test: one screenshot each of the Soul Grasp sky beam, Solar Lance beam with the progress bar, Life Stealer particles, and the Techniques tab showing both new icons. Look at them yourself.
- Scope: large → `./gradlew :immortal-cultivation:build` + `runClientGameTest` once.

## Docs
- `docs/system/03-status.md` (On main, not released + In flight), mod spec in `docs/mods/immortal-cultivation/`, findings in `02-findings.md`.

## Done when
- [ ] Soul Grasp shows a sky pillar, nothing else changed
- [ ] Solar Lance: 256 range, follows aim, line damage with doubling stages, red tint + orange sparks, draining bar, 12 s then 45 s cooldown
- [ ] Life Stealer: 16 range, drain formula with cap, heals caster, blood stream + splash, 24 s cooldown, old Blade levels kept
- [ ] build + client game tests green, screenshots looked at

## Risks / ask first
- If red tint on any entity needs a renderer mixin that touches many mob renderers, use the vanilla hurt overlay re-triggered each damage tick instead and say so.
