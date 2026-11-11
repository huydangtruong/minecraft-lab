# P-001 — PermaHearts

> **Tóm tắt cho Huy:** code mod đầu tiên. Chết mất 1 tim vĩnh viễn (sàn 1 tim), giết 100 mob hostile được +1 tim (trần 30), lưu theo người chơi, có lệnh `/hearts`, tin nhắn tiếng Anh + Việt, test JUnit + game test. Huy sẽ thấy: chơi thử trong `runClient`, sau đó `scripts/install.sh` rồi chơi với bạn.

- **Status:** ready
- **Mod / decision:** `permahearts`, D1, D2, D5 · **Branch:** `feat/P-001-permahearts` · **Depends on:** none (build skeleton already works)

## Goal

Huy (2026-11-11): "mỗi khi chết, sẽ mất một heart vĩnh viễn. Mỗi khi giết 100 mob hostile sẽ có thêm 1 heart vĩnh viễn." Rules, exact: [docs/mods/permahearts/spec.md](../mods/permahearts/spec.md). Read it first.

## Scope

- In: everything in `mods/permahearts/`.
- Out: HUD/client code, config files, other mods, any change to root Gradle files or pinned versions. Do not change `src/main/resources/fabric.mod.json` except to add nothing (mixins are not needed: only Fabric API events).

## Files (package `io.github.huydangtruong.permahearts`, under `mods/permahearts/src/main/java/io/github/huydangtruong/permahearts/`)

| File | Content |
|---|---|
| `HeartsData.java` (new) | `record HeartsData(int bonusHearts, int killProgress, int totalHostileKills, int totalDeaths)`; `static final HeartsData DEFAULT = new HeartsData(0,0,0,0)`. `bonusHearts` is the offset from 10 hearts and may be negative (−9..+20). Pure Java |
| `HeartRules.java` (new) | Pure Java, no Minecraft imports. Constants `BASE=10, MIN=1, MAX=30, KILLS_PER_HEART=100`. `maxHearts(d) = BASE + d.bonusHearts()`. `enum DeathOutcome {LOST, AT_FLOOR}`, `enum KillOutcome {PROGRESS, GAINED, AT_CAP}`; `record DeathResult(HeartsData data, DeathOutcome outcome)`, `record KillResult(HeartsData data, KillOutcome outcome)`. `onDeath(d)`: `totalDeaths+1`; if `maxHearts(d) > MIN` then `bonusHearts-1`/`LOST` else unchanged hearts/`AT_FLOOR`. `onHostileKill(d)`: `totalHostileKills+1`, `killProgress+1`; if progress reaches 100: progress=0 and, if `maxHearts(d) < MAX`, `bonusHearts+1`/`GAINED`, else `AT_CAP`; otherwise `PROGRESS`. `withHearts(d, n)` clamps n to 1..30 and sets `bonusHearts = n-BASE`. `withProgress(d, n)` clamps 0..99 |
| `HeartsAttachment.java` (new) | `Codec<HeartsData> CODEC` via `RecordCodecBuilder` (fields `bonus_hearts, kill_progress, total_kills, total_deaths`, each `Codec.INT.optionalFieldOf(name, 0)`); `AttachmentType<HeartsData> HEARTS = AttachmentRegistry.create(Identifier.fromNamespaceAndPath("permahearts","hearts"), b -> b.persistent(CODEC).copyOnDeath().initializer(() -> HeartsData.DEFAULT))`. Static helpers `get(ServerPlayer)` (`getAttachedOrCreate`) and `set(ServerPlayer, HeartsData)` (`setAttached`). Register by touching the class from `onInitialize` |
| `HealthApplier.java` (new) | `static void apply(ServerPlayer p, boolean refill)`: `AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH)`; id `Identifier.fromNamespaceAndPath("permahearts","bonus_hearts")`; `double extra = HeartsAttachment.get(p).bonusHearts() * 2.0`; if extra == 0 `a.removeModifier(id)` else `a.addOrReplacePermanentModifier(new AttributeModifier(id, extra, AttributeModifier.Operation.ADD_VALUE))`; then `refill ? p.setHealth(p.getMaxHealth()) : p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()))` |
| `Messages.java` (new) | One static method per message returning `Component.translatableWithFallback(key, englishFallback, args…)`. Keys/English fallbacks: `permahearts.msg.lost` "You lost a heart. Max hearts: %s", `.floor` "You are at the minimum of 1 heart", `.gained` "Hostile mobs defeated: +1 heart! Max hearts: %s", `.cap` "You already have the maximum of 30 hearts", `.progress` "Hostile %s/%s", `.status` "Hearts %s/30 · Hostile %s/100 · Kills %s · Deaths %s", `.set` "%s now has %s hearts", `.progress_set` "%s progress set to %s" |
| `HeartsCommand.java` (new) | `register(dispatcher)`: `/hearts` (no permission) → `sendSuccess` status of the executing player (`ctx.getSource().getPlayerOrException()`); `/hearts set <targets: EntityArgument.players()> <n: IntegerArgumentType.integer(1,30)>` and `/hearts progress <targets> <n: integer(0,99)>`, both `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`; each updates data with `HeartRules.withHearts/withProgress`, calls `HealthApplier.apply(p, false)`, replies with `.set`/`.progress_set` |
| `PermaHearts.java` (edit) | In `onInitialize`: touch `HeartsAttachment`; `CommandRegistrationCallback.EVENT.register((d, c, s) -> HeartsCommand.register(d))`; `ServerPlayerEvents.JOIN.register(p -> HealthApplier.apply(p, false))`; `ServerPlayerEvents.AFTER_RESPAWN.register((old, p, alive) -> …)`; `ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> …)` (details below). Keep the existing log line |
| `src/main/resources/assets/permahearts/lang/en_us.json`, `vi_vn.json` (new) | Same keys as Messages; Vietnamese text for vi_vn (e.g. lost: "Bạn mất 1 tim. Tim tối đa: %s") |

## Event logic

- `AFTER_RESPAWN (old, newPlayer, alive)`: if `!alive` (real death): `r = HeartRules.onDeath(HeartsAttachment.get(newPlayer))` (data was already carried by `copyOnDeath`); `HeartsAttachment.set(newPlayer, r.data())`; `HealthApplier.apply(newPlayer, true)`; send `lost(maxHearts)` or `floor` via `sendSystemMessage`. If `alive` (End return): `HealthApplier.apply(newPlayer, false)` only.
- `AFTER_DEATH (entity, source)`: ignore unless `entity instanceof Enemy`; `LivingEntity credit = entity.getKillCredit()`; ignore unless `credit instanceof ServerPlayer sp`. `r = HeartRules.onHostileKill(get(sp))`, set data. `PROGRESS` → `sp.sendOverlayMessage(progress(killProgress, 100))`. `GAINED` → `HealthApplier.apply(sp,false)`, `sp.setHealth(Math.min(sp.getMaxHealth(), sp.getHealth()+2f))`, `sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f)`, chat `gained`. `AT_CAP` → chat `cap`. Players are never `Enemy`, so PvP is ignored.
- Do not mix up: the AFTER_RESPAWN data must not be reset by the death; `totalDeaths` is incremented only in `onDeath`.

## Tests

| Test | File | Checks |
|---|---|---|
| `HeartRulesTest` (JUnit) | `src/test/java/.../HeartRulesTest.java` | default max 10; one death → 9; at 1 heart a death → still 1, `AT_FLOOR`, `totalDeaths` still +1; 99 kills → `PROGRESS`, 100th → `GAINED`, hearts +1, progress 0; at 30 hearts the 100th kill → `AT_CAP`, still 30, progress 0; progress survives a death; `withHearts(d, 0)` → 1, `(d, 99)` → 30; `withProgress(d, 150)` → 99 and `(d,-5)` → 0 |
| `HeartsCodecTest` (JUnit) | same dir | `HeartsAttachment.CODEC` round-trips a non-default value through `JsonOps.INSTANCE`; decoding `{}` gives `DEFAULT`. (If loading `HeartsAttachment` in plain JUnit fails because of the registry, move `CODEC` into `HeartsData` and have the attachment reference it) |
| `PermaHeartsGameTest` (game test, replaces `infrastructureRuns`) | `src/gametest/java/.../PermaHeartsGameTest.java` | `ServerPlayer p = context.makeMockServerPlayerInLevel()`. (1) set data bonus −3, `HealthApplier.apply(p,true)` → `p.getMaxHealth()==14f`. (2) spawn `EntityType.ZOMBIE` with `context.spawn(...)`, kill it with `zombie.hurtServer(level, level.damageSources().playerAttack(p), 1000f)` → `killProgress==1`. (3) a cow killed the same way → progress unchanged. (4) progress 99 + zombie → `maxHearts` +1 and `getMaxHealth()` +2. Use `context.assertTrue(cond, "msg")` then `context.succeed()`; use `maxTicks` ≥ 40 |

If `getKillCredit()` is null for the mock player (no `lastHurtByPlayer`), set the credit through the attack (use `playerAttack`) and check the real behavior; if still unworkable after 2 tries, keep the JUnit tests, leave the kill game test out, and report it (do not hack production code for the test).

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build          # JUnit + game tests must pass
./gradlew :permahearts:runClient      # then docs/workflow/playtest.md steps 1-6
```

Do not run `scripts/install.sh` (that is Huy's step).

## Docs

- `docs/mods/permahearts/spec.md`: update only if behavior had to differ (say why).
- `docs/mods/README.md` row: version/status.
- New API facts → `docs/system/02-findings.md`.

## Commit

`PermaHearts: lose a heart on death, gain one per 100 hostile kills (P-001)`; push the branch, never `main`.

## Done when

- [ ] All files above exist, `./gradlew :permahearts:build` green
- [ ] Playtest steps 1-6 done (or say which could not be done)
- [ ] Report sent in the team.md format

## Risks / ask first

- `AFTER_DEATH` firing for the player's own death is irrelevant (players are not `Enemy`).
- If `copyOnDeath` does not carry the attachment (data back to default after dying), stop and report to the manager: the fallback is `ServerPlayerEvents.COPY_FROM`.
- Keep rules in `HeartRules` pure; do not add Minecraft imports there.
