# P-014 — Flight and Life Steal Blade techniques

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Thêm 2 chiêu: **Bay** (bay như Creative, 20→60 giây, nhanh hơn tới +100%, hồi 120 giây) và **Huyết Kiếm** (cầm kiếm nhuốm máu, chuột trái đánh trúng thì gây sát thương = cơ bản + chiêu + linh dược và hút máu đúng lượng đó; không hồi chiêu). Sau đó phát hành.

- **Status:** merged
- **Mod / decision:** `permahearts`, D52 · **Branch:** `feat/P-014-flight-blade` · **Depends on:** P-011, P-012 (merged); P-013 (Phase Walk sight fog) runs in parallel on another branch

## Already done (manager)

`Technique.FLIGHT` ("Flight", cooldown 120 s) and `Technique.BLADE` ("Life Steal Blade", cooldown 0) exist with numbers in `TechniqueRules`: `flightSeconds`, `flightSpeedBonus`, `bladeDamage`, `BLADE_ATTACK_SPEED`, stat lines, tests. Placeholders so the build passes: `TechniqueCasts.cast` returns false for FLIGHT/BLADE, `FocusParticles` and `TechniquesTab.description` have simple cases. The focus items are registered per technique (check `FocusItems`): FLIGHT and BLADE still need item models and textures.

## Flight

1. Cast (instant, any click, technique mode as today): for `flightSeconds(level)` the player can fly like creative: `abilities.mayfly = true`, flying speed = vanilla creative flying speed × (1 + `flightSpeedBonus(level)`), sync abilities to the client (`onUpdateAbilities`). Double-jump toggles flying as in creative; the cast also starts flying at once.
2. End: remove mayfly/flying and restore the normal flying speed, then **slow falling for 5 s** (vanilla `MobEffects.SLOW_FALLING`) so nobody dies from the drop. Never change players in creative or spectator (refuse the cast, or skip the ability changes, and restore exactly on end). Survive relog/death correctly (end flight on death; on join, end any flight that has expired; store the end tick in a saved attachment).
3. HUD: show the remaining flight seconds (e.g. in the right-side bar cell of Flight, or a small timer above the hotbar).
4. Tests: game test (flight on → mayfly true and speed multiplied; after the time → off + slow falling; creative player untouched); client screenshot flying.

## Life Steal Blade

1. While BLADE is the selected technique, the hand shows the blade focus item (original art: a blood-stained sword). **Left click on a living entity** within the player's entity interaction range = a blade hit: client sends the target id (new kind on the existing skill payload); server checks range, line of sight, the blade cooldown of `1 / BLADE_ATTACK_SPEED` s (track server-side), switch on.
2. Damage = player's **base** attack damage (the `ATTACK_DAMAGE` attribute value **without** the held item's modifiers: base value plus the elixir/cultivation modifiers; check how `StatsSync` splits base/gear/bonus) + `bladeDamage(level)`. Deal it with the techniques' damage path (`TechniqueDamage`, hits players even with PvP off), with knockback like a normal hit; then heal the attacker by the damage **dealt** (health actually removed, so armour reduces both).
3. No cooldown on the technique itself (cooldown 0): it is held like a weapon. Swing animation on hit; a red particle burst from the target toward the attacker.
4. Right click does nothing with the blade. Mining/attacking blocks with the blade selected does nothing (same as other techniques).
5. Tests: game test (hit a zombie: damage and heal amounts with/without the Strength elixir; held diamond sword does not add); client screenshot holding the blade.

## Both

- Icons (original pixel art, 16×16) for `flight` (feathered wings) and `blade` (a sword dripping blood) in `tools/make_technique_sprites.py`; that script also writes the focus item textures. Item models / item definitions for `focus_flight` and `focus_blade` like the existing ones.
- Real descriptions in `TechniquesTab.description`, real particles in `FocusParticles` (flight: soft white clouds/feathers; blade: dark red drops).
- Vietnamese playtest steps in `docs/workflow/playtest.md`, new API facts in `docs/system/02-findings.md`.
- Verify: `./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest`.
