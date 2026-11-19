# P-059 Deadly Treasure (D150)

A guarded chest ~1000 blocks from a player, 2 days after the Crimson Master or the God Slayer left; lasts 1 day.

## Code (package `treasure/`)
- `TreasureRules` (pure): timings, groups of Mega guards (land / water), weighted loot (bottle + apple + 6..8 random, no repeats), `scheduleAt`.
- `TreasureSavedData` (`permahearts:deadly_treasure`, overworld): phase IDLE/PENDING/ACTIVE, start/end on `Wanderer.clockTime`, event id, x/z, chest pos, group, living guard UUIDs, chests to clean up.
- `DeadlyTreasure`: 1 Hz tick (start when due; place when a player is within 96 blocks and the spot is entity-ticking; guards without target attack the nearest survival player within 24; time-out = fail). Spot: 60% cave (air pocket 7x4x7 ≥75% air, no fluid, no sky, within 32 blocks), else surface; deep water (≥3) = waterlogged chest on the floor + water group. Lock via `UseBlockCallback` / `PlayerBlockBreakEvents.BEFORE`. `AFTER_DEATH` removes guards; last one = success. `ENTITY_LOAD` discards guards of a finished event. Failed chests are removed when their chunk loads. Command `/treasure`.
- Triggers: `Wanderer.leave` + `farewellIfFinished` (same line as `Danger.onWandererLeft`), `GodSlayerHunt.won` and the player kill.
- `MegaMobs`: attachment `permahearts:mega` (species, mult, treasure id; synced to all), creation (x health base, SCALE 1.3, knockback resistance 0.5, home radius 12, persistent, coloured name, hoglin/piglin immune to zombification, drowned with trident). `MegaDamageMixin` multiplies `hurtServer` damage whose `source.getEntity()` is Mega. Excluded from danger scaling.
- Client: `MegaRenderStateMixin` puts the species on the render state (`RenderStateDataKey`), `MegaGlowLayer` (registered by `LivingEntityRenderLayerRegistrationCallback` for the 16 types) draws `textures/entity/mega/<species>.png` with `RenderTypes.eyes`.
- Glow textures: `scripts/mega-glow/GenMegaGlow.java <26.3.jar> <out> [preview]` keeps eye/mouth/pattern pixels of the vanilla texture in the species colour.
- Crimson `RewardRoll.BASE_ITEMS`: iron, redstone, lapis, gunpowder removed.

## Known gaps
- Explosions (TNT) can still break the locked chest; hoppers under it can pull items.
- Endermen teleport and may leave the home radius.

## Tests
JUnit `TreasureRulesTest`. Game tests `TreasureGameTest` (place + lock + success, time-out, trigger, x4 damage). Client `TreasureClientTest` (title, glow line-up screenshots).
