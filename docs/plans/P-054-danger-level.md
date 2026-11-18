# P-054 — Danger level and elite mobs

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** thế giới có Sát Khí 0–30, tăng 1 sau mỗi lần Crimson Master rời đi. Mob đông hơn, mạnh hơn, ra cả ban ngày, mặc đồ tốt dần (Lv 30 có netherite). Từ Lv 10 có mob Elite với 5 phẩm chất. Mỗi lần tăng có chữ cam giữa màn hình (D137, D138).

- **Status:** in-dev
- **Mod / decision:** `immortal-cultivation`, D137, D138 · **Branch:** `feat/P-054-danger-level` · **Depends on:** none (P-053 is on main)

## Goal
A world-wide danger level that grows after every Crimson Master visit and scales hostile mobs: their count, strength, daytime spawns, new types, gear, and elite affixes.

## Rules (pure Java `danger/DangerRules.java`, JUnit `DangerRulesTest`)
Level `L` in 0..30 (0 = vanilla).
- `spawnBonus(L)`: `0.10·L` for L ≤ 20; `2.0 + 0.20·(L−20)` for 21..25; `3.0` for L ≥ 25.
- `strengthBonus(L)`: `0.075·L` for L ≤ 20; `1.5 + 0.15·(L−20)` above (2.25 at 25, 3.0 at 30). This applies to max health **and** attack damage, as `ADD_MULTIPLIED_BASE` modifiers.
- `daySpawnFactor(L)`: 0 for L < 8; linear 0.15 at L 8 → 1.0 at L 20. `rainSpawns(L) = L ≥ 6`.
- `unlockedTypes(L)`: Pillager 4, Vindicator 6, Blaze + Wither Skeleton 10 (light ≤ 7 only), Evoker 12, Piglin Brute 14 (never zombifies), Ravager 16.
- Gear (`gearRoll(L, rng)`): none extra for L ≤ 20. From 21, per-slot chance 20% at L 21 → 70% at L 30. Material weights move from iron to diamond to netherite, and the enchant chance rises. At L 30, every armor-capable mob gets **at least one netherite piece**.
- Elites: `eliteChance(L)`: 0 for L < 10; 2% at L 10 → 10% at L 20 → 15% at L 30 (linear in each part). `maxAffixes(L)`: 1 for 10–19, 2 for 20–29, 3 at 30 (roll 1..max, distinct).
- `notice(L)`: the title is "Danger level L". The subtitle gives the totals plus what this level unlocks, e.g. "+80% mobs, +60% strength, day spawns", "… , elites appear", "… , Ravagers appear", "… , more armor", "… , netherite armor". JUnit pins a few exact strings.

## Behaviour
1. **State:** `DangerSavedData` (overworld) `{ level, pendingAtTick }`. The codec uses the `permahearts` namespace and defaults to 0 for old worlds.
2. **Growth:** in `Wanderer.leave(...)` (every leave path goes through it; check `farewellIfFinished` and the give-up path too), set `pendingAtTick = now + 4800` (1/5 day). A server tick checks it, and when due: `level = min(30, level + 1)`. Then broadcast to every online player:
   - a **title** "Danger level X" in **orange** (same orange as the Crimson Master notice, D102);
   - the subtitle from `notice(L)` in a lighter colour;
   - one chat line with the same text;
   - a horn sound (e.g. raid horn).
   Gamerule off: no growth, and every effect below behaves as level 0.
3. **Strength:** on the first spawn of any `Enemy` (`ENTITY_LOAD` with a "danger applied" tag so it never applies twice), add the two modifiers `permahearts:danger_health` and `permahearts:danger_damage` from the current level, then heal to full. Exclusions: God Slayer, quest chieftains (tag), Crimson Master event bosses, Soul Grasp mobs, `OwnedSummon`s, Illusion clones, Wither, Ender Dragon, Warden. Mobs that already exist keep their values.
4. **More spawns (Overworld only):** raise the hostile cap and the spawn attempts by `1 + spawnBonus(L)`. Find the 26.3 natural spawn path with javap (`NaturalSpawner`, `MobCategory.MONSTER` max instances, `SpawnState`). Prefer one mixin on the cap or the attempt count. Add a hard ceiling gamerule (default 4× vanilla) so lag stays bounded.
5. **Day and rain spawns (Overworld):** when the sky light would block a monster spawn, allow it with probability `daySpawnFactor(L)` (or always in rain when `rainSpawns`). Tag such mobs `permahearts:day_spawn`; they never burn in sunlight (mixin on the sun-burn check, as Soul Grasp already does: reuse it).
6. **New types:** add the unlocked types to the Overworld monster spawn picks (weights small, like a witch). Piglin Brute gets zombification immunity. Blaze and Wither Skeleton spawn only at block light ≤ 7.
7. **Gear:** on spawn (same hook as 3), for armor-capable humanoids (`Zombie` family, `AbstractSkeleton`, `AbstractPiglin`, `AbstractIllager`, …), roll gear for L ≥ 21. Its drop chance stays vanilla-low (0.085).
8. **Elites:** on spawn, roll `eliteChance`. An elite gets:
   - ×2 max health (on top of danger);
   - orange custom name "<Affix…> <Type>" ("Swift Ironhide Zombie"), always visible;
   - orange particles every second.

   The 5 affixes:
   - **Swift:** +40% speed.
   - **Ironhide:** takes 40% less damage.
   - **Regenerating:** heals 1% of max health per second.
   - **Blazing:** a hit sets the target on fire for 4 s; the elite is fire immune.
   - **Venomous:** a hit poisons for 5 s.

   Store the affixes on the mob (attachment or tag list) so they survive reload, and so a Soul Grasp tame keeps them (do not strip them in `Grasp`). Drops: 50% chance of 1–3 emeralds.
9. **Commands** (op): `/danger info` (level, time to next increase, current numbers), `/danger set <0..30>` (sends the notice), `/danger elite <affix…>` (summons an elite zombie in front of the caller, for testing).
10. **Stats panel:** add a line "Danger level X" to the player stats screen (`StatsSync`) so it is visible at any time.

## Tests
- JUnit `DangerRulesTest`: all curves at 0, 1, 8, 10, 20, 21, 25, 26, 30; affix count; gear at L 30 always has a netherite piece (many seeded rolls); notice strings.
- Server game tests:
  1. `Wanderer.leave` → level +1 after 4800 ticks, not before; capped at 30.
  2. A zombie spawned at L 20 has 2.5× base health and damage; a God Slayer or chieftain does not change.
  3. A day-spawned zombie does not burn.
  4. `/danger elite swift venomous` → orange name and speed; a hit poisons a target.
  5. Ironhide takes 40% less damage; Regenerating heals.
  6. At L 30, gear rolls give ≥ 1 netherite piece.
  7. Gamerule off → no growth and no effects.
- Client screenshot test: the orange "Danger level X" title with its subtitle, and an elite zombie with its orange name.

## Verify
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :immortal-cultivation:build
./gradlew :immortal-cultivation:runClientGameTest
```
LARGE change. Known flaky tests (farm-seeds, golem guardian, thunder piglin) may fail once: rerun.

## Docs
Vietnamese section in `docs/mods/immortal-cultivation/spec.md` (danger table, elites). New API names go in `02-findings.md`.

## Commit
Branch `feat/P-054-dev` from `feat/P-054-danger-level`. Commit each part (rules, state + notice, strength + gear, spawns, elites, commands), then `git push . feat/P-054-dev:feat/P-054-danger-level`. Do not push to origin. Commits in English with the Co-Authored-By line.

## Done when
- [ ] All of the above works; JUnit, server and client game tests green.
- [ ] Old worlds load at level 0.

## Risks / ask first
- **Lag:** +300% hostile mobs per player is heavy. Keep the ceiling gamerule. Report the mob count at L 25 in a flat test world if easy.
- Changing natural spawn rules is the riskiest mixin. If the cap/attempt hook is not clean on 26.3, report before trying anything invasive.
- Day spawns must not happen in Peaceful, or in light-protected spots that vanilla also forbids for block light (keep the block-light check, relax only sky light).
