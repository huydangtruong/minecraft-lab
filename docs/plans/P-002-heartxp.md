# P-002 — HeartXP bar, cap 50

> **Tóm tắt cho Huy:** bỏ luật "100 kill = 1 tim", thay bằng HeartXP: giết mob hostile được XP bằng máu tối đa của mob (Zombie +20, Warden +500), đủ 500 = +1 tim, dư thì giữ. Trần lên 50 tim. Thêm thanh HeartXP ở góc dưới bên trái màn hình (cần cài mod ở cả host và bạn). Bản 0.2.0.

- **Status:** ready
- **Mod / decision:** `permahearts`, D7, D8, D9 · **Branch:** `feat/P-002-heartxp` · **Depends on:** P-001 (merged, 0.1.1)

## Goal

Huy (2026-11-12): "ở góc trái, bên dưới có 1 thanh HeartXP, mỗi lần đầy thanh này mới cộng 1 tim … lượng xp này đem lại dựa theo số heart của mob" and "XP = máu mob. Mỗi thanh để đầy sẽ cần là 500." Exact rules: [docs/mods/permahearts/spec.md](../mods/permahearts/spec.md) — read it first. Death rules (−1 heart, floor 1, respawn ordering fix from 0.1.1) do not change.

## Scope

- In: everything in `mods/permahearts/` (+ `fabric.mod.json` client entrypoint), `mod_version=0.2.0`, docs listed below.
- Out: root Gradle files, pinned versions, data migration (no real save has PermaHearts data yet: old `kill_progress` is simply dropped), any HUD config/settings screen.

## Files (main package `io.github.huydangtruong.permahearts`, paths under `mods/permahearts/`)

| File | Change |
|---|---|
| `src/main/java/.../HeartsData.java` | Rename component `killProgress` → `heartXp` (0..499). `CODEC` field `"kill_progress"` → `"heart_xp"`. Stays free of `net.minecraft` imports |
| `src/main/java/.../HeartRules.java` | `MAX = 50`; replace `KILLS_PER_HEART` with `XP_PER_HEART = 500`. `enum KillOutcome { PROGRESS, GAINED, AT_CAP }` stays; `KillResult(HeartsData data, KillOutcome outcome, int xpGained, int heartsGained)`. New `static int xpFor(float mobMaxHealth)` = `Math.max(1, Math.round(mobMaxHealth))`. `onHostileKill(HeartsData d, int xp)`: `kills+1`; if already at `MAX` → `heartXp=0`, `AT_CAP`, gained 0. Else `total = heartXp + xp`; while `total >= 500 && hearts < MAX` { hearts++, total -= 500 }; if hearts reached `MAX` → `total = 0`. Outcome `GAINED` if any heart gained, else `PROGRESS` (a kill that lands exactly on the cap is `GAINED`). `withHearts` clamps 1..50; when the result is `MAX` also set `heartXp=0`. `withProgress` → rename `withXp(d, n)` clamps 0..499 (and 0 when at `MAX`). New `static float xpFraction(HeartsData d)` = `heartXp / 500f` (1f at `MAX`) for the HUD |
| `src/main/java/.../HeartsAttachment.java` | Add `.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly())` to the builder. `public static final StreamCodec<ByteBuf, HeartsData> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, HeartsData::bonusHearts, ByteBufCodecs.VAR_INT, HeartsData::heartXp, ByteBufCodecs.VAR_INT, HeartsData::totalHostileKills, ByteBufCodecs.VAR_INT, HeartsData::totalDeaths, HeartsData::new)` (4-arg `composite` overload; if the generic bound complains, type it `StreamCodec<RegistryFriendlyByteBuf, HeartsData>`). `syncWith` takes `StreamCodec<? super RegistryFriendlyByteBuf, A>`. Sync happens automatically on every `setAttached` |
| `src/main/java/.../PermaHearts.java` | `AFTER_DEATH`: `int xp = HeartRules.xpFor(entity.getMaxHealth())`, call `onHostileKill(sp, xp)`. In `onHostileKill`: `PROGRESS` → `sp.sendOverlayMessage(Messages.xpGained(r.xpGained()))`; `GAINED` → apply, heal `2f * heartsGained` (capped at max), sound, chat `Messages.gained(heartsGained, maxHearts)`; `AT_CAP` → chat `cap()` only if the player was already at cap before this kill (avoid double message); keep the respawn phase code as is |
| `src/main/java/.../Messages.java` | `gained(int hearts, int max)` "+%s heart(s)! Max hearts: %s"; `cap()` "You already have the maximum of 50 hearts"; replace `progress` with `xpGained(int xp)` "+%s HeartXP"; `status(hearts, xp, kills, deaths)` "Hearts %s/50 · HeartXP %s/500 · Kills %s · Deaths %s"; `progressSet` → `xpSet(name, xp)` "%s HeartXP set to %s". Add `hudLabel(int xp)` "HeartXP %s/500" and `hudMax()` "HeartXP MAX" (keys `permahearts.hud.label`, `permahearts.hud.max`) |
| `src/main/java/.../HeartsCommand.java` | `set` range `integer(1, 50)`; literal `progress` → `xp` with `integer(0, 499)` using `HeartRules.withXp` and `Messages.xpSet` |
| `src/main/resources/assets/permahearts/lang/en_us.json`, `vi_vn.json` | Match every key above; remove `permahearts.msg.progress`, `.progress_set`. VN: "+%s HeartXP", "Bạn được +%s tim! Tim tối đa: %s", "Bạn đã đạt tối đa 50 tim", "Tim %s/50 · HeartXP %s/500 · Số kill %s · Số lần chết %s", "HeartXP %s/500", "HeartXP TỐI ĐA" |
| `src/main/resources/fabric.mod.json` | Add `"client": ["io.github.huydangtruong.permahearts.client.PermaHeartsClient"]` to `entrypoints` |
| `src/client/java/io/github/huydangtruong/permahearts/client/PermaHeartsClient.java` (new) | `implements ClientModInitializer`; `HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("permahearts","heart_xp"), new HeartXpHud())`. Delete `src/client/java/.gitkeep` |
| `src/client/java/.../client/HeartXpHud.java` (new) | `implements HudElement`; `extractRenderState(GuiGraphicsExtractor g, DeltaTracker t)`: `Minecraft mc = Minecraft.getInstance(); LocalPlayer p = mc.player`; return if `p == null`, `mc.options.hideGui`, `p.isCreative()` or `p.isSpectator()` (verify these names with javap on `LocalPlayer`/`Options`). `HeartsData d = p.getAttachedOrElse(HeartsAttachment.HEARTS, HeartsData.DEFAULT)`. Layout (GUI-scaled px): `x = 4`, bar `w = 102, h = 5`, `barY = g.guiHeight() - 4 - h`, label at `y = barY - 10`. Draw: `g.text(mc.font, label, x, labelY, 0xFFFF5555, true)` where label = `Messages.hudLabel(d.heartXp())` or `hudMax()` at cap; `g.fill(x, barY, x+w, barY+h, 0xA0000000)` background; `g.fill(x+1, barY+1, x+1+(int)((w-2)*HeartRules.xpFraction(d)), barY+h-1, 0xFFE03C3C)` fill; `g.outline(x, barY, w, h, 0xFF3A0A0A)` (check whether `outline` takes width/height or x2/y2 with javap) |

Do not touch: root `build.gradle`, `settings.gradle`, `gradle.properties`, `scripts/`.

## Steps

1. Rules + data (`HeartsData`, `HeartRules`) and their JUnit tests; `./gradlew :permahearts:test` green.
2. Server glue (`HeartsAttachment` sync, `PermaHearts`, `Messages`, `HeartsCommand`, lang); game tests green.
3. Client HUD (`fabric.mod.json`, `PermaHeartsClient`, `HeartXpHud`); `./gradlew :permahearts:build` green (compiles the client source set).
4. `mod_version=0.2.0` in `mods/permahearts/gradle.properties`; docs.

## Tests

| Test | Checks |
|---|---|
| `HeartRulesTest` (rewrite the kill tests) | `xpFor(20f)==20`, `xpFor(0.5f)==1`, `xpFor(499.6f)==500`; 480 XP + zombie (20) → `GAINED`, +1 heart, `heartXp==0`; 300 + Wither (300) → +1 heart, `heartXp==100`; 0 + 1200 → +2 hearts, `heartXp==200`; 49 hearts + 1200 → 50 hearts, `heartXp==0`, `GAINED` with `heartsGained==1`; at 50 hearts + 20 → `AT_CAP`, 50, `heartXp==0`; XP survives `onDeath`; `withHearts(d, 99)` → 50; `withXp(d, 600)` → 499, `(d,-5)` → 0; `xpFraction` 0.5 at 250 XP and 1 at cap |
| `HeartsCodecTest` | round-trip with field `heart_xp` |
| `PermaHeartsGameTest` (update) | zombie kill → `heartXp == 20` (use `(int) zombie.getMaxHealth()` in the assert, difficulty may change it); cow → unchanged; `new HeartsData(0, 480, …)` + zombie → 11 hearts, max health 22; spawn `EntityTypes.WARDEN` and kill → +1 heart from 0 XP (if the warden cannot be spawned/killed in the test structure, use Wither: 300 XP; say which in the report); cap test becomes 50 hearts / 100 health; admin test: `hearts set @s 45` → 90 health, `hearts xp @s 250` → 250, `hearts set @s 51` rejected. Keep the death/floor/join tests unchanged |

The HUD cannot be checked by server game tests. Do not run `runClient`. Say in the report that the HUD was only compiled.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build      # JUnit + game tests + client compile
```

## Docs

- `docs/mods/README.md`: version 0.2.0, status.
- `docs/workflow/playtest.md`: replace the PermaHearts steps 4–6 with HeartXP steps (`/hearts xp @s 480` + kill a zombie → +1 heart, bar back to 0; `/summon warden` kill → +1 heart; `/hearts set @s 50` + kill → "MAX"; look at the bar bottom-left).
- New API facts (HUD, `GuiGraphicsExtractor`, `StreamCodec`, attachment sync) → `docs/system/02-findings.md`.

## Commit

`PermaHearts 0.2.0: HeartXP from mob max health, HUD bar, cap 50 (P-002)`. Push the branch, never `main`.

## Done when

- [ ] Steps 1–4, `./gradlew :permahearts:build` green
- [ ] Report in the team.md format (say: HUD compiled, not seen in game)

## Risks / ask first

- If `syncWith` makes the game tests fail (mock player connection), report it with the exact error; do not drop sync (the HUD needs it).
- Ender Dragon kill credit comes through its parts; it is not game-tested. Note it for Huy's playtest.
