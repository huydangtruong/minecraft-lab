# P-083 — Time Rewind (defense technique)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** chiêu mới "Time Rewind": bấm là thời gian trong hộp 16x16x16 quanh mình dừng lại và mất màu; lăn chuột để tua ngược/tua tới quái, mũi tên, khối bị phá; bấm lại (hoặc hết giờ) thì thời gian chạy tiếp từ chỗ đó. Máu, giáp, độ no của mình cũng được tua.

- **Status:** done on the branch (2026-11-27), not released
- **Mod / decision:** `immortal-cultivation`, D268 · **Branch:** `feat/P-083-time-rewind` (from `feat/P-082-purgatory-dungeon`) · **Depends on:** P-082 (docs only)

## Goal
A new technique `Technique.CHRONO` ("chrono", "Time Rewind", 140 s, not charged), in the defense group (D114). Behaviour is D268.

## Scope
- In: recording while the technique sits in a skill slot; the frozen box; scroll scrubbing; resume by recast or at the end of the duration; caster health/armor durability/food rewind; block history (changes before the cast); grayscale box (shader); a small timeline HUD; techniques tab text and icon; levels 1..20.
- Out: rewinding other players, the caster's position, block changes made during the stop; taking back drops; chest contents of blocks that come back (D270).

## Numbers (pure Java, `TechniqueRules`)
- `chronoDurationSeconds(level)` = 14 + 10·(l-1)/19 (14 → 24).
- `chronoRewindSeconds(level)` = 8 + 16·(l-1)/19 (8 → 24); the usable rewind is also capped by how long the technique has been in a slot (D268).
- Box: 16x16x16, x/z centred on the caster's feet, min y = feet y - 4. Fixed where cast.
- Scroll: one notch = 10 ticks; scroll down = back, up = forward (never past the cast tick). The shown time moves toward the target at 2x speed (smooth playback).
- Cooldown 140 s, fixed (`Technique` table; `TechniqueRules.cooldownSeconds` must not scale it), starts when time runs again.

## Design (verified names are in 02-findings; javap anything new)
1. **History (server, pure Java core `ChronoHistory`)**: a ring buffer per caster, one frame per tick, capped at `chronoRewindSeconds(level)` (max 24 s = 480 frames). Recording starts when CHRONO enters a slot, clears when it leaves. Each frame: for every non-player entity within 24 blocks (a box bigger than the zone so mobs walking in can walk back out) id → x, y, z, yRot, xRot, health (living), deltaMovement. Entity first-seen and removed events: on removal (death, despawn, pickup) keep its full save data (`Entity.saveWithoutId`-style; check the 26.3 name with javap) so it can be re-created when scrubbing back. Caster: health, food level + saturation, armor slot damage values. Block changes within 24 blocks of the caster: (pos, old state, new state, block entity data) with the tick — hook `Level.setBlock` on the server only for levels with an active recorder; keep it a cheap bounds check.
2. **Freeze**: a mixin on `TickRateManager.isEntityFrozen(Entity)` (exists in 26.3; used by client and server) returning true for entities inside an active zone, except players. Server sends the zone (min corner, size, end tick) to clients in range with a S2C payload, so the client freezes the same entities (arrows stop, no client prediction). Projectiles entering the zone stop at the edge because they are frozen as soon as they are inside.
3. **Hits during the stop**: damage applies at once; knockback is stored on the entity (vanilla adds to deltaMovement, which is not applied while frozen) and happens when time runs.
4. **Scrubbing**: client sends scroll notches (C2S) while its zone is active (cancel the normal scroll ring, D125, while scrubbing — `HotbarSelectionMixin` is where scroll is handled). Server moves a cursor tick in [castTick - usable, castTick]; each server tick it steps the shown tick toward the cursor and applies that frame: teleport entities (no interpolation reset needed: set pos + `setOldPosAndRot`-like; check names), set health, re-create entities removed after that tick, discard entities that first appeared after it (keep their data so scrubbing forward brings them back), apply block changes newer than that tick in reverse (forward scrub re-applies them). Caster stats set from the frame.
5. **Resume** (recast or duration end): the world continues from the shown tick: drop history newer than it, unfreeze, start the 140 s cooldown, and restart recording.
6. **Grayscale box (client)** — spike first (max half a day):
   - A post effect `assets/permahearts/post_effect/chrono.json` with a fragment shader that reads the main colour and **depth** buffers, rebuilds each pixel's world position and desaturates it when it is inside the box (when the camera is inside the box: everything grey, D268). Needs per-frame uniforms (box min/max relative to the camera, inverse view-projection). 26.3 `PostPass` takes uniforms from JSON (`Map<String, List<UniformValue>>`); find how to update them per frame (mixin/accessor on the pass's uniform buffer) and how depth is given to a pass. Look at `GameRenderer.getRequestedPostEffects()` / `END_OF_FRAME_POST_EFFECT` for how effects are requested.
   - Fallback if the spike fails: full-screen grey (static uniforms, like vanilla `creeper.json` with `post/color_convolve`) while the camera is inside, and a see-through grey box (translucent faces) drawn around the zone from outside. Tell the manager before falling back.
7. **HUD**: a thin timeline above the armor/food bars while a zone of the caster is active: the recorded span, a marker for the shown tick, "-3.5 s", and the time left of the duration.

## Files
| File | Where | Change |
|---|---|---|
| `technique/Technique.java` | enum | `CHRONO("chrono", "Time Rewind", 140, false)` in the defense group |
| `technique/TechniqueRules.java` | numbers | durations, box, notch size |
| `technique/chrono/` (new) | | `ChronoHistory` (pure), `ChronoZone`, `Chrono` (server glue), payloads |
| `mixin/` | | `TickRateManager` freeze hook, `Level.setBlock` recorder hook |
| `client/` | | scroll capture, HUD timeline, post effect / fallback box |
| `assets/permahearts/` | | lang, technique icon, post effect + shader |
| tests | `src/test`, `src/gametest` | see below |

## Tests
- JUnit `ChronoRulesTest`: numbers by level, usable rewind capped by slot time, ring buffer, cursor clamping.
- Game tests `'*:chrono*'`: a zombie walks, cast, scrub back 2 s → it is back where it was and its health too; a zombie killed before the cast comes back, its drops stay; a block broken before the cast comes back, one broken during the stop does not; an arrow fired into the zone stops; recast resumes and starts the 140 s cooldown; caster health/food/armor rewound, position not.
- Client test: one screenshot inside and one outside the box (grey), the HUD timeline.
- Checklist: areas = technique bar/list (`*SkillBarLogicTest`, `*TechniqueDataTest`, `'*:technique_game*'`) + new chrono; change type = new logic + shared enum → full `build` and `runClientGameTest`.

## Docs
02-findings (every new API), 03-status, `docs/mods/immortal-cultivation/spec.md` technique table, techniques-levels.md.

## Done when
- [x] Everything in D268 works in `runClient`, tests green, screenshots look grey in the box.

## Risks / ask first
- The grayscale box shader (step 6) is the main unknown.
- Entities with passengers, item frames, paintings: skip non-living entities that are not projectiles/items/TNT (they are not rewound).
- Players in the zone are never frozen (D268); a PvP target can walk out.

## Result (2026-11-27)
- Server: `technique/chrono/Chrono` (recorder per player, zone, smooth scrub, resume), `FrameRing` + `ChronoCursor` (pure, JUnit), `ChronoNet` payloads, `ChronoFreeze`. Mixins `ChronoFreezeMixin` (TickRateManager), `ChronoBlockMixin` (Level.setBlock), `LivingEntityChronoAccess`, `ArrowChronoAccess`.
- Client: `ClientChrono` (zones, wheel, freezes projectiles/items in a box), HUD timeline in `SkillBarHud`, `ChronoGrey` + `ChronoPostMixin` + accessors feed `post_effect/chrono.json` / `shaders/post/chrono.fsh` (ray vs box test with the depth buffer: the spike worked, no fallback).
- Manager choices: D270.
- Tests: `ChronoRulesTest`; `ChronoGameTest` (2: rewind of mob/kill/block/spawn/caster, freeze + hits + end); `ChronoClientTest` screenshots 130-133.

## D272 (2026-11-27, playtest)
- Dying mobs were revived in place and stayed lying and red on clients: now `Chrono.fresh` swaps in a copy with the same UUID; fire ticks are recorded per frame and the on-fire flag is set by hand. The caster is held (`Zone.casterHeld`, `ClientChrono.held` freezes the local player) and teleported along their recorded position, with velocity and fall distance; a fresh press of a movement key or a click sends `ChronoNet.Release`. Tests: both `'*:chrono*'` tests extended, `ChronoClientTest` checks the husks and the player position and walks free.
