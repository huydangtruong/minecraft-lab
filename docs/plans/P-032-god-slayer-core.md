# P-032 — God Slayer core (Lv 1, hunt, appearance, reward, commands)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** boss God Slayer (quỷ Oni song kiếm) với đủ khả năng nội tại: chạy nhanh, bay theo, bơi gấp 3 cá heo, phá block, tự dịch chuyển tới người chơi, chống lừa. Đợt này chỉ có chiêu Lv 1 (chém thường).
> Nó xuất hiện từ ngày 100, mỗi lần thứ 4 Crimson Master tới, khi có người ≥ 30 tim; báo trước 60 s, săn lần lượt tối đa 10 phút. Hạ được: +100 level, 5 rương, boss lên level. Lệnh `/boss summon`, `/boss giveup`, `/boss level`.

- **Status:** ready
- **Mod / decision:** `permahearts`, D96, D97 (idea + answers: `docs/mods/permahearts/god-slayer-idea.md`) · **Branch:** `feat/P-032-god-slayer` · **Depends on:** none
- **Next:** P-033 adds the skills of Lv 2–10. Build the AI so a skill is one small class plugged into a list by level (see "Skill hook").

## Goal

A new boss entity `permahearts:god_slayer` with an original Oni look, a world-wide level (1–10), the hunt flow, the spawn trigger, the reward and three OP commands. Only the Lv 1 skill (one sword slash) in this plan, but every passive ability.

## Numbers (pure Java `godslayer/GodSlayerRules.java`, JUnit)

- `scale(level) = 1 + 0.2 * (level - 1)`, level clamped 1..10.
- `maxHealth(level) = 500 * scale` → 500, 600 … 1400.
- `slashDamage(level) = 30 * scale` → 30 … 84. Every later skill damage also multiplies by `scale`.
- `MAX_DAMAGE_PER_HIT = 40` (after armor etc.: clamp the amount the boss takes).
- `HUNT_TICKS = 12000` (10 min), `WARNING_TICKS = 1200` (60 s).
- `MIN_DAY = 100` (overworld day = dayTime / 24000, use the same day source the Crimson Master code uses), `VISIT_PERIOD = 4` (visits 4, 8, 12 …), `MIN_HEARTS = 30`.
- Teleport: target farther than `TELEPORT_RANGE = 48` blocks, or no distance progress toward the target for `STUCK_TICKS = 100` while it cannot hit it, or the target in another dimension → teleport to a free spot 3–5 blocks from the target.
- Slash: reach like a big mob (~3.5 blocks), cooldown 20 ticks.
- Swim speed: 3 × the dolphin's water speed (read the dolphin's attribute/move-control numbers with javap, note them in 02-findings.md).
- Regen (anti-cheese): if the boss has taken player damage in the last 5 s but cannot reach that player (no path and the player is out of reach), or no player has hit it for 30 s while it is hunting → heal 2% max health per second.
- Reward: `rewardBoxes(rng)` → 5 tiers, each SILVER/GOLD/DIAMOND/EMERALD at 25% (D96). Not the D90 odds.
- Test what is pure: scale table, damage clamp, teleport decision (`shouldTeleport(distance, sameDimension, ticksNoProgress)`), visit/day/hearts eligibility, reward roll counts.

## Entity

- `godslayer/GodSlayer.java` extends `Monster` (check the right base with javap; it needs ground, flying and swimming movement). Register like the other entities (`WandererNpc`, `WarriorAngel`, `VioletGuardian`): entity type, attributes, client renderer.
- **Size:** about 3 blocks tall (base hitbox ~1.0 × 2.9 or vanilla scale attribute — pick one and say which).
- **Movement modes** (switch every few ticks):
  - Ground: fast walk (MOVEMENT_SPEED ~0.4, follow range 128), ground path navigation, step height 1.5.
  - Air: when the target is airborne/flying, standing ≥ 4 blocks above ground the boss cannot path to, or on a pillar → flying move control + flying navigation, **no noPhysics** (does not pass walls, D96). Back to ground when the target is grounded and reachable.
  - Water: when in water → swim with 3× dolphin speed (custom move control or speed boost in water); no drowning.
- **Block breaking** (passive): when stuck (no progress, colliding horizontally) or the path is blocked, break the non-bedrock, non-unbreakable blocks in its hitbox + 1 around, like the Wither's `destroyBlocks` (look at `WitherBoss` with javap / the source pattern). Ignore `mobGriefing`? → **respect** it: when off, teleport instead.
- **Anti-cheese** (passive):
  - Immune to all damage whose source entity is not a player (or a player's projectile / summon owned by a player): fall, fire, lava, drowning, suffocation, cactus, explosions not caused by a player, other mobs. `fireImmune`.
  - `MAX_DAMAGE_PER_HIT` clamp.
  - Cannot ride (boats, minecarts), cannot be leashed, not pushed by pistons, no knockback (KNOCKBACK_RESISTANCE 1).
  - Technique control effects (Frozen Breath slow, Repulse push) work at ~10%: reduce the slow/push on this entity (find where these techniques apply them; a check on the entity type or a tag is enough).
  - Persistent, no despawn, no drops, `setPersistenceRequired`.
- **Lv 1 skill — Slash:** one sword hit for `slashDamage(level)` when in reach, cooldown 20 ticks, swing animation.
- **Skill hook** (for P-033): `GodSlayerSkill` interface (`unlockLevel()`, `canStart(boss, target)`, `tick(...)`, `isDone()`), a list in `GodSlayerSkills` filtered by the current level; the AI picks one ready skill at a time. Slash is the only entry now.

## Look (original art, nothing from other games)

- Humanoid model (player/zombie-like body, the vanilla humanoid model is fine), red skin, two small horns, white messy hair, black samurai shoulder plates, yellow glowing eyes (an emissive eye layer like the spider/enderman eyes layer).
- Two katanas: one in each hand (render katana items in both hands; a simple katana item texture, not obtainable).
- Texture drawn by a script `tools/make_god_slayer_texture.py` (same way `tools/make_angel_texture.py` draws the Warrior Angel). Keep the script in the repo.
- When flying: a trail of black smoke particles. Arrival/teleport: a burst of smoke and reverse portal particles + a deep sound.
- Boss bar: `ServerBossEvent`, color RED, overlay NOTCHED_10, name `God Slayer — Lv N`, shown to every player in the same dimension within 96 blocks, darken screen.

## Saved state (`godslayer/GodSlayerSavedData.java`, world SavedData, overworld)

- `level` (1..10, start 1), `visitCount` handled where the Crimson Master arrives (see Trigger), hunt state: `phase` (IDLE, WARNING, HUNTING), `warningEndTick`, `huntEndTick`, `bossUuid`, `currentTarget`, `killed` (set of player UUIDs killed this hunt), `firstTargetName`.
- On server start with phase HUNTING but no boss → end the hunt quietly. WARNING survives a restart.

## Hunt flow (`godslayer/GodSlayerHunt.java`, ticked from the existing scheduler)

1. **Start** (trigger or `/boss summon`): pick a random online, non-creative/spectator player with ≥ 30 hearts (any player for `/boss summon`, or the one named). Phase WARNING for 60 s:
   - Title + subtitle to every player: "God Slayer" / "is coming for <A> in 60s" (English, `Messages`), then a chat line, and a drum-like sound (`NOTE_BLOCK_BASEDRUM` low pitch, a few hits).
   - A countdown in the action bar for everyone during the last 10 s.
2. **Appear** at the then-current position of A: 15–20 blocks away on a free spot (reuse `SpawnSpots` logic used by the bosses), same dimension as A. Phase HUNTING for 10 min. Target A.
3. **Hunting:** chase the target with the passive abilities. When the target dies (any cause while targeted, or killed by the boss): add to `killed`, pick the next online, non-creative/spectator player not in `killed` (any dimension, any hearts) and **teleport to them** (cross-dimension: `teleportTo(ServerLevel, ...)`, check the 26.3 name). Players who log off are skipped; if they log back in during the hunt they are valid targets again.
4. **End:**
   - **Defeated** (boss dies, killed by a player): reward the killer (kill credit player; if none, the last player that hit it), level +1 (max 10), broadcast "God Slayer has fallen. It will return stronger. (Lv N)".
   - **Won:** hunt time over, or no valid target left → broadcast a taunt (e.g. "God Slayer: *Weak. I will come back.*"), smoke burst, remove the boss. Level unchanged.
   - **/boss giveup** → remove the boss, clear the state, short chat line. Level unchanged.
5. Only one hunt at a time.

## Trigger (Crimson Master)

- Where the Crimson Master arrives (in `Wanderer` / `WandererScheduler`; find the arrival point), increment `visitCount` (store it with the God Slayer data or the wanderer data, one place).
- If day ≥ 100 and `visitCount % 4 == 0`:
  - Some online player has ≥ 30 hearts → start the hunt (WARNING).
  - Else → chat to everyone: "God Slayer: *Weaklings. Come back when your hearts can bear my blade.*", no hunt.
- Does not change Crimson Master behavior otherwise.

## Reward

- Killer: `giveExperienceLevels(100)` and 5 reward boxes, tier rolled now at 25% each, `RewardBoxes.item(tier)` (or the box data the quest reward uses; follow how a box is given in `Wanderer`/`RewardContainer`). Full inventory → drop at the player's feet.
- Chat to the killer listing what they got.

## Commands (`godslayer/BossCommand.java`, OP like `/hearts set`)

- `/boss summon [player]` → start a hunt now (with the 60 s warning), skipping the day/visit/hearts checks. Refuse if a hunt is running.
- `/boss giveup` → end the running hunt (warning or hunting).
- `/boss level [1..10]` → show or set the level (for tests).

## Files

| File | Where | Change |
|---|---|---|
| `godslayer/*.java` | main | new: `GodSlayer`, `GodSlayerRules` (pure), `GodSlayerSavedData`, `GodSlayerHunt`, `GodSlayerSkill`, `GodSlayerSkills`, `SlashSkill`, `BossCommand`, entity registration |
| `PermaHearts.java` | main | register entity, attributes, command, hunt tick, death/damage hooks |
| `wanderer/Wanderer.java` or `WandererScheduler.java` | main | visit count + trigger |
| technique Frozen Breath / Repulse code | main | ~10% effect on the boss |
| `Messages.java`, `en_us.json` | main | texts, entity name "God Slayer" |
| `client/GodSlayerRenderer.java`, `PermaHeartsClient.java` | client | renderer, eyes layer, katanas |
| `textures/entity/god_slayer*.png`, katana item texture | resources | from `tools/make_god_slayer_texture.py` |
| `test/.../GodSlayerRulesTest.java` | test | JUnit |
| `gametest/.../GodSlayerGameTest.java` | gametest | server game tests |
| `gametest/.../GodSlayerClientTest.java` | gametest | one screenshot of the boss (muted client) |

## Tests

- JUnit: everything in "Numbers".
- Server game tests (keep each one in a loaded arena; avoid timing flakes — see the fixes in commit 06f58ba):
  - spawn at Lv 1: max health 500; `/boss level 3` then spawn → 700.
  - non-player damage (lava, fall, a zombie's hit) does nothing; a player's 1000 hit takes exactly 40.
  - slash hits a player for 30 at Lv 1.
  - teleport: target moved 60 blocks away → boss within 6 blocks after a few ticks.
  - target in the Nether → boss follows into the Nether.
  - kill the boss with a player → killer +100 levels and 5 boxes in inventory; level becomes 2; state cleared.
  - hunt ends at the time limit → boss removed, level unchanged.
  - trigger: day 100, visit 4, player with 30 hearts → WARNING; with 29 hearts → no hunt.
  - water: in water its speed is clearly above a dolphin's (measure distance over N ticks).
- Client test: one screenshot of the boss standing, front view (check the look; send the path in the report).

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```

Run the full build once at the end. Client tests muted (MuteTestClientMixin).

## Docs

- `docs/system/02-findings.md`: every new API name checked with javap (flying/swim move controls, Wither block breaking, cross-dimension teleport, boss bar overlay, dolphin speed).
- `docs/mods/permahearts/spec.md`: a short Vietnamese "God Slayer" section (passives, Lv 1, trigger, hunt, reward, commands).
- `docs/workflow/playtest.md`: a Vietnamese section to try it (`/boss level`, `/boss summon`, fly/swim/pillar/Nether escape, kill it, reward).

## Commit

Small commits on `feat/P-032-dev`, merge into `feat/P-032-god-slayer`, push both. English messages ending with the Co-Authored-By line.

## Done when

- [ ] Boss spawns with the Oni look, boss bar, Lv 1 slash.
- [ ] Ground, flying (no wall passing), swimming 3× dolphin, block breaking, teleport (far, stuck, other dimension).
- [ ] Anti-cheese rules hold in tests.
- [ ] Hunt: 60 s warning, one target at a time, next target after a kill, 10 min limit.
- [ ] Trigger on the Crimson Master's 4th visit from day 100 with a ≥ 30-heart player; taunt otherwise.
- [ ] Reward and level-up on defeat; commands work.
- [ ] `./gradlew :permahearts:build` green.

## Risks / ask first

- Flying without wall passing can get stuck under ceilings: teleport covers it; say in the report if the flying feels bad.
- If a passive ability needs much more than the plan (e.g. custom pathfinding), stop and ask instead of growing the scope.
- Do not touch Huy's saves or run `install.sh`.
