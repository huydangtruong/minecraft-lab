# P-006 — Quest events: Blood Moon, Explore, Boss

> **Tóm tắt cho Huy:** thêm 3 loại nhiệm vụ cho Lữ Khách. **Trăng máu**: bấm Bắt đầu → trời thành đêm, mỗi 10 giây 5 zombie quanh mỗi người (tối đa 30 con), không ngủ được, người bấm sống tới sáng là xong. **Thám hiểm**: đứng trong một Làng/Đền/Pháo đài… **Hạ thủ lĩnh**: bấm Triệu hồi → trùm to gấp đôi, 150 máu, 4 đệ tử. Bản 0.6.0.

- **Status:** merged
- **Mod / decision:** `permahearts`, D13 · **Branch:** `feat/P-006-events` · **Depends on:** P-005 (merged, 0.5.0)

## Goal

Rules: [spec.md](../mods/permahearts/spec.md) "Lữ Khách và nhiệm vụ" + D13. Extend P-005's model, generator, menu and screen; do not restructure them. Read the P-005 code in `mods/permahearts/src/*/java/.../wanderer/` and the client screen first.

## Scope

- In: `mods/permahearts/` only, `mod_version=0.6.0`, docs below.
- Out: sky colour / shaders (atmosphere comes from the boss bar's fog + darken flags), custom mob models, pinned versions.

## Verified API (26.3, javap)

- Time: `ServerClockManager clock = server.clockManager()` (also `ServerLevel#clockManager()`); overworld clock holder = `server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD)` (check the getter name on the lookup); `clock.moveToTimeMarker(holder, ClockTimeMarkers.NIGHT)` / `ClockTimeMarkers.DAY`; `clock.isAtTimeMarker(...)`. Read: `level.getOverworldClockTime()`, `level.isDarkOutside()`, `level.isBrightOutside()`. `/time set` uses the same clock (`TimeCommand` → `Registries.WORLD_CLOCK`).
- Boss bar: `new ServerBossEvent(UUID, Component, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)`, `setProgress(float)`, `setDarkenScreen(true)`, `setCreateWorldFog(true)`, `addPlayer(ServerPlayer)` / `removeAllPlayers()` (check these two names).
- Sleep: `EntitySleepEvents.ALLOW_SLEEPING.register((Player p, BlockPos pos) -> problem or null)`; return `new Player.BedSleepingProblem(Messages.bloodMoonNoSleep())`.
- Entities: `level.getEntities(EntityTypeTest<Entity,T>, Predicate<? super T>)` (use `EntityTypeTest.forClass(Zombie.class)` or `EntityTypes.ZOMBIE`), `Entity#addTag(String)` / `getTags()` (persist in saves), `Mob#setTarget`, `Mob#setDropChance(EquipmentSlot, float)`, `Mob#setItemSlot` (check: only `setItemSlotAndDropWhenKilled` is protected on `Mob`; `LivingEntity#setItemSlot(EquipmentSlot, ItemStack)` should be public), `Mob#finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null)`, `EntitySpawnReason.EVENT`. Attributes `Attributes.SCALE`, `MAX_HEALTH`, `ATTACK_DAMAGE` via `getAttribute(..).setBaseValue(..)`.
- Structures: `level.structureManager().getStructureWithPieceAt(BlockPos, Predicate<Holder<Structure>>)` → `StructureStart#isValid()`. Keys: `BuiltinStructures.DESERT_PYRAMID`, `JUNGLE_TEMPLE`, `PILLAGER_OUTPOST`, `SWAMP_HUT`, `IGLOO`, `OCEAN_MONUMENT`, `ANCIENT_CITY`, `TRIAL_CHAMBERS`, `FORTRESS`, `BASTION_REMNANT`, `SHIPWRECK`, `TRAIL_RUINS`; tags `StructureTags.VILLAGE`, `MINESHAFT`, `RUINED_PORTAL`. Predicate: `h -> h.is(key)` or `h.is(tag)`.

## Model changes (pure, no Minecraft imports)

- `QuestType` += `BLOOD_MOON`, `EXPLORE`, `BOSS` (append at the end; saved data uses names).
- For the three new types `required = 1`, `progress` 0/1. `target`: EXPLORE = structure id or `#tag` (`"#minecraft:village"`, `"minecraft:fortress"`); BOSS = entity id (`minecraft:zombie` or `minecraft:skeleton`); BLOOD_MOON = `""`.
- `QuestGenerator`: per line pick type by weight COLLECT 30, KILL 30, EXPLORE 15, BLOOD_MOON 12, BOSS 13; at most one BLOOD_MOON and one BOSS per quest; no duplicate targets. EXPLORE pool (weights): `#minecraft:village` 4, `#minecraft:ruined_portal` 3, `minecraft:pillager_outpost` 2, `#minecraft:mineshaft` 2, `minecraft:shipwreck` 2, `minecraft:desert_pyramid` 1, `minecraft:jungle_temple` 1, `minecraft:swamp_hut` 1, `minecraft:igloo` 1, `minecraft:trail_ruins` 1, `minecraft:trial_chambers` 1, `minecraft:ocean_monument` 1, `minecraft:ancient_city` 1, `minecraft:fortress` 1, `minecraft:bastion_remnant` 1.
- `QuestLogic`: `explore(Quest, Predicate<String> isInside)` → first not-done EXPLORE line whose target matches → done; `eventDone(Quest, int line)`.
- `WandererData` gains `Optional<BloodMoonState> bloodMoon` (`int line, String starterUuid, long startGameTime`) and `Optional<BossState> boss` (`int line, String bossUuid`), both in the codec as optional fields (old saves load fine). JUnit round-trip.

## Server glue (`wanderer/`)

| File | What |
|---|---|
| `BloodMoon.java` (new) | `start(server, line, ServerPlayer starter)`: refuse if one is running or the line is done, or the starter is not in the Overworld. Move the overworld clock to NIGHT, save state, boss bar "Trăng Máu" (red, darken + fog) to every Overworld player, chat broadcast. `tick(server)` (every 20 ticks from the scheduler; wave every 200 ticks since `startGameTime`, use `server.overworld().getGameTime()`): for each Overworld player not creative/spectator, spawn `min(5, 30 - alive)` zombies at random 3–6 block offsets, surface y from `getHeight(MOTION_BLOCKING_NO_LEAVES)`, `finalizeSpawn`, `addTag("permahearts_blood_moon")`, `setTarget(player)`; `alive` = tagged zombies in the Overworld. Boss bar progress = elapsed / 12000. Success: elapsed ≥ 200 ticks and `overworld.isBrightOutside()`, or elapsed ≥ 13000 → line done, broadcast `lineCompleted`, clear state + bar. Fail: the starter dies (`AFTER_DEATH` with `ServerPlayer` uuid = starter) or leaves (`ServerPlayerEvents.LEAVE`) → clear state + bar, broadcast `bloodMoonFailed`, line stays not done (button usable again). Leftover zombies are not removed (they burn at sunrise). Players joining during it get the bar (`JOIN`). `ALLOW_SLEEPING` blocks beds while running |
| `QuestBoss.java` (new) | `summon(server, line, ServerPlayer by)`: needs the NPC entity loaded; refuse if a live boss for this quest exists. Spawn at 5 blocks from the NPC on the surface: entity type from `target`; `MAX_HEALTH` base 150 + `setHealth(150)`, `SCALE` base 2.0, `ATTACK_DAMAGE` base ×1.5, iron armor set + diamond helmet (helmet = no sun burning), zombie: iron sword, skeleton: bow; drop chance 0 for gear; `setPersistenceRequired()`, custom name `Messages.bossName(entityName)` visible, `addTag("permahearts_boss")`; 4 minions of the same type (normal stats, helmet so they do not burn, tag `permahearts_boss_minion`), all `setTarget(by)`. Save `BossState`. Boss bar (purple, PROGRESS) showing boss health to players within 64 blocks, updated from the scheduler tick. Boss `AFTER_DEATH` (uuid match, any cause) → line done, broadcast, clear state + bar. If the boss is gone without dying (unloaded and lost), the next summon press replaces it |
| `Wanderer.java` (edit) | `startEvent(server, line, player)` dispatches by type (BLOOD_MOON → BloodMoon.start, BOSS → QuestBoss.summon). Explore check every 40 ticks (scheduler): for each player with an accepted quest that has a not-done EXPLORE line, `structureManager().getStructureWithPieceAt(player.blockPosition(), predicate)` in the player's own level → done + broadcast. Expose `markExploredForTest(server, target)` |
| `WandererScheduler.java` (edit) | call `BloodMoon.tick`, `QuestBoss.tick`, explore check |
| `PermaHearts.java` (edit) | register `ALLOW_SLEEPING`, `LEAVE`, death hooks (or route them through `Wanderer`) |
| `WandererMenu.java` (edit) | `clickMenuButton(player, 10 + i)` → `Wanderer.startEvent`. New data slot per line `state[i]` (0 idle, 1 running) read from `bloodMoon`/`boss` state |
| `client/WandererScreen.java` (edit) | Rows: EXPLORE "Đặt chân tới %s" + structure name (lang keys below); BLOOD_MOON "Sống sót qua đêm trăng máu" + button **Bắt đầu** (disabled while running → label "Đang diễn ra…"); BOSS "Hạ %s" + button **Triệu hồi** (running → "Đang chiến đấu…"). Buttons send `handleInventoryButtonClick(containerId, 10 + i)`. Done rows show the reward slot exactly like P-005 |
| `Messages` + lang | `lineExplore`, `lineBloodMoon`, `lineBoss`, `start` "Start"/"Bắt đầu", `summon` "Summon"/"Triệu hồi", `running` "In progress…"/"Đang diễn ra…", `bloodMoonStarted` "The Blood Moon rises! Survive until dawn" / "Trăng máu đã lên! Sống sót tới sáng", `bloodMoonFailed` "The Blood Moon claimed %s. Try again" / "Trăng máu đã hạ gục %s. Thử lại nhé", `bloodMoonNoSleep` "You cannot sleep under the Blood Moon" / "Không thể ngủ dưới trăng máu", `bloodMoonBar` "Blood Moon"/"Trăng Máu", `bossName` "%s Chieftain"/"Thủ Lĩnh %s", `bossSummoned`, `bossAlreadyHere`. Structure names `permahearts.structure.<path>` (VN: Làng, Cổng dịch chuyển đổ nát, Tiền đồn cướp làng, Hầm mỏ bỏ hoang, Tàu đắm, Đền sa mạc, Đền rừng rậm, Chòi phù thủy, Lều tuyết, Tàn tích cổ, Phòng thử thách, Đền dưới biển, Thành phố cổ, Pháo đài Nether, Tàn tích Bastion) |

## Tests

- JUnit: generator with all types (≤1 BLOOD_MOON, ≤1 BOSS, 1000 seeds), `explore` matching, codec round-trip with blood moon + boss state, and an old P-005-shaped JSON without them still loads.
- Game tests (each: `enable(true)`, `Wanderer.reset` at start and in `finally`; restore the clock to DAY in `finally` where you change it):
  - `bloodMoonStartsNightAndSpawns`: quest with a BLOOD_MOON line, accept, `startEvent` → `overworld.isDarkOutside()`, call one wave → 1..5 tagged zombies near the player; 10 waves → ≤ 30 tagged alive.
  - `bloodMoonFailsOnStarterDeath`: start, starter `setHealth(0)` + `die` → state cleared, line not done.
  - `bloodMoonSucceedsAtDawn`: start, set game time/elapsed past 200 and move the clock to DAY, tick → line done. If game tests pin the clock (`TestEnvironmentDefinition$ClockTime`), test the decision function directly and say so.
  - `bossSummonAndKill`: BOSS zombie line, summon → one `permahearts_boss` zombie with max health 150 and scale 2, 4 minions; second summon refused; kill the boss with `playerAttack` 1000 → line done, boss state cleared.
  - `exploreMarksLine`: `markExploredForTest(server, "#minecraft:village")` → that line done, other EXPLORE target untouched.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```

Do not run `runClient`, `scripts/install.sh`, `scripts/package.sh`.

## Docs

- `docs/mods/README.md`: 0.6.0. `docs/workflow/playtest.md`: blood moon / boss / explore steps (`/quest summon` until a quest has them, or `/quest complete` the others). `docs/system/02-findings.md`: clock, boss bar, structure lookup facts.

## Commit

`PermaHearts 0.6.0: Blood Moon, Explore and Boss quest lines (P-006)`; push the branch, never `main`.

## Done when

- [ ] `./gradlew :permahearts:build` green, all old tests still green
- [ ] Report in team.md format

## Risks / ask first

- Blood-moon zombies give HeartXP like any zombie (intended; tell the manager if it looks abusive in tests, do not change it).
- If moving the clock inside game tests breaks other tests, isolate with the decision-function approach rather than skipping.
