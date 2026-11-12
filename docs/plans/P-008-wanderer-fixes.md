# P-008 — Wanderer fixes: vanishing boss, mob-proof NPC, Give up button

> **Tóm tắt cho Huy:** sửa 3 thứ bạn gặp khi chơi thử 0.7.0. (1) Bấm Summon thủ lĩnh, nút thành "Fighting…" nhưng không thấy trùm: tìm và sửa nguyên nhân; trùm mất thì nút quay về "Summon" để gọi lại, không bị kẹt nữa. (2) Quái không còn đuổi theo Lữ Khách. (3) Thêm nút **Give up** chữ đỏ ở cuối danh sách nhiệm vụ: bấm 2 lần để xác nhận, Lữ Khách bỏ đi, phần thưởng chưa lấy bị mất, 3–5 ngày sau người mới tới. Bản 0.7.1.

- **Status:** planned
- **Mod / decision:** `permahearts`, D19 (new) · **Branch:** `feat/P-008-wanderer-fixes` · **Depends on:** P-007 (merged, 0.7.0)

## Goal

Three bugs and requests from Huy's first real playtest of 0.7.0 (2026-11-12):

1. **Vanishing boss.** The Vindicator Chieftain was summoned: the button switched to "Fighting…", but no boss was anywhere. After that the line is stuck: the button is disabled while `eventState == 1`, and `eventState` only checks that `WandererData.boss` is present, not that the boss is alive.
2. **The NPC attracts hostile mobs.** Zombies and illagers target it as they target any wandering trader, so mobs crowd around it.
3. **Give up button.** It is red text at the bottom of the quest list and skips this Wanderer.

## What the manager already found (reproduce this first)

- **Repro (client game test world, flat, `difficulty normal`, player creative):**
  1. `Wanderer.reset`.
  2. `startQuestForTest` with lines `BOSS minecraft:vindicator` and `BOSS minecraft:zombie`.
  3. `accept`, then `startEvent(s, 0, player)` returns true.
  4. The boss exists with 150 HP at ~26 ticks.
  5. At ~126 ticks `overworld().getEntity(bossUuid)` is null, while `WandererData.boss` still holds the UUID. The boss took no damage (HP 150, `getLastDamageSource()` null).
- **Where it leaves:**
  - `ServerEntityEvents.ENTITY_UNLOAD` fired for the boss about 40 ticks after the summon.
  - The stack went through `PersistentEntitySectionManager.updateChunkStatus` → `stopTracking` → `ServerLevel$EntityCallbacks.onTrackingEnd`.
  - `getRemovalReason()` was null at that point.
  - So the entity is not killed. It looks unloaded or untracked, although the player stands a few blocks away.
- **Not yet known:**
  - Why this happens.
  - Whether it is Vindicator/Raider specific. Raider, PatrollingMonster, AbstractIllager and Vindicator bytecode has no `discard()` except for item pickup.
  - Whether it also happens to the zombie, skeleton, evoker and blaze bosses over a longer time. The existing `bossSummonAndKill` kills the zombie at once.
- **Server game tests share the global Wanderer state and run concurrently in a batch.** A probe in `WandererGameTest` saw other tests' `Wanderer.reset` wipe its state within one tick. Do long-running (100+ tick) checks in the **client game test** (`QuestScreenClientTest`, one world), not in `WandererGameTest`.

## Scope

- In: `mods/permahearts/` only. Set `mod_version=0.7.1` and update the docs listed below.
- Out: root Gradle files, `scripts/`, pinned versions. No install or package.

## Verified API (26.3, javap)

- `Entity#setPermanentlyInvulnerable(boolean)` exists (there is no `setInvulnerable`). `Entity#isInvulnerable()` returns `isPermanentlyInvulnerable() || isTemporarilyInvulnerable()`.
- `LivingEntity#canBeSeenAsEnemy()` returns `!isInvulnerable() && canBeSeenByAnyone()`. A permanently invulnerable NPC is therefore not a valid target. Confirm with `javap -c` that `TargetingConditions#test(ServerLevel, LivingEntity, LivingEntity)` calls `canBeSeenAsEnemy`, and write it in 02-findings.md.
- `GameTestHelper#runAfterDelay(long, Runnable)`.
- Client test: `TestServerContext#runOnServer` / `computeOnServer`, `ClientGameTestContext#waitTicks`.

## Files (package `io.github.huydangtruong.permahearts`, under `mods/permahearts/src/`)

| File | Change |
|---|---|
| `main/.../wanderer/QuestBoss.java` | **1. Root cause.** Find why the boss stops being tracked and fix it at the source. Log `ENTITY_UNLOAD` and removal with stack, position, chunk pos, and entity section status in a scratch run; do not commit the logging. Check the spawn path: `snapTo` before `finalizeSpawn`, the spawn position chunk, `addFreshEntity` result, `setPersistenceRequired`, raid/patrol flags (`Raider#setCanJoinRaid(false)`, `PatrollingMonster#setPatrolLeader(false)` if relevant), and our own listeners (`ENTITY_LOAD` discarding wanderers must not touch bosses). **2. Robustness, whatever the cause.** Use the result of `ow.addFreshEntity(boss)`; if false, don't save `BossState` and return false. Add `static boolean bossAlive(MinecraftServer)` (live entity with the saved UUID). |
| `main/.../wanderer/Wanderer.java` | `eventState(line)`: the boss counts as running only while `QuestBoss.bossAlive(server)`. The blood moon is unchanged. A lost boss then shows "Summon" again, and summoning replaces the state (already allowed by `summon`). Add `giveUp(MinecraftServer, ServerPlayer by)`. It ends the blood moon without the fail message and removes its mobs. It removes the boss and minions (reuse the `onReset` cleanups of `BloodMoon`/`QuestBoss`). It discards the NPC, `REWARDS.remove(questId)`, and saves `Phase.WAITING`, no NPC, no quest, `nextDay = today + daysUntilNext`. It broadcasts `Messages.gaveUp(by.getDisplayName())`. Only valid while phase PRESENT. |
| `main/.../wanderer/WandererNpc.java` + `PermaHearts.java` | On spawn: `npc.setPermanentlyInvulnerable(true)`. In the existing `ENTITY_LOAD` hook: a valid wanderer also gets `setPermanentlyInvulnerable(true)`, so NPCs already in 0.7.0 worlds are fixed. Keep the `ALLOW_DAMAGE` guard. Any `ENTITY_LOAD` of a mob tagged `QuestBoss.TAG` whose UUID ≠ the saved boss UUID is discarded, so a re-summoned boss never meets an old copy. |
| `main/.../wanderer/WandererMenu.java` | `clickMenuButton(99)` → `Wanderer.giveUp(server, player)`, server side only, return true. Layout: `LINE_STEP` 36 → **32**, so 4 rows end at y 146. Add `GIVE_UP_Y = LINE_TOP + LINES * LINE_STEP` (146), `GIVE_UP_W = 70`, `GIVE_UP_H = 16`. |
| `client/.../WandererScreen.java` | A **Give up** button centred under the rows: `(QUEST_W - GIVE_UP_W) / 2`, `GIVE_UP_Y`, 70×16, label red (`Messages.giveUp().copy().withStyle(ChatFormatting.RED)`). Show it both before and after Accept. The first click changes the label to `Messages.giveUpConfirm()` (red) for 60 client ticks (`containerTick`). A second click within that time sends `handleInventoryButtonClick(containerId, 99)`; otherwise the label goes back. The row text keeps max 3 lines (27 px fits in 32). The slot square stays vertically centred (`LINE_SLOT_Y0` is derived, so it follows). |
| `main/.../Messages.java` + `lang/en_us.json` | `giveUp` "Give up", `giveUpConfirm` "Sure? Click again", `gaveUp` "%s gave up the Wanderer's quests. The Wanderer moves on". English only (D16). |
| `mods/permahearts/gradle.properties` | `mod_version=0.7.1` |

## Tests

- **Client game test (`QuestScreenClientTest`), after the existing shots:**
  - For each boss type (`zombie`, `skeleton`, `evoker`, `blaze`, `vindicator`): reset, start a quest with that boss line, accept, summon, wait 200 ticks. Assert `QuestBoss.bossAlive` and `eventState == 1`. Fail with the type name.
  - Then discard the boss by hand. Assert `eventState == 0` and that a second summon works.
  - Screenshot `05-boss-vindicator` with the player facing the boss.
  - The `02-quest-lines` shot must show the red Give up button.
  - Add `06-give-up-confirm`, taken after one click.
- **Server game tests (`WandererGameTest`, short, ≤ 40 ticks):**
  - `npcIsNotATarget`: a zombie next to the NPC. Assert `TargetingConditions.forCombat().test(level, zombie, npc)` is false, or after 20 ticks `zombie.getTarget() != npc`.
  - `giveUpEndsTheVisit`: a quest with a boss summoned. Then `giveUp`. Assert:
    - phase is WAITING;
    - there is no NPC, no boss and no boss state;
    - `nextDay - today` is in 3..5.
  - `lostBossCanBeResummoned`: summon, `boss.discard()`. Assert `eventState == 0` and that `startEvent` works again.
- All existing tests still pass.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build              # JUnit + server game tests
./gradlew :permahearts:runClientGameTest  # opens a Minecraft window; boss long-run checks + screenshots
```

Open the PNGs yourself with Read: Give up button, confirm label, boss visible. Copy them to `/private/tmp/claude-501/-Users-truongdanghuy-Coding-Minecraft-Lab/1b2f7ca5-d050-43d2-b256-60f38b38a436/scratchpad/screens-p008/`. Do not run `scripts/install.sh` or `scripts/package.sh`.

## Docs

- `docs/system/01-decisions.md`, **D19**:
  - Give up: anyone can press it, with two clicks to confirm.
  - The Wanderer leaves, unclaimed rewards and deposited items are lost, and the next Wanderer comes in 3–5 days.
  - The NPC is never a target for mobs.
- `docs/mods/permahearts/spec.md`: Give up, the NPC is ignored by mobs, a lost boss can be summoned again.
- `docs/system/02-findings.md`:
  - The boss root cause.
  - `canBeSeenAsEnemy` and `setPermanentlyInvulnerable`.
  - The concurrent game test note.
- `docs/mods/README.md`: 0.7.1, not yet installed.
- `docs/workflow/playtest.md`: the Give up and boss checks.

## Commit

`PermaHearts 0.7.1: fix vanishing boss, mob-proof Wanderer, Give up button (P-008)`. Push the branch, never `main`.

## Done when

- [ ] Root cause of the vanishing boss named in the report, fixed, and the 5-boss long-run client check green
- [ ] `./gradlew :permahearts:build` green, client test green
- [ ] Report in the team.md format, with screenshot paths

## Risks / ask first

- If the root cause can't be found after a solid try, ship the robustness part: the button comes back and the boss can be re-summoned. Report what you saw, and don't guess a fix.
- `setPermanentlyInvulnerable` also makes `/kill` fail on the NPC. That is fine; `/quest reset` removes it.
