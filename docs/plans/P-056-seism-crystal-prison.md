# P-056 — Seism and Crystal Prison (techniques 17 and 18)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** hai chiêu mới. Seism (hệ Đất): bấm nhanh thì tảng đá đập xuống điểm ngắm; giữ 1 s thì 3 đợt sóng gai đá quanh mình. Crystal Prison: nhốt mục tiêu (chuột trái) hoặc chính mình (chuột phải) trong lồng crystal xanh ngọc, mob trong lồng mất khả năng tấn công (D142, D143).

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D142, D143 (also D114 groups, D99 stun, D86 Thunder aim) · **Branch:** `feat/P-056-seism-crystal` · **Depends on:** none

Units: 1 heart = 2 HP. All rules go in pure Java (`technique/SeismRules.java`, `technique/CrystalRules.java`) with JUnit. Glue stays thin. Follow how existing techniques register: `Technique` enum, `TechniqueRules`, `TechniqueCasts`, `TechniqueNet`, `ClientSkills`, `TechniquesTab`, `FocusParticles`, sprite script `tools/make_technique_sprites.py`, lang.

## A. Seism (`SEISM("seism", "Seism", 28, …)`, Attack group after Thunder Strike)
**Input:** right click on the selected slot.
- Released before 1 s → **Seismic Impact**, cast on release.
- Held to 1 s (show the centre charge bar) → **Seismic Wave**, at Lv ≥ 3 only. At Lv 1–2 the bar does not appear; holding then releasing casts Impact, and the hold hint reads "Seismic Wave unlocks at Lv 3".

**Cooldown:** 28 s, the same for both modes. It starts when either mode fires. Left click does nothing special (or Impact; pick the simpler path and say which).

**Impact**
- Aim point = crosshair ray hit on a block, max **48** blocks. When aiming at the sky, use the point at 48 blocks and drop it straight to the ground.
- A boulder appears 5 blocks above the point. Use a display entity scaling from small to full (a cobblestone/stone block display), not a real block. During **0.2 s (4 ticks)** the impact point keeps following the crosshair (client sends the updated point each tick; the server clamps it to 48 blocks).
- On impact: damage `impactHearts(L) = 5 + L/2` (integer division: Lv 1 = 5, Lv 2 = 6, Lv 20 = 15) to **hostile mobs only** (`Enemy`, not bosses) within radius **3**.
- Effects: dust and stone particles, a heavy thud, a small screen shake for players within 8 blocks. No block damage.

**Wave**
- After the 1 s hold, three pulses at t = 0, 1, 2 s, centred on the caster's **current** position. Radius 3, 4, 5.
- Each pulse:
  - pops 2-block pointed dripstone visuals (block displays rising and sinking within ~0.6 s, no real blocks) on rings out to the radius;
  - hits every `LivingEntity` in the radius except the caster: players, passive mobs, summons and grasped mobs included.
- Per hit, with `steps = max(0, (L − 3) / 2)`:
  - `waveHearts(L) = min(8, 2 + steps)`;
  - `waveLift(L) = 5 + steps` blocks (13 at Lv 19–20);
  - knockback 2 blocks horizontally, away from the caster.
- Height rule: `dy = floor(target.y − caster.y)`. For `dy ≥ 1`, hearts −dy and lift −dy (floor 0); knockback unchanged.
- Skip any target that is airborne (not on ground and not in water); that is how a launched mob is not hit again.
- Bosses (God Slayer, quest chieftains, Wither, Ender Dragon, Warden) take the damage only: no lift, no knockback.
- In a Death Match the opponent is hit like anyone else; outside one, other players are hit too (D142).
- Fall damage from the lift is normal vanilla.
- Lift velocity: compute the vertical speed that reaches `lift` blocks, with a JUnit-tested helper using gravity 0.08 and drag 0.98.
- During the 3 s of pulses the caster has an `ADD_MULTIPLIED_TOTAL −0.8` movement speed modifier (`permahearts:seism_slow`), so −80% of the current speed including potions and beacons. Remove it at the end, and also on death or logout.

**Text:** one short sentence per mode in the Techniques tab and the detail page. Example: "Tap: a boulder smashes where you aim." / "Hold 1 s (Lv 3+): three quake waves around you hit everything near."

## B. Crystal Prison (`CRYSTAL("crystal", "Crystal Prison", 48, …)`, Control/defense group after Undying Jutsu)
**Cooldown:** 48 s, the same for both buttons.

**Block:** a new block `permahearts:crystal_prison`:
- translucent, turquoise (blue-green) tint, like stained glass;
- light level 7;
- no drops, no item form, not in the creative tab;
- a state property `tier` 1..20. `getDestroyProgress` uses hardness `crystalHardness(tier) = 0.3 + (3 − 0.3)·(tier−1)/19`, and `getExplosionResistance` uses `crystalBlast(tier) = 0.3 + (10 − 0.3)·(tier−1)/19` (javap the 26.3 method names; put them in 02-findings).
- Mobs cannot path through it, arrows stop on it, anyone can break it.

**Cage geometry** (`CrystalRules.cage(origin)` returns the interior and shell positions; JUnit):
- The interior is 3×3×3 air, centred horizontally on the target's block X/Z.
- The floor face is at the target's feet block Y; the target is lifted 1 block to stand on it. The floor rests on the ground below, which is not touched.
- Interior Y = floor+1..floor+3. Top face at floor+4.
- Each of the 6 faces is a 3×3 square of crystal: 54 blocks, no edges or corners.
- Every interior position becomes air (water, lava and blocks are removed; old blocks are lost, D143). Every shell position becomes crystal, whatever was there.
- Never replace unbreakable blocks (hardness < 0: bedrock, barrier, portals, end portal frame, command blocks); leave them as they are.
- A block entity in the way is removed, and its container contents drop (vanilla `Containers.dropContents` or equivalent).

**Left click:** ray from the crosshair, max **64** blocks, like Thunder Strike's aim (entity first, else the block hit).
- With an entity: the cage forms around it.
- Without one: an empty cage at the block hit.
- Collect the living entities whose bounding box overlaps the interior. Push out anything taller or wider than 3 blocks (bbHeight > 3 or bbWidth > 3) silently. Keep at most **2**; if more, keep 2 at random and push the rest out (teleport just outside the shell, on the side nearest them).

**Right click:** the cage forms around the **caster** plus the **nearest** other living entity within the 3×3×3 zone. Push out the others and the too-big ones the same way.

**Disarm** (`permahearts:crystal_disarm`, a timer like the Heaven Blast stun but movement stays allowed):
- No melee, no ranged attack, no breath, no spells: clear the target, block attack goals, block `performRangedAttack`, etc. Reuse the Stun hooks where possible.
- Duration `disarmSeconds(L) = 3 + 0.5·(L−1)` (12.5 s at Lv 20).
- Applies to hostile mobs and to the Death Match opponent.
- Never applies to the caster, to other players outside a Death Match (they are caged but keep attacking), or to allies (summons, grasped mobs, tamed pets).

**Lifetime:** `cageSeconds(L) = 8 + (L−1)` (27 s at Lv 20). Track the cage server-side (positions + expiry, saved so a restart still clears it). On expiry, remove the remaining crystal blocks; broken ones simply stay gone.

**Enderman:** while inside a live cage (or while disarmed), an enderman cannot teleport (mixin on its teleport methods).

**Text:** one short sentence per button.

## Tests
- JUnit:
  - Seism: `impactHearts` at Lv 1/2/3/20; `waveHearts`/`waveLift` at Lv 3/4/5/7/15/19/20; height reduction; the lift velocity helper reaches about the target height.
  - Crystal: hardness and blast at Lv 1/10/20; disarm and cage times; cage geometry has 27 interior and 54 shell positions, no edges or corners.
- Server game tests:
  1. Impact damages a zombie for 10 HP at Lv 1 and leaves a cow in the radius unharmed.
  2. Wave at Lv 3: a zombie and a cow on the ground both take 4 HP and are lifted; an airborne target is skipped; a target 1 block higher takes 2 HP less.
  3. Boss: a God Slayer takes Wave damage but does not move.
  4. Wave is unavailable at Lv 2.
  5. Seism cooldown is 28 s after either mode.
  6. Crystal left click on a zombie in water: the interior is air, there are 54 crystal blocks, the zombie does not attack a player in the cage for 3 s, and after 8 s the crystal is gone.
  7. 3 zombies in the zone: 2 are caged and 1 is pushed out. A ghast is pushed out.
  8. Right click: the caster and the nearest zombie are caged; the caster can attack.
  9. An enderman in a cage does not teleport.
  10. A crystal block at Lv 20 takes longer to break than at Lv 1.
- Client screenshot test (muted): an Impact boulder mid-fall, Wave spikes, a crystal cage at night with light, and the Techniques tab showing both techniques.

## Verify
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :immortal-cultivation:build
./gradlew :immortal-cultivation:runClientGameTest
```
LARGE change. Known flaky tests (farm-seeds, golem guardian, thunder piglin) may fail once: rerun.

## Docs
`docs/mods/immortal-cultivation/techniques-levels.md` (Vietnamese): cooldown line, a short section and a level table for each technique. New API names go in `02-findings.md`.

## Commit
Branch `feat/P-056-dev` from `feat/P-056-seism-crystal`. Commit Seism and Crystal Prison separately, then `git push . feat/P-056-dev:feat/P-056-seism-crystal`. Do not push to origin. Commits in English with the Co-Authored-By line.

## Risks / ask first
- Per-state hardness: if 26.3 cannot vary destroy time per state cleanly, fall back to 20 registered block variants, or to a block entity read in `getDestroyProgress`. Report which one you used.
- Boulder and spike visuals use display entities (javap `Display.BlockDisplay` names on 26.3). If they are too costly, use particles plus one block display, and report it.
