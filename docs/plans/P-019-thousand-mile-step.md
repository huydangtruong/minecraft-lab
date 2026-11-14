# P-019 — Thousand Mile Step (long-range teleport technique)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Chiêu thứ 9 **Thiên Lý Bộ**: gồng như Fireball rồi dịch chuyển thật xa theo hướng nhìn, như ném ngọc Ender nhưng xa hơn: Lv 1 hết lực ~150 block (gấp 3), Lv 20 ~2500 block (gấp 50). Hồi 60 giây.

- **Status:** in-dev
- **Mod / decision:** `permahearts`, D58 · **Branch:** `feat/P-019-teleport` · **Depends on:** P-011…P-018 (merged)

## Already done (manager)

`Technique.TELEPORT` ("Thousand Mile Step", cooldown 60 s, `charged = true`, so the client already charges it with right click and casts at full charge with left click) and `TechniqueRules.PEARL_RANGE` (50), `teleportPearls(level)` (3 → 50), `teleportRange(level)` (150 → 2500 blocks), stat lines, JUnit. Placeholders: `TechniqueCasts.cast` returns false for TELEPORT, simple cases in `FocusParticles` and `TechniquesTab.description`.

## To do

1. **Cast** (`TechniqueCasts`, new `technique/ThousandMileStep.java`): distance `d = teleportRange(level) × charge` (charge has the same 20% floor as the orbs). Destination along the caster's look direction:
   - First raycast the look ray up to `d` against blocks **only through loaded chunks** (`ClipContext`, collider, no fluids). If it hits a block, land on the hit side like an ender pearl would (stand on top if it is a floor, or in front of a wall).
   - If nothing is hit in loaded chunks, take the point `d` blocks away **horizontally** along the look direction and land on its surface: load that chunk (synchronous is acceptable; it is one chunk), stand on the `MOTION_BLOCKING_NO_LEAVES` heightmap top (or the first safe 2-block air gap above solid ground).
   - Never land in the void, inside blocks, or in lava: if there is no safe spot, **refuse** (return false so no cooldown starts) and show an overlay message "No safe place to land".
   - Stay in the same dimension. No fall damage from the teleport itself (unlike a pearl, it costs no health); reset fall distance.
   - Teleport with `ServerPlayer#teleportTo(ServerLevel, x, y, z, Set.of(), yRot, xRot, false)` (see findings), dismount first. Sounds: vanilla enderman/chorus teleport at both ends; portal particles at both ends; a short particle trail along the first blocks of the path.
2. **Icon** (original pixel art, 16×16) `teleport` in `tools/make_technique_sprites.py`: e.g. a dark violet orb with a white streak or footsteps with motion lines; the script also writes `focus_teleport.png`. Item model + item definition + lang `item.permahearts.focus_teleport` = "Thousand Mile Step Focus", like the other focus items.
3. Particles in `FocusParticles` (portal swirl, as now, maybe a little stronger), description in `TechniquesTab` (keep it short; it wraps).
4. **Tests**: game tests: level 1 full charge on a long flat floor lands about `teleportRange(1)` away (allow the raycast/landing tolerance you choose, e.g. ±3 blocks) or at the first wall it hits; no safe ground → refused and no cooldown; cooldown starts on success. Client screenshot: focus item in hand and after a teleport.
5. Vietnamese playtest steps in `docs/workflow/playtest.md`, new API facts in `docs/system/02-findings.md`.

Verify: `./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest`.
