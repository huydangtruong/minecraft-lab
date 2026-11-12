# P-008: Wanderer fixes (cave boss, mob-proof NPC, Give up, 2-day stay)

> **Tóm tắt cho Huy:** sửa và thêm những thứ bạn gặp khi chơi thử 0.7.0. Bản này là 0.7.1.
> 1. Thủ lĩnh và quái Trăng máu xuất hiện ngay chỗ Lữ Khách / người chơi đứng, kể cả trong hang. Trước đây chúng hiện trên mặt đất phía trên hang. Nếu trùm mất, nút quay về "Summon".
> 2. Quái không đuổi theo Lữ Khách nữa.
> 3. Nút **Give up** chữ đỏ ở cuối danh sách nhiệm vụ. Bấm 2 lần để xác nhận thì Lữ Khách bỏ đi.
> 4. Lữ Khách chỉ ở lại **2 ngày Minecraft**. Bên phải màn hình, dưới dòng giới thiệu, có đếm ngược thời gian trước khi rời đi.

- **Status:** merged
- **Mod / decision:** `permahearts`, D19, D20 (new)
- **Branch:** `feat/P-008-wanderer-fixes`
- **Depends on:** P-007 (merged, 0.7.0)

## Goal

These requests come from Huy's first real playtest of 0.7.0 on 2026-11-12.

### 1. Boss not found (root cause known)

- What Huy did: he was in a cave, ran `/quest summon` (the NPC appeared next to him), switched to creative, and pressed Summon on a Vindicator Chieftain line. The button showed "Fighting…", but even in spectator he found no boss around the cave.
- Root cause: `QuestBoss.surface()` takes `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES` at the target x/z. Underground, that is the **surface above the cave**, which can be dozens of blocks up. The boss bar only shows within 64 blocks. `BloodMoon.spawn` has the same bug: its waves spawn on the surface above a player who is in a cave.
- What else is broken: the line then stays stuck. The button is disabled while `eventState == 1`, and `eventState` only checks that `WandererData.boss` is present, not that the boss is alive.

### 2. Second boss problem (seen by the manager, cause unknown)

- Setting: client game test world, flat, `difficulty normal`, player creative. The summon is a Vindicator boss at the player's level.
- What happens:
  - At about 26 ticks the boss is alive with 150 HP.
  - At about 126 ticks `getEntity(uuid)` returns null, but the boss took no damage.
  - `ENTITY_UNLOAD` fired about 40 ticks after the summon. Its stack ran through `PersistentEntitySectionManager.updateChunkStatus` → `stopTracking`, and `getRemovalReason()` was null.
- Hard to tell apart: this may come from the client test world itself (the summon happened right after screenshots and screen closes). Check it once the spawn-height fix is in.
- Testing note: server game tests share the global Wanderer state and run at the same time in a batch, so a probe there got reset by other tests. Do long (100+ tick) checks in the **client game test**.

### 3. The NPC attracts hostile mobs

Zombies and illagers target it, as they target any wandering trader.

### 4. Give up

A red-text button at the bottom of the quest list that sends this Wanderer away.

### 5. Two-day stay

- The Wanderer leaves **2 Minecraft days** after it arrives.
- The quest screen shows a countdown under the intro text in the right panel.

## Scope

- In: `mods/permahearts/` only, `mod_version=0.7.1`, and the docs listed below.
- Out: root Gradle files, `scripts/`, pinned versions. Do not install or package.

## Verified API (26.3, javap)

- `Entity#setPermanentlyInvulnerable(boolean)` exists; `setInvulnerable` does not. `Entity#isInvulnerable()` returns `isPermanentlyInvulnerable() || isTemporarilyInvulnerable()`.
- `LivingEntity#canBeSeenAsEnemy()` returns `!isInvulnerable() && canBeSeenByAnyone()`. So a permanently invulnerable NPC is not a valid target.
  - Confirm with `javap -c` that `TargetingConditions#test(ServerLevel, LivingEntity, LivingEntity)` calls `canBeSeenAsEnemy`.
  - Note it in 02-findings.md.
- Time: `Wanderer.currentDay` = `overworld().getOverworldClockTime() / 24000`. The scheduler `work()` runs once per second. Phases are WAITING → PLANNED (coordinates in chat; the NPC spawns when a player is within NEAR) → PRESENT.
- Client test helpers: `TestServerContext#runOnServer` / `computeOnServer` and `ClientGameTestContext#waitTicks`.

## Design decisions (record as D19 / D20)

### D19: Give up and a mob-proof NPC

- Anyone can press Give up. It takes 2 clicks within 3 s.
- What giving up does:
  - Running events stop, and their mobs, the boss and the minions are removed.
  - The NPC leaves. Unclaimed rewards and deposited items are lost.
  - The next Wanderer comes in 3–5 days.
- Mobs never target the NPC.

### D20: Two-day stay

- The 2 days (48000 ticks of the overworld clock) start **when the coordinates are announced** (PLANNED). That is when the Wanderer arrives in the world. With `/quest summon`, announcing and appearing happen together.
- If nobody reaches the Wanderer in time, the visit expires too.
- One chat warning goes out 2400 ticks (≈ 2 real minutes) before it leaves.
- It does not leave while a Blood Moon or boss fight is running. It leaves right after that event ends.
- Leaving works like Give up, but with its own message.
- Sleeping or the Blood Moon's jump to night move the overworld clock forward, so they also shorten the stay. That is fine: the stay is measured in Minecraft days.

## Files (package `io.github.huydangtruong.permahearts`, under `mods/permahearts/src/`)

### `main/.../wanderer/QuestBoss.java`

Replace `surface()` with `standSpot(ServerLevel, BlockPos origin, int dx, int dz)`.

- Search for a block at the **origin's Y**: try x/z offset by dx/dz, then each y in origin.y ±4.
- A spot is valid when the block below is solid (`isFaceSturdy(UP)` or `!getCollisionShape().isEmpty()`) and there are 2 air blocks above it (`getCollisionShape().isEmpty()`). That is enough at scale 2; the boss gets pushed out of tight spots.
- If nothing fits, fall back to the origin, which is the NPC's own position.
- Boss: the origin is the NPC position, 3–5 blocks towards the summoner. Minions: around the boss spot.
- Check the result of `ow.addFreshEntity(boss)`. If it is false, don't save `BossState` and return false.
- Add `static boolean bossAlive(MinecraftServer)`: true when a live entity has the saved UUID.

Then do the second-problem check above:

- With the new spot, does a Vindicator boss in a flat creative world still disappear after about 40 ticks?
  - If yes: log `ENTITY_UNLOAD` and removal with stack, chunk position and entity-section status in a scratch run (don't commit the logs). Find the cause, fix it, and report it.
  - If no: say so.

### `main/.../wanderer/BloodMoon.java`

`spawn`: use the same `standSpot` around the **target player's** position at the player's Y (3–6 blocks away, random angle). Don't use the heightmap.

Move `standSpot` into a small shared helper (e.g. `SpawnSpots`) so both classes use it.

### `main/.../wanderer/Wanderer.java`

- `eventState(line)`: the boss counts as running only while `QuestBoss.bossAlive(server)`. The Blood Moon stays as it is.
- `giveUp(MinecraftServer, ServerPlayer by)` and `leave(MinecraftServer, Component message)`, with `giveUp` calling `leave`. `leave` does this:
  1. Ends the Blood Moon without the fail message.
  2. Clears the event mobs, the boss and the minions (reuse `BloodMoon.onReset` / `QuestBoss.onReset` cleanups).
  3. Discards the NPC and calls `REWARDS.remove(questId)`.
  4. Saves `Phase.WAITING` with no NPC and no quest, and `nextDay = today + daysUntilNext`.
  5. Broadcasts the message.
- `giveUp` is valid only while the phase is PLANNED or PRESENT.
- `secondsLeft(server)` returns the ticks left until `leaveAt`, for the screen and `/quest`.

### `main/.../wanderer/WandererData.java`

- New field `long leaveAt`: the overworld clock time when the visit ends. Codec `optionalFieldOf("leave_at", -1L)`.
- `plan()` sets `leaveAt = clockTime + 48000`.
- Old 0.7.0 saves have `-1` while PLANNED or PRESENT. The scheduler sets those to `clockTime + 48000` once.
- Add a `warned` flag (`optionalFieldOf("leave_warned", false)`) so the warning goes out only once.

### `main/.../wanderer/WandererScheduler.java`

In `work()`, for PLANNED or PRESENT:

- Set `leaveAt` if it is missing.
- At `leaveAt - 2400`, if not warned yet: broadcast `Messages.leavingSoon()` and set `warned`.
- Once `clock ≥ leaveAt` and no Blood Moon or boss is running: `Wanderer.leave(server, Messages.leftAfterStay())`.

### `main/.../wanderer/WandererMenu.java`

- Sync the time left to the client with 2 `DataSlot`s: minutes and seconds of real time, or one slot in seconds capped at 32767. Clamp; 48000 ticks = 2400 s, which fits.
- `clickMenuButton(99)` → `Wanderer.giveUp(server, player)`, server side only, returns true.
- Layout:
  - `LINE_STEP` goes from 36 to **32**, so 4 rows end at y 146.
  - `GIVE_UP_Y = LINE_TOP + LINES * LINE_STEP` (146), `GIVE_UP_W = 70`, `GIVE_UP_H = 16`.
  - `LINE_SLOT_Y0` is derived, so it follows.

### `client/.../WandererScreen.java`

- **Give up button:** centred under the rows at `(QUEST_W - GIVE_UP_W) / 2, GIVE_UP_Y`, size 70×16.
  - Red label: `Messages.giveUp().copy().withStyle(ChatFormatting.RED)`.
  - Shown both before and after Accept.
  - The first click sets the label to `Messages.giveUpConfirm()` (red) for 60 client ticks, counted in `containerTick`. A second click within that time sends `handleInventoryButtonClick(containerId, 99)`; otherwise the label goes back.
- **Right panel**, info column next to the preview:
  - Under the existing text (`questOffer` before Accept, `questSummary` after), draw `Messages.leavesIn(timeText)`, wrapped with `wrapped(...)` and no shadow.
  - `timeText` shows Minecraft time and real time, e.g. "1 day 7 h (≈ 31 min)". Put the formatting in a pure helper `StayTime.format(long ticksLeft)` with no Minecraft imports and test it with JUnit.
  - If the column overflows the preview box height (56 px), shorten the summary text rather than overlap "Inventory".

### `main/.../wanderer/QuestCommand.java`

`/quest` also prints the time left.

### `main/.../wanderer/WandererNpc.java` + `PermaHearts.java`

- On spawn, call `npc.setPermanentlyInvulnerable(true)`.
- In the existing `ENTITY_LOAD` hook, a valid wanderer also gets `setPermanentlyInvulnerable(true)`. This fixes NPCs already standing in 0.7.0 worlds. Keep the `ALLOW_DAMAGE` guard.
- In `ENTITY_LOAD`, discard any mob tagged `QuestBoss.TAG` whose UUID differs from the saved boss UUID.

### `main/.../Messages.java` + `lang/en_us.json`

| Key | Text |
|---|---|
| `giveUp` | "Give up" |
| `giveUpConfirm` | "Sure? Click again" |
| `gaveUp` | "%s gave up the Wanderer's quests. The Wanderer moves on" |
| `leavesIn` | "Leaves in %s" |
| `leavingSoon` | "The Wanderer will leave soon" |
| `leftAfterStay` | "The Wanderer's time is up. The Wanderer moves on" |
| Day and hour units for `StayTime` | English only (D16) |

### `mods/permahearts/gradle.properties`

`mod_version=0.7.1`

## Tests

### JUnit

`StayTime.format`:

- 48000 → "2 days 0 h (≈ 40 min)"
- 25200 → "1 day 1 h (≈ 21 min)"
- 600 → "0 h (≈ 30 s)", or the format you choose. Make the cases match it.
- Never negative.

### Client game test (`QuestScreenClientTest`, after the existing shots)

- **Cave boss:**
  1. Fill a 7×7×4 stone box around the player.
  2. Carve out a 7×3×5 air room with an 8-block roof above it, using `/fill`.
  3. Put the NPC inside and summon a `vindicator` boss.
  4. Assert the boss is within 4 blocks of the NPC's Y and within 8 blocks horizontally.
  5. Take screenshot `05-cave-boss` facing the boss.
- **Each boss type** (`zombie`, `skeleton`, `evoker`, `blaze`, `vindicator`):
  1. Reset, start a quest with that boss line, accept and summon.
  2. Wait 200 ticks.
  3. Assert `QuestBoss.bossAlive` and `eventState == 1`. Fail with the type name.
  4. Discard the boss by hand, then assert `eventState == 0` and that summoning again works.
- **Screenshots:**
  - `02-quest-lines` must show the red Give up button and the "Leaves in" line.
  - Add `06-give-up-confirm`, taken after one click.

### Server game tests (`WandererGameTest`, short, at most 40 ticks)

- `npcIsNotATarget`: a zombie next to the NPC. `TargetingConditions.forCombat().test(level, zombie, npc)` is false.
- `giveUpEndsTheVisit`: a quest with a summoned boss, then `giveUp`. Check:
  - the phase is WAITING;
  - there is no NPC, no boss and no boss state;
  - `nextDay - today` is in 3..5.
- `visitExpires`:
  1. Set `leaveAt` to now + 1.
  2. Advance the clock with `server.clockManager()` or set `leaveAt` in the past.
  3. Run `WandererScheduler.work`.
  4. The phase is WAITING.
  5. With a boss state present, the visit does **not** expire.
- `lostBossCanBeResummoned`: summon, then `boss.discard()`. `eventState == 0` and `startEvent` works again.

All existing tests must still pass.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build              # JUnit + server game tests
./gradlew :permahearts:runClientGameTest  # opens a Minecraft window: cave boss, boss long-run checks, screenshots
```

- Open the PNGs yourself with Read.
- Copy them to `/private/tmp/claude-501/-Users-truongdanghuy-Coding-Minecraft-Lab/1b2f7ca5-d050-43d2-b256-60f38b38a436/scratchpad/screens-p008/`.
- Do not run `scripts/install.sh` or `scripts/package.sh`.

## Docs

- `docs/system/01-decisions.md`: add D19 and D20 as above.
- `docs/mods/permahearts/spec.md`:
  - Give up.
  - The NPC is ignored by mobs.
  - Bosses and Blood Moon mobs spawn at your level.
  - A lost boss can be summoned again.
  - The 2-day stay, the countdown and the warning.
- `docs/system/02-findings.md`:
  - Heightmap in caves.
  - `canBeSeenAsEnemy` and `setPermanentlyInvulnerable`.
  - Concurrent game tests.
  - The result of the second-problem check.
- `docs/mods/README.md`: 0.7.1, not installed yet.
- `docs/workflow/playtest.md`: Give up, the cave boss and the countdown.

## Commit

`PermaHearts 0.7.1: bosses spawn at your level, mob-proof Wanderer, Give up, 2-day stay (P-008)`

Push the branch, never `main`.

## Done when

- [ ] Cave boss check green, and the 5-boss long-run check green
- [ ] The second-problem check (flat-world unload) is answered in the report
- [ ] `./gradlew :permahearts:build` green, and the client test green
- [ ] Report in the team.md format, with screenshot paths

## Risks / ask first

- `setPermanentlyInvulnerable` also makes `/kill` fail on the NPC. That's fine, because `/quest reset` removes it.
- A countdown on the 2 synced data slots must not break `stillValid` or the slot indices. The data slots are not item slots.
