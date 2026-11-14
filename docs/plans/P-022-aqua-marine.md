# P-022 — Aqua Marine technique

> Tóm tắt (VN): chiêu thứ 12 **Aqua Marine**: thở dưới nước không mất bong bóng, bơi nhanh, đào dưới nước dễ. Hồi 120 giây; tồn tại 30 giây (Lv 1) → 70 giây (Lv 20); Lv 20 bơi nhanh gấp đôi hiệu ứng cá heo và đào gấp đôi phù phép Aqua Affinity.
> Decision: D64. Branch: dev on `feat/P-022-dev`, merged by the manager into `feat/P-022-aqua-marine`.

- **Status:** ready
- **Dev:** one Sonnet subagent

## Technique

New `Technique.AQUA`, id `aqua`, display name **"Aqua Marine"**. Instant cast, not charged, **cooldown 120 s**. It is a timed buff on the caster, like Cloud Travel (`technique/Flight.java`); reuse that pattern for the timer, the countdown and ending on death/logout.

While it is active:

1. **Breathing.** The air supply never goes down underwater and the bubbles stay full. This is like Water Breathing, but do not just add the vanilla effect if a cleaner hook exists. Restore full air on cast.
2. **Swimming speed.**
   - Measure the bonus from vanilla Dolphin's Grace in a game test. Use steady-state horizontal swim speed, sprint-swimming and plain swimming: with Dolphin's Grace minus without.
   - Aqua Marine gives `aquaSwimFactor(level) = linear(level, 1.0, 2.0)` times that bonus. Lv 1 equals Dolphin's Grace; Lv 20 is twice it.
   - Choose the mechanism with javap, for example the water slow-down used in `LivingEntity` travel and how `MobEffects.DOLPHINS_GRACE` changes it, or `Attributes.WATER_MOVEMENT_EFFICIENCY`. Note what you found in `02-findings.md`.
3. **Mining underwater.**
   - Vanilla Aqua Affinity lifts `Attributes.SUBMERGED_MINING_SPEED` from 0.2 to 1.0.
   - Aqua Marine sets it to `aquaMiningSpeed(level) = linear(level, 1.0, 2.0)`. Lv 1 equals Aqua Affinity; Lv 20 is twice it.
   - Use a transient attribute modifier, removed when the buff ends.
   - Leave the separate "not on ground" penalty alone.

**Numbers** go in `TechniqueRules` (pure Java, JUnit):
- `aquaSeconds(level) = linear(level, 30, 70)`, rounded the same way as `flightSeconds`.
- `aquaSwimFactor` and `aquaMiningSpeed`, as above.
- Stat lines on the detail page: Lasts, Swim speed (e.g. "×1.0 dolphin"), Underwater mining (e.g. "×1.0 aqua affinity").

**Look and wiring:**
- Bubble particles around the player while active, sparse.
- An original 16×16 icon in `tools/make_technique_sprites.py` (a blue-green wave or water drop with bubbles) and the `focus_aqua` copy.
- Lang entry and description: "Breathe, swim fast and dig freely underwater."
- Every exhaustive switch over `Technique`; grep `ANGEL` to find them.

## Tests

- **JUnit:** for the numbers.
- **Game test:**
  - A player underwater keeps full air for the duration.
  - Swim speed: at Lv 1 it is about the Dolphin's Grace speed; at Lv 20 the bonus over plain swimming is about twice the Dolphin's Grace bonus. Allow a tolerance.
  - `SUBMERGED_MINING_SPEED` is 1.0 at Lv 1 and 2.0 at Lv 20, and back to vanilla after the buff ends.
  - The buff ends after `aquaSeconds`; use a test hook.
- **Client test:** one screenshot of the technique's detail page or the player underwater with the buff.

## Done when

- `./gradlew :permahearts:build` is green.
- The client tests are green, and the dev has looked at the new screenshot.
- `playtest.md` (Vietnamese) has steps and `02-findings.md` has the API names.
- Report in ≤10 lines.
