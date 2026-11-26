# P-082 — Purgatory dungeon floors

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Luyện Ngục không còn là hộp bedrock sống sót 6 phút. Mỗi tầng là hầm ngục ngẫu nhiên nhiều phòng (mineshaft, trial chamber, end city, ancient city, Nether, đền biển, thư viện, vườn), phòng khóa tới khi giết hết quái, giết trùm ở phòng cuối là thắng. Có bài test chống kẹt cho mọi tầng.

- **Status:** review
- **Mod / decision:** `immortal-cultivation`, D255-D258 · **Branch:** `feat/P-082-purgatory-dungeon` · **Depends on:** P-081 (D254, branch base)

## Goal
Trial-chamber-like floors: about 7-15 random rooms on a 6×6 cell grid (1 or 2 layers), each module with entrances and exits, locked rooms, special rooms and a boss room at the end. A floor is never built unless the stuck checker passes.

## Scope
- In: pure generator (`purgatory/dungeon/`), block palette and builder, room/boss runtime, Adventure mode, 10 min limit, tests, screenshots.
- Out: new mob types, new rewards, team-run hand test (needs 2 real players).

## Files
| File | Where | Change |
|---|---|---|
| `purgatory/dungeon/*` | main | new: Dir, Mat, Style, Grid, Port, RoomKind, Room, Canvas, Modules (21 modules), BossRooms (6), Layout, Walker (stuck checker), DungeonRules, Dungeon |
| `purgatory/DungeonBuilder.java` | main | new: style palettes, place / clear blocks, loot chests |
| `purgatory/PurgatoryFloor.java` | main | new: room states, gates, waves, traps, darkness, rest, boss and its bar |
| `purgatory/Purgatory.java`, `PurgatorySavedData.java`, `PurgatoryRules.java` | main | run seed and room states, Adventure, 10 min, no trickle spawns, no creepers |
| `purgatory/PurgatoryBox.java` | main | deleted |
| `DungeonLayoutTest`, `PurgatoryRulesTest` | test | stuck check over many seeds and floors |
| `PurgatoryGameTest`, `PurgatoryClientTest` | gametest | world = checked grid, locked room, boss win, timeout; room tour screenshots |

## Tests
- Test scope checklist: areas = purgatory; change type = logic + world blocks + UI screenshots; commands = `test --tests '*Purgatory*' --tests '*Dungeon*'`, `runGameTest -Pgt='*:purgatory*'`, `runClientGameTest`.

## Done when
- [x] Every floor in the JUnit sweep passes the stuck check
- [x] The world holds exactly the checked grid
- [x] Locked room seals and opens; boss kill wins; timeout loses
- [x] Huy plays a floor by hand (2026-11-26: too short, few mobs, skills broke walls, sent home too fast)
- [x] D259: bedrock skin + skill guards (`Purgatory.guarded`), 10+ rooms, more mobs, 2 waves in locked/treasure rooms
- [x] D260: 30 s after the boss dies (paid at once, gates open), Come back now (screen + inventory button)
- [x] D261: more path mobs, rune shulker bag (follows the winner home), bosses 2× size and +20% damage
- [x] D262: 1-6 extra rooms at random, mostly two-door rooms (forks ~12%), loops only join dead ends
- [x] D263: richer modules (mezzanines with stairs, columns, low cover walls, floating platforms with bridges), wooden gift chests in ~50% of rooms (simple_dungeon loot)
- [x] D265: Seal Pillar rooms (RoomKind.PILLAR, 3 modules, 60-health still wither skeleton inside two glowing block displays; shatters on death, room opens; reinforcements every 20 s; game test + screenshots)
- [x] D264: reward rune max level by floor (3 below 20 ... 8 from 100), rune stone colour by kind (custom_model_data select)
- [ ] Huy plays again

## Risks / ask first
- Old saved runs (before P-082) end as a loss that costs nothing; their 64³ box is cleared.
- Big mobs (ravager) only spawn where they fit; a room may get fewer mobs than the wave number.
