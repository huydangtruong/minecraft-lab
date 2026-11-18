# P-053 — Soul Grasp: tame mobs with a light tether

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** chiêu thứ 16 Soul Grasp (D134). Giữ chuột phải nối sợi sáng tới mob để thu phục; chuột trái mở danh sách mob với các nút Follow / Đứng yên / Canh gác / Gọi về / Từ bỏ. Lv 1 bắt 1 con tối đa 10 tim, Lv 20 bắt 11 con tối đa 57,5 tim.

- **Status:** merged
- **Mod / decision:** `immortal-cultivation`, D134, D135, D136 (also D60, D75, D79, D114) · **Branch:** `feat/P-053-soul-grasp` · **Depends on:** none

## Goal
Add technique `GRASP("grasp", "Soul Grasp", 120, …)`: a held right click tethers a looked-at mob and tames it when the charge completes. A left click opens a list screen to manage the tamed mobs.

## Rules (pure Java, `technique/GraspRules.java`, JUnit)
Level `L` in 1..20:
- `maxTameHealth(L) = 20 + 5·(L−1)` HP (20 at Lv 1, 115 at Lv 20).
- `bonusHealth(L) = 5 + 2·(L−1)` HP (5 at Lv 1, 43 at Lv 20).
- `maxMobs(L) = 1 + L/2` (integer division: Lv 1 = 1, Lv 2 = 2, Lv 3 = 2, Lv 20 = 11).
- `chargeSeconds(mobMaxHp) = clamp(mobMaxHp / 20, 1, 5)` (hearts ÷ 10).
- Limits: select range 16 blocks; break at 24 blocks; break after 20 ticks without line of sight; follow teleport at > 12 blocks; guard radius 8; release peace 60 ticks; cooldown 120 s at every level, started **only on success**.
- `hearts(hp) = ceil(hp / 2)` for the list.
- Eligibility, from facts the glue passes in (no Minecraft types): a refusal reason enum `OK | TOO_STRONG(levelNeeded) | FULL | NOT_TAMEABLE`. Not tameable: players, bosses (God Slayer, quest chieftain tag, Wither, Ender Dragon, Warden), Crimson Master NPC, `OwnedSummon`s, Illusion clones, mobs already owned (any `OwnableEntity` with an owner, or another player's grasped mob), wandering traders (villagers **are** tameable, D135), water mobs (`WaterAnimal`, fish, Guardian, Elder Guardian), Slime and Magma Cube.

## Behaviour
1. **Aim:** while Soul Grasp is the selected slot, the mob under the crosshair within 16 blocks gets a faint yellow outline (client only). Invalid targets get a red outline, and pressing right click shows the reason as an overlay line (e.g. "Too strong (needs Lv 5)", "You already hold 3 mobs", "This mob cannot be tamed").
2. **Charge:** holding right click on a valid target sends a start packet. The server checks again and keeps the tether in per-player state. Each tick it checks range, line of sight and target alive. The mob keeps its AI; getting hit does **not** cancel. The client draws a gold-white beam from the hand to the mob and an open hand of light around the mob that closes with progress. A rising tone plays, and the centre charge bar is reused. The player is slowed like drawing a bow and cannot cast other techniques. Releasing early, or any break condition, cancels with no cooldown.
3. **Tame on completion:** the server re-checks eligibility and slots, then:
   - stores owner data on the mob: attachment `GraspedMob { owner UUID, number, mode, guardPos, bonusApplied }`;
   - adds `bonusHealth(L)` as an attribute modifier `permahearts:grasp_bonus` (not a base change, so release can remove it cleanly) and heals to full;
   - sets the custom name "<Type> <n>" in **yellow**, always visible;
   - sets `setPersistenceRequired()`; clears the target; replaces the target goals (below);
   - starts the 120 s cooldown; plays the hand-closing burst and a lock sound.
4. **Number:** per-player counter per entity type, saved in the player attachment, never reused ("Skeleton 7" = the 7th skeleton this player tamed).
5. **AI of a grasped mob** (server glue, `technique/grasp/GraspGoals.java`): remove target-selector goals that target players or the owner's allies, and add:
   - owner-hurt-by / owner-hurt target goals (reuse `DefendOwnerGoal` where it fits);
   - nearest hostile `Monster` within 16 blocks in Guard mode;
   - the Death Match opponent via `OwnedSummon.isMatchOpponent`.
   Movement goals by mode:
   - **Follow:** like a wolf; teleport next to the owner when more than 12 blocks away in the same dimension.
   - **Stay:** hold position and still fight what comes close.
   - **Guard:** patrol within 8 blocks of `guardPos` (set when the mode is chosen).
   The mob never targets its owner, never burns in sunlight, and never despawns. Its own attack stays vanilla (skeletons shoot, blazes and ghasts shoot fireballs, creepers swell and explode next to the target, which kills the creeper).
6. **Peace with golems and wolves:** Iron Golem, Snow Golem and wolves never target a grasped mob. Do this with a predicate check in their target selection (mixin or goal wrap). Grep how Violet Guardian already avoids being hit.
7. **Kill credit:** kills by a grasped mob give the owner HeartXP like the other summons (follow the `OwnedSummon` kill-credit path).
8. **Level up:** when the player's Soul Grasp level changes, update the bonus modifier on every loaded grasped mob at once. Unloaded mobs get it on `ENTITY_LOAD` (compare `bonusApplied` with the owner's current level; the owner's level is stored in the player record, so it works when the owner is offline).
9. **Death:** remove the mob from the owner's list (freeing the slot) and send the owner one chat line "Your Skeleton 7 died". If the owner is offline, remove it silently.
10. **Release:** remove the attachment, the bonus modifier (clamp health), the name and persistence, and restore vanilla goals (simplest: re-run `registerGoals` or save/reload the entity, whichever is verified to work). The mob ignores the ex-owner for 60 ticks, then behaves as normal.
11. **Player record** (in `TechniqueAttachment` or a new attachment, codec fields under the `permahearts` namespace): a list of `{ uuid, typeId, number, mode, dimension, lastPos }` plus the counters. `lastPos` and `dimension` are refreshed every second from loaded mobs.

## Passive mobs, villagers and gear (D135)
- **Attack for non-fighters:** when a villager or a passive animal (`Animal`, or any mob without an `ATTACK_DAMAGE` attribute) is grasped, give it an attack: villager **3**, passive animals **2** damage (`GraspRules`). If the attribute is missing on the type, add it through a transient attribute path that is verified on 26.3 (javap `AttributeMap` / `AttributeSupplier`), and add a melee attack goal. Their vanilla panic/flee goals must not override fighting. Remove all of this on release.
- **Gear pickup:** a grasped mob picks up armor, weapons and shields **thrown by its owner** (`ItemEntity` thrower = owner). It equips them in the right slot, swaps for a better item and drops the old one. Never pick up other items. Picked gear drops at 100% on death. Mark each given item (a custom data flag or a list in `GraspedMob`) so release can tell it from the mob's own spawn gear.
- **Stats work everywhere:** armor and sword modifiers apply through equipment attributes, and the attack uses `ATTACK_DAMAGE`. Mobs without an armor render layer (sheep, cow, villager…) show no armor; that is accepted. The list tooltip lists the gear.
- **Release:** drop every owner-given item, keep the mob's own spawn gear, and set `setCanPickUpLoot(false)` for good (also for zombies that could before).
- Tests: a grasped sheep damages a zombie for 2 and a villager for 3; a zombie picks up an iron chestplate thrown by its owner but not one thrown by another player; release drops the chestplate and the mob does not pick it up again.

## List screen (left click), `client/GraspScreen.java`, modelled on `WarpScreen`
Each row shows:
- the mob's icon (spawn-egg item or a small entity render);
- its name in yellow;
- **under the name, one stats line** (D136), each item with its icon **before** the number:
  - heart icon + hearts (`ceil(hp / 2)`), hover tooltip "Health 14 / 25 (bonus +5)";
  - armor icon (vanilla armor sprite) + the mob's `ARMOR` attribute value, gear included;
  - sword icon + the mob's `ATTACK_DAMAGE` value, held weapon included. A mob that only shoots (skeleton, ghast…) shows its melee value; that is fine.

  The gear tooltip (D135) shows on hover over the armor and sword numbers;
- distance and dimension, or a dim "far away" if not loaded;
- the current mode, highlighted.

Buttons per row: **Follow**, **Stay**, **Guard**, **Call** (teleport to the owner, same dimension only, 10 s per-mob cooldown), **Release** (asks for confirmation on a second click). The header shows "3 / 5 mobs". The screen refreshes from a server snapshot packet while open (once per second).

## Visuals and text
- **Icon:** an open hand half-closing as if grabbing, with rays of light coming out of it. Same size and style as the other technique sprites (`textures/gui/sprites/...`). Gold and white, so it does not clash with Cosmic Warping's purple.
- Techniques tab text (short, like the others) and a detail-page stats list: catch up to N hearts, +X hearts, M mobs, cooldown 120 s.
- Technique list group **Allies**, after Illusion (D114).
- Lang keys under `permahearts`. Update `docs/mods/immortal-cultivation/techniques-levels.md` (cooldown line + level table) in Vietnamese.

## Files (start here, open others only as needed)
| File | Change |
|---|---|
| `technique/Technique.java` | add `GRASP` (allies group order) |
| `technique/GraspRules.java` (new) | all numbers and eligibility above |
| `technique/grasp/*` (new) | tether state, tame/release, `GraspedMob` attachment, goals, events |
| `technique/TechniqueRules.java`, `TechniqueCasts.java`, `TechniqueNet.java` | hold/charge path like `WARP`, packets (start, cancel, list snapshot, mode/call/release actions) |
| `client/ClientSkills.java`, `FocusParticles.java`, new beam/outline renderer, `GraspScreen.java`, `TechniquesTab.java` | aim outline, beam + hand, left-click screen, tab text |
| golem / snow golem / wolf target mixin | ignore grasped mobs |

## Tests
- JUnit `GraspRulesTest`: health cap, bonus, slots at Lv 1/2/3/19/20; charge seconds (10, 20, 40, 100, 300 HP); hearts rounding; eligibility reasons including FULL and TOO_STRONG level needed.
- Server game tests:
  1. Tame a zombie: yellow name "Zombie 1", max HP 25, persistent; it does not target the owner.
  2. Tame fails for a 40 HP mob at Lv 1 and when slots are full.
  3. Cancel when the mob goes beyond 24 blocks; no cooldown.
  4. Grasped zombie attacks a mob the owner hit.
  5. An Iron Golem ignores a grasped zombie.
  6. Level up updates the bonus.
  7. Death frees the slot and the owner gets the message.
  8. Release removes the name and bonus and the mob targets players again after 60 ticks.
  9. Follow teleports at > 12 blocks; Guard stays within 8 blocks.
- Client screenshot test: list screen with 2 mobs (hearts number, tooltip) and the aim outline + beam mid-charge. Keep client tests muted.

## Verify
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :immortal-cultivation:build
./gradlew :immortal-cultivation:runClientGameTest
```
Known flaky tests (farm-seeds, golem guardian, thunder piglin) may fail once: rerun.

## Commit
Branch `feat/P-053-dev` from `feat/P-053-soul-grasp`. Commit each part (rules, tame/tether, AI, screen, visuals), then ff-merge back. Do not push. Commits in English with the Co-Authored-By line. Add new API names to `docs/system/02-findings.md`.

## Done when
- [ ] All rules and behaviours above work; JUnit, server and client game tests green.
- [ ] Old saves load (no namespace changes; new codec fields optional with defaults).
- [ ] techniques-levels.md and the Techniques tab describe Soul Grasp.

## Risks / ask first
- **Peaceful difficulty** removes hostile mobs, grasped ones included. Accept it and drop them from the list with the death message, unless a cheap hook keeps them. Report which.
- Flying mobs (Ghast, Phantom, Blaze, Vex) follow poorly. Use flying move control where the mob has it. If one type breaks badly, exclude it and report; do not spend long on it.
- Enderman teleports away on its own, and creepers die when they explode. Both are fine (vanilla behaviour); just make sure the list stays correct.
- Restoring vanilla goals on release must be verified (javap the goal registration names on 26.3, see 00-environment.md).
