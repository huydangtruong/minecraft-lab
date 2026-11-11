# P-003 — Per-world on/off (game rule `permahearts:enabled`)

> **Tóm tắt cho Huy:** thêm game rule "PermaHearts" (mặc định tắt). Lúc tạo world: More → Game Rules → bật. Trong game: `/gamerule permahearts:enabled true|false`. Tắt thì không mất tim, không HeartXP, máu về 10 tim, ẩn thanh HeartXP; số tim đã có vẫn giữ để bật lại. Bản 0.3.0.

- **Status:** ready
- **Mod / decision:** `permahearts`, D10 · **Branch:** `feat/P-003-world-toggle` · **Depends on:** P-002 (merged, 0.2.0)

## Goal

Huy (2026-11-12): "lúc tạo thế giới … tick chọn mod rồi tạo … như tạo một game warcraft và chọn map" and in game "muốn bật hay tắt cái này cũng được". Rules: [spec.md](../mods/permahearts/spec.md) section "Bật/tắt theo world".

## Scope

- In: `mods/permahearts/` only, `mod_version=0.3.0`, docs below.
- Out: custom Create World UI (the vanilla Game Rules screen is the UI), root Gradle files, `scripts/`, pinned versions.

## Verified API (26.3)

- `net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder.forBoolean(false).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier.fromNamespaceAndPath("permahearts","enabled"))` → `net.minecraft.world.level.gamerules.GameRule<Boolean>`.
- Read: `server.getGameRules().get(RULE)` (`MinecraftServer#getGameRules()` and `ServerLevel#getGameRules()` both exist). Write: `GameRules#set(GameRule<T>, T, MinecraftServer)`.
- Change callback: `GameRuleEvents.changeCallback(RULE).register((Boolean value, MinecraftServer server) -> …)`.
- Translation key = `Util.makeDescriptionId("gamerule", id)` → `gamerule.permahearts.enabled`. Command name = `Identifier#toShortString` → `/gamerule permahearts:enabled true`.

## Files (package `io.github.huydangtruong.permahearts`, under `mods/permahearts/src/`)

| File | Change |
|---|---|
| `main/java/.../PermaHearts.java` | `public static final GameRule<Boolean> ENABLED = …` (above). `static boolean enabled(MinecraftServer s)`. Guards: `AFTER_DEATH` hostile-kill → return when disabled. `AFTER_RESPAWN` (keep the phase ordering!) → when disabled: copy `before` to the new player unchanged (no `onDeath`, no message), `HealthApplier.apply(p, true)`. `JOIN` → set the active flag (below) then apply. Register the change callback: for every `server.getPlayerList().getPlayers()`: set active flag, `HealthApplier.apply(p, false)`, `sendSystemMessage(Messages.toggled(value))` |
| `main/java/.../HeartsAttachment.java` | Second attachment `ACTIVE`: `AttachmentType<Boolean>` id `permahearts:active`, **not persistent**, `.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())` (check the exact `BOOL` field name with javap on `ByteBufCodecs`). Helper `setActive(ServerPlayer, boolean)`; set it also on the respawned player in `AFTER_RESPAWN` (not persistent ⇒ not copied) |
| `main/java/.../HealthApplier.java` | `apply(p, refill)`: if `!PermaHearts.enabled(p.level().getServer())` (or `p.level().getGameRules()`) → `removeModifier(MODIFIER_ID)` + health clamp/refill, and return; otherwise unchanged |
| `main/java/.../HeartsCommand.java` | `/hearts`: after the status line, when disabled also `sendSuccess(Messages.disabledHint())`. `set` / `xp` still edit the data (then `apply`, which respects the rule) |
| `main/java/.../Messages.java` + `lang/en_us.json`, `vi_vn.json` | `toggled(boolean on)`: "PermaHearts is now ON in this world" / "… OFF …" (keys `permahearts.msg.on`, `.off`); `disabledHint()`: "PermaHearts is off in this world. Turn on: /gamerule permahearts:enabled true" (`permahearts.msg.disabled`). Game rule name `gamerule.permahearts.enabled`: "PermaHearts: lose a heart on death, earn hearts with HeartXP" / VN "PermaHearts: chết mất tim, kiếm tim bằng HeartXP". If the Game Rules screen looks for a description key too (check `EditGameRulesScreen` / vanilla `en_us.json` for a `.description` suffix pattern), add `gamerule.permahearts.enabled.description` |
| `client/java/.../client/HeartXpHud.java` | Return early when `!p.getAttachedOrElse(HeartsAttachment.ACTIVE, false)` |

## Tests

Game tests run in parallel inside one server and share its game rules. Every test method is synchronous (tick 0), so:

- **Every existing game test** starts with `enable(context, true)`: `MinecraftServer s = context.getLevel().getServer(); s.getGameRules().set(PermaHearts.ENABLED, true, s);` — the default is now `false`, so without this they fail.
- New `disabledWorldIgnoresDeathsAndKills`: inside the method, `enable(false)`; mock player with `HeartsData(5, 100, 0, 0)` (15 hearts); `HealthApplier.apply(p, true)` → max health 20; kill a zombie → `heartXp` still 100; `dieAndRespawn` → new player data still 15 hearts / 100 XP / 0 deaths, max health 20, health 20; `ACTIVE` false on it. Then `enable(true)`, call `HealthApplier.apply(r, false)` → max health 30. **Restore `true` before every return/assert failure path** (use try/finally) so no other test sees `false`.
- New `toggleUpdatesOnlinePlayers`: mock player with 15 hearts, rule true + apply → 30; `set(false)` through `GameRules#set` (fires the callback) → max health 20 and `ACTIVE` false; `set(true)` → 30 and `ACTIVE` true; finally leave `true`.
- JUnit: unchanged (rules are not affected).

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build      # JUnit + all game tests + client compile
```

Do not run `runClient` or `scripts/install.sh`. The Game Rules screen and the HUD are only checked by Huy.

## Docs

- `docs/mods/README.md`: 0.3.0.
- `docs/workflow/playtest.md`: add "Create New World → More → Game Rules → PermaHearts on"; `/gamerule permahearts:enabled false` → 10 hearts, bar hidden; `true` → back.
- `docs/system/02-findings.md`: game rule facts above + anything new (translation keys, `ByteBufCodecs` boolean name).

## Commit

`PermaHearts 0.3.0: per-world game rule permahearts:enabled (P-003)`; push the branch, never `main`.

## Done when

- [ ] `./gradlew :permahearts:build` green, all old game tests still pass with the rule enabled
- [ ] Report in the team.md format (say: Game Rules screen and HUD not seen in game)

## Risks / ask first

- If `GameRules#set` from a game test does not fire `changeCallback`, report it (the in-game `/gamerule` path may differ); do not work around it by polling every tick.
