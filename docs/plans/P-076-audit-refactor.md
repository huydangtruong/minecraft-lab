# P-076 — Audit and refactor of immortal-cultivation

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** đọc lại toàn bộ code mod (server, client, test, tools), liệt kê lỗi thật, code trùng, code chết, class quá to. Refactor chỉ những bước giữ nguyên gameplay, có test che. Lỗi tìm được thì báo riêng, không sửa lén trong refactor.

- **Status:** draft (audit done, refactor steps waiting for approval)
- **Mod / decision:** `immortal-cultivation` · **Branch:** `feat/P-076-audit-refactor` · **Depends on:** release 0.44.0 (715401c)

## Goal

Make the code easier to change without changing what the player sees. Ids, `Identifier`s, codec fields, lang keys, game rule ids and the `permahearts` namespace stay frozen (D132).

## Audit method

- 377 Java files, about 48k lines, plus 13 Python sprite scripts.
- Three read-only passes: `technique/`, the rest of `src/main`, and `src/client` + tests + assets.
- Mechanical checks: grep for commented-out code and TODOs, payload classes, the lang keys and textures each reference, and the mixin json files against the mixin classes.
- I ran every `tools/*.py` script on a scratch copy of the assets and diffed the output against the committed PNGs.
- I checked the bugs marked "confirmed" myself, against the source and `javap`.

## Findings

### A. Real bugs (report to Huy; NOT fixed in this plan)

| # | Where | What breaks | Confidence |
|---|---|---|---|
| B1 | `wanderer/WandererMenu.quickMoveStack` (reward branch, ~l.321) + `RewardContainer.getItem/setItem` | Shift-clicking a done quest line's reward moves a copy of the "?" mystery box into the inventory. `slot.set(EMPTY)` then claims the real box through `Wanderer.claim` and throws it away. The player ends up with an inert "?" box and the line is marked claimed. No test covers shift-click. | confirmed |
| B2 | `client/StatsPanel.TrashSlot.isValidClickButton` (l.402) | Checks `button == 0 \|\| button == 1`. In 26.3 left is 1 and right is 3 (`InputConstants`, findings "Mouse buttons in screens"), so right click on the trash slot does nothing although the comment says "left or right". | confirmed (javap) |
| B3 | `client/TechniquesTab.clickDetail` (~l.543), `PassivesTab.click` (~l.436), `GraspScreen.mouseClicked` (~l.271) | Every mouse button counts, the middle and side buttons included: Upgrade, Reset, "+" (5 levels spent) and Release. | likely |
| B4 | `godslayer/GodSlayerHunt` frozen branch (l.254) | `tick()` runs every 5 ticks but `huntEndTick++` adds only 1, so `/boss freeze` slows the hunt timer to 4/5 speed instead of stopping it. Admin-only. | confirmed |
| B5 | Client static state | Nothing resets on disconnect: the only `DISCONNECT` hook is in the disabled `NotesTab`. Stale `ClientGrasp.held`, `GodSlayerWarningHud.endTick` (an absolute game time from the old world), `ClientSkills` charge and grace, and tab scroll/detail all carry into the next world. | likely |
| B6 | Server static state without a `SERVER_STOPPED` reset | `BloodMoon` bar, `lastWave` and `SIDES`; the `QuestBoss` bar; `DangerMobs.ELITES` (stale Mob refs). In singleplayer, opening a second world can reuse them. | maybe |
| B7 | `passive/CropOwners` | Owners are removed only when a player breaks the crop. A crop removed by water, a piston, an explosion or a villager leaves its owner behind, so a later crop at that spot gets the old owner's growth bonus. The map grows without limit. | likely |
| B8 | `client/GraspScreen.update` (l.47) | `scroll` is never clamped. With 12+ mobs, scroll to the bottom and release one: a blank row stays and the scroll thumb can overflow its track. | likely |
| B9 | `technique/ThousandMileStep` l.66-67 | `stopRiding()` runs before `teleportTo()`. When the teleport fails the player is dismounted with no cast. | confirmed (code order) |
| B10 | `technique/grasp/Grasp.callTo` l.740 | Uses `GraspGoals.freeSpotNear`, which has no ground or liquid check. Follow-teleport uses `SafeTeleport.spotOrNull` (D169/D170). A called mob can land in the air or in liquid near the player. | likely, low impact |
| B11 | Seism pulse, Repulse, Thunder, Fields (burn/freeze/heal) | They do not use the friend filter (`Lance.isFriend` / `TechniqueDamage.affectable`), so they hit the caster's own clones, guardian, angel and grasped mobs. The heal circle heals hostile mobs. PvP-off is checked only by Crystal Mayhem (D182). | maybe intended: ask Huy |
| B12 | `technique/warp/Warp.go` | The `yaw` parameter is unused, so the portal does not set facing. `Warp.freeSpot` has no lava check. | maybe |
| B13 | `technique/LifeStealer` | Sets `NoGravity` on the lifted target and that flag is saved. A crash mid-drain leaves the mob floating. | maybe |
| B14 | `treasure/DeadlyTreasure.start` | `/treasure start` while an event is active orphans the old chest and guards. Admin-only. | maybe |
| B15 | `technique/grasp/Grasp.handle` l.361 | Every action needs technique level ≥ 1, LIST and RELEASE included, so a player who reset Soul Grasp cannot release held mobs. | maybe |

### B. Dead or commented-out code

- The Notes client code in `StatsPanel` (about 60 commented lines) and `NotesNet.register()` in `ImmortalCultivation` are commented out **on purpose** (D217). No server work runs for Notes. Keep them.
- Unused: `Messages.questDone`, `Lance.beamEnd`, `Seism.activeVisuals`, `Grasp.clearMemory`, `GraspGoals.blockOf`, `DangerMobs.trackedElites`, the unused local `assign` in `TechniquesTab.drawList` and the unused args of `TechniquesTab.backRect`.
- Unused textures: `gui/sprites/technique/check.png` and `plus.png`. Nothing calls `sprite("check")` or `sprite("plus")`.
- No unused lang keys. All 172 keys are used directly or by dynamic keys: `item.*`, `effect.*`, `permahearts.stat.<id>`, `permahearts.structure.<id>`, `key.permahearts.slot_N`.
- `tools/__pycache__/make_heart_sprites.cpython-315.pyc` is committed and `.gitignore` has no `__pycache__/`.
- About 37 stale agent worktrees sit under `.claude/worktrees/` (not tracked; disk only).

### C. Duplicated logic

- **Sprite tools.** 10 scripts load `make_heart_sprites.py` through an `importlib` hack only to reuse `write_png`/`rgba`/`CLEAR`. Three more (`make_crystal_texture`, `make_grasp_sprite`, `make_trash_sprite`) carry their own PNG writer.
- **Sprite drift.** All scripts reproduce the committed PNGs byte for byte, except `make_technique_sprites.py`: it would overwrite the Life Stealer icon (`technique/blade.png`, `item/focus_blade.png`) with the old Life Steal Blade art, because P-061 changed the PNG and not the script. `lance.png` and `mayhem.png` have no generator.
- **Payload boilerplate.** 18 payloads in 12 classes repeat `new Type<>(Identifier.fromNamespaceAndPath(NAMESPACE, …))` plus `type()`. It is small and idiomatic, so it stays low priority.
- **Game test setup.** `survivor()` is copied in `FlightBladeGameTest` and `TechniqueEffectsGameTest`, and 19 classes call it across classes. `player()` has 3 copies, and `place`, `drop` and `cleanUp` several each. Profile names use `Math.random()`.
- **Client drawing.** The grey frame (3 copies), the inset box (3), the scroll thumb (6, two colours), button sprite plus label (6), hit test `in/over` (7) and scroll clamp (4) are copied across TechniquesTab, PassivesTab, AdminScreen, GraspScreen, WarpScreen, NotesTab and StatsPanel. In `ClientSkills` the cooldown expression appears twice and three branches of `onAttackClick` do the same thing.
- **Server helpers.** UUID-string parse with try/catch has 6 copies (Wanderer, QuestBoss, GodSlayerHunt ×2, Grasp, Warp). `remaining(Long,long)` has 3 (Flight, AquaMarine, TechniqueTimers). Give-or-drop item has 3 (GodSlayerHunt, TestModeCommand, RewardBoxes). The title+subtitle+animation packet trio has 4 (Danger, DeadlyTreasure, WandererBar, GodSlayerHunt). Boss-bar sync has 5, and WandererBar and TreasureBar are nearly the same.
- **Summon and follow.** The follow-teleport block (FOLLOW_FROM=6, TELEPORT_FROM=24 plus a spot search) is repeated in Illusion, WarriorAngel, VioletGuardian and Grasp. The free-spot search is in GraspGoals and VioletGuardian, both weaker than SafeTeleport. The friend/target filter has 8 variants that are **not** identical, so merging them would change behaviour (see B11).
- **Ray aim.** Lance, LifeStealer, Thunder, Mayhem, Crystal and Seism each have their own aim code.

### D. Rules living in Minecraft glue

- Grasp: tether slow, pickup radius, push strength.
- Illusion: sprint bonus, ranges, probabilities.
- Seism: spike height and count, boulder size.
- Knockback values: Thunder and Repulse.
- `Warp.PLACE_REACH`, the `TechniqueFields` heights, and the Crystal self-cage charge threshold.
- `ThousandMileStep` constants and its unsafe-block list.
- QuestBoss: health, scale, minions, reach.
- DangerMobs: armour tables, elite drops.
- DeadlyTreasure: cave chance and roomy test.
- Client: the stat diff "16 blocks → 18 blocks" (`TechniquesTab` l.396-419), the wheel groups (`TechniqueWheelScreen.GROUPS`, with no test that every technique is in exactly one group), and the food heal/regen strings (`StatsPanel` l.331-337).

### E. Oversized classes

| Class | Lines | Mixes |
|---|---|---|
| `technique/grasp/Grasp` | 986 | net records + handle, tether, tame, release, call/load, menu, tick |
| `technique/Illusion` | 784 | entity + 3 goals as inner classes |
| `technique/TechniqueRules` | 650 | rules of ~10 techniques |
| `client/TechniquesTab` | 610 | list, slots, detail page, stat diff |
| `godslayer/GodSlayerHunt` / `GodSlayer` | 580 / 573 | hunt flow + bar + reward / entity + skills |
| `treasure/DeadlyTreasure` | 562 | command + placement + event + text |
| `client/AdminScreen` | 544 | screen + 8 tab data builders |
| `technique/Mayhem` | 507 | cast + run state + tick |
| `client/PassivesTab` | 490 | 115 lines of gem pixel data + tab |
| `wanderer/Wanderer` | 489 | state, events, text, leave chest |
| `Messages` | 471 | ~99 statics for 4 areas |
| `client/StatsPanel` | 460 | wiring, stats page, 3 widget classes, Notes stubs |
| `ImmortalCultivation` | 295 | `onInitialize` ~180 lines of inline lambdas; two `AFTER_DEATH` registrations |

No single method is over ~90 lines.

### F. Naming

- Java class names that do not match display names: `ThousandMileStep` = Quantum Leap, `Mayhem` = Crystal Mayhem, `Crystal` = Crystal Prison, `Lance` = Solar Lance, `LifeStealer`/`Stealer*` = id `blade`, `VioletGuardian` = id `golem`, Spirit Walk = `PHASE`/`SHADOW`. Inside `ThousandMileStep`, a local named `violet` holds turquoise.
- Misplaced or odd names: `GraspRules` and `GraspCalls` sit in `technique/` while the rest of Grasp is in `technique/grasp/`. `AdminNetworking` is named unlike the other `*Net` classes. Three SavedData `TYPE` ids hard-code `"permahearts"` instead of `NAMESPACE`; the value stays the same either way.
- About 73 user-facing strings are hard-coded `Component.literal` while the rest go through lang. That is consistent with the current style, so leave it.

### G. Flaky tests

- **`GraspGameTest.aMobThatIsNotWhereItWasSeenIsNotFoundAndNoCooldownIsSpent`** (l.578). It sets the static `Grasp.setCallTimeout(GraspCalls.TIMEOUT_TICKS)` (100 ticks). Phase 2 calls again under that 100-tick deadline while chunk loading runs on worker threads that do not follow ticks. The sibling test uses 1200 for exactly this reason. Phase 1 also checks `phase == 130` exactly once instead of polling. The guards use `System.nanoTime`. The static timeout is never restored, so it leaks into later tests.
- `GolemGameTest.guardianAttacksAMonsterNearTheOwner` checks once at tick 150 against a husk 6 blocks away while pathing is random.
- `ThunderGameTest.aPigInTheBlastBecomesAZombifiedPiglin`: the explosion can kill the pig before the lightning converts it.
- `farmTakesSeedsFromTheChestAndPlantsEmptyFarmland`: the clone sometimes dies before tick 1202.

### H. Docs

- `docs/system/03-status.md` "In flight" still lists P-060, P-061, P-053, P-054… as in flight, though they were all released long ago.

## Refactor steps (proposed; each one commit, behaviour identical)

Risk: **L** = mechanical, compile + tests prove it; **M** = many files moved, or the look depends on exact pixels; **H** = high chance of a subtle behaviour change.

| Step | What | Files | Risk | Proof |
|---|---|---|---|---|
| R1 | Tools: one `tools/pngkit.py` (`write_png`, `rgba`, `CLEAR`). All 13 scripts use a plain `import pngkit`; the 3 private writers and the `importlib` hack go away. Remove the committed `__pycache__` and add `__pycache__/` to `.gitignore`. Add a header note in `make_technique_sprites.py` that `blade`, `lance` and `mayhem` are hand-made, and skip writing `blade` so a rerun cannot overwrite the Life Stealer icon. | `tools/*`, `.gitignore` | L | Run every script on a scratch copy: all PNGs byte-identical to the committed ones |
| R2 | Game test kit: `GameTestKit` (survivor, player, place, drop, cleanUp, `waitUntil(c, cond, maxTicks, msg)`). Copies replaced; profile names from a counter instead of `Math.random()`. Test-only. | `src/gametest/*` | L | full `build` (all server game tests) |
| R3 | Deflake the Grasp "not where it was seen" test: short timeout for the wrong-place call, then 1200 for the real call, poll instead of `== 130`, tick counters instead of `nanoTime`, restore the static timeout in `finally`. Also: poll the Golem test until the husk is hurt (≤400 ticks), and spawn the Thunder pig outside the explosion radius. Test-only. | `GraspGameTest`, `GolemGameTest`, `ThunderGameTest` | L | `runGameTest -Pgt='*:grasp*'` 5×, golem/thunder 3× each, then full `build` |
| R4 | Remove dead code from section B (unused methods, local and args). Keep the unused textures `check.png`/`plus.png` (deleting assets is Huy's call). | 6 Java files | L | full `build` |
| R5 | Small server helpers: `Uuids.parseOrNull`, one `remaining()` in `TechniqueTimers`, `Items.giveOrDrop(player, stack)`. Copies replaced 1:1 (same order of calls). | ~10 files | L | full `build` |
| R6 | Pure rules out of the client, with new JUnit: `StatDiff.compare` (from TechniquesTab), `WheelGroups` (from TechniqueWheelScreen, plus a test that every `Technique` is in exactly one group). Code moved verbatim. | client + 2 new pure classes + 2 tests | L | JUnit + `compileClientJava` + full `build` |
| R7 | Split `ImmortalCultivation.onInitialize` into `WandererEvents`, `DeathRespawnHandlers`, `KillXpHandler`. Same registration order. Do not merge the two `AFTER_DEATH` handlers (order matters). | root package | M | full `build` |
| R8 | Split `Grasp.java` by job: `GraspNet` (payload records + handle; same `Identifier`s), `GraspCallService` (call/load/CALL_READY), `GraspTether`. Move `GraspRules`/`GraspCalls` into `technique/grasp/`. Pure moves. | technique/grasp, tests imports | M | full `build` incl. `*:grasp*` |
| R9 | Move Illusion's inner goal classes into `technique/illusion/`. Pure moves. | Illusion + illusion/ | M | full `build` incl. `*:illusion*` |
| R10 | Split `Messages` into `HeartMessages`, `WandererMessages`, `QuestEventMessages`, `GodSlayerMessages`. Lang keys unchanged. | Messages + callers | M | full `build` |
| R11 | Client `Ui` helpers (frame, inset, scroll thumb with colour parameter, hit test, scroll clamp) used by all tabs and screens; move `PassiveGems` pixel data out of `PassivesTab`. Code moved verbatim, colours passed through. | 7 client files | M (pixel look) | `compileClientJava` + client game test suite once (screenshots compared with main) |
| R12 | Notes switch: replace the commented-out Notes lines in StatsPanel and ImmortalCultivation with `Features.NOTES = false` guards so the Notes code keeps compiling. Off stays off. This changes how D217 is kept off, so it needs a new decision row. | StatsPanel, ImmortalCultivation, new `Features` | L | full `build` + client suite |
| R13 | Docs: fix the stale "In flight" of `03-status.md`; add a code-name ↔ display-name table to `02-findings.md` (instead of renaming classes). | docs | L | – |

### Not proposed (deferred)

- **Merging the friend/target filters or the ray-aim code.** The variants differ, so a merge changes who gets hit (see B11).
- **Renaming Java classes to their display names.** Huge churn across code, tests and decision text for little gain; R13's table covers it.
- **Splitting `TechniqueRules`, `GodSlayer*`, `DeadlyTreasure`, `Wanderer`, `Mayhem`, `AdminScreen`, `TechniquesTab`.** Worth doing, but each is a separate batch.
- **A shared PlayerBar or title helper, a `Payloads.type()` helper, an `AdminNetworking` → `AdminNet` rename.** Low value.
- **Moving hard-coded numbers from glue into Rules classes (section D).** Do it when each area is next touched.
- **Fixing B1–B15.** Each is a bug fix with a decision row, after Huy says which ones.

## Tests

- Test scope checklist (team.md): area ticked = **Phần chung** (shared code, many areas); change type = refactor across many files. Commands: full `./gradlew :immortal-cultivation:build` after the series, plus area runs per step as listed. `runClientGameTest` once, after R11/R12 if approved.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :immortal-cultivation:build
```

## Docs

- `03-status.md` (R13 and the end of the batch).
- `01-decisions.md`: a row only if R12 is approved.

## Commit

One commit per step: `P-076 R<n>: <what>`. Push `feat/P-076-audit-refactor` only. No merge into `main`, no release.

## Done when

- [ ] Plan committed
- [ ] Approved steps committed one by one, pushed
- [ ] `build` green
- [ ] Bugs reported to Huy

## Risks / ask first

- R11 and R12 touch the UI. They must look pixel-identical; compare the client screenshots against main.
- B11 (techniques hitting your own allies, heal on hostiles, PvP) needs a decision from Huy, not a refactor.
