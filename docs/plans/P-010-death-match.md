# P-010 — Death Match quest line

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Crimson Master có thêm loại nhiệm vụ **Death Match**: bấm Start, rồi một nửa số người đang online (làm tròn lên, tính người khác nhau) phải bị người chơi khác giết. Chết trong trận do người chơi giết: **không mất tim, không rơi đồ**. Chỉ ra nhiệm vụ này khi có từ 2 người online. Bản 0.9.0.

- **Status:** ready
- **Mod / decision:** `permahearts` (Immortal Cultivation), D25 · **Branch:** `feat/P-010-death-match` · **Depends on:** none

## Goal

New event line `DEATH_MATCH`. Start button → needed = ceil(online players / 2) → each **distinct** player killed by **another** player while the match runs counts once → when the count reaches needed the line is done (reward slot, like Blood Moon / Boss). PvP deaths during the match keep hearts, inventory and XP.

## Rules (D25)

| Rule | Value |
|---|---|
| Offered | Only if at least **2 players are online when the quest is generated** (`Wanderer.newQuest`). Weight like a Blood Moon line; at most one Death Match per quest; target string `minecraft:player` (so the duplicate-target rule keeps it unique). |
| Start | Button "Start" on the line (same place as Blood Moon). Refused (button inactive, tooltip "Needs at least 2 players online") when fewer than 2 players are online or PvP is off on the server. On start: `needed = (online + 1) / 2`, saved as the line's `required`; progress 0. Broadcast: `Death Match! %1$s of %2$s players must fall to another player. Deaths in this match cost no hearts and no items.` |
| Counts | A `ServerPlayer` dies, its kill credit (`getKillCredit()`) is a **different** `ServerPlayer`, and the match is running. Each victim UUID counts once (`progress` = number of distinct victims). Any dimension. Broadcast: `%1$s fell to %2$s (Death Match %3$s/%4$s)`. |
| Done | progress ≥ required → `Wanderer.eventDone` (existing "line completed" flow), match state cleared. |
| Protection | For every death that matches **Counts** (also a second death of an already counted player, also the death that finishes the match): **no heart lost**, **no items dropped, inventory and XP kept** (like `keepInventory`). Message to the victim after respawn: `You fell in the Death Match: no heart lost, items kept.` Deaths to mobs, falls etc. during the match are normal deaths. |
| Ends otherwise | Give up, the Crimson Master leaving (time up) or `/quest reset` clear the match. A running Death Match does **not** hold the Crimson Master back (unlike Blood Moon / Boss in D20). Players leaving the server do not end it. |
| ImmortalCultivation off | Like other lines: kills do not count while the switch is off (follow what `Wanderer.onKill` / Blood Moon do today). |

## Steps

### 1. Pure rules (JUnit, no Minecraft imports)

- `QuestType`: append `DEATH_MATCH` (append only — saved by name). `isEvent()` already returns true for it.
- New `wanderer/DeathMatchRules.java`: `static int needed(int online)` = `(online + 1) / 2` (online < 2 → 0 = cannot start); `static boolean canOffer(int online)` = online ≥ 2.
- New `wanderer/DeathMatchState.java` record `(int line, List<String> fallen)` + `CODEC` (fallen = victim UUID strings). Add `Optional<DeathMatchState> deathMatch` to `WandererData` as **the last field**, `optionalFieldOf("death_match")`, with `withDeathMatch`, and update every `with*` and `DEFAULT` (old saves load with empty).
- `QuestLogic`: `static Quest deathMatchStarted(Quest q, int line, int needed)` (sets `required` = needed, progress 0) and `static Quest deathMatchProgress(Quest q, int line, int fallen)` (progress = min(fallen, required)). `QuestLine` needs a `withRequired` helper (package-private, like `withProgress`).
- `QuestGenerator.generate(long id, RandomGenerator rng, int online)`: new type in `pickType` (weight 12, the Blood Moon weight) only when `canOffer(online)`; otherwise re-pick. Keep `generate(long, RandomGenerator)` delegating with `online = 1` so existing tests and callers keep working. `Wanderer.newQuest` passes `server.getPlayerList().getPlayerCount()`.

### 2. Server glue — new `wanderer/DeathMatch.java`

- `start(server, line, player)`: checks line type, not done, no match running, online ≥ 2, PvP allowed (find the 26.3 API with javap: PvP became a game rule in recent versions — look for `GameRules` field like `PVP` / `pvp`; fall back to `MinecraftServer#isPvpAllowed` if that exists). Saves state + quest, broadcasts. Returns boolean (wire into `Wanderer.startEvent`).
- `running(server)`, `onReset(server)` (clear state; call from `Wanderer.leave` and `Wanderer.reset` next to `BloodMoon.onReset` / `QuestBoss.onReset`).
- `isProtectedDeath(server, ServerPlayer victim, DamageSource source)`: running && credit is another ServerPlayer. Use `victim.getKillCredit()`; if it is null at that moment, also accept `source.getEntity() instanceof ServerPlayer` (different from victim).
- `onPlayerKilled(server, victim)` from `AFTER_DEATH`: if protected-death conditions hold, add UUID if new, update quest progress, broadcast, finish when reached.
- `Wanderer.eventState`: 1 while the match for that line runs. `WandererScreen`: DEATH_MATCH is an event row (`isEventRow` / button label "Start" → "Running…" while state 1), icon `Items.IRON_SWORD`; inactive button + tooltip when fewer than 2 players online (send the online count to the client through the menu's data slots the way other numbers are sent, or disable server-side only and show the refusal message — dev's choice, report it).
- `Wanderer.targetName` / `lineText` switch cases: name `Death Match`; line text before start `Death Match: half the players must fall to another player`, after start `Death Match %s/%s fallen`. All strings in `Messages` + `lang/en_us.json` (English only, D16).

### 3. Keep hearts, items and XP on a protected death

- New persistent player attachment `permahearts:dm_protected` (Boolean, `copyOnDeath`, default false) in `HeartsAttachment` (or a new small class next to it).
- `ServerLivingEntityEvents.ALLOW_DEATH` (verify the name/signature in Fabric API 0.166.2 with javap): if the entity is a ServerPlayer and `DeathMatch.isProtectedDeath(...)`, set the flag to true. Always return true (never cancel deaths).
- Drops: add a mixin (first mixin in this mod: `permahearts.mixins.json`, `"mixins"` entry in `fabric.mod.json`, package `io.github.huydangtruong.permahearts.mixin`). Look up with javap where vanilla 26.3 decides player death drops and XP (expected: `Player#dropEquipment(ServerLevel)` and the XP reward method that checks `GameRules` keep-inventory). Cancel both when the flag is set. Write down the exact names in `docs/system/02-findings.md`.
- Restore: `ServerPlayerEvents.COPY_FROM (old, new, alive)`: when `!alive` and old's flag is set, copy inventory (`getInventory().replaceWith(old.getInventory())` or the 26.3 equivalent) and experience (level, progress, total, score) the way vanilla `ServerPlayer#restoreFrom` does for keepInventory — read `restoreFrom` with javap -c and mirror it.
- Hearts: in the existing `AFTER_RESPAWN` handler in `PermaHearts`, when `!alive` and the flag is set: keep `before` (no `HeartRules.onDeath`), `HealthApplier.apply(p, true)`, send the protected message, then clear the flag. The flag must be cleared on the new player in every path.
- Because the flag is persistent and copied, a host that quits while a victim is on the death screen still restores correctly on the next respawn.

### 4. Version

`mod_version=0.9.0`.

## Files

| File | Change |
|---|---|
| `wanderer/QuestType.java` | append `DEATH_MATCH` |
| `wanderer/DeathMatchRules.java`, `DeathMatchState.java`, `DeathMatch.java` | new |
| `wanderer/WandererData.java`, `QuestLine.java`, `QuestLogic.java`, `QuestGenerator.java`, `Wanderer.java` | as above |
| `PermaHearts.java`, `HeartsAttachment.java` | ALLOW_DEATH, COPY_FROM, AFTER_DEATH, AFTER_RESPAWN changes, flag attachment |
| `mixin/…`, `permahearts.mixins.json`, `fabric.mod.json` | drops/XP cancel |
| `client/WandererScreen.java`, `WandererMenu.java` (if a data slot is added) | event row |
| `Messages.java`, `lang/en_us.json` | new strings |
| `mods/permahearts/gradle.properties` | 0.9.0 |
| tests (below) | |

## Tests

- JUnit: `needed(2)=1, needed(3)=2, needed(4)=2, needed(5)=3, needed(1)=0`; `canOffer(1)=false, canOffer(2)=true`; `generate(..., online=1)` never contains DEATH_MATCH over 2000 seeds; with `online=2` some quest contains exactly one DEATH_MATCH and never two; `deathMatchStarted` / `deathMatchProgress`; `WandererData` codec round trip with and without `death_match` (old save without the field loads).
- Game tests (`WandererGameTest`), with 2–3 mock players (`helper.makeMockServerPlayerInLevel()`; check they count in `getPlayerList().getPlayerCount()`, and if not, add a package-private test override for the online count and say so):
  - start refused with 1 player; with 3 players `required == 2`;
  - player A kills B with `damageSources().playerAttack(A)` → progress 1; B killed again → still 1; C killed → done, state cleared;
  - B (killed by A in the match) keeps its inventory items and its hearts after respawn (`server.getPlayerList().respawn(...)` or the mock equivalent — reuse how existing heart tests respawn);
  - B killed by a zombie during the match → loses a heart (normal death) and does not count;
  - Give up during a match clears it.
- Client screenshot test: add a DEATH_MATCH line to the fixed quest in `QuestScreenClientTest` (screenshot 02 shows it). Keep the existing shots.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
./gradlew :permahearts:runClientGameTest
```
Copy the screenshots from `mods/permahearts/build/run/clientGameTest/screenshots/` to `build/screens/` at the worktree root before anything runs `build` again.

## Docs

`docs/mods/permahearts/spec.md` (Death Match section, in Vietnamese like the rest), `docs/system/02-findings.md` (PvP API, drop/XP methods, ALLOW_DEATH, COPY_FROM order), `docs/mods/README.md` (0.9.0 line). D25 is already recorded.

## Commit

Small commits on `feat/P-010-death-match`, English messages ending with `Co-Authored-By: Claude Sonnet <noreply@anthropic.com>`. Push the branch, never main. Do not run `scripts/install.sh`, `package.sh` or `release.sh`. Report ≤ 10 lines.

## Done when

- [ ] Build green (all old tests + new ones), client test screenshots show the Death Match line.
- [ ] A protected death keeps hearts, items and XP; a normal death during the match still costs a heart.
- [ ] Old saves (no `death_match`, no flag) load unchanged.

## Risks / ask first

- If cancelling drops is impossible without touching more than the two vanilla methods, stop and report the method names found instead of patching widely.
- Mock players may not behave like real ones for respawn; if a protection test cannot run as a game test, cover it in the client test (one real player can be killed by a mock attacker? if not, report it untested) — do not fake it.
